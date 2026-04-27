package com.swe.project.progressmanager.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import com.swe.project.progressmanager.config.RabbitConfig;
import com.swe.project.progressmanager.dto.ProgressUpdatedEvent;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ProgressUpdatedListener {
    ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = RabbitConfig.PROGRESS_UPDATED_QUEUE)
    public void handleProgressUpdated(String message) {
        try {
            ProgressUpdatedEvent event = objectMapper.readValue(message, ProgressUpdatedEvent.class);
            System.out.println("Received progress update: " + event);
        } catch (Exception e) {
            System.err.println("Failed to process progress update message: " + e.getMessage());
        }
    }
}
