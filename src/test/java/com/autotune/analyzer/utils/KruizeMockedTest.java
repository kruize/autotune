package com.autotune.analyzer.utils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

class KruizeMockedTest {

    private static final String BASE_URL = "http://mock-cluster:8080";

    private HttpClient mockHttpClient;
    private HttpResponse<String> mockHttpResponse;
    private KruizeUtils kruizeUtils;
    private Logger logger;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        mockHttpResponse = mock(HttpResponse.class);
        logger = mock(Logger.class);

        kruizeUtils = new KruizeUtils(BASE_URL, mockHttpClient);
    }

    // Helper Methods
    static Map<String, Object> basePayload() {
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> filter = new HashMap<>();
        filter.put("include", new HashMap<>());
        filter.put("exclude", new HashMap<>());
        payload.put("filter", filter);
        payload.put("metadata_profile", "cluster-metadata-local-monitoring");
        payload.put("measurement_duration", "15mins");
        payload.put("time_range", new HashMap<>());
        return payload;
    }

    static Map<String, Object> filteredPayload() {
        Map<String, Object> payload = basePayload();
        Map<String, Object> filter = (Map<String, Object>) payload.get("filter");
        Map<String, Object> includeMap = (Map<String, Object>) filter.get("include");
        includeMap.put("namespace", new String[]{"default"});
        includeMap.put("workload", new String[]{"sysbench"});
        includeMap.put("containers", new String[]{"sysbench"});
        return payload;
    }

    // Fully Mocked POST /bulk 
    static Stream<Arguments> postBulkPayloadProvider() {
        return Stream.of(
                arguments(new HashMap<>(), true),
                arguments(basePayload(), true),
                arguments(filteredPayload(), true)
        );
    }

    @ParameterizedTest
    @MethodSource("postBulkPayloadProvider")
    void testPostBulkApiMocked(Map<String, Object> payload, Boolean expectedJobIdPresent) throws Exception {
        // Mock HTTP response
        when(mockHttpResponse.body()).thenReturn("{\"job_id\":\"mock-job-123\"}");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockHttpResponse);

        // Call API
        Map<String, Object> response = kruizeUtils.postBulkApi(payload, logger);

        boolean jobIdPresent = response.containsKey("job_id") && response.get("job_id") instanceof String;
        assertEquals(expectedJobIdPresent, jobIdPresent);

        // Verify logging
        verify(logger).info("Sending POST request to URL: {}/bulk", BASE_URL);
        verify(logger).info("Request Payload: {}", payload);
    }

    // ---------------- Partially Mocked GET /bulk ----------------
    static Stream<Arguments> partialMockProvider() {
        Map<String, Object> expected1 = new HashMap<>();
        expected1.put("namespace", new String[]{"default"});
        expected1.put("workload", new String[]{"wl1"});
        expected1.put("containers", new String[]{"ctr1"});

        Map<String, Object> filterSetup1 = new HashMap<>();
        Map<String, Object> includeMap1 = new HashMap<>();
        includeMap1.put("namespace", new String[]{"default"});
        includeMap1.put("workload", new String[]{"wl1"});
        includeMap1.put("containers", new String[]{"ctr1"});
        filterSetup1.put("include", includeMap1);

        return Stream.of(
                arguments(filterSetup1, expected1)
        );
    }

    @ParameterizedTest
    @MethodSource("partialMockProvider")
    void testBulkApiPartialMocked(Map<String, Object> filterSetup, Map<String, Object> expected) throws Exception {
        // Mock POST response
        when(mockHttpResponse.body()).thenReturn("{\"job_id\":\"mock-job-123\"}");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockHttpResponse);

        // Build payload
        Map<String, Object> payload = basePayload();
        Map<String, Object> filter = (Map<String, Object>) payload.get("filter");
        filter.putAll(filterSetup);

        // Call POST API
        Map<String, Object> response = kruizeUtils.postBulkApi(payload, logger);
        assertEquals("mock-job-123", response.get("job_id"));
    }

    // ---------------- POST with varied responses ----------------
    static Stream<Arguments> variedPayloadProvider() {
        return Stream.of(
                arguments(new HashMap<>(), Map.of("job_id", "mock-job-123")),
                arguments(basePayload(), Map.of("job_id", "mock-job-456")),
                arguments(filteredPayload(), Map.of("job_id", "mock-job-789")),
                arguments(basePayload(), new HashMap<>()) // No job_id
        );
    }

    @ParameterizedTest
    @MethodSource("variedPayloadProvider")
    void testBulkPostRequestVaried(Map<String, Object> payload, Map<String, Object> mockReturnJson) throws Exception {
        // Mock response
        String body = OBJECT_MAPPER.writeValueAsString(mockReturnJson);
        when(mockHttpResponse.body()).thenReturn(body);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockHttpResponse);

        Map<String, Object> response = kruizeUtils.postBulkApi(payload, logger);
        boolean jobIdPresent = response.containsKey("job_id") && response.get("job_id") instanceof String;
        boolean expectedJobIdPresent = mockReturnJson.containsKey("job_id");
        assertEquals(expectedJobIdPresent, jobIdPresent);
    }

    // ---------------- POST throws IOException ----------------
    @Test
    void testBulkPostRequestThrowsIOException() throws Exception {
        when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new IOException("Connection failed"));

        IOException exception = assertThrows(IOException.class, () -> kruizeUtils.postBulkApi(basePayload(), logger));
        assertEquals("Connection failed", exception.getMessage());
    }
}
