package com.swe.project.progressmanager.client;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.swe.project.progressmanager.dto.ClickRequest;

@Component
public class ProgressAccessClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${progress.access.url:http://progress-access:8080}")
    private String progressAccessUrl;

    public void recordClick(String learnerId, String topicId, String label) {

        String url = progressAccessUrl + "/progress/click";

        ClickRequest request = new ClickRequest(learnerId, topicId, label);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ClickRequest> entity = new HttpEntity<>(request, headers);

        restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Void.class
        );
    }

    public ResponseEntity<String> getProgress(String learnerId) {
        String url = progressAccessUrl + "/progress/" + learnerId;
        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            String.class
        );
        return response;
    }

}