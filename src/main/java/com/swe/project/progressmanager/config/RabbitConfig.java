package com.swe.project.progressmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;

@Configuration
public class RabbitConfig {

    public static final String TOPIC_COMPLETED_EXCHANGE = "topic.completed.exchange";
    public static final String TOPIC_COMPLETED_ROUTING_KEY = "completed.#";
    public static final String TOPIC_COMPLETED_QUEUE = "topic.completed.queue";

    public static final String PROGRESS_UPDATED_EXCHANGE = "progress.updated.exchange";
    public static final String PROGRESS_UPDATED_ROUTING_KEY = "progress.updated";
    public static final String PROGRESS_UPDATED_QUEUE = "progress.updated.queue";

    @Bean
    public Queue topicCompletedQueue() {
        return new Queue(TOPIC_COMPLETED_QUEUE, true);
    }

    @Bean
    public TopicExchange topicCompletedExchange() {
        return new TopicExchange(TOPIC_COMPLETED_EXCHANGE);
    }
    
    @Bean
    public Queue progressUpdatedQueue() {
        return new Queue(PROGRESS_UPDATED_QUEUE, true);
    }

    @Bean
    public TopicExchange progressUpdatedExchange() {
        return new TopicExchange(PROGRESS_UPDATED_EXCHANGE);
    }

    @Bean
    public Binding progressUpdatedBinding(Queue progressUpdatedQueue,
                                         TopicExchange progressUpdatedExchange) {
        return BindingBuilder
                .bind(progressUpdatedQueue)
                .to(progressUpdatedExchange)
                .with(PROGRESS_UPDATED_ROUTING_KEY);
    }
}