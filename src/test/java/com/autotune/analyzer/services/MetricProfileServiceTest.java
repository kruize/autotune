/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.analyzer.services;

import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.performanceProfiles.utils.PerformanceProfileUtil;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.StringReader;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the error handling of {@link MetricProfileService#doPost}.
 * Invalid input must result in HTTP 400, while unexpected failures result in HTTP 500
 * with a sanitized message (no internal error details).
 */
class MetricProfileServiceTest {

    private MetricProfileService service;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        service = new MetricProfileService();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    private void givenRequestBody(String body) throws Exception {
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
    }

    @Test
    @DisplayName("Missing mandatory field in the input JSON returns HTTP 400")
    void doPostMissingMandatoryFieldReturnsBadRequest() throws Exception {
        givenRequestBody("{}");

        service.doPost(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST),
                eq("Validation failed: JSONObject[\"apiVersion\"] not found."));
        verify(response, never()).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
    }

    @Test
    @DisplayName("Malformed JSON input returns HTTP 400")
    void doPostMalformedJsonReturnsBadRequest() throws Exception {
        givenRequestBody("{ this is not json");

        service.doPost(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), startsWith("Validation failed: "));
        verify(response, never()).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
    }

    private static final String VALID_PROFILE_PREFIX = "{\"apiVersion\":\"recommender.com/v1\",\"kind\":\"KruizePerformanceProfile\","
            + "\"metadata\":{\"name\":\"test-profile\"},\"profile_version\":1.0,\"k8s_type\":\"openshift\","
            + "\"slo\":{\"slo_class\":\"resource_usage\",\"direction\":\"minimize\","
            + "\"objective_function\":{\"function_type\":\"source\"},\"function_variables\":[";

    @Test
    @DisplayName("Missing aggregation_functions (would be a NullPointerException) returns HTTP 400")
    void doPostMissingAggregationFunctionsReturnsBadRequest() throws Exception {
        givenRequestBody(VALID_PROFILE_PREFIX
                + "{\"name\":\"cpuRequest\",\"datasource\":\"prometheus\",\"value_type\":\"double\"}]}}");

        service.doPost(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), startsWith("Validation failed: "));
        verify(response, never()).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
    }

    @Test
    @DisplayName("Fields with an unexpected shape (would be a ClassCastException) return HTTP 400")
    void doPostUnexpectedFieldShapeReturnsBadRequest() throws Exception {
        // function_variables items are strings instead of objects
        givenRequestBody(VALID_PROFILE_PREFIX + "\"not-an-object\"]}}");
        service.doPost(request, response);

        // aggregation_functions items are strings instead of objects
        givenRequestBody(VALID_PROFILE_PREFIX
                + "{\"name\":\"cpuRequest\",\"datasource\":\"prometheus\",\"value_type\":\"double\","
                + "\"aggregation_functions\":[\"avg\"]}]}}");
        service.doPost(request, response);

        verify(response, times(2)).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), startsWith("Validation failed: "));
        verify(response, never()).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
    }

    @Test
    @DisplayName("Failed validation result with HTTP 500 is sanitized before it is sent to the client")
    void doPostFailedValidationWithServerErrorIsSanitized() throws Exception {
        givenRequestBody("{}");
        String internalDetail = "Validation failed: java.lang.NullPointerException at PerformanceProfileValidation.java:602";

        try (MockedStatic<Converters.KruizeObjectConverters> converters = mockStatic(Converters.KruizeObjectConverters.class);
             MockedStatic<PerformanceProfileUtil> util = mockStatic(PerformanceProfileUtil.class)) {
            converters.when(() -> Converters.KruizeObjectConverters.convertInputJSONToCreateMetricProfile(anyString()))
                    .thenReturn(mock(PerformanceProfile.class));
            util.when(() -> PerformanceProfileUtil.validateAndAddMetricProfile(any(), any()))
                    .thenReturn(new ValidationOutputData(false, internalDetail, HttpServletResponse.SC_INTERNAL_SERVER_ERROR));

            service.doPost(request, response);
        }

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                AnalyzerErrorConstants.INTERNAL_SERVER_ERROR_MSG);
        verify(response, never()).sendError(anyInt(), contains("NullPointerException"));
    }

    @Test
    @DisplayName("Failed validation result with a client error keeps its message")
    void doPostFailedValidationWithClientErrorKeepsMessage() throws Exception {
        givenRequestBody("{}");
        String message = "Validation failed: k8s_type foo is not supported";

        try (MockedStatic<Converters.KruizeObjectConverters> converters = mockStatic(Converters.KruizeObjectConverters.class);
             MockedStatic<PerformanceProfileUtil> util = mockStatic(PerformanceProfileUtil.class)) {
            converters.when(() -> Converters.KruizeObjectConverters.convertInputJSONToCreateMetricProfile(anyString()))
                    .thenReturn(mock(PerformanceProfile.class));
            util.when(() -> PerformanceProfileUtil.validateAndAddMetricProfile(any(), any()))
                    .thenReturn(new ValidationOutputData(false, message, HttpServletResponse.SC_BAD_REQUEST));

            service.doPost(request, response);
        }

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, message);
    }

    @Test
    @DisplayName("Unexpected exception returns HTTP 500 with a generic message and does not leak details")
    void doPostUnexpectedExceptionReturnsSanitizedInternalServerError() throws Exception {
        givenRequestBody("{}");
        String internalDetail = "jdbc:postgresql://internal-db:5432 connection refused";

        try (MockedStatic<Converters.KruizeObjectConverters> converters = mockStatic(Converters.KruizeObjectConverters.class)) {
            converters.when(() -> Converters.KruizeObjectConverters.convertInputJSONToCreateMetricProfile(anyString()))
                    .thenThrow(new IllegalStateException(internalDetail));

            service.doPost(request, response);
        }

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                AnalyzerErrorConstants.INTERNAL_SERVER_ERROR_MSG);
        verify(response, never()).sendError(anyInt(), contains(internalDetail));
    }
}
