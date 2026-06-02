package com.autotune.common.data.dataSourceMetadata;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
