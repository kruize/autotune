package com.autotune.analyzer.workerimpl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autotune.analyzer.serviceObjects.BulkInput;
import com.autotune.analyzer.serviceObjects.BulkJobStatus;

/**
 * Unit tests for BulkJobManager label filtering functionality.
 * Tests cover label normalization, PromQL escaping, filter building, and experiment naming.
 */
@ExtendWith(MockitoExtension.class)
class BulkJobManagerLabelFilterTest {

    @Mock
    private BulkJobStatus mockJobStatus;

    @Mock
    private BulkInput mockBulkInput;

    private BulkJobManager bulkJobManager;
    private String testJobId;

    @BeforeEach
    void setUp() {
        testJobId = "test-job-label-filter";
        bulkJobManager = new BulkJobManager(testJobId, mockJobStatus, mockBulkInput);
    }

    // ==================== escapePromQLLabelValue Tests ====================

    @Test
    @DisplayName("Test escapePromQLLabelValue with null value")
    void testEscapePromQLLabelValue_Null() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("escapePromQLLabelValue", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(bulkJobManager, (String) null);
        assertEquals("", result);
    }

    @Test
    @DisplayName("Test escapePromQLLabelValue with simple value")
    void testEscapePromQLLabelValue_Simple() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("escapePromQLLabelValue", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(bulkJobManager, "simple-value");
        assertEquals("simple-value", result);
    }

    @Test
    @DisplayName("Test escapePromQLLabelValue with backslash")
    void testEscapePromQLLabelValue_Backslash() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("escapePromQLLabelValue", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(bulkJobManager, "value\\with\\backslash");
        assertEquals("value\\\\with\\\\backslash", result);
    }

    @Test
    @DisplayName("Test escapePromQLLabelValue with double quotes")
    void testEscapePromQLLabelValue_DoubleQuotes() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("escapePromQLLabelValue", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(bulkJobManager, "value\"with\"quotes");
        assertEquals("value\\\"with\\\"quotes", result);
    }

    @Test
    @DisplayName("Test escapePromQLLabelValue with newline")
    void testEscapePromQLLabelValue_Newline() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("escapePromQLLabelValue", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(bulkJobManager, "value\nwith\nnewline");
        assertEquals("value\\nwith\\nnewline", result);
    }

    @Test
    @DisplayName("Test escapePromQLLabelValue with tab")
    void testEscapePromQLLabelValue_Tab() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("escapePromQLLabelValue", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(bulkJobManager, "value\twith\ttab");
        assertEquals("value\\twith\\ttab", result);
    }

    @Test
    @DisplayName("Test escapePromQLLabelValue with carriage return")
    void testEscapePromQLLabelValue_CarriageReturn() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("escapePromQLLabelValue", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(bulkJobManager, "value\rwith\rCR");
        assertEquals("value\\rwith\\rCR", result);
    }

    @Test
    @DisplayName("Test escapePromQLLabelValue with multiple special characters")
    void testEscapePromQLLabelValue_Multiple() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("escapePromQLLabelValue", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(bulkJobManager, "value\\\"with\n\t\rmany");
        assertEquals("value\\\\\\\"with\\n\\t\\rmany", result);
    }

    @Test
    @DisplayName("Test escapePromQLLabelValue with empty string")
    void testEscapePromQLLabelValue_Empty() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("escapePromQLLabelValue", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(bulkJobManager, "");
        assertEquals("", result);
    }

    // ==================== buildLabelFilters Tests ====================

    @Test
    @DisplayName("Test buildLabelFilters with null labels")
    void testBuildLabelFilters_Null() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("buildLabelFilters", Map.class);
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(bulkJobManager, (Map<String, Object>) null);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test buildLabelFilters with empty labels")
    void testBuildLabelFilters_Empty() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("buildLabelFilters", Map.class);
        method.setAccessible(true);
        
        Map<String, Object> labels = new HashMap<>();
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(bulkJobManager, labels);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test buildLabelFilters with single label")
    void testBuildLabelFilters_SingleLabel() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("buildLabelFilters", Map.class);
        method.setAccessible(true);
        
