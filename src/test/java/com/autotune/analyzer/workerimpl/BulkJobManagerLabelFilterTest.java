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

import java.lang.reflect.Method;
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
    void setup() throws Exception {
        originalExperimentNameFormat = KruizeDeploymentInfo.experiment_name_format;
        KruizeDeploymentInfo.experiment_name_format =
                "%datasource%-%clustername%-%namespace%-%workloadname%-%workloadtype%-%containername%";

        BulkInput bulkInput = mock(BulkInput.class);
        when(bulkInput.getDatasource()).thenReturn("prometheus");
        BulkJobStatus jobStatus = mock(BulkJobStatus.class);

        bulkJobManager = new BulkJobManager("job-label-test", jobStatus, bulkInput);
    }

    @AfterEach
    void tearDown() throws Exception {
        KruizeDeploymentInfo.experiment_name_format = originalExperimentNameFormat;
    }

    // Helper methods to invoke private methods using reflection
    @SuppressWarnings("unchecked")
    private String invokeBuildLabelFilters(Map<String, Object> labels, boolean exclude) throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("buildLabelFilters", Map.class, boolean.class);
        method.setAccessible(true);
        return (String) method.invoke(bulkJobManager, labels, exclude);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> invokeBuildResourceFilters(BulkInput.Filter filter, boolean exclude) throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("buildResourceFilters", BulkInput.Filter.class, boolean.class);
        method.setAccessible(true);
        return (Map<String, String>) method.invoke(bulkJobManager, filter, exclude);
    }

    @Nested
    @DisplayName("buildLabelFilters — Include filters")
    class IncludeLabelFilters {

        @Test
        @DisplayName("Single string label: app=heap-oom")
        void singleStringLabel() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", "heap-oom");

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_app=\"heap-oom\"", result);
        }

        @Test
        @DisplayName("Single list label with one value: app=[system-oom]")
        void singleListLabelOneValue() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", List.of("system-oom"));

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_app=\"system-oom\"", result);
        }

        @Test
        @DisplayName("Single list label with multiple values: app=[heap-oom,system-oom]")
        void singleListLabelMultipleValues() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", List.of("heap-oom", "system-oom"));

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_app=~\"heap-oom|system-oom\"", result);
        }

        @Test
        @DisplayName("Multiple labels AND: app=system-oom AND version=v1")
        void multipleLabelsAnd() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", "system-oom");
            labels.put("version", "v1");

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_app=\"system-oom\",label_version=\"v1\"", result);
        }

        @Test
        @DisplayName("List + string labels: app=[kruize,kruize-db] AND version=v1")
        void mixedListAndStringLabels() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", List.of("kruize", "kruize-db"));
            labels.put("version", "v1");

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_app=~\"kruize|kruize-db\",label_version=\"v1\"", result);
        }
    }

    @Nested
    @DisplayName("buildLabelFilters — Exclude filters")
    class ExcludeLabelFilters {

        @Test
        @DisplayName("Exclude single label: app!=heap-oom")
        void excludeSingleLabel() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", "heap-oom");

            String result = invokeBuildLabelFilters(labels, true);

            assertEquals("label_app!=\"heap-oom\"", result);
        }

        @Test
        @DisplayName("Exclude list label: app!~[heap-oom,system-oom]")
        void excludeListLabel() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", List.of("heap-oom", "system-oom"));

            String result = invokeBuildLabelFilters(labels, true);

            assertEquals("label_app!~\"heap-oom|system-oom\"", result);
        }
    }

    @Nested
    @DisplayName("buildLabelFilters — Edge cases")
    class LabelFilterEdgeCases {

        @Test
        @DisplayName("Null key is skipped")
        void nullKeySkipped() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put(null, "value");
            labels.put("app", "heap-oom");

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_app=\"heap-oom\"", result);
        }

        @Test
        @DisplayName("Blank key is skipped")
        void blankKeySkipped() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("  ", "value");
            labels.put("app", "heap-oom");

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_app=\"heap-oom\"", result);
        }

        @Test
        @DisplayName("Null value is skipped")
        void nullValueSkipped() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", null);
            labels.put("version", "v1");

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_version=\"v1\"", result);
        }

        @Test
        @DisplayName("Empty string value is skipped")
        void emptyStringValueSkipped() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", "");
            labels.put("version", "v1");

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_version=\"v1\"", result);
        }

        @Test
        @DisplayName("Empty list value is skipped")
        void emptyListValueSkipped() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", List.of());
            labels.put("version", "v1");

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_version=\"v1\"", result);
        }

        @Test
        @DisplayName("List with only null/empty entries is skipped")
        void listWithOnlyNullEntriesSkipped() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            List<String> nullList = new ArrayList<>();
            nullList.add(null);
            nullList.add("");
            nullList.add("  ");
            labels.put("app", nullList);
            labels.put("version", "v1");

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_version=\"v1\"", result);
        }

        @Test
        @DisplayName("All labels invalid returns empty string")
        void allLabelsInvalidReturnsEmpty() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("", "value");
            labels.put("app", "");
            labels.put("other", null);

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("", result);
        }

        @Test
        @DisplayName("Unsupported value type is skipped")
        void unsupportedValueTypeSkipped() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", 123);
            labels.put("version", "v1");

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_version=\"v1\"", result);
        }

        @Test
        @DisplayName("No trailing comma when last entry is skipped")
        void noTrailingCommaWhenLastSkipped() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", "heap-oom");
            labels.put("bad", "");

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_app=\"heap-oom\"", result);
            assertFalse(result.endsWith(","), "Result must not end with a trailing comma");
        }

        @Test
        @DisplayName("No leading comma when first entry is skipped")
        void noLeadingCommaWhenFirstSkipped() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("bad", "");
            labels.put("app", "heap-oom");

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_app=\"heap-oom\"", result);
            assertFalse(result.startsWith(","), "Result must not start with a leading comma");
        }

        @Test
        @DisplayName("Special characters in values are escaped")
        void specialCharactersEscaped() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", "my\"app");

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_app=\"my\\\"app\"", result);
        }
    }

    @Nested
    @DisplayName("buildResourceFilters — Label filter integration")
    class ResourceFiltersIntegration {

        @Test
        @DisplayName("Valid labels produce podLabelFilter in resource map")
        void validLabelsProducePodLabelFilter() throws Exception {
            BulkInput.Filter filter = mock(BulkInput.Filter.class);
            when(filter.getNamespace()).thenReturn(List.of("causa-demo"));
            when(filter.getWorkload()).thenReturn(null);
            when(filter.getContainers()).thenReturn(null);
            when(filter.getLabels()).thenReturn(Map.of("app", "heap-oom"));

            Map<String, String> result = invokeBuildResourceFilters(filter, false);

            assertEquals("causa-demo", result.get("namespaceRegex"));
            assertEquals("label_app=\"heap-oom\"", result.get("podLabelFilter"));
        }

        @Test
        @DisplayName("All-invalid labels do NOT produce podLabelFilter key")
        void allInvalidLabelsNoPodLabelFilter() throws Exception {
            BulkInput.Filter filter = mock(BulkInput.Filter.class);
            when(filter.getNamespace()).thenReturn(List.of("causa-demo"));
            when(filter.getWorkload()).thenReturn(null);
            when(filter.getContainers()).thenReturn(null);
            Map<String, Object> badLabels = new LinkedHashMap<>();
            badLabels.put("", "value");
            badLabels.put("key", "");
            when(filter.getLabels()).thenReturn(badLabels);

            Map<String, String> result = invokeBuildResourceFilters(filter, false);

            assertFalse(result.containsKey("podLabelFilter"),
                    "podLabelFilter must not be present when all label entries are invalid");
        }

        @Test
        @DisplayName("Null labels map does not produce podLabelFilter key")
        void nullLabelsNoPodLabelFilter() throws Exception {
            BulkInput.Filter filter = mock(BulkInput.Filter.class);
            when(filter.getNamespace()).thenReturn(List.of("causa-demo"));
            when(filter.getWorkload()).thenReturn(null);
            when(filter.getContainers()).thenReturn(null);
            when(filter.getLabels()).thenReturn(null);

            Map<String, String> result = invokeBuildResourceFilters(filter, false);

            assertFalse(result.containsKey("podLabelFilter"));
        }

        @Test
        @DisplayName("Empty labels map does not produce podLabelFilter key")
        void emptyLabelsMapNoPodLabelFilter() throws Exception {
            BulkInput.Filter filter = mock(BulkInput.Filter.class);
            when(filter.getNamespace()).thenReturn(null);
            when(filter.getWorkload()).thenReturn(null);
            when(filter.getContainers()).thenReturn(null);
            when(filter.getLabels()).thenReturn(Map.of());

            Map<String, String> result = invokeBuildResourceFilters(filter, false);

            assertFalse(result.containsKey("podLabelFilter"));
        }

        @Test
        @DisplayName("Null filter returns empty resource map")
        void nullFilterReturnsEmptyMap() throws Exception {
            Map<String, String> result = invokeBuildResourceFilters(null, false);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Namespace + workload + container + labels all set")
        void allFiltersSet() throws Exception {
            BulkInput.Filter filter = mock(BulkInput.Filter.class);
            when(filter.getNamespace()).thenReturn(List.of("causa-demo", "openshift-tuning"));
            when(filter.getWorkload()).thenReturn(List.of("heap-oom"));
            when(filter.getContainers()).thenReturn(List.of("heap-oom"));
            when(filter.getLabels()).thenReturn(Map.of("app", "heap-oom", "version", "v1"));

            Map<String, String> result = invokeBuildResourceFilters(filter, false);

            assertEquals("causa-demo|openshift-tuning", result.get("namespaceRegex"));
            assertEquals("heap-oom", result.get("workloadRegex"));
            assertEquals("heap-oom", result.get("containerRegex"));
            assertTrue(result.containsKey("podLabelFilter"));
            assertTrue(result.get("podLabelFilter").contains("label_app=\"heap-oom\""));
            assertTrue(result.get("podLabelFilter").contains("label_version=\"v1\""));
        }

        @Test
        @DisplayName("Exclude filter uses != and !~ operators")
        void excludeFilterOperators() throws Exception {
            BulkInput.Filter filter = mock(BulkInput.Filter.class);
            when(filter.getNamespace()).thenReturn(null);
            when(filter.getWorkload()).thenReturn(null);
            when(filter.getContainers()).thenReturn(null);
            when(filter.getLabels()).thenReturn(Map.of(
                    "app", List.of("heap-oom", "system-oom")
            ));

            Map<String, String> result = invokeBuildResourceFilters(filter, true);

            assertEquals("label_app!~\"heap-oom|system-oom\"", result.get("podLabelFilter"));
        }
    }

    @Nested
    @DisplayName("Key sanitization — dots and slashes replaced with underscores")
    class KeySanitization {

        @Test
        @DisplayName("Dot in label key is replaced with underscore: app.kubernetes.io → app_kubernetes_io")
        void dotReplacedWithUnderscore() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app.kubernetes.io", "heap-oom");

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_app_kubernetes_io=\"heap-oom\"", result);
        }

        @Test
        @DisplayName("Slash in label key is replaced with underscore: app/name → app_name")
        void slashReplacedWithUnderscore() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app/name", "heap-oom");

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_app_name=\"heap-oom\"", result);
        }

        @Test
        @DisplayName("Mixed dots and slashes: k8s.io/component → k8s_io_component")
        void mixedDotsAndSlashes() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("k8s.io/component", "etcd");

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_k8s_io_component=\"etcd\"", result);
        }

        @Test
        @DisplayName("Key without dots/slashes unchanged: app → label_app")
        void plainKeyUnchanged() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", "foo");

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_app=\"foo\"", result);
        }

        @Test
        @DisplayName("Sanitization applies to exclude filters too")
        void sanitizationInExcludeMode() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app.kubernetes.io/name", "heap-oom");

            String result = invokeBuildLabelFilters(labels, true);

            assertEquals("label_app_kubernetes_io_name!=\"heap-oom\"", result);
        }
    }

    @Nested
    @DisplayName("escapePromQLLabelValue")
    class EscapeTests {

        @Test
        @DisplayName("Backslash is escaped")
        void backslashEscaped() throws Exception {
            assertEquals("a\\\\b", BulkJobManager.escapePromQLLabelValue("a\\b"));
        }

        @Test
        @DisplayName("Double quote is escaped")
        void doubleQuoteEscaped() throws Exception {
            assertEquals("a\\\"b", BulkJobManager.escapePromQLLabelValue("a\"b"));
        }

        @Test
        @DisplayName("Newline is escaped")
        void newlineEscaped() throws Exception {
            assertEquals("a\\nb", BulkJobManager.escapePromQLLabelValue("a\nb"));
        }

        @Test
        @DisplayName("Plain string unchanged")
        void plainStringUnchanged() throws Exception {
            assertEquals("heap-oom", BulkJobManager.escapePromQLLabelValue("heap-oom"));
        }
    }

    @Nested
    @DisplayName("escapePromQLRegexValue — regex metacharacters escaped")
    class RegexEscapeTests {

        @Test
        @DisplayName("Dot is escaped for regex")
        void dotEscaped() throws Exception {
            assertEquals("heap\\.oom", BulkJobManager.escapePromQLRegexValue("heap.oom"));
        }

        @Test
        @DisplayName("Plus is escaped for regex")
        void plusEscaped() throws Exception {
            assertEquals("app\\+v2", BulkJobManager.escapePromQLRegexValue("app+v2"));
        }

        @Test
        @DisplayName("Multi-value list with regex chars produces escaped regex")
        void multiValueWithRegexChars() throws Exception {
            Map<String, Object> labels = new LinkedHashMap<>();
            labels.put("app", List.of("heap.oom", "system+oom"));

            String result = invokeBuildLabelFilters(labels, false);

            assertEquals("label_app=~\"heap\\.oom|system\\+oom\"", result);
        }

        @Test
        @DisplayName("Plain string unchanged in regex escape")
        void plainStringUnchanged() throws Exception {
            assertEquals("heap-oom", BulkJobManager.escapePromQLRegexValue("heap-oom"));
        }
    }
}
