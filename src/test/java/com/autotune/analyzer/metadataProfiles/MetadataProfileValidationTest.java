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
 * - queryValidator(): verifies all $...$ filter placeholders are stripped before
 *   the query is sent to Prometheus, preventing HTTP 400 syntax errors.
 * - validateMetricQueryPattern(): verifies the sum-by clause matcher and error
 *   reporting for unsupported clause shapes.
 * - matchSumByClause(): verifies each MetadataProfileQueryIdentifier is correctly
 *   matched from the real queries in the payload.
 */
class MetadataProfileValidationTest {

    private MetadataProfileValidation validation;
    private DataSourceInfo mockDataSourceInfo;

    // Queries from the updateMetadata payload that triggered the bug
    private static final String NAMESPACE_QUERY =
            "sum by (namespace) (avg_over_time(kube_namespace_status_phase{$NAMESPACE_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))";

    private static final String WORKLOAD_QUERY =
            "sum by (namespace, workload, workload_type) (avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ $WORKLOAD_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))";

    private static final String WORKLOAD_WITH_LABEL_QUERY =
            "sum by (namespace, workload, workload_type) (max_over_time(kube_pod_labels{$NAMESPACE_FILTERS$ $CONTAINER_FILTERS$ $LABELS$}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (namespace, pod) group_left(workload, workload_type) max_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ $WORKLOAD_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))";

    private static final String CONTAINER_QUERY =
            "sum by (container, image, workload, workload_type, namespace) (avg_over_time(kube_pod_container_info{$NAMESPACE_FILTERS$ $CONTAINER_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (pod, namespace) group_left(workload, workload_type) avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{$UNSUPPORTED_WORKLOAD_TYPES$ $WORKLOAD_FILTERS$}[$MEASUREMENT_DURATION_IN_MIN$m]))";

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
    // queryValidator — placeholder stripping (the core bug fix)
    // =========================================================================
    @Nested
    @DisplayName("queryValidator — filter placeholders stripped before Prometheus call")
    class QueryValidatorPlaceholderTests {

        /**
         * Captures the query string actually passed to the operator and asserts that
         * none of the $...$ placeholder tokens remain in it.
         */
        private void assertNoPlaceholdersReachPrometheus(
                String rawQuery,
                String metricName,
                AnalyzerConstants.MetadataProfileQueryIdentifier identifier,
                JsonArray fakeResult) throws Exception {

            DataSourceOperatorImpl mockOperator = mock(DataSourceOperatorImpl.class);
            ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
            when(mockOperator.getResultArrayForQuery(any(), queryCaptor.capture()))
                    .thenReturn(fakeResult);

            try (MockedStatic<DataSourceOperatorImpl> staticMock =
                         mockStatic(DataSourceOperatorImpl.class)) {
                DataSourceOperatorImpl mockImpl = mock(DataSourceOperatorImpl.class);
                staticMock.when(DataSourceOperatorImpl::getInstance).thenReturn(mockImpl);
                when(mockImpl.getOperator(anyString())).thenReturn(mockOperator);

                StringBuilder errorString = new StringBuilder();
                validation.queryValidator(rawQuery, metricName, errorString, mockDataSourceInfo, identifier);

                String capturedQuery = queryCaptor.getValue();
                assertFalse(capturedQuery.contains("$NAMESPACE_FILTERS$"),
                        "Query sent to Prometheus must not contain $NAMESPACE_FILTERS$");
                assertFalse(capturedQuery.contains("$WORKLOAD_FILTERS$"),
                        "Query sent to Prometheus must not contain $WORKLOAD_FILTERS$");
                assertFalse(capturedQuery.contains("$CONTAINER_FILTERS$"),
                        "Query sent to Prometheus must not contain $CONTAINER_FILTERS$");
                assertFalse(capturedQuery.contains("$LABELS$"),
                        "Query sent to Prometheus must not contain $LABELS$");
                assertFalse(capturedQuery.contains("$MEASUREMENT_DURATION_IN_MIN$"),
                        "Query sent to Prometheus must not contain $MEASUREMENT_DURATION_IN_MIN$");
                assertFalse(capturedQuery.contains("$UNSUPPORTED_WORKLOAD_TYPES$"),
                        "Query sent to Prometheus must not contain $UNSUPPORTED_WORKLOAD_TYPES$");
            }
        }