        Map<String, Object> labels = new HashMap<>();
        labels.put("app", "nginx");
        
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(bulkJobManager, labels);
        
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("podLabelFilter"));
        assertTrue(result.containsKey("namespaceLabelFilter"));
        assertEquals("label_app=\"nginx\"", result.get("podLabelFilter"));
        assertEquals("label_app=\"nginx\"", result.get("namespaceLabelFilter"));
    }

    @Test
    @DisplayName("Test buildLabelFilters with label containing special characters")
    void testBuildLabelFilters_SpecialChars() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("buildLabelFilters", Map.class);
        method.setAccessible(true);
        
        Map<String, Object> labels = new HashMap<>();
        labels.put("app.kubernetes.io/component", "controller");
        
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(bulkJobManager, labels);
        
        assertNotNull(result);
        // Special chars should be normalized to underscores
        assertTrue(result.get("podLabelFilter").contains("label_app_kubernetes_io_component"));
    }

    @Test
    @DisplayName("Test buildLabelFilters with label containing hyphens")
    void testBuildLabelFilters_Hyphens() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("buildLabelFilters", Map.class);
        method.setAccessible(true);
        
        Map<String, Object> labels = new HashMap<>();
        labels.put("pod-template-hash", "abc123");
        
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(bulkJobManager, labels);
        
        assertNotNull(result);
        // Hyphens should be normalized to underscores
        assertTrue(result.get("podLabelFilter").contains("label_pod_template_hash"));
    }

    @Test
    @DisplayName("Test buildLabelFilters with array of values")
    void testBuildLabelFilters_ArrayValues() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("buildLabelFilters", Map.class);
        method.setAccessible(true);
        
        Map<String, Object> labels = new HashMap<>();
        labels.put("app", Arrays.asList("nginx", "redis", "postgres"));
        
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(bulkJobManager, labels);
        
        assertNotNull(result);
        String podFilter = result.get("podLabelFilter");
        // Should contain all three values
        assertTrue(podFilter.contains("label_app=\"nginx\""));
        assertTrue(podFilter.contains("label_app=\"redis\""));
        assertTrue(podFilter.contains("label_app=\"postgres\""));
    }

    @Test
    @DisplayName("Test buildLabelFilters with multiple labels")
    void testBuildLabelFilters_MultipleLabels() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("buildLabelFilters", Map.class);
        method.setAccessible(true);
        
        Map<String, Object> labels = new HashMap<>();
        labels.put("app", "nginx");
        labels.put("env", "production");
        labels.put("tier", "frontend");
        
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(bulkJobManager, labels);
        
        assertNotNull(result);
        String podFilter = result.get("podLabelFilter");
        // Should contain all labels
        assertTrue(podFilter.contains("label_app=\"nginx\""));
        assertTrue(podFilter.contains("label_env=\"production\""));
        assertTrue(podFilter.contains("label_tier=\"frontend\""));
    }

    @Test
    @DisplayName("Test buildLabelFilters with label value requiring escaping")
    void testBuildLabelFilters_EscapedValue() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("buildLabelFilters", Map.class);
        method.setAccessible(true);
        
        Map<String, Object> labels = new HashMap<>();
        labels.put("app", "value\"with\"quotes");
        
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(bulkJobManager, labels);
        
        assertNotNull(result);
        String podFilter = result.get("podLabelFilter");
        // Quotes should be escaped
        assertTrue(podFilter.contains("label_app=\"value\\\"with\\\"quotes\""));
    }

    // ==================== buildResourceFilters Tests ====================

    @Test
    @DisplayName("Test buildResourceFilters with null filter")
    void testBuildResourceFilters_Null() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("buildResourceFilters", BulkInput.Filter.class);
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(bulkJobManager, (BulkInput.Filter) null);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test buildResourceFilters with namespace filter")
    void testBuildResourceFilters_Namespace() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("buildResourceFilters", BulkInput.Filter.class);
        method.setAccessible(true);
        
        BulkInput.Filter filter = new BulkInput.Filter();
        filter.setNamespace(Arrays.asList("default", "kube-system"));
        
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(bulkJobManager, filter);
        
        assertNotNull(result);
        assertTrue(result.containsKey("namespaceRegex"));
        assertEquals("default|kube-system", result.get("namespaceRegex"));
    }

    @Test
    @DisplayName("Test buildResourceFilters with workload filter")
    void testBuildResourceFilters_Workload() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("buildResourceFilters", BulkInput.Filter.class);
        method.setAccessible(true);
        
        BulkInput.Filter filter = new BulkInput.Filter();
        filter.setWorkload(Arrays.asList("nginx", "redis"));
        
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(bulkJobManager, filter);
        
        assertNotNull(result);
        assertTrue(result.containsKey("workloadRegex"));
        assertEquals("nginx|redis", result.get("workloadRegex"));
    }

    @Test
    @DisplayName("Test buildResourceFilters with container filter")
    void testBuildResourceFilters_Container() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("buildResourceFilters", BulkInput.Filter.class);
        method.setAccessible(true);
        
        BulkInput.Filter filter = new BulkInput.Filter();
        filter.setContainers(Arrays.asList("app", "sidecar"));
        
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(bulkJobManager, filter);
        
        assertNotNull(result);
        assertTrue(result.containsKey("containerRegex"));
        assertEquals("app|sidecar", result.get("containerRegex"));
    }

    @Test
    @DisplayName("Test buildResourceFilters with label filter")
    void testBuildResourceFilters_Labels() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("buildResourceFilters", BulkInput.Filter.class);
        method.setAccessible(true);
        
        BulkInput.Filter filter = new BulkInput.Filter();
        Map<String, Object> labels = new HashMap<>();
        labels.put("app", "nginx");
        filter.setLabels(labels);
        
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(bulkJobManager, filter);
        
        assertNotNull(result);
        assertTrue(result.containsKey("podLabelFilter"));
        assertTrue(result.containsKey("namespaceLabelFilter"));
    }

    @Test
    @DisplayName("Test buildResourceFilters with all filters combined")
    void testBuildResourceFilters_Combined() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("buildResourceFilters", BulkInput.Filter.class);
        method.setAccessible(true);
        
        BulkInput.Filter filter = new BulkInput.Filter();
        filter.setNamespace(Arrays.asList("default"));
        filter.setWorkload(Arrays.asList("nginx"));
        filter.setContainers(Arrays.asList("app"));
        Map<String, Object> labels = new HashMap<>();
        labels.put("env", "prod");
        filter.setLabels(labels);
        
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(bulkJobManager, filter);
        
        assertNotNull(result);
        assertEquals(5, result.size());
        assertTrue(result.containsKey("namespaceRegex"));
        assertTrue(result.containsKey("workloadRegex"));
        assertTrue(result.containsKey("containerRegex"));
        assertTrue(result.containsKey("podLabelFilter"));
        assertTrue(result.containsKey("namespaceLabelFilter"));
    }

    @Test
    @DisplayName("Test buildResourceFilters with empty namespace list")
    void testBuildResourceFilters_EmptyNamespace() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("buildResourceFilters", BulkInput.Filter.class);
        method.setAccessible(true);
        
        BulkInput.Filter filter = new BulkInput.Filter();
        filter.setNamespace(new ArrayList<>());
        
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(bulkJobManager, filter);
        
        assertNotNull(result);
        assertTrue(result.containsKey("namespaceRegex"));
        assertEquals("", result.get("namespaceRegex"));
    }

    // ==================== getLabelsForExperimentName Tests ====================

    @Test
    @DisplayName("Test getLabelsForExperimentName with null filter")
    void testGetLabelsForExperimentName_Null() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("getLabelsForExperimentName", BulkInput.FilterWrapper.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(bulkJobManager, (BulkInput.FilterWrapper) null);
        assertNull(result);
    }

    @Test
    @DisplayName("Test getLabelsForExperimentName with no include filter")
    void testGetLabelsForExperimentName_NoInclude() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("getLabelsForExperimentName", BulkInput.FilterWrapper.class);
        method.setAccessible(true);
        
        BulkInput.FilterWrapper filter = new BulkInput.FilterWrapper();
        String result = (String) method.invoke(bulkJobManager, filter);
        assertNull(result);
    }

    @Test
    @DisplayName("Test getLabelsForExperimentName with empty labels")
    void testGetLabelsForExperimentName_EmptyLabels() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("getLabelsForExperimentName", BulkInput.FilterWrapper.class);
        method.setAccessible(true);
        
        BulkInput.FilterWrapper filterWrapper = new BulkInput.FilterWrapper();
        BulkInput.Filter filter = new BulkInput.Filter();
        filter.setLabels(new HashMap<>());
        filterWrapper.setInclude(filter);
        
        String result = (String) method.invoke(bulkJobManager, filterWrapper);
        assertNull(result);
    }

    @Test
    @DisplayName("Test getLabelsForExperimentName with single label")
    void testGetLabelsForExperimentName_SingleLabel() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("getLabelsForExperimentName", BulkInput.FilterWrapper.class);
        method.setAccessible(true);
        
        BulkInput.FilterWrapper filterWrapper = new BulkInput.FilterWrapper();
        BulkInput.Filter filter = new BulkInput.Filter();
        Map<String, Object> labels = new HashMap<>();
        labels.put("app", "nginx");
        filter.setLabels(labels);
        filterWrapper.setInclude(filter);
        
        String result = (String) method.invoke(bulkJobManager, filterWrapper);
        assertNotNull(result);
        assertEquals("app=\"nginx\"", result);
    }

    @Test
    @DisplayName("Test getLabelsForExperimentName with array value uses first element")
    void testGetLabelsForExperimentName_ArrayValue() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("getLabelsForExperimentName", BulkInput.FilterWrapper.class);
        method.setAccessible(true);
        
        BulkInput.FilterWrapper filterWrapper = new BulkInput.FilterWrapper();
        BulkInput.Filter filter = new BulkInput.Filter();
        Map<String, Object> labels = new HashMap<>();
        labels.put("app", Arrays.asList("nginx", "redis", "postgres"));
        filter.setLabels(labels);
        filterWrapper.setInclude(filter);
        
        String result = (String) method.invoke(bulkJobManager, filterWrapper);
        assertNotNull(result);
        // Should use first value from array
        assertEquals("app=\"nginx\"", result);
    }

    @Test
    @DisplayName("Test getLabelsForExperimentName with special characters in value")
    void testGetLabelsForExperimentName_SpecialChars() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("getLabelsForExperimentName", BulkInput.FilterWrapper.class);
        method.setAccessible(true);
        
        BulkInput.FilterWrapper filterWrapper = new BulkInput.FilterWrapper();
        BulkInput.Filter filter = new BulkInput.Filter();
        Map<String, Object> labels = new HashMap<>();
        labels.put("app", "value\"with\"quotes");
        filter.setLabels(labels);
        filterWrapper.setInclude(filter);
        
        String result = (String) method.invoke(bulkJobManager, filterWrapper);
        assertNotNull(result);
        // Should escape quotes
        assertTrue(result.contains("value\\\"with\\\"quotes"));
    }

    @Test
    @DisplayName("Test getLabelsForExperimentName preserves original key names")
    void testGetLabelsForExperimentName_PreservesKeys() throws Exception {
        Method method = BulkJobManager.class.getDeclaredMethod("getLabelsForExperimentName", BulkInput.FilterWrapper.class);
        method.setAccessible(true);
        
        BulkInput.FilterWrapper filterWrapper = new BulkInput.FilterWrapper();
        BulkInput.Filter filter = new BulkInput.Filter();
        Map<String, Object> labels = new HashMap<>();
        labels.put("app.kubernetes.io/component", "controller");
        filter.setLabels(labels);
        filterWrapper.setInclude(filter);
        
        String result = (String) method.invoke(bulkJobManager, filterWrapper);
        assertNotNull(result);
        // Should preserve original key with dots and slashes
        assertTrue(result.contains("app.kubernetes.io/component"));
    }

    @Test
    @DisplayName("Test getLabelsForExperimentName with multiple distinct labels (order unspecified)")
    void testGetLabelsForExperimentName_MultipleLabels() throws Exception {
        // Note: This test intentionally does not assert on label ordering, only on presence.
        // If getLabelsForExperimentName guarantees deterministic ordering, tighten these
        // assertions accordingly (e.g., by checking relative indexOf positions).

        Method method = BulkJobManager.class.getDeclaredMethod(
                "getLabelsForExperimentName",
                BulkInput.FilterWrapper.class
        );
        method.setAccessible(true);

        BulkInput.FilterWrapper filterWrapper = new BulkInput.FilterWrapper();
        BulkInput.Filter filter = new BulkInput.Filter();
        Map<String, Object> labels = new HashMap<>();
        labels.put("app", "payments-service");
        labels.put("env", "prod");
        labels.put("version", "v1");
        filter.setLabels(labels);
        filterWrapper.setInclude(filter);

        String result = (String) method.invoke(bulkJobManager, filterWrapper);

        assertNotNull(result);
        // Assert that all labels and their values are present in the resulting string,
        // irrespective of ordering.
        assertTrue(result.contains("app"));
        assertTrue(result.contains("payments-service"));
        assertTrue(result.contains("env"));
        assertTrue(result.contains("prod"));
        assertTrue(result.contains("version"));
        assertTrue(result.contains("v1"));
    }
}

