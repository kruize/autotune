/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import com.autotune.analyzer.metadataProfiles.utils.MetadataProfileUtil;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceOperatorImpl;
import com.autotune.utils.KruizeConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MetadataProfileValidation.
 *
 * Covers:
 * - validateMetricQueryPattern(): validates the sum by clause and delegates to queryValidator.
 * - queryValidator(): substitutes $MEASUREMENT_DURATION_IN_MIN$, $UNSUPPORTED_WORKLOAD_TYPES$
 *   before running the query against Prometheus, then verifies result identifiers.
 * - matchSumByClause(): verifies each MetadataProfileQueryIdentifier is correctly matched.
 * - appendFiltersToQuery(): appends a filter string verbatim into the first selector of a query.
 *
 * Query fixtures mirror the bulk metadata profile JSON
 * (manifests/autotune/metadata-profiles/bulk_cluster_metadata_local_monitoring.json)
 * and the update fixture
 * (tests/scripts/local_monitoring_tests/json_files/update_cluster_metadata_local_monitoring.json).
 */
class MetadataProfileValidationTest {

    private MetadataProfileValidation validation;
    private DataSourceInfo mockDataSourceInfo;

    // ---- Bulk-profile queries (contain $...$ bulk-only placeholders) ----
    // These are valid in bulk metadata profiles used at data-gathering time,
    // but must be REJECTED by the updateMetadata API.
    private static final String NAMESPACE_QUERY =
            "sum by (namespace) (avg_over_time(kube_namespace_status_phase{$NAMESPACE_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))";

    private static final String WORKLOAD_QUERY =
            "sum by (namespace, workload, workload_type) (avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ $WORKLOAD_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))";

    private static final String WORKLOAD_WITH_LABEL_QUERY =
            "sum by (namespace, workload, workload_type) (max_over_time(kube_pod_labels{$NAMESPACE_FILTERS$ $CONTAINER_FILTERS$ $LABELS$}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (namespace, pod) group_left(workload, workload_type) max_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ $WORKLOAD_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))";

    private static final String CONTAINER_QUERY =
            "sum by (container, image, workload, workload_type, namespace) (avg_over_time(kube_pod_container_info{$NAMESPACE_FILTERS$ $CONTAINER_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (pod, namespace) group_left(workload, workload_type) avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ $WORKLOAD_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))";

    // ---- Update-payload queries (valid for updateMetadata) ----
    // Bulk-only placeholders are replaced with concrete label selectors.
    // $MEASUREMENT_DURATION_IN_MIN$ and $UNSUPPORTED_WORKLOAD_TYPES$ are retained;
    // they are substituted automatically during validation and stored as-is in the database.
    // These mirror update_cluster_metadata_local_monitoring.json.
    private static final String UPDATE_NAMESPACE_QUERY =
            "sum by (namespace) (avg_over_time(kube_namespace_status_phase{namespace!=\"\", namespace=~\"openshift-tuning|monitoring\"}[$MEASUREMENT_DURATION_IN_MIN$m]))";

    private static final String UPDATE_WORKLOAD_QUERY =
            "sum by (namespace, workload, workload_type) (avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ workload!=\"\", workload=\"kruize\"}[$MEASUREMENT_DURATION_IN_MIN$m]))";

    private static final String UPDATE_WORKLOAD_WITH_LABEL_QUERY =
            "sum by (namespace, workload, workload_type) (max_over_time(kube_pod_labels{namespace!=\"\", namespace=~\"openshift-tuning|monitoring\"}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (namespace, pod) group_left(workload, workload_type) max_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ workload!=\"\", workload=\"kruize\"}[$MEASUREMENT_DURATION_IN_MIN$m]))";

    private static final String UPDATE_CONTAINER_QUERY =
            "sum by (container, image, workload, workload_type, namespace) (avg_over_time(kube_pod_container_info{namespace!=\"\", namespace=~\"openshift-tuning|monitoring\", container!=\"\", container=\"kruize\"}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (pod, namespace) group_left(workload, workload_type) avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ workload!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]))";

    @BeforeEach
    void setup() {
        validation = new MetadataProfileValidation(new HashMap<>());
        mockDataSourceInfo = mock(DataSourceInfo.class);
        when(mockDataSourceInfo.getProvider()).thenReturn("prometheus");
    }

    // -------------------------------------------------------------------------
    // Helper: build a minimal JsonArray result containing the given label keys
    // -------------------------------------------------------------------------
    private static JsonArray buildResultArray(String... labelKeys) {
        JsonArray resultArray = new JsonArray();
        JsonObject entry = new JsonObject();
        JsonObject metric = new JsonObject();
        for (String key : labelKeys) {
            metric.addProperty(key, "test-value");
        }
        entry.add(KruizeConstants.JSONKeys.METRIC, metric);
        resultArray.add(entry);
        return resultArray;
    }

    // =========================================================================
    // matchSumByClause
    // =========================================================================
    @Nested
    @DisplayName("matchSumByClause — identifier detection from real queries")
    class MatchSumByClauseTests {

        @Test
        @DisplayName("Namespace query maps to NAMESPACE_QUERY_IDENTIFIER")
        void namespaceQueryIdentifier() {
            AnalyzerConstants.MetadataProfileQueryIdentifier result =
                    MetadataProfileUtil.matchSumByClause(UPDATE_NAMESPACE_QUERY);
            assertEquals(AnalyzerConstants.MetadataProfileQueryIdentifier.NAMESPACE_QUERY_IDENTIFIER, result);
        }

