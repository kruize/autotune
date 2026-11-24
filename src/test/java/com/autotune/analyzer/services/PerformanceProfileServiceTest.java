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
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.database.dao.ExperimentDAOImpl;
import com.autotune.database.service.ExperimentDBService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the error handling of {@link PerformanceProfileService#doDelete}.
 */
class PerformanceProfileServiceTest {

    private static final String PROFILE_NAME = "resource-optimization-openshift";

    private PerformanceProfileService service;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseBody;

    @BeforeEach
    void setUp() throws Exception {
        service = new PerformanceProfileService();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
    }

    @Test
    @DisplayName("Unexpected exception on delete returns HTTP 500 with a generic message and does not leak details")
    void doDeleteUnexpectedExceptionReturnsSanitizedInternalServerError() throws Exception {
        when(request.getParameter(AnalyzerConstants.ServiceConstants.PERF_PROFILE_NAME)).thenReturn(PROFILE_NAME);
        String internalDetail = "could not execute statement; SQL [n/a]; constraint [kruize_performance_profiles_pkey]";

        try (MockedConstruction<ExperimentDBService> ignored = mockConstruction(ExperimentDBService.class,
                (mock, context) -> doThrow(new RuntimeException(internalDetail))
                        .when(mock).loadPerformanceProfileFromDBByName(any(), anyString()))) {

            service.doDelete(request, response);
        }

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(response).setContentType(AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE);
        String body = responseBody.toString();
        assertTrue(body.contains(AnalyzerErrorConstants.INTERNAL_SERVER_ERROR_MSG), body);
        assertFalse(body.contains(internalDetail), "Internal error details must not be returned to the client: " + body);
    }

    @Test
    @DisplayName("Failed DAO result with a server error (HibernateException) is sanitized before it is sent to the client")
    void doDeleteFailedDaoResultReturnsSanitizedInternalServerError() throws Exception {
        when(request.getParameter(AnalyzerConstants.ServiceConstants.PERF_PROFILE_NAME)).thenReturn(PROFILE_NAME);
        String internalDetail = "org.hibernate.exception.JDBCConnectionException: connection to internal-db:5432 refused";

        try (MockedConstruction<ExperimentDBService> dbService = mockConstruction(ExperimentDBService.class,
                (mock, context) -> {
                    doAnswer(invocation -> {
                        Map<String, PerformanceProfile> map = invocation.getArgument(0);
                        map.put(PROFILE_NAME, mock(PerformanceProfile.class));
                        return null;
                    }).when(mock).loadPerformanceProfileFromDBByName(any(), anyString());
                    when(mock.getExperimentsCountFromDBByProfileName(PROFILE_NAME)).thenReturn(0L);
                });
             MockedConstruction<ExperimentDAOImpl> dao = mockConstruction(ExperimentDAOImpl.class,
                     (mock, context) -> when(mock.deletePerformanceProfileByName(PROFILE_NAME))
                             .thenReturn(new ValidationOutputData(false, internalDetail, HttpServletResponse.SC_INTERNAL_SERVER_ERROR)))) {

            service.doDelete(request, response);
        }

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String body = responseBody.toString();
        assertTrue(body.contains(AnalyzerErrorConstants.INTERNAL_SERVER_ERROR_MSG), body);
        assertFalse(body.contains(internalDetail), "Internal error details must not be returned to the client: " + body);
    }

    @Test
    @DisplayName("Failed DAO result that is a client error (not found) keeps its message")
    void doDeleteFailedDaoResultNotFoundKeepsMessage() throws Exception {
        when(request.getParameter(AnalyzerConstants.ServiceConstants.PERF_PROFILE_NAME)).thenReturn(PROFILE_NAME);
        String notFoundMsg = "Performance profile not found";

        try (MockedConstruction<ExperimentDBService> dbService = mockConstruction(ExperimentDBService.class,
                (mock, context) -> {
                    doAnswer(invocation -> {
                        Map<String, PerformanceProfile> map = invocation.getArgument(0);
                        map.put(PROFILE_NAME, mock(PerformanceProfile.class));
                        return null;
                    }).when(mock).loadPerformanceProfileFromDBByName(any(), anyString());
                    when(mock.getExperimentsCountFromDBByProfileName(PROFILE_NAME)).thenReturn(0L);
                });
             MockedConstruction<ExperimentDAOImpl> dao = mockConstruction(ExperimentDAOImpl.class,
                     (mock, context) -> when(mock.deletePerformanceProfileByName(PROFILE_NAME))
                             .thenReturn(new ValidationOutputData(false, notFoundMsg, HttpServletResponse.SC_NOT_FOUND)))) {

            service.doDelete(request, response);
        }

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        assertTrue(responseBody.toString().contains(notFoundMsg), responseBody.toString());
    }

    @Test
    @DisplayName("Missing profile name returns HTTP 400 with a JSON body")
    void doDeleteMissingNameReturnsBadRequest() throws Exception {
        when(request.getParameter(AnalyzerConstants.ServiceConstants.PERF_PROFILE_NAME)).thenReturn(null);

        service.doDelete(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(response).setContentType(AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE);
        assertTrue(responseBody.toString().contains(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_PERF_PROFILE_NAME));
    }
}
