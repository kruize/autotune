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
    @DisplayName("appendFiltersToQuery — strips leading comma for empty selector, appends verbatim otherwise")
    class AppendFiltersEmptySelectorTests {

        @Test
        @DisplayName("Leading comma is stripped when selector is empty, producing valid PromQL")
        void leadingCommaStrippedForEmptySelector() {
            String query = "sum by (namespace) (avg_over_time(kube_namespace_status_phase{}[15m]))";
            String filterWithLeadingComma = ", namespace=~\"openshift-tuning|monitoring\"";

            String result = MetadataProfileUtil.appendFiltersToQuery(query, filterWithLeadingComma);

            // appendFiltersToQuery strips the leading ", " when the selector is empty to produce
            // valid PromQL: {namespace=~"..."} not {, namespace=~"..."}.
            assertFalse(result.contains("{,"),
                    "Result must not contain a leading comma inside the selector");
            assertTrue(result.contains("{namespace=~"),
                    "Filter must be present without a leading comma");
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
        @DisplayName("Filter is injected into a single empty-selector query, leading comma stripped")
        void filterInjectedIntoSingleSelector() {
            String query = "sum by (namespace, workload, workload_type) (avg_over_time(some_metric{}[15m]))";
            String filter = ", workload=\"kruize\"";

            String result = MetadataProfileUtil.appendFiltersToQuery(query, filter);

            // Leading ", " is stripped when the selector is empty
            assertTrue(result.contains("some_metric{workload=\"kruize\"}"),
                    "Filter must be appended inside the first selector without a leading comma");
        }

        @Test
        @DisplayName("Filter is injected into the first selector of a joined query, leading comma stripped")
        void filterInjectedIntoFirstSelectorOfJoinedQuery() {
            String query =
                    "sum by (namespace, workload, workload_type) " +
                    "(max_over_time(kube_pod_labels{}[15m]) * on (namespace, pod) " +
                    "group_left(workload, workload_type) " +
                    "max_over_time(namespace_workload_pod:kube_pod_owner:relabel{}[15m]))";
            String filter = ", namespace!=\"\"";

            String result = MetadataProfileUtil.appendFiltersToQuery(query, filter);

            // Leading ", " is stripped when the selector is empty
            assertTrue(result.contains("kube_pod_labels{namespace!=\"\"}"),
                    "Filter must be appended into the first (kube_pod_labels) selector without a leading comma");
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
    // findSelectorCloseBrace — quote-aware brace scanning
    // =========================================================================
    @Nested
    @DisplayName("findSelectorCloseBrace — finds closing brace respecting quoted label values")
    class FindSelectorCloseBraceTests {

        @Test
        @DisplayName("Returns index of plain closing brace in a simple selector")
        void simpleSelectorCloseBrace() {
            String query = "some_metric{namespace!=\"\", workload=\"kruize\"}[15m]";
            int openBrace = query.indexOf("{");
            int close = MetadataProfileUtil.findSelectorCloseBrace(query, openBrace + 1);
            assertEquals(query.indexOf("}"), close, "Must find the real closing brace");
        }

        @Test
        @DisplayName("Skips } inside a double-quoted label value")
        void skipsCloseBraceInsideQuotedValue() {
            // workload_type!~"(?i)foo{bar}" — the } inside the quoted regex must be skipped
            String query = "some_metric{workload_type!~\"(?i)foo{bar}\"}[15m]";
            int openBrace = query.indexOf("{");
            int close = MetadataProfileUtil.findSelectorCloseBrace(query, openBrace + 1);
            int expected = query.lastIndexOf("}");
            assertEquals(expected, close, "Must skip the } inside the quoted regex value");
        }

        @Test
        @DisplayName("Handles escaped quote inside label value")
        void handlesEscapedQuoteInsideValue() {
            String query = "some_metric{label=\"val\\\"ue\"}[15m]";
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
            // workload_type!~"(?i)Deployment{Config}" — the } inside the regex must not be
            // mistaken for the selector boundary.
            String query =
                    "sum by (namespace, workload, workload_type) " +
                    "(avg_over_time(namespace_workload_pod:kube_pod_owner:relabel" +
                    "{workload_type!~\"(?i)Deployment{Config}\"}[15m]))";
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
            // The second selector contains a quoted } inside a regex value.
            // The key correctness property: the quoted } must not be treated as a selector
            // boundary, so the second selector survives intact in the output.
            String query =
                    "sum by (namespace, workload, workload_type) " +
                    "(max_over_time(kube_pod_labels{namespace!=\"\"}[15m]) * on (namespace, pod) " +
                    "group_left(workload, workload_type) " +
                    "max_over_time(namespace_workload_pod:kube_pod_owner:relabel" +
                    "{workload_type!~\"(?i)foo{bar}\"}[15m]))";
            String filter = ", workload=\"kruize\"";

            String result = MetadataProfileUtil.appendFiltersToMatchingSelector(
                    query, AnalyzerConstants.WORKLOAD, filter);

            // The quoted } inside "(?i)foo{bar}" must remain intact in the output
            assertTrue(result.contains("workload_type!~\"(?i)foo{bar}\"}"),
                    "Quoted } inside regex value in the second selector must remain intact");
            assertFalse(result.isEmpty(), "Result must not be empty");
        }
    }

    // =========================================================================
    // validateMetricQueryPattern — unknown placeholder rejection
    // =========================================================================
    @Nested
    @DisplayName("validateMetricQueryPattern — unknown placeholders are rejected with a clear error")
    class UnknownPlaceholderRejectionTests {

        @Test
        @DisplayName("A completely unknown placeholder is rejected before reaching Prometheus")
        void unknownPlaceholderRejected() {
            StringBuilder errorString = new StringBuilder();
            // $CLUSTER_FILTERS$ is not in SUPPORTED_PLACEHOLDERS
            String queryWithUnknown =
                    "sum by (namespace) (avg_over_time(kube_namespace_status_phase" +
                    "{namespace!=\"\", $CLUSTER_FILTERS$}[15m]))";
            validation.validateMetricQueryPattern(
                    queryWithUnknown, AnalyzerConstants.NAMESPACE_METADATA_QUERY, errorString, mockDataSourceInfo);

            assertFalse(errorString.isEmpty(),
                    "An error must be appended for an unknown placeholder");
            assertTrue(errorString.toString().contains("$CLUSTER_FILTERS$"),
                    "Error must name the unknown placeholder");
            assertTrue(errorString.toString().contains(AnalyzerConstants.NAMESPACE_METADATA_QUERY),
                    "Error must reference the metric name");
        }

        @Test
        @DisplayName("A misspelled supported placeholder is also rejected as unknown")
        void misspelledPlaceholderRejected() {
            StringBuilder errorString = new StringBuilder();
            // $NAMESPACE_FILTER$ (missing trailing S) is not in SUPPORTED_PLACEHOLDERS
            String queryWithTypo =
                    "sum by (namespace) (avg_over_time(kube_namespace_status_phase" +
                    "{$NAMESPACE_FILTER$}[15m]))";
            validation.validateMetricQueryPattern(
                    queryWithTypo, AnalyzerConstants.NAMESPACE_METADATA_QUERY, errorString, mockDataSourceInfo);

            assertFalse(errorString.isEmpty(),
                    "An error must be appended for a misspelled placeholder");
            assertTrue(errorString.toString().contains("$NAMESPACE_FILTER$"),
                    "Error must name the misspelled placeholder");
        }

        @Test
        @DisplayName("SUPPORTED_PLACEHOLDERS that are valid in update payloads are not rejected as unknown")
        void updatePayloadPlaceholdersNotRejected() throws Exception {
            // $MEASUREMENT_DURATION_IN_MIN$ and $UNSUPPORTED_WORKLOAD_TYPES$ are in
            // SUPPORTED_PLACEHOLDERS and also in UPDATE_PAYLOAD_PLACEHOLDERS — must not be flagged.
            DataSourceOperatorImpl mockOperator = mock(DataSourceOperatorImpl.class);
            when(mockOperator.getResultArrayForQuery(any(), anyString()))
                    .thenReturn(buildResultArray("namespace"));

            try (MockedStatic<DataSourceOperatorImpl> staticMock =
                         mockStatic(DataSourceOperatorImpl.class)) {
                DataSourceOperatorImpl mockImpl = mock(DataSourceOperatorImpl.class);
                staticMock.when(DataSourceOperatorImpl::getInstance).thenReturn(mockImpl);
                when(mockImpl.getOperator(anyString())).thenReturn(mockOperator);

                StringBuilder errorString = new StringBuilder();
                String queryWithTemplateVars =
                        "sum by (namespace) (avg_over_time(kube_namespace_status_phase" +
                        "{namespace!=\"\", $UNSUPPORTED_WORKLOAD_TYPES$}[$MEASUREMENT_DURATION_IN_MIN$m]))";
                validation.validateMetricQueryPattern(
                        queryWithTemplateVars, AnalyzerConstants.NAMESPACE_METADATA_QUERY,
                        errorString, mockDataSourceInfo);

                assertFalse(errorString.toString().contains("unknown placeholder"),
                        "Known template variables must not be flagged as unknown placeholders");
            }
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