        @Test
        @DisplayName("Workload query maps to WORKLOAD_QUERY_IDENTIFIER")
        void workloadQueryIdentifier() {
            AnalyzerConstants.MetadataProfileQueryIdentifier result =
                    MetadataProfileUtil.matchSumByClause(UPDATE_WORKLOAD_QUERY);
            assertEquals(AnalyzerConstants.MetadataProfileQueryIdentifier.WORKLOAD_QUERY_IDENTIFIER, result);
        }

        @Test
        @DisplayName("WorkloadWithPodLabelFilter query maps to WORKLOAD_QUERY_IDENTIFIER")
        void workloadWithLabelQueryIdentifier() {
            AnalyzerConstants.MetadataProfileQueryIdentifier result =
                    MetadataProfileUtil.matchSumByClause(UPDATE_WORKLOAD_WITH_LABEL_QUERY);
            assertEquals(AnalyzerConstants.MetadataProfileQueryIdentifier.WORKLOAD_QUERY_IDENTIFIER, result);
        }

        @Test
        @DisplayName("Container query maps to CONTAINER_QUERY_IDENTIFIER")
        void containerQueryIdentifier() {
            AnalyzerConstants.MetadataProfileQueryIdentifier result =
                    MetadataProfileUtil.matchSumByClause(UPDATE_CONTAINER_QUERY);
            assertEquals(AnalyzerConstants.MetadataProfileQueryIdentifier.CONTAINER_QUERY_IDENTIFIER, result);
        }

        @Test
        @DisplayName("Query without sum by clause returns null")
        void noSumByReturnsNull() {
            String query = "avg_over_time(kube_namespace_status_phase[$MEASUREMENT_DURATION_IN_MIN$m])";
            assertNull(MetadataProfileUtil.matchSumByClause(query));
        }

        @Test
        @DisplayName("Query with unrecognised sum by identifiers returns null")
        void unknownIdentifiersReturnNull() {
            String query = "sum by (foo, bar) (some_metric[$MEASUREMENT_DURATION_IN_MIN$m])";
            assertNull(MetadataProfileUtil.matchSumByClause(query));
        }
    }

    // =========================================================================
    // Positive test 1: Default metadata configuration
    // Verifies that the default/fixture update payload queries pass validation:
    //   - $MEASUREMENT_DURATION_IN_MIN$ and $UNSUPPORTED_WORKLOAD_TYPES$ are substituted correctly
    //   - the query is structurally valid (sum by clause matches)
    //   - the update succeeds (no error appended)
    // =========================================================================
    @Nested
    @DisplayName("Positive test 1 — Default metadata configuration validates successfully")
    class DefaultMetadataConfigTests {

        @Test
        @DisplayName("Namespace query from default fixture passes validation with template variable substitution")
        void defaultNamespaceQueryPassesValidation() throws Exception {
            DataSourceOperatorImpl mockOperator = mock(DataSourceOperatorImpl.class);
            when(mockOperator.getResultArrayForQuery(any(), anyString()))
                    .thenReturn(buildResultArray("namespace"));

            try (MockedStatic<DataSourceOperatorImpl> staticMock =
                         mockStatic(DataSourceOperatorImpl.class)) {
                DataSourceOperatorImpl mockImpl = mock(DataSourceOperatorImpl.class);
                staticMock.when(DataSourceOperatorImpl::getInstance).thenReturn(mockImpl);
                when(mockImpl.getOperator(anyString())).thenReturn(mockOperator);

                StringBuilder errorString = new StringBuilder();
                // UPDATE_NAMESPACE_QUERY contains $MEASUREMENT_DURATION_IN_MIN$ — mirrors fixture
                validation.validateMetricQueryPattern(
                        UPDATE_NAMESPACE_QUERY,
                        "namespacesForAdditionalLabel",
                        errorString,
                        mockDataSourceInfo);

                assertTrue(errorString.isEmpty(),
                        "Default namespace fixture query must pass validation without error");
            }
        }

        @Test
        @DisplayName("$MEASUREMENT_DURATION_IN_MIN$ is substituted before the query reaches Prometheus")
        void measurementDurationPlaceholderIsSubstituted() throws Exception {
            DataSourceOperatorImpl mockOperator = mock(DataSourceOperatorImpl.class);
            ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
            when(mockOperator.getResultArrayForQuery(any(), queryCaptor.capture()))
                    .thenReturn(buildResultArray("namespace"));

            try (MockedStatic<DataSourceOperatorImpl> staticMock =
                         mockStatic(DataSourceOperatorImpl.class)) {
                DataSourceOperatorImpl mockImpl = mock(DataSourceOperatorImpl.class);
                staticMock.when(DataSourceOperatorImpl::getInstance).thenReturn(mockImpl);
                when(mockImpl.getOperator(anyString())).thenReturn(mockOperator);

                StringBuilder errorString = new StringBuilder();
                validation.validateMetricQueryPattern(
                        UPDATE_NAMESPACE_QUERY,
                        "namespacesForAdditionalLabel",
                        errorString,
                        mockDataSourceInfo);

                String capturedQuery = queryCaptor.getValue();
                assertFalse(capturedQuery.contains("$MEASUREMENT_DURATION_IN_MIN$"),
                        "Placeholder must be substituted before the query is sent to Prometheus");
                assertTrue(capturedQuery.contains(
                        String.valueOf(AnalyzerConstants.DEFAULT_MEASUREMENT_DURATION_INT) + "m"),
                        "Substituted duration value must appear in the query sent to Prometheus");
                assertTrue(errorString.isEmpty(), "No error should be appended");
            }
        }

