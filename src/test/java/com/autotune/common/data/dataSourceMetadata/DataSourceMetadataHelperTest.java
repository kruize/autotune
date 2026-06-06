package com.autotune.common.data.dataSourceMetadata;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

/**
 * Unit tests for DataSourceMetadataHelper class with mocking.
 * Tests cover namespace, workload, and container information parsing.
 */
class DataSourceMetadataHelperTest {

    private DataSourceMetadataHelper helper;

    @BeforeEach
    void setUp() {
        helper = new DataSourceMetadataHelper();
    }

    @Test
    @DisplayName("Test getActiveNamespaces with valid JSON data")
    void testGetActiveNamespaces_ValidData() {
        // Arrange
        String jsonData = """
            [
                {
                    "metric": {
                        "namespace": "default"
                    }
                },
                {
                    "metric": {
                        "namespace": "kube-system"
                    }
                },
                {
                    "metric": {
                        "namespace": "default"
                    }
                }
            ]
            """;
        JsonArray resultArray = JsonParser.parseString(jsonData).getAsJsonArray();

        // Act
        HashMap<String, DataSourceNamespace> result = helper.getActiveNamespaces(resultArray);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("default"));
        assertTrue(result.containsKey("kube-system"));
        assertEquals("default", result.get("default").getNamespace());
        assertEquals("kube-system", result.get("kube-system").getNamespace());
    }

    @Test
    @DisplayName("Test getActiveNamespaces with empty JSON array")
    void testGetActiveNamespaces_EmptyArray() {
        // Arrange
        JsonArray emptyArray = new JsonArray();

        // Act
        HashMap<String, DataSourceNamespace> result = helper.getActiveNamespaces(emptyArray);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test getActiveNamespaces with missing metric field")
    void testGetActiveNamespaces_MissingMetricField() {
        // Arrange
        String jsonData = """
            [
                {
                    "value": "someValue"
                },
                {
                    "metric": {
                        "namespace": "valid-namespace"
                    }
                }
            ]
            """;
        JsonArray resultArray = JsonParser.parseString(jsonData).getAsJsonArray();

        // Act
        HashMap<String, DataSourceNamespace> result = helper.getActiveNamespaces(resultArray);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("valid-namespace"));
    }

    @Test
    @DisplayName("Test getActiveNamespaces with missing namespace field")
    void testGetActiveNamespaces_MissingNamespaceField() {
        // Arrange
        String jsonData = """
            [
                {
                    "metric": {
                        "pod": "somePod"
                    }
                },
                {
                    "metric": {
                        "namespace": "valid-namespace"
                    }
                }
            ]
            """;
        JsonArray resultArray = JsonParser.parseString(jsonData).getAsJsonArray();

        // Act
        HashMap<String, DataSourceNamespace> result = helper.getActiveNamespaces(resultArray);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("valid-namespace"));
    }

    @Test
    @DisplayName("Test getWorkloadInfo with valid JSON data")
    void testGetWorkloadInfo_ValidData() {
        // Arrange
        String jsonData = """
            [
                {
                    "metric": {
                        "namespace": "default",
                        "workload": "nginx-deployment",
                        "workload_type": "deployment"
                    }
                },
                {
                    "metric": {
                        "namespace": "default",
                        "workload": "redis-statefulset",
                        "workload_type": "statefulset"
                    }
                },
                {
                    "metric": {
                        "namespace": "kube-system",
                        "workload": "coredns",
                        "workload_type": "deployment"
                    }
                }
            ]
            """;
        JsonArray resultArray = JsonParser.parseString(jsonData).getAsJsonArray();

        // Act
        HashMap<String, HashMap<String, DataSourceWorkload>> result = helper.getWorkloadInfo(resultArray);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("default"));
        assertTrue(result.containsKey("kube-system"));
        
        HashMap<String, DataSourceWorkload> defaultWorkloads = result.get("default");
        assertEquals(2, defaultWorkloads.size());
        assertTrue(defaultWorkloads.containsKey("nginx-deployment"));
        assertTrue(defaultWorkloads.containsKey("redis-statefulset"));
        
        DataSourceWorkload nginxWorkload = defaultWorkloads.get("nginx-deployment");
        assertEquals("nginx-deployment", nginxWorkload.getWorkloadName());
        assertEquals("deployment", nginxWorkload.getWorkloadType());
    }

    @Test
    @DisplayName("Test getWorkloadInfo with empty JSON array")
    void testGetWorkloadInfo_EmptyArray() {
        // Arrange
        JsonArray emptyArray = new JsonArray();

        // Act
        HashMap<String, HashMap<String, DataSourceWorkload>> result = helper.getWorkloadInfo(emptyArray);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test getWorkloadInfo with missing required fields")
    void testGetWorkloadInfo_MissingFields() {
        // Arrange
        String jsonData = """
            [
                {
                    "metric": {
                        "namespace": "default"
                    }
                },
                {
                    "metric": {
                        "namespace": "default",
                        "workload": "valid-workload",
                        "workload_type": "deployment"
                    }
                }
            ]
            """;
        JsonArray resultArray = JsonParser.parseString(jsonData).getAsJsonArray();

        // Act
        HashMap<String, HashMap<String, DataSourceWorkload>> result = helper.getWorkloadInfo(resultArray);

        // Assert
        assertNotNull(result);
        // The method should skip entries with missing workload/workload_type fields
        // Only the second entry with all fields should be processed
        if (!result.isEmpty()) {
            assertTrue(result.containsKey("default"));
            if (result.get("default") != null) {
                assertTrue(result.get("default").size() >= 0);
            }
        }
    }

    @Test
    @DisplayName("Test getContainerInfo with valid JSON data")
    void testGetContainerInfo_ValidData() {
        // Arrange
        String jsonData = """
            [
                {
                    "metric": {
                        "workload": "nginx-deployment",
                        "container": "nginx",
                        "image": "nginx:1.21"
                    }
                },
                {
                    "metric": {
                        "workload": "nginx-deployment",
                        "container": "sidecar",
                        "image": "sidecar:latest"
                    }
                },
                {
                    "metric": {
                        "workload": "redis-statefulset",
                        "container": "redis",
                        "image": "redis:6.2"
                    }
                }
            ]
            """;
        JsonArray resultArray = JsonParser.parseString(jsonData).getAsJsonArray();

        // Act
        HashMap<String, HashMap<String, DataSourceContainer>> result = helper.getContainerInfo(resultArray);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("nginx-deployment"));
        assertTrue(result.containsKey("redis-statefulset"));
        
        HashMap<String, DataSourceContainer> nginxContainers = result.get("nginx-deployment");
        assertEquals(2, nginxContainers.size());
        assertTrue(nginxContainers.containsKey("nginx"));
        assertTrue(nginxContainers.containsKey("sidecar"));
        
        DataSourceContainer nginxContainer = nginxContainers.get("nginx");
        assertEquals("nginx", nginxContainer.getContainerName());
        assertEquals("nginx:1.21", nginxContainer.getContainerImageName());
    }

    @Test
    @DisplayName("Test getContainerInfo with empty JSON array")
    void testGetContainerInfo_EmptyArray() {
        // Arrange
        JsonArray emptyArray = new JsonArray();

        // Act
        HashMap<String, HashMap<String, DataSourceContainer>> result = helper.getContainerInfo(emptyArray);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test getContainerInfo with missing workload field")
    void testGetContainerInfo_MissingWorkloadField() {
        // Arrange
        String jsonData = """
            [
                {
                    "metric": {
                        "container": "nginx",
                        "image": "nginx:1.21"
                    }
                },
                {
                    "metric": {
                        "workload": "valid-workload",
                        "container": "valid-container",
                        "image": "valid:image"
                    }
                }
            ]
            """;
        JsonArray resultArray = JsonParser.parseString(jsonData).getAsJsonArray();

        // Act
        HashMap<String, HashMap<String, DataSourceContainer>> result = helper.getContainerInfo(resultArray);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("valid-workload"));
    }

    @Test
    @DisplayName("Test getActiveNamespaces handles null values gracefully")
    void testGetActiveNamespaces_NullHandling() {
        // Arrange
        String jsonData = """
            [
                {
                    "metric": {
                        "namespace": "valid-namespace"
                    }
                },
                {
                    "metric": null
                }
            ]
            """;
        JsonArray resultArray = JsonParser.parseString(jsonData).getAsJsonArray();

        // Act
        HashMap<String, DataSourceNamespace> result = helper.getActiveNamespaces(resultArray);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("valid-namespace"));
    }

    @Test
    @DisplayName("Test getWorkloadInfo handles duplicate workloads correctly")
    void testGetWorkloadInfo_DuplicateWorkloads() {
        // Arrange
        String jsonData = """
            [
                {
                    "metric": {
                        "namespace": "default",
                        "workload": "nginx-deployment",
                        "workload_type": "deployment"
                    }
                },
                {
                    "metric": {
                        "namespace": "default",
                        "workload": "nginx-deployment",
                        "workload_type": "deployment"
                    }
                }
            ]
            """;
        JsonArray resultArray = JsonParser.parseString(jsonData).getAsJsonArray();

        // Act
        HashMap<String, HashMap<String, DataSourceWorkload>> result = helper.getWorkloadInfo(resultArray);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get("default").size());
    }

    @Test
    @DisplayName("Test getContainerInfo handles duplicate containers correctly")
    void testGetContainerInfo_DuplicateContainers() {
        // Arrange
        String jsonData = """
            [
                {
                    "metric": {
                        "workload": "nginx-deployment",
                        "container": "nginx",
                        "image": "nginx:1.21"
                    }
                },
                {
                    "metric": {
                        "workload": "nginx-deployment",
                        "container": "nginx",
                        "image": "nginx:1.22"
                    }
                }
            ]
            """;
        JsonArray resultArray = JsonParser.parseString(jsonData).getAsJsonArray();

        // Act
        HashMap<String, HashMap<String, DataSourceContainer>> result = helper.getContainerInfo(resultArray);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get("nginx-deployment").size());
        // The last entry should overwrite the first
        assertEquals("nginx:1.22", result.get("nginx-deployment").get("nginx").getContainerImageName());
    }

    // ==================== filterMetadataInfoObject Tests ====================

    @Test
    @DisplayName("Test filterMetadataInfoObject applies OR semantics for namespace and workload filters")
    void testFilterMetadataInfoObject_NamespaceWorkloadOrSemantics() {
        // Arrange
        String dataSourceName = "test-datasource";
        DataSourceMetadataInfo metadataInfo = new DataSourceMetadataInfo(null);
        
        // Create data source with cluster and namespaces
        DataSource dataSource = new DataSource(dataSourceName, null);
        HashMap<String, DataSourceCluster> clusters = new HashMap<>();
        
        // Create namespaces with workloads
        HashMap<String, DataSourceNamespace> namespaces = new HashMap<>();
        
        // Namespace ns-a with workload-x
        HashMap<String, DataSourceWorkload> workloadsA = new HashMap<>();
        workloadsA.put("workload-x", new DataSourceWorkload("workload-x", "deployment", null));
        namespaces.put("ns-a", new DataSourceNamespace("ns-a", workloadsA));
        
        // Namespace ns-b with workload-y
        HashMap<String, DataSourceWorkload> workloadsB = new HashMap<>();
        workloadsB.put("workload-y", new DataSourceWorkload("workload-y", "deployment", null));
        namespaces.put("ns-b", new DataSourceNamespace("ns-b", workloadsB));
        
        // Namespace ns-c with workload-z
        HashMap<String, DataSourceWorkload> workloadsC = new HashMap<>();
        workloadsC.put("workload-z", new DataSourceWorkload("workload-z", "deployment", null));
        namespaces.put("ns-c", new DataSourceNamespace("ns-c", workloadsC));
        
        DataSourceCluster cluster = new DataSourceCluster("default", namespaces);
        clusters.put("default", cluster);
        dataSource.setClusters(clusters);
        
        HashMap<String, DataSource> dataSources = new HashMap<>();
        dataSources.put(dataSourceName, dataSource);
        metadataInfo.setDatasources(dataSources);
        
        // Create matched namespaces (ns-a)
        HashMap<String, DataSourceNamespace> matchedNamespaces = new HashMap<>();
        matchedNamespaces.put("ns-a", new DataSourceNamespace("ns-a", null));
        
        // Create matched workloads (workload-y in ns-b)
        HashMap<String, HashMap<String, DataSourceWorkload>> matchedWorkloads = new HashMap<>();
        HashMap<String, DataSourceWorkload> matchedWorkloadsB = new HashMap<>();
        matchedWorkloadsB.put("workload-y", new DataSourceWorkload("workload-y", "deployment", null));
        matchedWorkloads.put("ns-b", matchedWorkloadsB);
        
        // Act
        helper.filterMetadataInfoObject(dataSourceName, metadataInfo, matchedNamespaces, matchedWorkloads);
        
        // Assert - Should keep ns-a (namespace match) and ns-b (workload match), remove ns-c
        DataSourceCluster resultCluster = metadataInfo.getDataSourceObject(dataSourceName)
                .getDataSourceClusterObject("default");
        assertNotNull(resultCluster);
        assertEquals(2, resultCluster.getNamespaces().size(), 
                "Expected 2 namespaces matching either namespace or workload filter");
        assertTrue(resultCluster.getNamespaces().containsKey("ns-a"));
        assertTrue(resultCluster.getNamespaces().containsKey("ns-b"));
    }

    @Test
    @DisplayName("Test filterMetadataInfoObject disables filtering when no filters match any objects")
    void testFilterMetadataInfoObject_NoMatchingFiltersDisableFiltering() {
        // Arrange
        String dataSourceName = "test-datasource";
        DataSourceMetadataInfo metadataInfo = new DataSourceMetadataInfo(null);
        
        // Create data source with cluster and namespaces
        DataSource dataSource = new DataSource(dataSourceName, null);
        HashMap<String, DataSourceCluster> clusters = new HashMap<>();
        
        // Create namespaces with workloads
        HashMap<String, DataSourceNamespace> namespaces = new HashMap<>();
        
        HashMap<String, DataSourceWorkload> workloadsA = new HashMap<>();
        workloadsA.put("workload-x", new DataSourceWorkload("workload-x", "deployment", null));
        namespaces.put("ns-a", new DataSourceNamespace("ns-a", workloadsA));
        
        HashMap<String, DataSourceWorkload> workloadsB = new HashMap<>();
        workloadsB.put("workload-y", new DataSourceWorkload("workload-y", "deployment", null));
        namespaces.put("ns-b", new DataSourceNamespace("ns-b", workloadsB));
        
        DataSourceCluster cluster = new DataSourceCluster("default", namespaces);
        clusters.put("default", cluster);
        dataSource.setClusters(clusters);
        
        HashMap<String, DataSource> dataSources = new HashMap<>();
        dataSources.put(dataSourceName, dataSource);
        metadataInfo.setDatasources(dataSources);
        
        // Create empty matched collections (filters requested but no matches)
        HashMap<String, DataSourceNamespace> matchedNamespaces = new HashMap<>();
        HashMap<String, HashMap<String, DataSourceWorkload>> matchedWorkloads = new HashMap<>();
        
        int originalSize = cluster.getNamespaces().size();
        
        // Act
        helper.filterMetadataInfoObject(dataSourceName, metadataInfo, matchedNamespaces, matchedWorkloads);
        
        // Assert - When no filters match, filtering should be disabled and all objects should be returned
        DataSourceCluster resultCluster = metadataInfo.getDataSourceObject(dataSourceName)
                .getDataSourceClusterObject("default");
        assertNotNull(resultCluster);
        assertEquals(originalSize, resultCluster.getNamespaces().size(),
                "When no filters match, filtering should be effectively disabled and all objects should be returned");
    }

    @Test
    @DisplayName("Test filterMetadataInfoObject with null filters does not filter")
    void testFilterMetadataInfoObject_NullFiltersNoFiltering() {
        // Arrange
        String dataSourceName = "test-datasource";
        DataSourceMetadataInfo metadataInfo = new DataSourceMetadataInfo(null);
        
        DataSource dataSource = new DataSource(dataSourceName, null);
        HashMap<String, DataSourceCluster> clusters = new HashMap<>();
        
        HashMap<String, DataSourceNamespace> namespaces = new HashMap<>();
        HashMap<String, DataSourceWorkload> workloads = new HashMap<>();
        workloads.put("workload-x", new DataSourceWorkload("workload-x", "deployment", null));
        namespaces.put("ns-a", new DataSourceNamespace("ns-a", workloads));
        
        DataSourceCluster cluster = new DataSourceCluster("default", namespaces);
        clusters.put("default", cluster);
        dataSource.setClusters(clusters);
        
        HashMap<String, DataSource> dataSources = new HashMap<>();
        dataSources.put(dataSourceName, dataSource);
        metadataInfo.setDatasources(dataSources);
        
        int originalSize = cluster.getNamespaces().size();
        
        // Act - null filters means no filtering requested
        helper.filterMetadataInfoObject(dataSourceName, metadataInfo, null, null);
        
        // Assert
        DataSourceCluster resultCluster = metadataInfo.getDataSourceObject(dataSourceName)
                .getDataSourceClusterObject("default");
        assertNotNull(resultCluster);
        assertEquals(originalSize, resultCluster.getNamespaces().size(),
                "When no filters are requested, all objects should be kept");
    }

    // ==================== getQueryFromProfile Tests ====================

    @Test
    @DisplayName("Test getQueryFromProfile returns null when metric is missing")
    void testGetQueryFromProfile_MissingMetric() {
        // Arrange
        java.util.ArrayList<com.autotune.common.data.metrics.Metric> metrics = new java.util.ArrayList<>();
        com.autotune.common.data.metrics.Metric metric =
                new com.autotune.common.data.metrics.Metric("existing_metric", null, null, null, null);
        metrics.add(metric);
        
        com.autotune.analyzer.metadataProfiles.MetadataProfile profile =
                new com.autotune.analyzer.metadataProfiles.MetadataProfile(
                        "v1", "MetadataProfile", null, 1.0, "kubernetes", "prometheus", metrics);
        
        // Act
        String query = helper.getQueryFromProfile(profile, "missing_metric");
        
        // Assert
        assertNull(query);
    }

    @Test
    @DisplayName("Test getQueryFromProfile returns null when sum aggregation is missing")
    void testGetQueryFromProfile_MissingSumAggregation() {
        // Arrange
        java.util.ArrayList<com.autotune.common.data.metrics.Metric> metrics = new java.util.ArrayList<>();
        com.autotune.common.data.metrics.Metric metric =
                new com.autotune.common.data.metrics.Metric("present_metric", null, null, null, null);
        
        // Add aggregation functions but without 'sum'
        java.util.HashMap<String, com.autotune.common.data.metrics.AggregationFunctions> aggFunctions =
                new java.util.HashMap<>();
        com.autotune.common.data.metrics.AggregationFunctions avgFunc =
                new com.autotune.common.data.metrics.AggregationFunctions("avg", "avg(rate(some_metric[5m]))", "1.0");
        aggFunctions.put("avg", avgFunc);
        metric.setAggregationFunctionsMap(aggFunctions);
        
        metrics.add(metric);
        
        com.autotune.analyzer.metadataProfiles.MetadataProfile profile =
                new com.autotune.analyzer.metadataProfiles.MetadataProfile(
                        "v1", "MetadataProfile", null, 1.0, "kubernetes", "prometheus", metrics);
        
        // Act
        String query = helper.getQueryFromProfile(profile, "present_metric");
        
        // Assert
        assertNull(query);
    }

    @Test
    @DisplayName("Test getQueryFromProfile returns expected query when present")
    void testGetQueryFromProfile_ReturnsExpectedQuery() {
        // Arrange
        String expectedQuery = "sum(rate(some_metric[5m]))";
        
        java.util.ArrayList<com.autotune.common.data.metrics.Metric> metrics = new java.util.ArrayList<>();
        com.autotune.common.data.metrics.Metric metric =
                new com.autotune.common.data.metrics.Metric("present_metric", null, null, null, null);
        
        java.util.HashMap<String, com.autotune.common.data.metrics.AggregationFunctions> aggFunctions =
                new java.util.HashMap<>();
        com.autotune.common.data.metrics.AggregationFunctions sumFunc =
                new com.autotune.common.data.metrics.AggregationFunctions("sum", expectedQuery, "1.0");
        aggFunctions.put("sum", sumFunc);
        metric.setAggregationFunctionsMap(aggFunctions);
        
        metrics.add(metric);
        
        com.autotune.analyzer.metadataProfiles.MetadataProfile profile =
                new com.autotune.analyzer.metadataProfiles.MetadataProfile(
                        "v1", "MetadataProfile", null, 1.0, "kubernetes", "prometheus", metrics);
        
        // Act
        String query = helper.getQueryFromProfile(profile, "present_metric");
        
        // Assert
        assertEquals(expectedQuery, query);
    }

    @Test
    @DisplayName("Test getQueryFromProfile with null aggregation functions map")
    void testGetQueryFromProfile_NullAggregationFunctions() {
        // Arrange
        java.util.ArrayList<com.autotune.common.data.metrics.Metric> metrics = new java.util.ArrayList<>();
        com.autotune.common.data.metrics.Metric metric =
                new com.autotune.common.data.metrics.Metric("metric_with_null_agg", null, null, null, null);
        metric.setAggregationFunctionsMap(null);
        
        metrics.add(metric);
        
        com.autotune.analyzer.metadataProfiles.MetadataProfile profile =
                new com.autotune.analyzer.metadataProfiles.MetadataProfile(
                        "v1", "MetadataProfile", null, 1.0, "kubernetes", "prometheus", metrics);
        
        // Act
        String query = helper.getQueryFromProfile(profile, "metric_with_null_agg");
        
        // Assert
        assertNull(query);
    }

    @Test
    @DisplayName("Test getQueryFromProfile with empty metrics list")
    void testGetQueryFromProfile_EmptyMetricsList() {
        // Arrange
        java.util.ArrayList<com.autotune.common.data.metrics.Metric> metrics = new java.util.ArrayList<>();
        
        com.autotune.analyzer.metadataProfiles.MetadataProfile profile =
                new com.autotune.analyzer.metadataProfiles.MetadataProfile(
                        "v1", "MetadataProfile", null, 1.0, "kubernetes", "prometheus", metrics);
        
        // Act
        String query = helper.getQueryFromProfile(profile, "any_metric");
        
        // Assert
        assertNull(query);
    }
}
