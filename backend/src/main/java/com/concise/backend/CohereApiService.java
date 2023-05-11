package com.concise.backend;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CohereApiService {

    private static final String API_BASE_URL = "https://api.cohere.ai";

    @Value("${cohere.api.key}")
    private String API_KEY;

    private static String apiKeyStatic;

    @PostConstruct
    public void init() {
        apiKeyStatic = API_KEY;
    }

    private RestTemplate restTemplate;

    public CohereApiService() {
        this.restTemplate = new RestTemplate();
    }

    public String summarize(String text, String length, String format, String model,
                            String additionalCommand, double temperature) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKeyStatic);

        String requestBody = """
                {
                    "text": "%s",
                    "length": "%s",
                    "format": "%s",
                    "model": "%s",
                    "additional_command": "%s",
                    "temperature": %f
                }
                """.formatted(text, length, format, model, additionalCommand, temperature);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(API_BASE_URL + "/v1/summarize", HttpMethod.POST, entity, String.class);

        return response.getBody();
    }
}