        @Test
        @DisplayName("$UNSUPPORTED_WORKLOAD_TYPES$ is substituted before the query reaches Prometheus")
        void unsupportedWorkloadTypesPlaceholderIsSubstituted() throws Exception {
            DataSourceOperatorImpl mockOperator = mock(DataSourceOperatorImpl.class);
            ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
            when(mockOperator.getResultArrayForQuery(any(), queryCaptor.capture()))
                    .thenReturn(buildResultArray("namespace", "workload", "workload_type"));

            try (MockedStatic<DataSourceOperatorImpl> staticMock =
                         mockStatic(DataSourceOperatorImpl.class)) {
                DataSourceOperatorImpl mockImpl = mock(DataSourceOperatorImpl.class);
                staticMock.when(DataSourceOperatorImpl::getInstance).thenReturn(mockImpl);
                when(mockImpl.getOperator(anyString())).thenReturn(mockOperator);

                StringBuilder errorString = new StringBuilder();
                validation.validateMetricQueryPattern(
                        UPDATE_WORKLOAD_QUERY,
                        "workloadsForAdditionalLabel",
                        errorString,
                        mockDataSourceInfo);

                String capturedQuery = queryCaptor.getValue();
                assertFalse(capturedQuery.contains("$UNSUPPORTED_WORKLOAD_TYPES$"),
                        "Placeholder must be substituted before the query is sent to Prometheus");
                assertFalse(capturedQuery.contains("{,"),
                        "Selector must not contain a leading comma after placeholder substitution");
                assertTrue(errorString.isEmpty(), "No error should be appended");
            }
        }
    }

    // =========================================================================
    // Positive test 2: User-customized metadata/query
    // Verifies that a valid user-supplied query (different from the default fixture,
    // e.g. scoped to a specific namespace) also passes validation.
    // =========================================================================
    @Nested
    @DisplayName("Positive test 2 — User-customized metadata query validates successfully")
    class UserCustomizedMetadataTests {

        @Test
        @DisplayName("Custom namespace query scoped to a single namespace passes validation")
        void customNamespaceQueryPassesValidation() throws Exception {
            // User changes the namespace filter to target only "my-app-namespace"
            String customNamespaceQuery =
                    "sum by (namespace) (avg_over_time(kube_namespace_status_phase" +
                    "{namespace!=\"\", namespace=\"my-app-namespace\"}[$MEASUREMENT_DURATION_IN_MIN$m]))";

            DataSourceOperatorImpl mockOperator = mock(DataSourceOperatorImpl.class);
            when(mockOperator.getResultArrayForQuery(any(), anyString()))
                    .thenReturn(buildResultArray("namespace"));

            try (MockedStatic<DataSourceOperatorImpl> staticMock =
                         mockStatic(DataSourceOperatorImpl.class)) {
                DataSourceOperatorImpl mockImpl = mock(DataSourceOperatorImpl.class);
                staticMock.when(DataSourceOperatorImpl::getInstance).thenReturn(mockImpl);
                when(mockImpl.getOperator(anyString())).thenReturn(mockOperator);

                StringBuilder errorString = new StringBuilder();
                validation.validateMetricQueryPattern(
                        customNamespaceQuery,
                        "namespacesForAdditionalLabel",
                        errorString,
                        mockDataSourceInfo);

                assertTrue(errorString.isEmpty(),
                        "A user-customized namespace query must pass validation without error");
            }
        }

        @Test
        @DisplayName("Custom workload query scoped to a different workload passes validation")
        void customWorkloadQueryPassesValidation() throws Exception {
            // User changes the workload filter to target "my-service" instead of "kruize"
            String customWorkloadQuery =
                    "sum by (namespace, workload, workload_type) (avg_over_time(" +
                    "namespace_workload_pod:kube_pod_owner:relabel" +
                    "{$UNSUPPORTED_WORKLOAD_TYPES$ workload!=\"\", workload=\"my-service\"}" +
                    "[$MEASUREMENT_DURATION_IN_MIN$m]))";

            DataSourceOperatorImpl mockOperator = mock(DataSourceOperatorImpl.class);
            when(mockOperator.getResultArrayForQuery(any(), anyString()))
                    .thenReturn(buildResultArray("namespace", "workload", "workload_type"));

            try (MockedStatic<DataSourceOperatorImpl> staticMock =
                         mockStatic(DataSourceOperatorImpl.class)) {
                DataSourceOperatorImpl mockImpl = mock(DataSourceOperatorImpl.class);
                staticMock.when(DataSourceOperatorImpl::getInstance).thenReturn(mockImpl);
                when(mockImpl.getOperator(anyString())).thenReturn(mockOperator);

                StringBuilder errorString = new StringBuilder();
                validation.validateMetricQueryPattern(
                        customWorkloadQuery,
                        "workloadsForAdditionalLabel",
                        errorString,
                        mockDataSourceInfo);

                assertTrue(errorString.isEmpty(),
                        "A user-customized workload query must pass validation without error");
            }
        }
    }

    // =========================================================================
    // Negative test 1: Unsupported or unresolved placeholder
    // Verifies that bulk-only placeholders are rejected, and distinguishes them
    // from optional template variables that are absent but not required.
    // =========================================================================
    @Nested
    @DisplayName("Negative test 1 — Unsupported or unresolved placeholder is rejected")
    class UnresolvedPlaceholderTests {

