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

import com.autotune.analyzer.kruizeObject.KruizeObject;
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
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the error handling of {@link CreateExperiment#doDelete}.
 * Invalid input must result in HTTP 400, while database failures result in HTTP 500
 * with a generic message (no internal error details).
 */
class CreateExperimentDeleteTest {

    private static final String EXP_NAME = "test-experiment";

    private CreateExperiment service;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        service = new CreateExperiment();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    private void givenRequestBody(String body) throws Exception {
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
    }

    @Test
    @DisplayName("Empty array body returns HTTP 400")
    void doDeleteEmptyArrayReturnsBadRequest() throws Exception {
        givenRequestBody("[]");

        service.doDelete(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST,
                AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_DELETE_EXPERIMENT_INPUT);
        verify(response, never()).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
    }

    @Test
    @DisplayName("Array with a null entry returns HTTP 400 instead of a NullPointerException")
    void doDeleteNullEntryReturnsBadRequest() throws Exception {
        givenRequestBody("[null]");

        service.doDelete(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST,
                AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_DELETE_EXPERIMENT_INPUT);
        verify(response, never()).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
    }

    @Test
    @DisplayName("Literal null body returns HTTP 400")
    void doDeleteNullBodyReturnsBadRequest() throws Exception {
        givenRequestBody("null");

        service.doDelete(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST,
                AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_DELETE_EXPERIMENT_INPUT);
    }

    @Test
    @DisplayName("Failure to load the experiment from the DB returns HTTP 500 and not 'Experiment not found!'")
    void doDeleteLoadFailureReturnsInternalServerError() throws Exception {
        givenRequestBody("[{\"cluster_name\":\"c\",\"experiment_name\":\"" + EXP_NAME + "\"}]");
        String internalDetail = "jdbc:postgresql://internal-db:5432 connection refused";

        try (MockedConstruction<ExperimentDBService> ignored = mockConstruction(ExperimentDBService.class,
                (mock, context) -> doThrow(new Exception(internalDetail))
                        .when(mock).loadLMExperimentFromDBByName(any(), anyString()))) {

            service.doDelete(request, response);
        }

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                AnalyzerErrorConstants.INTERNAL_SERVER_ERROR_MSG);
        verify(response, never()).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Experiment that does not exist returns HTTP 400")
    void doDeleteExperimentNotFoundReturnsBadRequest() throws Exception {
        givenRequestBody("[{\"cluster_name\":\"c\",\"experiment_name\":\"" + EXP_NAME + "\"}]");

        try (MockedConstruction<ExperimentDBService> ignored = mockConstruction(ExperimentDBService.class)) {
            service.doDelete(request, response);
        }

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Experiment not found!");
    }

    @Test
    @DisplayName("Failed DAO delete with a server error returns HTTP 500 with a generic message")
    void doDeleteDaoFailureReturnsSanitizedInternalServerError() throws Exception {
        givenRequestBody("[{\"cluster_name\":\"c\",\"experiment_name\":\"" + EXP_NAME + "\"}]");
        String internalDetail = "org.hibernate.exception.JDBCConnectionException: connection to internal-db refused";

        try (MockedConstruction<ExperimentDBService> dbService = mockConstruction(ExperimentDBService.class,
                (mock, context) -> doAnswer(invocation -> {
                    Map<String, KruizeObject> map = invocation.getArgument(0);
                    map.put(EXP_NAME, mock(KruizeObject.class));
                    return null;
                }).when(mock).loadLMExperimentFromDBByName(any(), anyString()));
             MockedConstruction<ExperimentDAOImpl> dao = mockConstruction(ExperimentDAOImpl.class,
                     (mock, context) -> when(mock.deleteKruizeLMExperimentEntryByName(EXP_NAME))
                             .thenReturn(new ValidationOutputData(false, internalDetail, HttpServletResponse.SC_INTERNAL_SERVER_ERROR)))) {

            service.doDelete(request, response);
        }

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                AnalyzerErrorConstants.INTERNAL_SERVER_ERROR_MSG);
        verify(response, never()).sendError(anyInt(), contains(internalDetail));
    }
}
