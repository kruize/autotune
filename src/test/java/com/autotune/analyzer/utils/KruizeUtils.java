package com.autotune.analyzer.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class KruizeUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final String baseUrl;

    public KruizeUtils(String baseUrl, HttpClient httpClient) {
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
    }

    /**
     * POST /bulk
     */
    public Map<String, Object> postBulkApi(
            Map<String, Object> payload,
            Logger logger
    ) throws IOException, InterruptedException {

        if (logger != null) {
            logger.info("Sending POST request to URL: {}/bulk", baseUrl);
            logger.info("Request Payload: {}", payload);
        }

        String requestBody = OBJECT_MAPPER.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/bulk"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (logger != null) {
            logger.info("Response Status Code: {}", response.statusCode());
            logger.info("Response JSON: {}", response.body());
        }

        return OBJECT_MAPPER.readValue(
                response.body(),
                new TypeReference<Map<String, Object>>() {}
        );
    }

    /**
     * GET /bulk?job_id=...&include=...
     */
    public Map<String, Object> getBulkJobStatus(
            String jobId,
            String include,
            Logger logger
    ) throws IOException, InterruptedException {

        StringBuilder url = new StringBuilder(baseUrl + "/bulk?job_id=" + jobId);
        if (include != null) {
            url.append("&include=").append(include);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (logger != null) {
            logger.info("Response Status Code: {}", response.statusCode());
            logger.info("Response JSON: {}", response.body());
        }

        return OBJECT_MAPPER.readValue(
                response.body(),
                new TypeReference<Map<String, Object>>() {}
        );
    }
}