        @Test
        @DisplayName("Bulk-only $NAMESPACE_FILTERS$ placeholder in update payload is rejected")
        void bulkOnlyNamespaceFilterRejected() {
            StringBuilder errorString = new StringBuilder();
            validation.validateMetricQueryPattern(
                    NAMESPACE_QUERY,
                    "namespacesForAdditionalLabel",
                    errorString,
                    mockDataSourceInfo);

            assertFalse(errorString.isEmpty(),
                    "Bulk-only $NAMESPACE_FILTERS$ must be rejected in an update payload");
            assertTrue(errorString.toString().contains("$NAMESPACE_FILTERS$"),
                    "Error must name the offending bulk-only placeholder");
        }

        @Test
        @DisplayName("Bulk-only $WORKLOAD_FILTERS$ in a workload query is rejected")
        void bulkOnlyWorkloadFilterRejected() {
            StringBuilder errorString = new StringBuilder();
            validation.validateMetricQueryPattern(
                    WORKLOAD_QUERY,
                    "workloadsForAdditionalLabel",
                    errorString,
                    mockDataSourceInfo);

            assertFalse(errorString.isEmpty(),
                    "Bulk-only $WORKLOAD_FILTERS$ must be rejected in an update payload");
            assertTrue(errorString.toString().contains("$WORKLOAD_FILTERS$"),
                    "Error must name the offending bulk-only placeholder");
        }

        @Test
        @DisplayName("All four bulk-only placeholders in workloadsWithPodLabelFilter query are rejected")
        void allBulkOnlyPlaceholdersInLabelFilterQueryRejected() {
            StringBuilder errorString = new StringBuilder();
            validation.validateMetricQueryPattern(
                    WORKLOAD_WITH_LABEL_QUERY,
                    "workloadsWithPodLabelFilter",
                    errorString,
                    mockDataSourceInfo);

            assertFalse(errorString.isEmpty(),
                    "Bulk-only placeholders must be rejected in workloadsWithPodLabelFilter update payload");
        }

        @Test
        @DisplayName("$UNSUPPORTED_WORKLOAD_TYPES$ alone (without $WORKLOAD_FILTERS$) is not rejected")
        void updatePayloadTemplateVariableAloneIsAccepted() throws Exception {
            // $UNSUPPORTED_WORKLOAD_TYPES$ is in UPDATE_PAYLOAD_PLACEHOLDERS — it must NOT be rejected.
            // This distinguishes a genuinely unresolved bulk placeholder from an allowed template variable.
            DataSourceOperatorImpl mockOperator = mock(DataSourceOperatorImpl.class);
            when(mockOperator.getResultArrayForQuery(any(), anyString()))
                    .thenReturn(buildResultArray("namespace", "workload", "workload_type"));

            try (MockedStatic<DataSourceOperatorImpl> staticMock =
                         mockStatic(DataSourceOperatorImpl.class)) {
                DataSourceOperatorImpl mockImpl = mock(DataSourceOperatorImpl.class);
                staticMock.when(DataSourceOperatorImpl::getInstance).thenReturn(mockImpl);
                when(mockImpl.getOperator(anyString())).thenReturn(mockOperator);

                StringBuilder errorString = new StringBuilder();
                // Only $UNSUPPORTED_WORKLOAD_TYPES$ — no bulk-only $WORKLOAD_FILTERS$
                validation.validateMetricQueryPattern(
                        UPDATE_WORKLOAD_QUERY,
                        "workloadsForAdditionalLabel",
                        errorString,
                        mockDataSourceInfo);

                assertFalse(errorString.toString().contains("bulk-only"),
                        "$UNSUPPORTED_WORKLOAD_TYPES$ must not be reported as a bulk-only placeholder");
            }
        }

        @Test
        @DisplayName("A completely unknown $CLUSTER_FILTERS$ placeholder is rejected with its name in the error")
        void unknownPlaceholderRejected() {
            StringBuilder errorString = new StringBuilder();
            String queryWithUnknown =
                    "sum by (namespace) (avg_over_time(kube_namespace_status_phase" +
                    "{namespace!=\"\", $CLUSTER_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))";
            validation.validateMetricQueryPattern(
                    queryWithUnknown, "namespacesForAdditionalLabel", errorString, mockDataSourceInfo);

            assertFalse(errorString.isEmpty(),
                    "An error must be appended for an unknown placeholder");
            assertTrue(errorString.toString().contains("$CLUSTER_FILTERS$"),
                    "Error must name the unknown placeholder");
            assertTrue(errorString.toString().contains("namespacesForAdditionalLabel"),
                    "Error must reference the metric name");
        }

        @Test
        @DisplayName("A misspelled supported placeholder ($NAMESPACE_FILTER$ missing trailing S) is rejected")
        void misspelledPlaceholderRejected() {
            StringBuilder errorString = new StringBuilder();
            String queryWithTypo =
                    "sum by (namespace) (avg_over_time(kube_namespace_status_phase" +
                    "{$NAMESPACE_FILTER$}[$MEASUREMENT_DURATION_IN_MIN$m]))";
            validation.validateMetricQueryPattern(
                    queryWithTypo, "namespacesForAdditionalLabel", errorString, mockDataSourceInfo);

            assertFalse(errorString.isEmpty(),
                    "An error must be appended for a misspelled placeholder");
            assertTrue(errorString.toString().contains("$NAMESPACE_FILTER$"),
                    "Error must name the misspelled placeholder");
        }
    }

    // =========================================================================
    // Negative test 2: Malformed Prometheus query
    // Verifies that a query with consecutive commas (,,) in a selector is rejected
    // by the validation pipeline (Prometheus returns a 400 syntax error).
    // =========================================================================
    @Nested
    @DisplayName("Negative test 2 — Malformed Prometheus query with double commas is rejected")
    class MalformedQueryTests {

