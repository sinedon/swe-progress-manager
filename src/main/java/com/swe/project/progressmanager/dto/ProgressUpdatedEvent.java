package com.swe.project.progressmanager.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressUpdatedEvent {
    private String learnerId;
    private int completedTopicCount;
    private int totalHotspotClicks;
    private String lastCompletedTopicId;
    private LocalDateTime updatedAt;
}