        @Test
        @DisplayName("namespacesForAdditionalLabel: $NAMESPACE_FILTERS$ is stripped before Prometheus call")
        void namespaceQueryStripsNamespaceFilters() throws Exception {
            JsonArray result = buildResultArray("namespace");
            assertNoPlaceholdersReachPrometheus(
                    NAMESPACE_QUERY,
                    AnalyzerConstants.NAMESPACE_METADATA_QUERY,
                    AnalyzerConstants.MetadataProfileQueryIdentifier.NAMESPACE_QUERY_IDENTIFIER,
                    result);
        }

        @Test
        @DisplayName("workloadsForAdditionalLabel: $WORKLOAD_FILTERS$ and $UNSUPPORTED_WORKLOAD_TYPES$ are stripped")
        void workloadQueryStripsAllPlaceholders() throws Exception {
            JsonArray result = buildResultArray("namespace", "workload", "workload_type");
            assertNoPlaceholdersReachPrometheus(
                    WORKLOAD_QUERY,
                    AnalyzerConstants.WORKLOAD_METADATA_QUERY,
                    AnalyzerConstants.MetadataProfileQueryIdentifier.WORKLOAD_QUERY_IDENTIFIER,
                    result);
        }

        @Test
        @DisplayName("workloadsWithPodLabelFilter: $NAMESPACE_FILTERS$, $CONTAINER_FILTERS$, $LABELS$ are stripped")
        void workloadWithLabelQueryStripsAllPlaceholders() throws Exception {
            JsonArray result = buildResultArray("namespace", "workload", "workload_type");
            assertNoPlaceholdersReachPrometheus(
                    WORKLOAD_WITH_LABEL_QUERY,
                    AnalyzerConstants.WORKLOAD_METADATA_QUERY_WITH_LABEL_FILTER,
                    AnalyzerConstants.MetadataProfileQueryIdentifier.WORKLOAD_QUERY_IDENTIFIER,
                    result);
        }

        @Test
        @DisplayName("containersForAdditionalLabel: $NAMESPACE_FILTERS$, $CONTAINER_FILTERS$, $WORKLOAD_FILTERS$ are stripped")
        void containerQueryStripsAllPlaceholders() throws Exception {
            JsonArray result = buildResultArray("container", "image", "workload", "workload_type", "namespace");
            assertNoPlaceholdersReachPrometheus(
                    CONTAINER_QUERY,
                    AnalyzerConstants.CONTAINER_METADATA_QUERY,
                    AnalyzerConstants.MetadataProfileQueryIdentifier.CONTAINER_QUERY_IDENTIFIER,
                    result);
        }

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
    // selector normalization after placeholder replacement
    // =========================================================================
    @Nested
    @DisplayName("queryValidator — selector normalization after placeholder stripping")
    class SelectorNormalizationTests {

        @Test
        @DisplayName("Leading comma from getUnsupportedWorkloadTypesFilter is removed before Prometheus call")
        void unsupportedWorkloadTypesLeadingCommaIsStripped() throws Exception {
            // $UNSUPPORTED_WORKLOAD_TYPES$ expands to ", workload_type!~..." (leading comma).
            // With $WORKLOAD_FILTERS$ stripped to "", the raw selector becomes
            // {, workload_type!~"..."  } which must be normalized to {workload_type!~"..."}.
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
                        WORKLOAD_QUERY,
                        AnalyzerConstants.WORKLOAD_METADATA_QUERY,
                        errorString,
                        mockDataSourceInfo,
                        AnalyzerConstants.MetadataProfileQueryIdentifier.WORKLOAD_QUERY_IDENTIFIER);

                String capturedQuery = queryCaptor.getValue();
                assertFalse(capturedQuery.contains("{,"),
                        "Selector must not start with a leading comma after normalization");
                assertFalse(capturedQuery.contains("{, "),
                        "Selector must not start with a leading comma-space after normalization");
                assertTrue(errorString.isEmpty(),
                        "No error should be appended when result identifiers match");
            }
        }

        @Test
        @DisplayName("Container query selector is normalized: no leading or trailing comma")
        void containerQuerySelectorNormalized() throws Exception {
            JsonArray result = buildResultArray("container", "image", "workload", "workload_type", "namespace");
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
                        CONTAINER_QUERY,
                        AnalyzerConstants.CONTAINER_METADATA_QUERY,
                        errorString,
                        mockDataSourceInfo,
                        AnalyzerConstants.MetadataProfileQueryIdentifier.CONTAINER_QUERY_IDENTIFIER);