        @Test
        @DisplayName("Query with consecutive commas in selector is rejected with a syntax error message")
        void doubleCommaInSelectorRejected() throws Exception {
            // A double comma {namespace!=\"\",,namespace=~\"...\"} is malformed PromQL.
            // Prometheus responds with HTTP 400 — the validation must catch this and report it.
            String malformedQuery =
                    "sum by (namespace) (avg_over_time(kube_namespace_status_phase" +
                    "{namespace!=\"\",,namespace=~\"openshift-tuning|monitoring\"}" +
                    "[$MEASUREMENT_DURATION_IN_MIN$m]))";

            DataSourceOperatorImpl mockOperator = mock(DataSourceOperatorImpl.class);
            when(mockOperator.getResultArrayForQuery(any(), anyString()))
                    .thenThrow(new java.io.IOException("Unexpected response status: 400"));

            try (MockedStatic<DataSourceOperatorImpl> staticMock =
                         mockStatic(DataSourceOperatorImpl.class)) {
                DataSourceOperatorImpl mockImpl = mock(DataSourceOperatorImpl.class);
                staticMock.when(DataSourceOperatorImpl::getInstance).thenReturn(mockImpl);
                when(mockImpl.getOperator(anyString())).thenReturn(mockOperator);

                StringBuilder errorString = new StringBuilder();
                validation.validateMetricQueryPattern(
                        malformedQuery,
                        "namespacesForAdditionalLabel",
                        errorString,
                        mockDataSourceInfo);

                assertFalse(errorString.isEmpty(),
                        "A malformed query with double commas must produce a validation error");
                assertTrue(errorString.toString().contains("Please review the metric query"),
                        "Error must contain the syntax-error hint message");
                assertTrue(errorString.toString().contains("namespacesForAdditionalLabel"),
                        "Error must reference the metric name");
            }
        }
    }

    // =========================================================================
    // validateMetricQueryPattern — sum by clause validation (entry point)
    // =========================================================================
    @Nested
    @DisplayName("validateMetricQueryPattern — validates sum by clause then delegates to queryValidator")
    class ValidateMetricQueryPatternEntryTests {

        @Test
        @DisplayName("Update-payload query passes through validateMetricQueryPattern without error")
        void updatePayloadQueryNotRejected() throws Exception {
            DataSourceOperatorImpl mockOperator = mock(DataSourceOperatorImpl.class);
            when(mockOperator.getResultArrayForQuery(any(), anyString()))
                    .thenReturn(buildResultArray("namespace"));

            try (MockedStatic<DataSourceOperatorImpl> staticMock =
                         mockStatic(DataSourceOperatorImpl.class)) {
                DataSourceOperatorImpl mockImpl = mock(DataSourceOperatorImpl.class);
                staticMock.when(DataSourceOperatorImpl::getInstance).thenReturn(mockImpl);
                when(mockImpl.getOperator(anyString())).thenReturn(mockOperator);

                StringBuilder errorString = new StringBuilder();
                validation.validateMetricQueryPattern(
                        UPDATE_NAMESPACE_QUERY,
                        "namespacesForAdditionalLabel",
                        errorString,
                        mockDataSourceInfo);

                assertTrue(errorString.isEmpty(),
                        "Update-payload query must pass validateMetricQueryPattern without error");
            }
        }

        @Test
        @DisplayName("Query with invalid sum by clause is rejected with a descriptive error")
        void invalidSumByClauseRejected() {
            StringBuilder errorString = new StringBuilder();
            String badQuery = "avg_over_time(kube_namespace_status_phase{namespace!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m])";
            validation.validateMetricQueryPattern(badQuery, "namespacesForAdditionalLabel", errorString, mockDataSourceInfo);

            assertFalse(errorString.isEmpty(), "An error must be appended for an invalid sum by clause");
            assertTrue(errorString.toString().contains("namespacesForAdditionalLabel"),
                    "Error must reference the metric name");
        }
    }

    // =========================================================================
    // queryValidator — pre-expanded query execution
    // =========================================================================
    @Nested
    @DisplayName("queryValidator — validates update-payload queries against Prometheus")
    class QueryValidatorTests {

        @Test
        @DisplayName("No error appended when Prometheus returns expected identifiers")
        void noErrorWhenResultIdentifiersMatch() throws Exception {
            JsonArray result = buildResultArray("namespace");
            DataSourceOperatorImpl mockOperator = mock(DataSourceOperatorImpl.class);
            when(mockOperator.getResultArrayForQuery(any(), anyString())).thenReturn(result);

            try (MockedStatic<DataSourceOperatorImpl> staticMock =
                         mockStatic(DataSourceOperatorImpl.class)) {
                DataSourceOperatorImpl mockImpl = mock(DataSourceOperatorImpl.class);
                staticMock.when(DataSourceOperatorImpl::getInstance).thenReturn(mockImpl);
                when(mockImpl.getOperator(anyString())).thenReturn(mockOperator);

                StringBuilder errorString = new StringBuilder();
                validation.queryValidator(
                        UPDATE_NAMESPACE_QUERY,
                        "namespacesForAdditionalLabel",
                        errorString,
                        mockDataSourceInfo,
                        AnalyzerConstants.MetadataProfileQueryIdentifier.NAMESPACE_QUERY_IDENTIFIER);

                assertTrue(errorString.isEmpty(),
                        "No error should be appended when result identifiers match");
            }
        }

