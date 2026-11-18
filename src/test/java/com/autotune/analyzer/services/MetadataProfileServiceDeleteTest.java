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

import com.autotune.analyzer.metadataProfiles.MetadataProfile;
import com.autotune.analyzer.metadataProfiles.MetadataProfileCollection;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.database.dao.ExperimentDAOImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the error handling of {@link MetadataProfileService#doDelete}.
 */
class MetadataProfileServiceDeleteTest {

    private static final String PROFILE_NAME = "test-metadata-profile";

    private MetadataProfileService service;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseBody;

    @BeforeEach
    void setUp() throws Exception {
        service = new MetadataProfileService();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
        when(request.getParameter(AnalyzerConstants.MetadataProfileConstants.METADATA_PROFILE_NAME)).thenReturn(PROFILE_NAME);
        MetadataProfileCollection.getInstance().getMetadataProfileCollection().put(PROFILE_NAME, mock(MetadataProfile.class));
    }

    @AfterEach
    void tearDown() {
        MetadataProfileCollection.getInstance().getMetadataProfileCollection().remove(PROFILE_NAME);
    }

    @Test
    @DisplayName("DB failure returns HTTP 500 with a generic message and keeps the in-memory profile")
    void dbFailureReturnsInternalServerErrorAndKeepsProfile() throws Exception {
        String internalDetail = "connection to internal-db:5432 refused";

        try (MockedConstruction<ExperimentDAOImpl> dao = mockConstruction(ExperimentDAOImpl.class,
                (mock, context) -> when(mock.deleteKruizeLMMetadataProfileEntryByName(PROFILE_NAME))
                        .thenReturn(new ValidationOutputData(false, internalDetail,
                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR)))) {
            service.doDelete(request, response);
        }

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String body = responseBody.toString();
        assertTrue(body.contains(AnalyzerErrorConstants.INTERNAL_SERVER_ERROR_MSG), body);
        assertFalse(body.contains(internalDetail), body);
        assertTrue(MetadataProfileCollection.getInstance().getMetadataProfileCollection().containsKey(PROFILE_NAME));
    }

    @Test
    @DisplayName("Profile not found in the DB returns HTTP 400")
    void notFoundInDbReturnsBadRequest() throws Exception {
        try (MockedConstruction<ExperimentDAOImpl> dao = mockConstruction(ExperimentDAOImpl.class,
                (mock, context) -> when(mock.deleteKruizeLMMetadataProfileEntryByName(PROFILE_NAME))
                        .thenReturn(new ValidationOutputData(false, "not found", HttpServletResponse.SC_NOT_FOUND)))) {
            service.doDelete(request, response);
        }

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(response, never()).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("Successful DB delete removes the in-memory profile")
    void successRemovesProfile() throws Exception {
        try (MockedConstruction<ExperimentDAOImpl> dao = mockConstruction(ExperimentDAOImpl.class,
                (mock, context) -> when(mock.deleteKruizeLMMetadataProfileEntryByName(PROFILE_NAME))
                        .thenReturn(new ValidationOutputData(true, null, null)))) {
            service.doDelete(request, response);
        }

        assertFalse(MetadataProfileCollection.getInstance().getMetadataProfileCollection().containsKey(PROFILE_NAME));
    }
}