                String capturedQuery = queryCaptor.getValue();
                assertFalse(capturedQuery.contains("{,"),
                        "No selector should start with a leading comma");
                assertFalse(capturedQuery.contains(",}"),
                        "No selector should end with a trailing comma");
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
        @DisplayName("Namespace query with all-placeholder selector produces no leading comma after stripping")
        void namespaceQueryEmptySelectorAfterStripping() throws Exception {
            // After stripping $NAMESPACE_FILTERS$ the selector is {}.
            // appendFiltersToQuery must inject namespace=~"..." without a leading comma.
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
                assertFalse(capturedQuery.contains("{,"),
                        "No leading comma must appear inside the selector after filter injection");
                assertTrue(errorString.isEmpty(),
                        "No error should be appended when the query and result are valid");
            }
        }
    }

    // =========================================================================
    // appendFiltersToSecondSelector — workload filter on correct selector
    // =========================================================================
    @Nested
    @DisplayName("appendFiltersToSecondSelector — workload filter targets the joined-workload selector")
    class AppendFiltersSecondSelectorTests {

        @Test
        @DisplayName("workload= filter is injected into the second selector, not the first")
        void workloadFilterGoesIntoSecondSelector() {
            // First selector: kube_pod_labels{} — no workload label
            // Second selector: namespace_workload_pod:kube_pod_owner:relabel{} — carries workload
            String query =
                    "sum by (namespace, workload, workload_type) " +
                    "(max_over_time(kube_pod_labels{}[15m]) * on (namespace, pod) " +
                    "group_left(workload, workload_type) " +
                    "max_over_time(namespace_workload_pod:kube_pod_owner:relabel{}[15m]))";
            String filter = ", workload=\"kruize\"";

            String result = MetadataProfileUtil.appendFiltersToSecondSelector(query, filter);

            // First selector must be untouched
            assertTrue(result.contains("kube_pod_labels{}"),
                    "First selector must not have the workload filter injected");
            // Second selector must have the filter
            assertTrue(result.contains("namespace_workload_pod:kube_pod_owner:relabel{workload=\"kruize\"}"),
                    "Second selector must contain workload=\"kruize\"");
        }

        @Test
        @DisplayName("workloadsWithPodLabelFilter query: workload= filter reaches second selector via queryValidator")
        void workloadsWithLabelFilterQueryWorkloadInSecondSelector() throws Exception {
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
                // The first selector (kube_pod_labels) must NOT have workload=
                int firstOpen = capturedQuery.indexOf("{");
                int firstClose = capturedQuery.indexOf("}", firstOpen);
                String firstSelector = capturedQuery.substring(firstOpen + 1, firstClose);
                assertFalse(firstSelector.contains("workload="),
                        "workload= must NOT be injected into the first (kube_pod_labels) selector");
                // The second selector must contain workload=
                int secondOpen = capturedQuery.indexOf("{", firstClose + 1);
                int secondClose = capturedQuery.indexOf("}", secondOpen);
                String secondSelector = capturedQuery.substring(secondOpen + 1, secondClose);
                assertTrue(secondSelector.contains("workload="),
                        "workload= must be injected into the second (namespace_workload_pod) selector");
                assertTrue(errorString.isEmpty(),
                        "No error should be appended when result identifiers match");
            }
        }

        @Test
        @DisplayName("Falls back to first selector when query has only one selector")
        void fallsBackToFirstSelectorWhenOnlyOne() {
            String query = "sum by (namespace, workload, workload_type) (avg_over_time(some_metric{}[15m]))";
            String filter = ", workload=\"kruize\"";

            String result = MetadataProfileUtil.appendFiltersToSecondSelector(query, filter);

            assertTrue(result.contains("some_metric{workload=\"kruize\"}"),
                    "Should fall back to the first selector when no second selector exists");
        }
    }

    // =========================================================================
    // validateMetricQueryPattern — sum by clause validation
    // =========================================================================
    @Nested
    @DisplayName("validateMetricQueryPattern — sum by clause validation")
    class ValidateMetricQueryPatternTests {

        @Test
        @DisplayName("Valid namespace query passes sum-by check without error")
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
                        "No error should be appended for a valid namespace query");
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
