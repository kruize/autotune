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
package com.autotune.analyzer.recommendations.engine;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.database.service.ExperimentDBService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * updateRecommendations loads its results window with the same error handling as the old
 * remote branch of getResults: no rows is an error, a DB read failure is logged and the
 * request carries on (no raw DB message is sent back to the client).
 */
class RecommendationEngineLoadRemoteResultsTest {
    private static final Timestamp START = Timestamp.from(Instant.parse("2026-01-01T00:00:00Z"));
    private static final Timestamp END = Timestamp.from(Instant.parse("2026-01-16T00:00:00Z"));

    @Test
    @DisplayName("a DB read failure is logged and the request carries on, same as getResults")
    void dbFailureCarriesOn() throws Exception {
        ExperimentDBService service = mock(ExperimentDBService.class);
        when(service.loadRecommendationResultsOnto(any(), any(), any(), any()))
                .thenThrow(new Exception("Error while loading results from the database due to : connection refused"));

        String errorMsg = RecommendationEngine.loadRemoteResults(service, map(), new KruizeObject(), START, END);

        assertEquals("", errorMsg);
    }

    @Test
    @DisplayName("no rows in the window gives the no-metrics message")
    void noRowsGivesNoMetricsMessage() throws Exception {
        ExperimentDBService service = mock(ExperimentDBService.class);
        when(service.loadRecommendationResultsOnto(any(), any(), any(), any())).thenReturn(false);

        String errorMsg = RecommendationEngine.loadRemoteResults(service, map(), new KruizeObject(), START, END);

        assertFalse(errorMsg.isEmpty());
        assertTrue(errorMsg.startsWith("No metrics available from "), errorMsg);
        assertFalse(errorMsg.contains("database"), errorMsg);
    }

    @Test
    @DisplayName("rows found carries on with no error")
    void rowsFoundCarriesOn() throws Exception {
        ExperimentDBService service = mock(ExperimentDBService.class);
        when(service.loadRecommendationResultsOnto(any(), any(), any(), any())).thenReturn(true);

        assertEquals("", RecommendationEngine.loadRemoteResults(service, map(), new KruizeObject(), START, END));
    }

    private static Map<String, KruizeObject> map() {
        return new HashMap<>();
    }
}
