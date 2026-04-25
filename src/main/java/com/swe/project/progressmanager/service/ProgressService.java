package com.swe.project.progressmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swe.project.progressmanager.client.CompletionEngineClient;
import com.swe.project.progressmanager.client.ContentManagerClient;
import com.swe.project.progressmanager.client.ProgressAccessClient;
import com.swe.project.progressmanager.dto.CompletionRequest;
import com.swe.project.progressmanager.dto.CompletionResponse;

import java.util.Set;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ProgressService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final ContentManagerClient contentClient;
    private final ProgressAccessClient progressClient;
    private final CompletionEngineClient completionClient;

    // For parsing JSON responses from other services
    private final ObjectMapper objectMapper = new ObjectMapper();

    // constructor
    public ProgressService(ContentManagerClient contentClient,
                       ProgressAccessClient progressClient,
                       CompletionEngineClient completionClient) {
        this.contentClient = contentClient;
        this.progressClient = progressClient;
        this.completionClient = completionClient;
    }

    // Helper methods to interact with other services
    public Set<String> getTopicLabels(String topicId) {
        ResponseEntity<String> response = contentClient.getTopic(topicId);
        // Parse the response and extract labels
        Set<String> labels = Set.of();
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode hotspots = root.path("hotspots");
            if (hotspots.isMissingNode() || !hotspots.isArray()) {
                return Set.of(); // No labels found
            } else {
                for (JsonNode hotspot : hotspots) {
                    String label = hotspot.path("label").asText(null);
                    if (label != null && !label.isEmpty()) {
                        labels = Set.copyOf(labels);
                        labels.add(label);
                    }
                }
                return labels;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Set.of(); // Return empty set on error
        }
    }

    public Set<String> getClickedLabels(String learnerId, String topicId) {
        ResponseEntity<String> response = progressClient.getProgress(learnerId);
        // Parse the response and extract clicked labels for the topic
        Set<String> clickedLabels = Set.of();
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            for (JsonNode topicProgress : root) {
                if (topicId.equals(topicProgress.path("topicId").asText()) 
                    && learnerId.equals(topicProgress.path("learnerId").asText())) {
                    clickedLabels.add(topicProgress.path("label").asText());
                }
            }
            return clickedLabels;
        } catch (Exception e) {
            e.printStackTrace();
            return Set.of(); // Return empty set on error
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
        System.out.println("Calling ProgressAccess...");
        progressClient.recordClick(learnerId, topicId, label);
        CompletionRequest request = buildCompletionRequest(learnerId, topicId);
        System.out.println("Calling CompletionEngine...");
        CompletionResponse result = checkCompletion(request);
        System.out.println("Completion result: " + result);

        if ("COMPLETE".equals(result.getStatus())) {
            System.out.println(">>> Sending TEST_EVENT...");
            rabbitTemplate.convertAndSend(
                "topic-completed-exchange",
                "completed.test",
                "TEST_EVENT"
            );
        }
    }

    public ResponseEntity<?> getProgress(String learnerId) {
        return progressClient.getProgress(learnerId);
    }
}