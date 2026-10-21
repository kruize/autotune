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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MetadataProfileValidation.
 *
 * Covers:
 * - validateMetricQueryPattern(): rejects queries containing unresolved $...$ bulk-filter
 *   placeholders with a descriptive 400 error; the updateMetadata API requires pre-expanded queries.
 * - queryValidator(): validates pre-expanded queries against Prometheus and verifies result identifiers.
 * - matchSumByClause(): verifies each MetadataProfileQueryIdentifier is correctly matched.
 * - appendFiltersToQuery() / appendFiltersToMatchingSelector(): utility method contracts.
 */
class MetadataProfileValidationTest {

    private MetadataProfileValidation validation;
    private DataSourceInfo mockDataSourceInfo;

    // ---- Bulk-profile queries (contain $...$ placeholders) ----
    // These are valid in bulk metadata profiles used at data-gathering time,
    // but must be REJECTED by the updateMetadata API.
    private static final String BULK_NAMESPACE_QUERY =
            "sum by (namespace) (avg_over_time(kube_namespace_status_phase{$NAMESPACE_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))";

    private static final String BULK_WORKLOAD_QUERY =
            "sum by (namespace, workload, workload_type) (avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ $WORKLOAD_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))";

    private static final String BULK_WORKLOAD_WITH_LABEL_QUERY =
            "sum by (namespace, workload, workload_type) (max_over_time(kube_pod_labels{$NAMESPACE_FILTERS$ $CONTAINER_FILTERS$ $LABELS$}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (namespace, pod) group_left(workload, workload_type) max_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ $WORKLOAD_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))";

    private static final String BULK_CONTAINER_QUERY =
            "sum by (container, image, workload, workload_type, namespace) (avg_over_time(kube_pod_container_info{$NAMESPACE_FILTERS$ $CONTAINER_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (pod, namespace) group_left(workload, workload_type) avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ $WORKLOAD_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))";

    // ---- Pre-expanded queries (valid updateMetadata payload) ----
    // These mirror update_cluster_metadata_local_monitoring.json.
    private static final String NAMESPACE_QUERY =
            "sum by (namespace) (avg_over_time(kube_namespace_status_phase{namespace!=\"\", namespace=~\"openshift-tuning|monitoring\"}[15m]))";

    private static final String WORKLOAD_QUERY =
            "sum by (namespace, workload, workload_type) (avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=\"\", workload=\"kruize\"}[15m]))";

    private static final String WORKLOAD_WITH_LABEL_QUERY =
            "sum by (namespace, workload, workload_type) (max_over_time(kube_pod_labels{namespace!=\"\"}[15m]) * on (namespace, pod) group_left(workload, workload_type) max_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=\"\", workload=\"kruize\"}[15m]))";