        @Test
        @DisplayName("Error appended when Prometheus returns empty result array")
        void errorWhenResultIsEmpty() throws Exception {
            DataSourceOperatorImpl mockOperator = mock(DataSourceOperatorImpl.class);
            when(mockOperator.getResultArrayForQuery(any(), anyString())).thenReturn(new JsonArray());

            try (MockedStatic<DataSourceOperatorImpl> staticMock =
                         mockStatic(DataSourceOperatorImpl.class)) {
                DataSourceOperatorImpl mockImpl = mock(DataSourceOperatorImpl.class);
                staticMock.when(DataSourceOperatorImpl::getInstance).thenReturn(mockImpl);
                when(mockImpl.getOperator(anyString())).thenReturn(mockOperator);

                StringBuilder errorString = new StringBuilder();
                validation.queryValidator(
                        UPDATE_NAMESPACE_QUERY,
                        "namespacesForAdditionalLabel",
                        errorString,
                        mockDataSourceInfo,
                        AnalyzerConstants.MetadataProfileQueryIdentifier.NAMESPACE_QUERY_IDENTIFIER);

                assertFalse(errorString.isEmpty(),
                        "An error should be appended when the result array is empty");
            }
        }

        @Test
        @DisplayName("QUERY_SYNTAX_ERROR_MESSAGE appended when operator throws IOException")
        void syntaxErrorMessageAppendedOnIOException() throws Exception {
            DataSourceOperatorImpl mockOperator = mock(DataSourceOperatorImpl.class);
            when(mockOperator.getResultArrayForQuery(any(), anyString()))
                    .thenThrow(new java.io.IOException("Unexpected response status: 400"));

            try (MockedStatic<DataSourceOperatorImpl> staticMock =
                         mockStatic(DataSourceOperatorImpl.class)) {
                DataSourceOperatorImpl mockImpl = mock(DataSourceOperatorImpl.class);
                staticMock.when(DataSourceOperatorImpl::getInstance).thenReturn(mockImpl);
                when(mockImpl.getOperator(anyString())).thenReturn(mockOperator);

                StringBuilder errorString = new StringBuilder();
                validation.queryValidator(
                        UPDATE_NAMESPACE_QUERY,
                        "namespacesForAdditionalLabel",
                        errorString,
                        mockDataSourceInfo,
                        AnalyzerConstants.MetadataProfileQueryIdentifier.NAMESPACE_QUERY_IDENTIFIER);

                String error = errorString.toString();
                assertTrue(error.contains("namespacesForAdditionalLabel"),
                        "Error message must contain the metric name");
                assertTrue(error.contains("Please review the metric query"),
                        "Error message must contain the syntax error hint");
            }
        }
    }

    // =========================================================================
    // appendFiltersToQuery — filter injection behaviour
    // =========================================================================
    @Nested
    @DisplayName("appendFiltersToQuery — strips leading comma for empty selector, appends verbatim otherwise")
    class AppendFiltersEmptySelectorTests {

        @Test
        @DisplayName("Leading comma is stripped when selector is empty, producing valid PromQL")
        void leadingCommaStrippedForEmptySelector() {
            String query = "sum by (namespace) (avg_over_time(kube_namespace_status_phase{}[$MEASUREMENT_DURATION_IN_MIN$m]))";
            String filterWithLeadingComma = ", namespace=~\"openshift-tuning|monitoring\"";

            String result = MetadataProfileUtil.appendFiltersToQuery(query, filterWithLeadingComma);

            assertFalse(result.contains("{,"),
                    "Result must not contain a leading comma inside the selector");
            assertTrue(result.contains("{namespace=~"),
                    "Filter must be present without a leading comma");
        }

        @Test
        @DisplayName("Filter with leading comma is appended after existing selector content")
        void leadingCommaKeptWhenSelectorNotEmpty() {
            String query = "sum by (namespace) (avg_over_time(kube_namespace_status_phase{namespace!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]))";
            String filterWithLeadingComma = ", namespace=~\"openshift-tuning|monitoring\"";

            String result = MetadataProfileUtil.appendFiltersToQuery(query, filterWithLeadingComma);

            assertTrue(result.contains("namespace!=\"\", namespace=~"),
                    "Comma separator must be preserved between existing content and new filter");
        }

        @Test
        @DisplayName("queryValidator passes the query to Prometheus (filter injection is handled by caller)")
        void queryValidatorPassesQueryToPrometheus() throws Exception {
            JsonArray result = buildResultArray("namespace");
            DataSourceOperatorImpl mockOperator = mock(DataSourceOperatorImpl.class);
            ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
            when(mockOperator.getResultArrayForQuery(any(), queryCaptor.capture())).thenReturn(result);

            try (MockedStatic<DataSourceOperatorImpl> staticMock =
                         mockStatic(DataSourceOperatorImpl.class)) {
                DataSourceOperatorImpl mockImpl = mock(DataSourceOperatorImpl.class);
                staticMock.when(DataSourceOperatorImpl::getInstance).thenReturn(mockImpl);
                when(mockImpl.getOperator(anyString())).thenReturn(mockOperator);

                StringBuilder errorString = new StringBuilder();
                validation.queryValidator(
                        UPDATE_NAMESPACE_QUERY,
                        "namespacesForAdditionalLabel",
                        errorString,
                        mockDataSourceInfo,
                        AnalyzerConstants.MetadataProfileQueryIdentifier.NAMESPACE_QUERY_IDENTIFIER);

                String capturedQuery = queryCaptor.getValue();
                assertTrue(capturedQuery.contains("namespace"),
                        "Captured query must contain the namespace filter from the original query");
                assertTrue(errorString.isEmpty(),
                        "No error should be appended when result identifiers match");
            }
        }
    }

