package com.swe.project.progressmanager.controller;

import com.swe.project.progressmanager.dto.ClickRequest;
import com.swe.project.progressmanager.dto.CompletionRequest;
import com.swe.project.progressmanager.dto.CompletionResponse;
import com.swe.project.progressmanager.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/progress")
public class ProgressController {

    private final ProgressService service;

    public ProgressController(ProgressService service) {
        this.service = service;
    }
    
    @PostMapping("/click")
    public ResponseEntity<Void> recordClick(@RequestBody ClickRequest request) {
        service.recordClick(request.getLearnerId(), request.getTopicId(), request.getLabel());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{learnerId}/topic/{topicId}/complete")
    public ResponseEntity<?> checkCompletion(@PathVariable String learnerId, @PathVariable String topicId) {
        CompletionRequest completionRequest = service.buildCompletionRequest(learnerId, topicId);
        CompletionResponse result = service.checkCompletion(completionRequest);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{learnerId}")
    public ResponseEntity<?> getProgress(@PathVariable String learnerId) {
        return ResponseEntity.ok(service.getProgress(learnerId));
    }

    /* Will need these. No business logic here:
        CRUD on learner progress table in PostgreSQL. Business verbs:
    POST /progress/click (record a click), GET /progress/{learnerId} (return all click
    sets), PATCH /progress/{learnerId}/topic/{topicId}/complete.
    */

}