    private static final String CONTAINER_QUERY =
            "sum by (container, image, workload, workload_type, namespace) (avg_over_time(kube_pod_container_info{container!=\"\", container=\"kruize\"}[15m]) * on (pod, namespace) group_left(workload, workload_type) avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=\"\"}[15m]))";

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
                    MetadataProfileUtil.matchSumByClause(NAMESPACE_QUERY);
            assertEquals(AnalyzerConstants.MetadataProfileQueryIdentifier.NAMESPACE_QUERY_IDENTIFIER, result);
        }

        @Test
        @DisplayName("Workload query maps to WORKLOAD_QUERY_IDENTIFIER")
        void workloadQueryIdentifier() {
            AnalyzerConstants.MetadataProfileQueryIdentifier result =
                    MetadataProfileUtil.matchSumByClause(WORKLOAD_QUERY);
            assertEquals(AnalyzerConstants.MetadataProfileQueryIdentifier.WORKLOAD_QUERY_IDENTIFIER, result);
        }

        @Test
        @DisplayName("WorkloadWithPodLabelFilter query maps to WORKLOAD_QUERY_IDENTIFIER")
        void workloadWithLabelQueryIdentifier() {
            AnalyzerConstants.MetadataProfileQueryIdentifier result =
                    MetadataProfileUtil.matchSumByClause(WORKLOAD_WITH_LABEL_QUERY);
            assertEquals(AnalyzerConstants.MetadataProfileQueryIdentifier.WORKLOAD_QUERY_IDENTIFIER, result);
        }

        @Test
        @DisplayName("Container query maps to CONTAINER_QUERY_IDENTIFIER")
        void containerQueryIdentifier() {
            AnalyzerConstants.MetadataProfileQueryIdentifier result =
                    MetadataProfileUtil.matchSumByClause(CONTAINER_QUERY);
            assertEquals(AnalyzerConstants.MetadataProfileQueryIdentifier.CONTAINER_QUERY_IDENTIFIER, result);
        }

        @Test
        @DisplayName("Query without sum by clause returns null")
        void noSumByReturnsNull() {
            String query = "avg_over_time(kube_namespace_status_phase[15m])";
            assertNull(MetadataProfileUtil.matchSumByClause(query));
        }

        @Test
        @DisplayName("Query with unrecognised sum by identifiers returns null")
        void unknownIdentifiersReturnNull() {
            String query = "sum by (foo, bar) (some_metric[15m])";
            assertNull(MetadataProfileUtil.matchSumByClause(query));
        }
    }

    // =========================================================================
    // validateMetricQueryPattern — bulk placeholder rejection
    // =========================================================================
    @Nested
    @DisplayName("validateMetricQueryPattern — bulk-filter placeholders are rejected with a descriptive error")
    class UnresolvedPlaceholderRejectionTests {

        private void assertPlaceholderRejected(String bulkQuery, String metricName) {
            StringBuilder errorString = new StringBuilder();
            validation.validateMetricQueryPattern(bulkQuery, metricName, errorString, mockDataSourceInfo);

            assertFalse(errorString.isEmpty(),
                    "An error must be appended when the query contains unresolved placeholders");
            assertTrue(errorString.toString().contains(metricName),
                    "Error must reference the metric name");
            assertTrue(errorString.toString().contains("$"),
                    "Error must mention at least one placeholder token");
        }

        @Test
        @DisplayName("namespacesForAdditionalLabel bulk query is rejected: contains $NAMESPACE_FILTERS$")
        void namespacesBulkQueryRejected() {
            assertPlaceholderRejected(BULK_NAMESPACE_QUERY, AnalyzerConstants.NAMESPACE_METADATA_QUERY);
        }

        @Test
        @DisplayName("workloadsForAdditionalLabel bulk query is rejected: contains $WORKLOAD_FILTERS$")
        void workloadsBulkQueryRejected() {
            assertPlaceholderRejected(BULK_WORKLOAD_QUERY, AnalyzerConstants.WORKLOAD_METADATA_QUERY);
        }

        @Test
        @DisplayName("workloadsWithPodLabelFilter bulk query is rejected: contains $NAMESPACE_FILTERS$, $LABELS$")
        void workloadsWithLabelBulkQueryRejected() {
            assertPlaceholderRejected(BULK_WORKLOAD_WITH_LABEL_QUERY, AnalyzerConstants.WORKLOAD_METADATA_QUERY_WITH_LABEL_FILTER);
        }

        @Test
        @DisplayName("containersForAdditionalLabel bulk query is rejected: contains $CONTAINER_FILTERS$")
        void containersBulkQueryRejected() {
            assertPlaceholderRejected(BULK_CONTAINER_QUERY, AnalyzerConstants.CONTAINER_METADATA_QUERY);
        }

        @Test
        @DisplayName("Pre-expanded query without placeholders is NOT rejected")
        void preExpandedQueryNotRejected() throws Exception {
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
                        NAMESPACE_QUERY,
                        AnalyzerConstants.NAMESPACE_METADATA_QUERY,
                        errorString,
                        mockDataSourceInfo);

                assertTrue(errorString.isEmpty(),
                        "Pre-expanded query must not be rejected by placeholder check");
            }
        }

        @Test
        @DisplayName("Only $MEASUREMENT_DURATION_IN_MIN$ placeholder is also rejected")
        void measurementDurationPlaceholderRejected() {
            String queryWithDuration =
                    "sum by (namespace) (avg_over_time(kube_namespace_status_phase{namespace!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]))";
            assertPlaceholderRejected(queryWithDuration, AnalyzerConstants.NAMESPACE_METADATA_QUERY);
        }
    }

    // =========================================================================
    // queryValidator — pre-expanded query execution
    // =========================================================================
    @Nested
    @DisplayName("queryValidator — validates pre-expanded queries against Prometheus")
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
                        NAMESPACE_QUERY,
                        AnalyzerConstants.NAMESPACE_METADATA_QUERY,
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
                        NAMESPACE_QUERY,
                        AnalyzerConstants.NAMESPACE_METADATA_QUERY,
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
                        NAMESPACE_QUERY,
                        AnalyzerConstants.NAMESPACE_METADATA_QUERY,
                        errorString,
                        mockDataSourceInfo,
                        AnalyzerConstants.MetadataProfileQueryIdentifier.NAMESPACE_QUERY_IDENTIFIER);

                String error = errorString.toString();
                assertTrue(error.contains(AnalyzerConstants.NAMESPACE_METADATA_QUERY),
                        "Error message must contain the metric name");
                assertTrue(error.contains("Please review the metric query"),
                        "Error message must contain the syntax error hint");
            }
        }
    }

    // =========================================================================
    // appendFiltersToQuery — no leading comma when selector is empty
    // =========================================================================
    @Nested
    @DisplayName("appendFiltersToQuery — no leading comma when selector is empty")
    class AppendFiltersEmptySelectorTests {

        @Test
        @DisplayName("Filter with leading comma is stripped when selector is empty")
        void leadingCommaStrippedForEmptySelector() {
            String query = "sum by (namespace) (avg_over_time(kube_namespace_status_phase{}[15m]))";
            String filterWithLeadingComma = ", namespace=~\"openshift-tuning|monitoring\"";

            String result = MetadataProfileUtil.appendFiltersToQuery(query, filterWithLeadingComma);

            assertFalse(result.contains("{,"),
                    "Result must not contain a leading comma inside the selector");
            assertTrue(result.contains("{namespace=~"),
                    "Filter must be present without a leading comma");
        }

        @Test
        @DisplayName("Filter with leading comma is kept as-is when selector already has content")
        void leadingCommaKeptWhenSelectorNotEmpty() {
            String query = "sum by (namespace) (avg_over_time(kube_namespace_status_phase{namespace!=\"\"}[15m]))";
            String filterWithLeadingComma = ", namespace=~\"openshift-tuning|monitoring\"";

            String result = MetadataProfileUtil.appendFiltersToQuery(query, filterWithLeadingComma);

            assertTrue(result.contains("namespace!=\"\", namespace=~"),
                    "Comma separator must be preserved between existing content and new filter");
        }

        @Test
        @DisplayName("Pre-expanded namespace query with empty selector injects filter without a leading comma")
        void preExpandedEmptySelectorNoLeadingComma() throws Exception {
            // A pre-expanded query whose selector is bare {} should get the filter injected
            // without a leading comma: {namespace=~"..."} not {, namespace=~"..."}.
            String queryWithEmptySelector =
                    "sum by (namespace) (avg_over_time(kube_namespace_status_phase{}[15m]))";
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
                        queryWithEmptySelector,
                        AnalyzerConstants.NAMESPACE_METADATA_QUERY,
                        errorString,
                        mockDataSourceInfo,
                        AnalyzerConstants.MetadataProfileQueryIdentifier.NAMESPACE_QUERY_IDENTIFIER);

                String capturedQuery = queryCaptor.getValue();
                assertFalse(capturedQuery.contains("{,"),
                        "No leading comma must appear inside the selector after filter injection");
            }
        }
    }

    // =========================================================================
    // appendFiltersToMatchingSelector — structure-based selector targeting
    // =========================================================================
    @Nested
    @DisplayName("appendFiltersToMatchingSelector — filter targets selector whose metric name contains the identifier")
    class AppendFiltersMatchingSelectorTests {

        @Test
        @DisplayName("workload= filter goes into namespace_workload_pod selector, not kube_pod_labels")
        void workloadFilterGoesIntoMatchingSelector() {
            // namespace_workload_pod:kube_pod_owner:relabel contains "workload" in its name;
            // kube_pod_labels does not.
            String query =
                    "sum by (namespace, workload, workload_type) " +
                    "(max_over_time(kube_pod_labels{}[15m]) * on (namespace, pod) " +
                    "group_left(workload, workload_type) " +
                    "max_over_time(namespace_workload_pod:kube_pod_owner:relabel{}[15m]))";
            String filter = ", workload=\"kruize\"";

            String result = MetadataProfileUtil.appendFiltersToMatchingSelector(query, AnalyzerConstants.WORKLOAD, filter);

            assertTrue(result.contains("kube_pod_labels{}"),
                    "First selector (kube_pod_labels) must be untouched");
            assertTrue(result.contains("namespace_workload_pod:kube_pod_owner:relabel{workload=\"kruize\"}"),
                    "Matching selector must contain workload=\"kruize\"");
        }

        @Test
        @DisplayName("workloadsWithPodLabelFilter: workload= filter reaches the namespace_workload_pod selector via queryValidator")
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
                        WORKLOAD_WITH_LABEL_QUERY,
                        AnalyzerConstants.WORKLOAD_METADATA_QUERY_WITH_LABEL_FILTER,
                        errorString,
                        mockDataSourceInfo,
                        AnalyzerConstants.MetadataProfileQueryIdentifier.WORKLOAD_QUERY_IDENTIFIER);

                String capturedQuery = queryCaptor.getValue();
                int firstOpen = capturedQuery.indexOf("{");
                int firstClose = capturedQuery.indexOf("}", firstOpen);
                String firstSelector = capturedQuery.substring(firstOpen + 1, firstClose);
                assertFalse(firstSelector.contains("workload="),
                        "workload= must NOT be injected into the first (kube_pod_labels) selector");
                int secondOpen = capturedQuery.indexOf("{", firstClose + 1);
                int secondClose = capturedQuery.indexOf("}", secondOpen);
                String secondSelector = capturedQuery.substring(secondOpen + 1, secondClose);
                assertTrue(secondSelector.contains("workload="),
                        "workload= must be injected into the matching (namespace_workload_pod) selector");
                assertTrue(errorString.isEmpty(),
                        "No error should be appended when result identifiers match");
            }
        }

        @Test
        @DisplayName("A hypothetical joined workload query with a different name also routes correctly")
        void arbitraryJoinedWorkloadQueryRoutesCorrectly() {
            // Simulates a future metric name that is not workloadsWithPodLabelFilter but still
            // uses a joined query where "workload" appears in the second metric's name.
            String query =
                    "sum by (namespace, workload, workload_type) " +
                    "(max_over_time(some_pod_metric{}[15m]) * on (namespace, pod) " +
                    "group_left(workload, workload_type) " +
                    "max_over_time(namespace_workload_pod:some_other_relabel{}[15m]))";
            String filter = ", workload=\"kruize\"";

            String result = MetadataProfileUtil.appendFiltersToMatchingSelector(query, AnalyzerConstants.WORKLOAD, filter);

            assertTrue(result.contains("some_pod_metric{}"),
                    "First selector must be untouched");
            assertTrue(result.contains("namespace_workload_pod:some_other_relabel{workload=\"kruize\"}"),
                    "Matching selector must receive the filter regardless of the calling metric name");
        }

        @Test
        @DisplayName("Falls back to first selector when no metric name contains the identifier")
        void fallsBackToFirstSelectorWhenNoMatch() {
            String query = "sum by (namespace, workload, workload_type) (avg_over_time(some_metric{}[15m]))";
            String filter = ", workload=\"kruize\"";

            String result = MetadataProfileUtil.appendFiltersToMatchingSelector(query, AnalyzerConstants.WORKLOAD, filter);

            assertTrue(result.contains("some_metric{workload=\"kruize\"}"),
                    "Must fall back to the first selector when no metric name contains the identifier");
        }
    }

    // =========================================================================
    // validateMetricQueryPattern — sum by clause validation
    // =========================================================================
    @Nested
    @DisplayName("validateMetricQueryPattern — sum by clause validation")
    class ValidateMetricQueryPatternTests {

        @Test
        @DisplayName("Valid pre-expanded namespace query passes sum-by check without error")
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
                        NAMESPACE_QUERY,
                        AnalyzerConstants.NAMESPACE_METADATA_QUERY,
                        errorString,
                        mockDataSourceInfo);

                assertTrue(errorString.isEmpty(),
                        "No error should be appended for a valid pre-expanded namespace query");
            }
        }

        @Test
        @DisplayName("Query with unsupported sum-by clause appends INVALID_SUM_BY_CLAUSE error")
        void invalidSumByClauseAppendsError() {
            String badQuery = "sum by (foo, bar) (some_metric{namespace!=\"\"}[15m])";
            StringBuilder errorString = new StringBuilder();

            validation.validateMetricQueryPattern(
                    badQuery,
                    AnalyzerConstants.NAMESPACE_METADATA_QUERY,
                    errorString,
                    mockDataSourceInfo);

            assertFalse(errorString.isEmpty(),
                    "An error must be appended for an unrecognised sum-by clause");
            assertTrue(errorString.toString().contains(AnalyzerConstants.NAMESPACE_METADATA_QUERY),
                    "Error must reference the metric name");
        }

        @Test
        @DisplayName("Query with no sum by clause at all appends error")
        void noSumByClauseAppendsError() {
            String noSumByQuery = "avg_over_time(kube_namespace_status_phase{namespace!=\"\"}[15m])";
            StringBuilder errorString = new StringBuilder();

            validation.validateMetricQueryPattern(
                    noSumByQuery,
                    AnalyzerConstants.NAMESPACE_METADATA_QUERY,
                    errorString,
                    mockDataSourceInfo);

            assertFalse(errorString.isEmpty(),
                    "An error must be appended when there is no sum by clause");
        }
    }
}
