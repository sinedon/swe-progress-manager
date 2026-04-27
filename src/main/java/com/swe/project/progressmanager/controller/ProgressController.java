package com.swe.project.progressmanager.controller;

import com.swe.project.progressmanager.dto.ClickRequest;
import com.swe.project.progressmanager.dto.ProgressUpdatedEvent;
import com.swe.project.progressmanager.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @PostMapping("/click")
    public ResponseEntity<Void> recordClick(@RequestBody ClickRequest request) {
        progressService.recordClick(
                request.getLearnerId(),
                request.getTopicId(),
                request.getLabel()
        );

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{learnerId}")
    public ResponseEntity<?> getProgress(@PathVariable String learnerId) {
        return progressService.getProgress(learnerId);
    }

    @GetMapping("/{learnerId}/topic/{topicId}")
    public ResponseEntity<?> getProgressForTopic(@PathVariable String learnerId,
                                                 @PathVariable String topicId) {
        return progressService.getProgressForTopic(learnerId, topicId);
    }

    @DeleteMapping("/{learnerId}/topic/{topicId}/latest")
    public ResponseEntity<?> undoLatestClick(@PathVariable String learnerId,
                                             @PathVariable String topicId) {
        return progressService.undoLatestClick(learnerId, topicId);
    }

    @GetMapping("/{learnerId}/event")
    public ResponseEntity<ProgressUpdatedEvent> getLatestProgressUpdatedEvent(@PathVariable String learnerId) {
        return ResponseEntity.ok(progressService.getLatestProgressUpdatedEvent(learnerId));
    }

    @GetMapping("/events/latest")
    public ResponseEntity<ProgressUpdatedEvent> getLatestProgressUpdatedEvent() {
        return ResponseEntity.ok(progressService.getLatestProgressUpdatedEvent());
    }
}