    // =========================================================================
    // appendFiltersToQuery — filter injected into the first selector
    // =========================================================================
    @Nested
    @DisplayName("appendFiltersToQuery — filter is appended inside the first selector of the query")
    class AppendFiltersToQueryTests {

        @Test
        @DisplayName("Filter is injected into a single empty-selector query, leading comma stripped")
        void filterInjectedIntoSingleSelector() {
            String query = "sum by (namespace, workload, workload_type) (avg_over_time(some_metric{}[$MEASUREMENT_DURATION_IN_MIN$m]))";
            String filter = ", workload=\"kruize\"";

            String result = MetadataProfileUtil.appendFiltersToQuery(query, filter);

            assertTrue(result.contains("some_metric{workload=\"kruize\"}"),
                    "Filter must be appended inside the first selector without a leading comma");
        }

        @Test
        @DisplayName("Filter is injected into the first selector of a joined query, leading comma stripped")
        void filterInjectedIntoFirstSelectorOfJoinedQuery() {
            String query =
                    "sum by (namespace, workload, workload_type) " +
                    "(max_over_time(kube_pod_labels{}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (namespace, pod) " +
                    "group_left(workload, workload_type) " +
                    "max_over_time(namespace_workload_pod:kube_pod_owner:relabel{}[$MEASUREMENT_DURATION_IN_MIN$m]))";
            String filter = ", namespace!=\"\"";

            String result = MetadataProfileUtil.appendFiltersToQuery(query, filter);

            assertTrue(result.contains("kube_pod_labels{namespace!=\"\"}"),
                    "Filter must be appended into the first (kube_pod_labels) selector without a leading comma");
            assertTrue(result.contains("namespace_workload_pod:kube_pod_owner:relabel{}"),
                    "Second selector must be untouched");
        }

        @Test
        @DisplayName("workloadsWithPodLabelFilter: query already contains workload= so no extra filter is injected via queryValidator")
        void workloadsWithLabelFilterQueryWorkloadInMatchingSelector() throws Exception {
            JsonArray result = buildResultArray("namespace", "workload", "workload_type");
            DataSourceOperatorImpl mockOperator = mock(DataSourceOperatorImpl.class);
            ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
            when(mockOperator.getResultArrayForQuery(any(), queryCaptor.capture())).thenReturn(result);

            try (MockedStatic<DataSourceOperatorImpl> staticMock =
                         mockStatic(DataSourceOperatorImpl.class)) {
                DataSourceOperatorImpl mockImpl = mock(DataSourceOperatorImpl.class);
                staticMock.when(DataSourceOperatorImpl::getInstance).thenReturn(mockImpl);
                when(mockImpl.getOperator(anyString())).thenReturn(mockOperator);

                StringBuilder errorString = new StringBuilder();
                validation.queryValidator(
                        UPDATE_WORKLOAD_WITH_LABEL_QUERY,
                        AnalyzerConstants.WORKLOAD_METADATA_QUERY_WITH_LABEL_FILTER,
                        errorString,
                        mockDataSourceInfo,
                        AnalyzerConstants.MetadataProfileQueryIdentifier.WORKLOAD_QUERY_IDENTIFIER);

                String capturedQuery = queryCaptor.getValue();
                assertTrue(capturedQuery.contains("workload="),
                        "Captured query must still contain workload= from the original update-payload query");
                assertTrue(errorString.isEmpty(),
                        "No error should be appended when result identifiers match");
            }
        }

        @Test
        @DisplayName("Null or empty inputs return query unchanged")
        void nullOrEmptyInputsReturnQueryUnchanged() {
            String query = "sum by (namespace) (avg_over_time(some_metric{namespace!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]))";
            assertEquals(query, MetadataProfileUtil.appendFiltersToQuery(query, null));
            assertEquals(query, MetadataProfileUtil.appendFiltersToQuery(query, ""));
            assertNull(MetadataProfileUtil.appendFiltersToQuery(null, ", filter"));
        }
    }

    // =========================================================================
    // findSelectorCloseBrace — quote-aware brace scanning
    // =========================================================================
    @Nested
    @DisplayName("findSelectorCloseBrace — finds closing brace respecting quoted label values")
    class FindSelectorCloseBraceTests {

        @Test
        @DisplayName("Returns index of plain closing brace in a simple selector")
        void simpleSelectorCloseBrace() {
            String query = "some_metric{namespace!=\"\", workload=\"kruize\"}[$MEASUREMENT_DURATION_IN_MIN$m]";
            int openBrace = query.indexOf("{");
            int close = MetadataProfileUtil.findSelectorCloseBrace(query, openBrace + 1);
            assertEquals(query.indexOf("}"), close, "Must find the real closing brace");
        }

        @Test
        @DisplayName("Skips } inside a double-quoted label value")
        void skipsCloseBraceInsideQuotedValue() {
            String query = "some_metric{workload_type!~\"(?i)foo{bar}\"}[$MEASUREMENT_DURATION_IN_MIN$m]";
            int openBrace = query.indexOf("{");
            int close = MetadataProfileUtil.findSelectorCloseBrace(query, openBrace + 1);
            int expected = query.lastIndexOf("}");
            assertEquals(expected, close, "Must skip the } inside the quoted regex value");
        }

        @Test
        @DisplayName("Handles escaped quote inside label value")
        void handlesEscapedQuoteInsideValue() {
            String query = "some_metric{label=\"val\\\"ue\"}[$MEASUREMENT_DURATION_IN_MIN$m]";
            int openBrace = query.indexOf("{");
            int close = MetadataProfileUtil.findSelectorCloseBrace(query, openBrace + 1);
            int expected = query.lastIndexOf("}");
            assertEquals(expected, close, "Must handle escaped quote and find the real closing brace");
        }

