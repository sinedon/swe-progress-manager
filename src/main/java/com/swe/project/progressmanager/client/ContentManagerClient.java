package com.swe.project.progressmanager.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ContentManagerClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${content.manager.url:http://content-manager:8080}")
    private String contentManagerUrl;

    public ResponseEntity<String> getTopic(String topicId) {
        String url = contentManagerUrl + "/topics/" + topicId;

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                String.class
        );

        return response;
    }
}