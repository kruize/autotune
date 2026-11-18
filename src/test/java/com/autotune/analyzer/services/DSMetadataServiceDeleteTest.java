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

import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.dataSourceMetadata.DataSourceMetadataInfo;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceManager;
import com.autotune.common.exceptions.datasource.DataSourceDoesNotExist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the error handling of {@link DSMetadataService#doDelete}.
 */
class DSMetadataServiceDeleteTest {

    private static final String DS_NAME = "prometheus-1";
    private static final String VALID_BODY = "{\"version\":\"v1.0\",\"datasource_name\":\"" + DS_NAME
            + "\",\"metadata_profile\":\"cluster-metadata-local-monitoring\"}";

    private DSMetadataService service;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() throws Exception {
        service = new DSMetadataService();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    }

    private void givenRequestBody(String body) throws Exception {
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
    }

    /**
     * Stubs a data source manager where the data source and its metadata exist in the DB
     */
    private static void givenExistingDataSource(DataSourceManager mock) {
        when(mock.fetchDataSourceFromDBByName(DS_NAME)).thenReturn(mock(DataSourceInfo.class));
        when(mock.fetchDataSourceMetadataFromDBByName(DS_NAME, "false")).thenReturn(mock(DataSourceMetadataInfo.class));
    }

    @Test
    @DisplayName("Literal null body returns HTTP 400 instead of a NullPointerException")
    void doDeleteNullBodyReturnsBadRequest() throws Exception {
        givenRequestBody("null");

        service.doDelete(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
        verify(response, never()).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
    }

    @Test
    @DisplayName("Failure to delete the metadata from the DB returns HTTP 500 and skips the in-memory cleanup")
    void doDeleteDbFailureReturnsInternalServerErrorWithoutCleanup() throws Exception {
        givenRequestBody(VALID_BODY);
        String internalDetail = "connection to internal-db:5432 refused";

        try (MockedConstruction<DataSourceManager> manager = mockConstruction(DataSourceManager.class,
                (mock, context) -> {
                    givenExistingDataSource(mock);
                    doThrow(new IllegalStateException(internalDetail)).when(mock).deleteMetadataFromDB(DS_NAME);
                })) {

            service.doDelete(request, response);

            DataSourceManager mock = manager.constructed().get(0);
            verify(mock, never()).deleteMetadataFromDataSource(any());
        }

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                AnalyzerErrorConstants.INTERNAL_SERVER_ERROR_MSG);
        verify(response, never()).sendError(anyInt(), contains(internalDetail));
    }

    @Test
    @DisplayName("Metadata that is not found in the DB returns HTTP 400")
    void doDeleteMetadataNotFoundReturnsBadRequest() throws Exception {
        givenRequestBody(VALID_BODY);

        try (MockedConstruction<DataSourceManager> manager = mockConstruction(DataSourceManager.class,
                (mock, context) -> {
                    givenExistingDataSource(mock);
                    doThrow(new DataSourceDoesNotExist("not found")).when(mock).deleteMetadataFromDB(DS_NAME);
                })) {

            service.doDelete(request, response);

            verify(manager.constructed().get(0), never()).deleteMetadataFromDataSource(any());
        }

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), contains(DS_NAME));
        verify(response, never()).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
    }

    @Test
    @DisplayName("Successful delete removes the DB entry and then the in-memory metadata")
    void doDeleteSuccessCleansUpAfterDbDelete() throws Exception {
        givenRequestBody(VALID_BODY);

        try (MockedConstruction<DataSourceManager> manager = mockConstruction(DataSourceManager.class,
                (mock, context) -> givenExistingDataSource(mock))) {

            service.doDelete(request, response);

            DataSourceManager mock = manager.constructed().get(0);
            org.mockito.InOrder order = inOrder(mock);
            order.verify(mock).deleteMetadataFromDB(DS_NAME);
            order.verify(mock).deleteMetadataFromDataSource(any());
        }

        verify(response, never()).sendError(anyInt(), anyString());
        verify(response).setStatus(HttpServletResponse.SC_CREATED);
    }
}