        @Test
        @DisplayName("Returns -1 when no closing brace is found")
        void returnsMinusOneWhenNoBrace() {
            String query = "some_metric{label=\"value\"";
            int openBrace = query.indexOf("{");
            int close = MetadataProfileUtil.findSelectorCloseBrace(query, openBrace + 1);
            assertEquals(-1, close, "Must return -1 when no closing brace exists");
        }
    }

    // =========================================================================
    // appendFiltersToMatchingSelector — quote-aware selector targeting
    // =========================================================================
    @Nested
    @DisplayName("appendFiltersToMatchingSelector — correctly handles quoted label values containing braces")
    class AppendFiltersMatchingSelectorQuotedBraceTests {

        @Test
        @DisplayName("Filter is inserted correctly when a label value contains a } inside quotes")
        void filterInsertedCorrectlyWithQuotedBraceInValue() {
            String query =
                    "sum by (namespace, workload, workload_type) " +
                    "(avg_over_time(namespace_workload_pod:kube_pod_owner:relabel" +
                    "{workload_type!~\"(?i)Deployment{Config}\"}[$MEASUREMENT_DURATION_IN_MIN$m]))";
            String filter = ", workload=\"kruize\"";

            String result = MetadataProfileUtil.appendFiltersToMatchingSelector(
                    query, AnalyzerConstants.WORKLOAD, filter);

            assertTrue(result.contains("workload_type!~\"(?i)Deployment{Config}\", workload=\"kruize\""),
                    "Filter must be appended after the existing content, not inserted inside the quoted value");
            assertTrue(result.contains("workload_type!~\"(?i)Deployment{Config}\", workload=\"kruize\"}"),
                    "Selector must close correctly after the injected filter");
        }

        @Test
        @DisplayName("Quoted } in a later selector does not corrupt the result of filter injection")
        void quotedBraceInLaterSelectorDoesNotCorruptResult() {
            String query =
                    "sum by (namespace, workload, workload_type) " +
                    "(max_over_time(kube_pod_labels{namespace!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (namespace, pod) " +
                    "group_left(workload, workload_type) " +
                    "max_over_time(namespace_workload_pod:kube_pod_owner:relabel" +
                    "{workload_type!~\"(?i)foo{bar}\"}[$MEASUREMENT_DURATION_IN_MIN$m]))";
            String filter = ", workload=\"kruize\"";

            String result = MetadataProfileUtil.appendFiltersToMatchingSelector(
                    query, AnalyzerConstants.WORKLOAD, filter);

            assertTrue(result.contains("workload_type!~\"(?i)foo{bar}\"}"),
                    "Quoted } inside regex value in the second selector must remain intact");
            assertFalse(result.isEmpty(), "Result must not be empty");
        }
    }

    // =========================================================================
    // validateMetricQueryPattern — sum by clause validation
    // =========================================================================
    @Nested
    @DisplayName("validateMetricQueryPattern — sum by clause validation")
    class ValidateMetricQueryPatternTests {

        @Test
        @DisplayName("Valid update-payload namespace query passes sum-by check without error")
        void validNamespaceQueryNoError() throws Exception {
            DataSourceOperatorImpl mockOperator = mock(DataSourceOperatorImpl.class);
            when(mockOperator.getResultArrayForQuery(any(), anyString()))
                    .thenReturn(buildResultArray("namespace"));

            try (MockedStatic<DataSourceOperatorImpl> staticMock =
                         mockStatic(DataSourceOperatorImpl.class)) {
                DataSourceOperatorImpl mockImpl = mock(DataSourceOperatorImpl.class);
                staticMock.when(DataSourceOperatorImpl::getInstance).thenReturn(mockImpl);
                when(mockImpl.getOperator(anyString())).thenReturn(mockOperator);

                StringBuilder errorString = new StringBuilder();
                validation.validateMetricQueryPattern(
                        UPDATE_NAMESPACE_QUERY,
                        "namespacesForAdditionalLabel",
                        errorString,
                        mockDataSourceInfo);

                assertTrue(errorString.isEmpty(),
                        "No error should be appended for a valid update-payload namespace query");
            }
        }

        @Test
        @DisplayName("Query with unsupported sum-by clause appends INVALID_SUM_BY_CLAUSE error")
        void invalidSumByClauseAppendsError() {
            String badQuery = "sum by (foo, bar) (some_metric{namespace!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m])";
            StringBuilder errorString = new StringBuilder();

            validation.validateMetricQueryPattern(
                    badQuery,
                    "namespacesForAdditionalLabel",
                    errorString,
                    mockDataSourceInfo);

            assertFalse(errorString.isEmpty(),
                    "An error must be appended for an unrecognised sum-by clause");
            assertTrue(errorString.toString().contains("namespacesForAdditionalLabel"),
                    "Error must reference the metric name");
        }

        @Test
        @DisplayName("Query with no sum by clause at all appends error")
        void noSumByClauseAppendsError() {
            String noSumByQuery = "avg_over_time(kube_namespace_status_phase{namespace!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m])";
            StringBuilder errorString = new StringBuilder();

            validation.validateMetricQueryPattern(
                    noSumByQuery,
                    "namespacesForAdditionalLabel",
                    errorString,
                    mockDataSourceInfo);

            assertFalse(errorString.isEmpty(),
                    "An error must be appended when there is no sum by clause");
        }
    }
}
