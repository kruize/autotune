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
 * - validateMetricQueryPattern(): validates the sum by clause and delegates to queryValidator.
 * - queryValidator(): substitutes $MEASUREMENT_DURATION_IN_MIN$, $UNSUPPORTED_WORKLOAD_TYPES$,
 *   and ADDITIONAL_LABEL before running the query against Prometheus, then verifies result identifiers.
 * - matchSumByClause(): verifies each MetadataProfileQueryIdentifier is correctly matched.
 * - appendFiltersToQuery(): appends a filter string verbatim into the first selector of a query.
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
    // validateMetricQueryPattern — sum by clause validation (entry point)
    // =========================================================================
    @Nested
    @DisplayName("validateMetricQueryPattern — validates sum by clause then delegates to queryValidator")
    class ValidateMetricQueryPatternEntryTests {

        @Test
        @DisplayName("Pre-expanded query passes through validateMetricQueryPattern without error")
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
                        "Pre-expanded query must pass validateMetricQueryPattern without error");
            }
        }

        @Test
        @DisplayName("Query with invalid sum by clause is rejected with a descriptive error")
        void invalidSumByClauseRejected() {
            StringBuilder errorString = new StringBuilder();
            String badQuery = "avg_over_time(kube_namespace_status_phase{namespace!=\"\"}[15m])";
            validation.validateMetricQueryPattern(badQuery, AnalyzerConstants.NAMESPACE_METADATA_QUERY, errorString, mockDataSourceInfo);

            assertFalse(errorString.isEmpty(), "An error must be appended for an invalid sum by clause");
            assertTrue(errorString.toString().contains(AnalyzerConstants.NAMESPACE_METADATA_QUERY),
                    "Error must reference the metric name");
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
    // appendFiltersToQuery — filter injection behaviour
    // =========================================================================
    @Nested
    @DisplayName("appendFiltersToQuery — appends filter string verbatim into the first selector")
    class AppendFiltersEmptySelectorTests {

        @Test
        @DisplayName("Filter string is appended verbatim into an empty selector")
        void filterAppendedVerbatimIntoEmptySelector() {
            String query = "sum by (namespace) (avg_over_time(kube_namespace_status_phase{}[15m]))";
            String filterWithLeadingComma = ", namespace=~\"openshift-tuning|monitoring\"";

            String result = MetadataProfileUtil.appendFiltersToQuery(query, filterWithLeadingComma);

            // appendFiltersToQuery appends the filter string as-is; callers are responsible
            // for ensuring the filter string is valid PromQL in context.
            assertTrue(result.contains(filterWithLeadingComma),
                    "Filter string must appear verbatim in the result");
        }

        @Test
        @DisplayName("Filter with leading comma is appended after existing selector content")
        void leadingCommaKeptWhenSelectorNotEmpty() {
            String query = "sum by (namespace) (avg_over_time(kube_namespace_status_phase{namespace!=\"\"}[15m]))";
            String filterWithLeadingComma = ", namespace=~\"openshift-tuning|monitoring\"";

            String result = MetadataProfileUtil.appendFiltersToQuery(query, filterWithLeadingComma);

            assertTrue(result.contains("namespace!=\"\", namespace=~"),
                    "Comma separator must be preserved between existing content and new filter");
        }

        @Test
        @DisplayName("queryValidator passes the query to Prometheus (filter injection is handled by caller)")
        void queryValidatorPassesQueryToPrometheus() throws Exception {
            // When the query already contains the metric identifier, queryValidator passes it
            // to Prometheus without modification (other than template variable substitution).
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
                        NAMESPACE_QUERY,
                        AnalyzerConstants.NAMESPACE_METADATA_QUERY,
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
        @DisplayName("Filter is injected verbatim into a single-selector query")
        void filterInjectedIntoSingleSelector() {
            String query = "sum by (namespace, workload, workload_type) (avg_over_time(some_metric{}[15m]))";
            String filter = ", workload=\"kruize\"";

            String result = MetadataProfileUtil.appendFiltersToQuery(query, filter);

            // appendFiltersToQuery appends verbatim, so {}{filter} → {, filter}
            assertTrue(result.contains("some_metric{, workload=\"kruize\"}"),
                    "Filter must be appended verbatim inside the first selector");
        }

        @Test
        @DisplayName("Filter is injected verbatim into the first selector of a joined query")
        void filterInjectedIntoFirstSelectorOfJoinedQuery() {
            String query =
                    "sum by (namespace, workload, workload_type) " +
                    "(max_over_time(kube_pod_labels{}[15m]) * on (namespace, pod) " +
                    "group_left(workload, workload_type) " +
                    "max_over_time(namespace_workload_pod:kube_pod_owner:relabel{}[15m]))";
            String filter = ", namespace!=\"\"";

            String result = MetadataProfileUtil.appendFiltersToQuery(query, filter);

            // Filter goes verbatim into the first selector only
            assertTrue(result.contains("kube_pod_labels{, namespace!=\"\"}"),
                    "Filter must be appended verbatim into the first (kube_pod_labels) selector");
            assertTrue(result.contains("namespace_workload_pod:kube_pod_owner:relabel{}"),
                    "Second selector must be untouched");
        }

        @Test
        @DisplayName("workloadsWithPodLabelFilter: query already contains workload= so no extra filter is injected via queryValidator")
        void workloadsWithLabelFilterQueryWorkloadInMatchingSelector() throws Exception {
            // WORKLOAD_WITH_LABEL_QUERY already has workload= in it, so queryValidator skips
            // the appendFiltersToQuery call and passes the query to Prometheus unchanged.
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
                // Query already contains workload= so it is passed through unmodified
                // (after $MEASUREMENT_DURATION_IN_MIN$ substitution).
                assertTrue(capturedQuery.contains("workload="),
                        "Captured query must still contain workload= from the original pre-expanded query");
                assertTrue(errorString.isEmpty(),
                        "No error should be appended when result identifiers match");
            }
        }

        @Test
        @DisplayName("Null or empty inputs return query unchanged")
        void nullOrEmptyInputsReturnQueryUnchanged() {
            String query = "sum by (namespace) (avg_over_time(some_metric{namespace!=\"\"}[15m]))";
            assertEquals(query, MetadataProfileUtil.appendFiltersToQuery(query, null));
            assertEquals(query, MetadataProfileUtil.appendFiltersToQuery(query, ""));
            assertNull(MetadataProfileUtil.appendFiltersToQuery(null, ", filter"));
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
