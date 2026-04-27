package com.swe.project.progressmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swe.project.progressmanager.client.CompletionEngineClient;
import com.swe.project.progressmanager.client.ContentManagerClient;
import com.swe.project.progressmanager.client.ProgressAccessClient;
import com.swe.project.progressmanager.dto.CompletionRequest;
import com.swe.project.progressmanager.dto.CompletionResponse;
import com.swe.project.progressmanager.dto.ProgressUpdatedEvent;
import com.swe.project.progressmanager.dto.TopicCompletedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ProgressService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.progress.completed.queue:topic.completed.queue}")
    private String queue;

    private final ContentManagerClient contentClient;
    private final ProgressAccessClient progressClient;
    private final CompletionEngineClient completionClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<ProgressUpdatedEvent> latestProgressUpdatedEvent = new AtomicReference<>();
    private final Map<String, Set<String>> completedTopicIdsByLearner = new ConcurrentHashMap<>();
    private final Map<String, Integer> totalHotspotClicksByLearner = new ConcurrentHashMap<>();
    private final Map<String, String> lastCompletedTopicIdByLearner = new ConcurrentHashMap<>();

    public ProgressService(ContentManagerClient contentClient,
                           ProgressAccessClient progressClient,
                           CompletionEngineClient completionClient) {
        this.contentClient = contentClient;
        this.progressClient = progressClient;
        this.completionClient = completionClient;
    }

    public Set<String> getTopicLabels(String topicId) {
        ResponseEntity<String> response = contentClient.getTopic(topicId);
        Set<String> labels = ConcurrentHashMap.newKeySet();

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode hotspots = root.path("hotspots");

            if (hotspots.isMissingNode() || !hotspots.isArray()) {
                return labels;
            }

            for (JsonNode hotspot : hotspots) {
                String label = hotspot.path("label").asText(null);

                if (label != null && !label.isBlank()) {
                    labels.add(label);
                }
            }

            return labels;
        } catch (Exception e) {
            e.printStackTrace();
            return Set.of();
        }
    }

    public Set<String> getClickedLabels(String learnerId, String topicId) {
        ResponseEntity<String> response = progressClient.getProgress(learnerId);
        Set<String> clickedLabels = ConcurrentHashMap.newKeySet();

        try {
            JsonNode root = objectMapper.readTree(response.getBody());

            if (!root.isArray()) {
                return clickedLabels;
            }

            for (JsonNode topicProgress : root) {
                String progressTopicId = topicProgress.path("topicId").asText();
                String progressLearnerId = topicProgress.path("learnerId").asText();

                if (topicId.equals(progressTopicId) && learnerId.equals(progressLearnerId)) {
                    String label = topicProgress.path("label").asText(null);

                    if (label != null && !label.isBlank()) {
                        clickedLabels.add(label);
                    }
                }
            }

            return clickedLabels;
        } catch (Exception e) {
            e.printStackTrace();
            return Set.of();
        }
    }

    public CompletionRequest buildCompletionRequest(String learnerId, String topicId) {
        Set<String> allLabels = getTopicLabels(topicId);
        Set<String> clickedLabels = getClickedLabels(learnerId, topicId);

        return new CompletionRequest(allLabels, clickedLabels);
    }

    public CompletionResponse checkCompletion(CompletionRequest request) {
        return completionClient.checkCompletion(request.getAllLabels(), request.getClickedLabels());
    }

    public void recordClick(String learnerId, String topicId, String label) {
        progressClient.recordClick(learnerId, topicId, label);

        totalHotspotClicksByLearner.merge(learnerId, 1, Integer::sum);

        CompletionRequest request = buildCompletionRequest(learnerId, topicId);
        CompletionResponse result = checkCompletion(request);
        boolean isComplete = "COMPLETE".equals(result.getStatus());

        if (isComplete) {
            Set<String> completedTopics = completedTopicIdsByLearner.computeIfAbsent(
                    learnerId,
                    key -> ConcurrentHashMap.newKeySet()
            );

            boolean firstTimeCompleted = completedTopics.add(topicId);
            lastCompletedTopicIdByLearner.put(learnerId, topicId);

            if (firstTimeCompleted) {
                TopicCompletedEvent event = new TopicCompletedEvent(
                        learnerId,
                        topicId,
                        request.getClickedLabels().size(),
                        request.getAllLabels().size(),
                        LocalDateTime.now()
                );

                rabbitTemplate.convertAndSend(queue, event);
            }
        }

        updateLatestProgressUpdatedEvent(learnerId);
    }

    public ResponseEntity<?> getProgress(String learnerId) {
        return progressClient.getProgress(learnerId);
    }

    public ResponseEntity<?> getProgressForTopic(String learnerId, String topicId) {
        return progressClient.getProgressForTopic(learnerId, topicId);
    }

    public ResponseEntity<?> undoLatestClick(String learnerId, String topicId) {
        ResponseEntity<?> response = progressClient.undoLatestClick(learnerId, topicId);

        if (response.getStatusCode().is2xxSuccessful()) {
            totalHotspotClicksByLearner.compute(learnerId, (key, value) -> {
                if (value == null || value <= 0) {
                    return 0;
                }

                return value - 1;
            });

            try {
                CompletionRequest request = buildCompletionRequest(learnerId, topicId);
                CompletionResponse result = checkCompletion(request);

                if (!"COMPLETE".equals(result.getStatus())) {
                    Set<String> completedTopics = completedTopicIdsByLearner.get(learnerId);

                    if (completedTopics != null) {
                        completedTopics.remove(topicId);
                    }
                }
            } catch (Exception ignored) {
                // The persisted undo still succeeded. Keep the UI event best-effort.
            }

            updateLatestProgressUpdatedEvent(learnerId);
        }

        return response;
    }

    public ProgressUpdatedEvent getLatestProgressUpdatedEvent(String learnerId) {
        ProgressUpdatedEvent latest = latestProgressUpdatedEvent.get();

        if (latest != null && learnerId.equals(latest.getLearnerId())) {
            return latest;
        }

        return buildProgressUpdatedEvent(learnerId);
    }

    public ProgressUpdatedEvent getLatestProgressUpdatedEvent() {
        ProgressUpdatedEvent latest = latestProgressUpdatedEvent.get();

        if (latest != null) {
            return latest;
        }

        return new ProgressUpdatedEvent(null, 0, 0, null, LocalDateTime.now());
    }

    private void updateLatestProgressUpdatedEvent(String learnerId) {
        latestProgressUpdatedEvent.set(buildProgressUpdatedEvent(learnerId));
    }

    private ProgressUpdatedEvent buildProgressUpdatedEvent(String learnerId) {
        Set<String> completedTopics = completedTopicIdsByLearner.getOrDefault(learnerId, Set.of());
        int totalHotspotClicks = totalHotspotClicksByLearner.getOrDefault(learnerId, 0);
        String lastCompletedTopicId = lastCompletedTopicIdByLearner.get(learnerId);

        return new ProgressUpdatedEvent(
                learnerId,
                completedTopics.size(),
                totalHotspotClicks,
                lastCompletedTopicId,
                LocalDateTime.now()
        );
    }
}