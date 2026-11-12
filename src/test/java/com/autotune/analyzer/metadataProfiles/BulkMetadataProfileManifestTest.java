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
package com.autotune.analyzer.metadataProfiles;

import com.autotune.analyzer.utils.AnalyzerConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the shipped bulk metadata profile: every workload/container query, including the
 * label-aware {@code workloadsWithPodLabelFilter} query, must carry {@code $UNSUPPORTED_WORKLOAD_TYPES$}
 * so that unsupported workloads are not discovered when a pod label filter is used.
 */
class BulkMetadataProfileManifestTest {

    private static final Path MANIFEST =
            Paths.get("manifests/autotune/metadata-profiles/bulk_cluster_metadata_local_monitoring.json");

    private static String queryOf(JSONObject queryVariable) {
        return queryVariable.getJSONArray("aggregation_functions").getJSONObject(0).getString("query");
    }

    @Test
    @DisplayName("workloadsWithPodLabelFilter query contains $UNSUPPORTED_WORKLOAD_TYPES$")
    void labelAwareWorkloadQueryHasUnsupportedTypesPlaceholder() throws Exception {
        JSONArray variables = new JSONObject(Files.readString(MANIFEST)).getJSONArray("query_variables");
        boolean found = false;
        for (int i = 0; i < variables.length(); i++) {
            JSONObject variable = variables.getJSONObject(i);
            if (AnalyzerConstants.WORKLOAD_METADATA_QUERY_WITH_LABEL_FILTER.equals(variable.getString("name"))) {
                found = true;
                assertTrue(queryOf(variable).contains(AnalyzerConstants.UNSUPPORTED_WORKLOAD_TYPES_VARIABLE),
                        "Label-aware workload query must contain " + AnalyzerConstants.UNSUPPORTED_WORKLOAD_TYPES_VARIABLE);
            }
        }
        assertTrue(found, AnalyzerConstants.WORKLOAD_METADATA_QUERY_WITH_LABEL_FILTER + " not found in manifest");
    }

    @Test
    @DisplayName("All workload and container queries contain $UNSUPPORTED_WORKLOAD_TYPES$")
    void allWorkloadAndContainerQueriesHaveUnsupportedTypesPlaceholder() throws Exception {
        JSONArray variables = new JSONObject(Files.readString(MANIFEST)).getJSONArray("query_variables");
        for (int i = 0; i < variables.length(); i++) {
            JSONObject variable = variables.getJSONObject(i);
            String name = variable.getString("name");
            if (name.startsWith("workloads") || name.startsWith("containers")) {
                assertTrue(queryOf(variable).contains(AnalyzerConstants.UNSUPPORTED_WORKLOAD_TYPES_VARIABLE),
                        name + " must contain " + AnalyzerConstants.UNSUPPORTED_WORKLOAD_TYPES_VARIABLE);
            }
        }
    }
}
