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
package com.autotune.common.datasource;

import com.autotune.analyzer.metadataProfiles.MetadataProfile;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.dataSourceMetadata.DataSourceMetadataHelper;
import com.autotune.common.data.metrics.AggregationFunctions;
import com.autotune.common.data.metrics.Metric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DataSourceMetadataHelper.getQueryFromProfile().
 *
 * The method uses name.contains(metricName) substring matching so that any
 * profile naming convention (AcrossCluster, ForAdditionalLabel, etc.) is
 * resolved as long as the entry name contains the short keyword.
 *
 * This is the fix for the regression introduced when getQueryTemplate() was
 * changed to pass exact full names instead of the short keywords NAMESPACE,
 * WORKLOAD, CONTAINER defined in AnalyzerConstants.
 */
class DataSourceMetadataHelperQueryTest {

    private DataSourceMetadataHelper helper;

    // Stable query strings for assertions
    private static final String NS_QUERY  = "sum by (namespace) (avg_over_time(kube_namespace_status_phase{}[15m]))";
    private static final String WL_QUERY  = "sum by (namespace, workload) (avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{}[15m]))";
    private static final String CON_QUERY = "sum by (container, namespace) (avg_over_time(kube_pod_container_info{}[15m]))";
    private static final String WL_LABEL_QUERY = "sum by (namespace, workload) (max_over_time(kube_pod_labels{}[15m]))";

    @BeforeEach
    void setUp() {
        helper = new DataSourceMetadataHelper();
    }

    // -----------------------------------------------------------------------
    // Helper: build a Metric with a single "sum" aggregation function
    // -----------------------------------------------------------------------
    private Metric buildMetric(String name, String query) {
        Metric metric = new Metric(name, query, "prometheus", "double", "container");
        AggregationFunctions agg = new AggregationFunctions("sum", query, null);
        HashMap<String, AggregationFunctions> aggMap = new HashMap<>();
        aggMap.put("sum", agg);
        metric.setAggregationFunctionsMap(aggMap);
        return metric;
    }

    // -----------------------------------------------------------------------
    // Helper: build a MetadataProfile with the given list of metrics
    // -----------------------------------------------------------------------
    private MetadataProfile buildProfile(ArrayList<Metric> metrics) {
        return new MetadataProfile(
                "recommender.com/v1",
                "KruizeMetadataProfile",
                null,
                1.0,
                "openshift",
                "prometheus",
                metrics
        );
    }

    // -----------------------------------------------------------------------
    // Nested test class: original "AcrossCluster" naming convention
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("AcrossCluster naming convention (original update_cluster_metadata_local_monitoring.json)")
    class AcrossClusterNaming {

        private MetadataProfile profile;

        @BeforeEach
        void setUp() {
            ArrayList<Metric> metrics = new ArrayList<>();
            metrics.add(buildMetric("namespacesAcrossCluster",  NS_QUERY));
            metrics.add(buildMetric("workloadsAcrossCluster",   WL_QUERY));
            metrics.add(buildMetric("containersAcrossCluster",  CON_QUERY));
            profile = buildProfile(metrics);
        }

        @Test
        @DisplayName("contains('namespace') resolves namespacesAcrossCluster")
        void namespaceKeyword_resolvesAcrossCluster() {
            String result = helper.getQueryFromProfile(profile, AnalyzerConstants.NAMESPACE);
            assertEquals(NS_QUERY, result);
        }

        @Test
        @DisplayName("contains('workload') resolves workloadsAcrossCluster")
        void workloadKeyword_resolvesAcrossCluster() {
            String result = helper.getQueryFromProfile(profile, AnalyzerConstants.WORKLOAD);
            assertEquals(WL_QUERY, result);
        }

        @Test
        @DisplayName("contains('container') resolves containersAcrossCluster")
        void containerKeyword_resolvesAcrossCluster() {
            String result = helper.getQueryFromProfile(profile, AnalyzerConstants.CONTAINER);
            assertEquals(CON_QUERY, result);
        }

        @Test
        @DisplayName("exact lookup of 'workloadsWithPodLabelFilter' returns null when entry is absent")
        void labelFilterEntry_missingReturnsNull() {
            String result = helper.getQueryFromProfile(profile, AnalyzerConstants.WORKLOAD_METADATA_QUERY_WITH_LABEL_FILTER);
            assertNull(result, "Should return null when workloadsWithPodLabelFilter is not in the profile");
        }
    }

    // -----------------------------------------------------------------------
    // Nested test class: "ForAdditionalLabel" naming convention
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("ForAdditionalLabel naming convention (bulk_cluster_metadata_local_monitoring.json)")
    class ForAdditionalLabelNaming {

        private MetadataProfile profile;

        @BeforeEach
        void setUp() {
            ArrayList<Metric> metrics = new ArrayList<>();
            metrics.add(buildMetric("namespacesForAdditionalLabel",  NS_QUERY));
            metrics.add(buildMetric("workloadsForAdditionalLabel",   WL_QUERY));
            metrics.add(buildMetric("workloadsWithPodLabelFilter",   WL_LABEL_QUERY));
            metrics.add(buildMetric("containersForAdditionalLabel",  CON_QUERY));
            profile = buildProfile(metrics);
        }

        @Test
        @DisplayName("contains('namespace') resolves namespacesForAdditionalLabel")
        void namespaceKeyword_resolvesForAdditionalLabel() {
            String result = helper.getQueryFromProfile(profile, AnalyzerConstants.NAMESPACE);
            assertEquals(NS_QUERY, result);
        }

        @Test
        @DisplayName("contains('workload') resolves workloadsForAdditionalLabel (first match)")
        void workloadKeyword_resolvesForAdditionalLabel() {
            // workloadsForAdditionalLabel appears before workloadsWithPodLabelFilter → first match wins
            String result = helper.getQueryFromProfile(profile, AnalyzerConstants.WORKLOAD);
            assertEquals(WL_QUERY, result);
        }

        @Test
        @DisplayName("contains('container') resolves containersForAdditionalLabel")
        void containerKeyword_resolvesForAdditionalLabel() {
            String result = helper.getQueryFromProfile(profile, AnalyzerConstants.CONTAINER);
            assertEquals(CON_QUERY, result);
        }

        @Test
        @DisplayName("exact lookup of 'workloadsWithPodLabelFilter' returns label-filter query")
        void labelFilterExactLookup_resolvesLabelFilterEntry() {
            String result = helper.getQueryFromProfile(profile, AnalyzerConstants.WORKLOAD_METADATA_QUERY_WITH_LABEL_FILTER);
            assertEquals(WL_LABEL_QUERY, result);
        }
    }

    // -----------------------------------------------------------------------
    // Nested test class: edge cases
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Empty profile returns null for any keyword")
        void emptyProfile_returnsNull() {
            MetadataProfile empty = buildProfile(new ArrayList<>());
            assertNull(helper.getQueryFromProfile(empty, AnalyzerConstants.NAMESPACE));
            assertNull(helper.getQueryFromProfile(empty, AnalyzerConstants.WORKLOAD));
            assertNull(helper.getQueryFromProfile(empty, AnalyzerConstants.CONTAINER));
        }

        @Test
        @DisplayName("No matching entry returns null")
        void noMatch_returnsNull() {
            ArrayList<Metric> metrics = new ArrayList<>();
            metrics.add(buildMetric("namespacesAcrossCluster", NS_QUERY));
            MetadataProfile profile = buildProfile(metrics);
            assertNull(helper.getQueryFromProfile(profile, AnalyzerConstants.CONTAINER));
        }
    }
}
