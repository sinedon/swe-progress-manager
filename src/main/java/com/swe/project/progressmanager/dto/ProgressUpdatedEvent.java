package com.swe.project.progressmanager.dto;

import java.time.LocalDateTime;

public class ProgressUpdatedEvent {

    private String learnerId;
    private int completedTopicCount;
    private int totalHotspotClicks;
    private String lastCompletedTopicId;
    private LocalDateTime updatedAt;

    public ProgressUpdatedEvent() {
    }

    public ProgressUpdatedEvent(String learnerId,
                                int completedTopicCount,
                                int totalHotspotClicks,
                                String lastCompletedTopicId,
                                LocalDateTime updatedAt) {
        this.learnerId = learnerId;
        this.completedTopicCount = completedTopicCount;
        this.totalHotspotClicks = totalHotspotClicks;
        this.lastCompletedTopicId = lastCompletedTopicId;
        this.updatedAt = updatedAt;
    }

    public String getLearnerId() {
        return learnerId;
    }

    public void setLearnerId(String learnerId) {
        this.learnerId = learnerId;
    }

    public int getCompletedTopicCount() {
        return completedTopicCount;
    }

    public void setCompletedTopicCount(int completedTopicCount) {
        this.completedTopicCount = completedTopicCount;
    }

    public int getTotalHotspotClicks() {
        return totalHotspotClicks;
    }

    public void setTotalHotspotClicks(int totalHotspotClicks) {
        this.totalHotspotClicks = totalHotspotClicks;
    }

    public String getLastCompletedTopicId() {
        return lastCompletedTopicId;
    }

    public void setLastCompletedTopicId(String lastCompletedTopicId) {
        this.lastCompletedTopicId = lastCompletedTopicId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}