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
package com.autotune.analyzer.workerimpl;

import com.autotune.analyzer.serviceObjects.BulkInput;
import com.autotune.analyzer.serviceObjects.BulkJobStatus;
import com.autotune.operator.KruizeDeploymentInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BulkJobManager label filter logic.
 *
 * Covers:
 * - buildLabelFilters(): PromQL label filter string generation
 * - buildResourceFilters(): resource filter map construction including labels
 * - Edge cases: empty values, null keys, mixed types, comma placement
 */
class BulkJobManagerLabelFilterTest {

    private BulkJobManager bulkJobManager;
    private String originalExperimentNameFormat;

    @BeforeEach
    void setup() {
        originalExperimentNameFormat = KruizeDeploymentInfo.experiment_name_format;
        KruizeDeploymentInfo.experiment_name_format =
                "%datasource%-%clustername%-%namespace%-%workloadname%-%workloadtype%-%containername%";

        BulkInput bulkInput = mock(BulkInput.class);
        when(bulkInput.getDatasource()).thenReturn("prometheus");
        BulkJobStatus jobStatus = mock(BulkJobStatus.class);

        bulkJobManager = new BulkJobManager("job-label-test", jobStatus, bulkInput);
    }

    @AfterEach
    void tearDown() {
        KruizeDeploymentInfo.experiment_name_format = originalExperimentNameFormat;
    }

    @Nested
    @DisplayName("buildLabelFilters — Include filters")
    class IncludeLabelFilters {

