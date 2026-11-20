/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
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
package com.autotune.database.helper;

import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.database.table.KruizeResultsEntry;
import com.autotune.utils.KruizeConstants;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Compact JSONB contract for {@code convertExperimentResultToExperimentResultsTable}.
 * The stored {@code extended_data} tree must keep kubernetes_objects and contain no pretty-print whitespace.
 */
public class ConvertExperimentResultToResultsTableTest {

    @Test
    @DisplayName("Results convert stores compact kubernetes_objects JSON and copies interval columns")
    public void testCompactExtendedData() {
        Instant end = Instant.parse("2024-01-01T00:15:00Z");
        Instant start = end.minusSeconds(900);

        ExperimentResultData resultData = new ExperimentResultData();
        resultData.setVersion("1.0");
        resultData.setExperiment_name("exp-a");
        resultData.setCluster_name("cluster-a");
        resultData.setIntervalStartTime(Timestamp.from(start));
        resultData.setIntervalEndTime(Timestamp.from(end));
        resultData.setKubernetes_objects(List.of(new K8sObject("demo-app", "deployment", "default")));

        KruizeResultsEntry entry = DBHelpers.Converters.KruizeObjectConverters
                .convertExperimentResultToExperimentResultsTable(resultData);

        assertNotNull(entry);
        assertEquals("1.0", entry.getVersion());
        assertEquals("exp-a", entry.getExperiment_name());
        assertEquals("cluster-a", entry.getCluster_name());
        assertEquals(Timestamp.from(start), entry.getInterval_start_time());
        assertEquals(Timestamp.from(end), entry.getInterval_end_time());
        assertEquals(15.0, entry.getDuration_minutes());

        JsonNode extendedData = entry.getExtended_data();
        assertNotNull(extendedData);
        JsonNode kubernetesObjects = extendedData.get(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS);
        assertNotNull(kubernetesObjects);
        assertEquals(1, kubernetesObjects.size());
        assertEquals("deployment", kubernetesObjects.get(0).get("type").asText());
        assertEquals("demo-app", kubernetesObjects.get(0).get("name").asText());
        assertEquals("default", kubernetesObjects.get(0).get("namespace").asText());

        String compact = extendedData.toString();
        assertFalse(compact.contains("\n"));
        assertEquals(
                "{\"kubernetes_objects\":[{\"type\":\"deployment\",\"name\":\"demo-app\",\"namespace\":\"default\"}]}",
                compact
        );
    }
}