        @Test
        @DisplayName("Single string label: app=heap-oom")
        void singleStringLabel() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", "heap-oom");

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_app=\"heap-oom\"", result);
        }

        @Test
        @DisplayName("Single list label with one value: app=[system-oom]")
        void singleListLabelOneValue() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", List.of("system-oom"));

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_app=\"system-oom\"", result);
        }

        @Test
        @DisplayName("Single list label with multiple values: app=[heap-oom,system-oom]")
        void singleListLabelMultipleValues() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", List.of("heap-oom", "system-oom"));

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_app=~\"heap-oom|system-oom\"", result);
        }

        @Test
        @DisplayName("Multiple labels AND: app=system-oom AND version=v1")
        void multipleLabelsAnd() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", "system-oom");
            labels.put("version", "v1");

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_app=\"system-oom\",label_version=\"v1\"", result);
        }

        @Test
        @DisplayName("List + string labels: app=[kruize,kruize-db] AND version=v1")
        void mixedListAndStringLabels() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", List.of("kruize", "kruize-db"));
            labels.put("version", "v1");

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_app=~\"kruize|kruize-db\",label_version=\"v1\"", result);
        }
    }

    @Nested
    @DisplayName("buildLabelFilters — Exclude filters")
    class ExcludeLabelFilters {

        @Test
        @DisplayName("Exclude single label: app!=heap-oom")
        void excludeSingleLabel() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", "heap-oom");

            String result = bulkJobManager.buildLabelFilters(labels, true);

            assertEquals("label_app!=\"heap-oom\"", result);
        }

        @Test
        @DisplayName("Exclude list label: app!~[heap-oom,system-oom]")
        void excludeListLabel() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", List.of("heap-oom", "system-oom"));

            String result = bulkJobManager.buildLabelFilters(labels, true);

            assertEquals("label_app!~\"heap-oom|system-oom\"", result);
        }
    }

    @Nested
    @DisplayName("buildLabelFilters — Edge cases")
    class LabelFilterEdgeCases {

        @Test
        @DisplayName("Null key is skipped")
        void nullKeySkipped() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put(null, "value");
            labels.put("app", "heap-oom");

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_app=\"heap-oom\"", result);
        }

        @Test
        @DisplayName("Blank key is skipped")
        void blankKeySkipped() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("  ", "value");
            labels.put("app", "heap-oom");

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_app=\"heap-oom\"", result);
        }

        @Test
        @DisplayName("Null value is skipped")
        void nullValueSkipped() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", null);
            labels.put("version", "v1");

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_version=\"v1\"", result);
        }

        @Test
        @DisplayName("Empty string value is skipped")
        void emptyStringValueSkipped() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", "");
            labels.put("version", "v1");

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_version=\"v1\"", result);
        }

        @Test
        @DisplayName("Empty list value is skipped")
        void emptyListValueSkipped() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", List.of());
            labels.put("version", "v1");

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_version=\"v1\"", result);
        }

        @Test
        @DisplayName("List with only null/empty entries is skipped")
        void listWithOnlyNullEntriesSkipped() {
            Map<String, Object> labels = new LinkedHashMap<>();
            List<String> nullList = new ArrayList<>();
            nullList.add(null);
            nullList.add("");
            nullList.add("  ");
            labels.put("app", nullList);
            labels.put("version", "v1");

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_version=\"v1\"", result);
        }

        @Test
        @DisplayName("All labels invalid returns empty string")
        void allLabelsInvalidReturnsEmpty() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("", "value");
            labels.put("app", "");
            labels.put("other", null);

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("", result);
        }

        @Test
        @DisplayName("Unsupported value type is skipped")
        void unsupportedValueTypeSkipped() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", 123);
            labels.put("version", "v1");

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_version=\"v1\"", result);
        }

        @Test
        @DisplayName("No trailing comma when last entry is skipped")
        void noTrailingCommaWhenLastSkipped() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", "heap-oom");
            labels.put("bad", "");

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_app=\"heap-oom\"", result);
            assertFalse(result.endsWith(","), "Result must not end with a trailing comma");
        }

        @Test
        @DisplayName("No leading comma when first entry is skipped")
        void noLeadingCommaWhenFirstSkipped() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("bad", "");
            labels.put("app", "heap-oom");

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_app=\"heap-oom\"", result);
            assertFalse(result.startsWith(","), "Result must not start with a leading comma");
        }

        @Test
        @DisplayName("Special characters in values are escaped")
        void specialCharactersEscaped() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", "my\"app");

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_app=\"my\\\"app\"", result);
        }
    }

    @Nested
    @DisplayName("buildResourceFilters — Label filter integration")
    class ResourceFiltersIntegration {

        @Test
        @DisplayName("Valid labels produce podLabelFilter in resource map")
        void validLabelsProducePodLabelFilter() {
            BulkInput.Filter filter = mock(BulkInput.Filter.class);
            when(filter.getNamespace()).thenReturn(List.of("causa-demo"));
            when(filter.getWorkload()).thenReturn(null);
            when(filter.getContainers()).thenReturn(null);
            when(filter.getLabels()).thenReturn(Map.of("app", "heap-oom"));

            Map<String, String> result = bulkJobManager.buildResourceFilters(filter, false);

            assertEquals("causa-demo", result.get("namespaceRegex"));
            assertEquals("label_app=\"heap-oom\"", result.get("podLabelFilter"));
        }

        @Test
        @DisplayName("All-invalid labels do NOT produce podLabelFilter key")
        void allInvalidLabelsNoPodLabelFilter() {
            BulkInput.Filter filter = mock(BulkInput.Filter.class);
            when(filter.getNamespace()).thenReturn(List.of("causa-demo"));
            when(filter.getWorkload()).thenReturn(null);
            when(filter.getContainers()).thenReturn(null);
            Map<String, Object> badLabels = new LinkedHashMap<>();
            badLabels.put("", "value");
            badLabels.put("key", "");
            when(filter.getLabels()).thenReturn(badLabels);

            Map<String, String> result = bulkJobManager.buildResourceFilters(filter, false);

            assertFalse(result.containsKey("podLabelFilter"),
                    "podLabelFilter must not be present when all label entries are invalid");
        }

        @Test
        @DisplayName("Null labels map does not produce podLabelFilter key")
        void nullLabelsNoPodLabelFilter() {
            BulkInput.Filter filter = mock(BulkInput.Filter.class);
            when(filter.getNamespace()).thenReturn(List.of("causa-demo"));
            when(filter.getWorkload()).thenReturn(null);
            when(filter.getContainers()).thenReturn(null);
            when(filter.getLabels()).thenReturn(null);

            Map<String, String> result = bulkJobManager.buildResourceFilters(filter, false);

            assertFalse(result.containsKey("podLabelFilter"));
        }

        @Test
        @DisplayName("Empty labels map does not produce podLabelFilter key")
        void emptyLabelsMapNoPodLabelFilter() {
            BulkInput.Filter filter = mock(BulkInput.Filter.class);
            when(filter.getNamespace()).thenReturn(null);
            when(filter.getWorkload()).thenReturn(null);
            when(filter.getContainers()).thenReturn(null);
            when(filter.getLabels()).thenReturn(Map.of());

            Map<String, String> result = bulkJobManager.buildResourceFilters(filter, false);

            assertFalse(result.containsKey("podLabelFilter"));
        }

        @Test
        @DisplayName("Null filter returns empty resource map")
        void nullFilterReturnsEmptyMap() {
            Map<String, String> result = bulkJobManager.buildResourceFilters(null, false);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Namespace + workload + container + labels all set")
        void allFiltersSet() {
            BulkInput.Filter filter = mock(BulkInput.Filter.class);
            when(filter.getNamespace()).thenReturn(List.of("causa-demo", "openshift-tuning"));
            when(filter.getWorkload()).thenReturn(List.of("heap-oom"));
            when(filter.getContainers()).thenReturn(List.of("heap-oom"));
            when(filter.getLabels()).thenReturn(Map.of("app", "heap-oom", "version", "v1"));

            Map<String, String> result = bulkJobManager.buildResourceFilters(filter, false);

            assertEquals("causa-demo|openshift-tuning", result.get("namespaceRegex"));
            assertEquals("heap-oom", result.get("workloadRegex"));
            assertEquals("heap-oom", result.get("containerRegex"));
            assertTrue(result.containsKey("podLabelFilter"));
            assertTrue(result.get("podLabelFilter").contains("label_app=\"heap-oom\""));
            assertTrue(result.get("podLabelFilter").contains("label_version=\"v1\""));
        }

        @Test
        @DisplayName("Exclude filter uses != and !~ operators")
        void excludeFilterOperators() {
            BulkInput.Filter filter = mock(BulkInput.Filter.class);
            when(filter.getNamespace()).thenReturn(null);
            when(filter.getWorkload()).thenReturn(null);
            when(filter.getContainers()).thenReturn(null);
            when(filter.getLabels()).thenReturn(Map.of(
                    "app", List.of("heap-oom", "system-oom")
            ));

            Map<String, String> result = bulkJobManager.buildResourceFilters(filter, true);

            assertEquals("label_app!~\"heap-oom|system-oom\"", result.get("podLabelFilter"));
        }
    }

    @Nested
    @DisplayName("Key sanitization — dots and slashes replaced with underscores")
    class KeySanitization {

        @Test
        @DisplayName("Dot in label key is replaced with underscore: app.kubernetes.io → app_kubernetes_io")
        void dotReplacedWithUnderscore() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app.kubernetes.io", "heap-oom");

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_app_kubernetes_io=\"heap-oom\"", result);
        }

        @Test
        @DisplayName("Slash in label key is replaced with underscore: app/name → app_name")
        void slashReplacedWithUnderscore() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app/name", "heap-oom");

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_app_name=\"heap-oom\"", result);
        }

        @Test
        @DisplayName("Mixed dots and slashes: k8s.io/component → k8s_io_component")
        void mixedDotsAndSlashes() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("k8s.io/component", "etcd");

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_k8s_io_component=\"etcd\"", result);
        }

        @Test
        @DisplayName("Key without dots/slashes unchanged: app → label_app")
        void plainKeyUnchanged() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", "foo");

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_app=\"foo\"", result);
        }

        @Test
        @DisplayName("Sanitization applies to exclude filters too")
        void sanitizationInExcludeMode() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app.kubernetes.io/name", "heap-oom");

            String result = bulkJobManager.buildLabelFilters(labels, true);

            assertEquals("label_app_kubernetes_io_name!=\"heap-oom\"", result);
        }
    }

    @Nested
    @DisplayName("escapePromQLLabelValue")
    class EscapeTests {

        @Test
        @DisplayName("Backslash is escaped")
        void backslashEscaped() {
            assertEquals("a\\\\b", BulkJobManager.escapePromQLLabelValue("a\\b"));
        }

        @Test
        @DisplayName("Double quote is escaped")
        void doubleQuoteEscaped() {
            assertEquals("a\\\"b", BulkJobManager.escapePromQLLabelValue("a\"b"));
        }

        @Test
        @DisplayName("Newline is escaped")
        void newlineEscaped() {
            assertEquals("a\\nb", BulkJobManager.escapePromQLLabelValue("a\nb"));
        }

        @Test
        @DisplayName("Plain string unchanged")
        void plainStringUnchanged() {
            assertEquals("heap-oom", BulkJobManager.escapePromQLLabelValue("heap-oom"));
        }
    }

    @Nested
    @DisplayName("escapePromQLRegexValue — regex metacharacters escaped")
    class RegexEscapeTests {

        @Test
        @DisplayName("Dot is escaped for regex")
        void dotEscaped() {
            assertEquals("heap\\.oom", BulkJobManager.escapePromQLRegexValue("heap.oom"));
        }

        @Test
        @DisplayName("Plus is escaped for regex")
        void plusEscaped() {
            assertEquals("app\\+v2", BulkJobManager.escapePromQLRegexValue("app+v2"));
        }

        @Test
        @DisplayName("Multi-value list with regex chars produces escaped regex")
        void multiValueWithRegexChars() {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", List.of("heap.oom", "system+oom"));

            String result = bulkJobManager.buildLabelFilters(labels, false);

            assertEquals("label_app=~\"heap\\.oom|system\\+oom\"", result);
        }

        @Test
        @DisplayName("Plain string unchanged in regex escape")
        void plainStringUnchanged() {
            assertEquals("heap-oom", BulkJobManager.escapePromQLRegexValue("heap-oom"));
        }
    }
}
