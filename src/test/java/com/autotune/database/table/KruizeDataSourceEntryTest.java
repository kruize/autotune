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

package com.autotune.database.table;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for KruizeDataSourceEntry cluster functionality.
 * The clusters column is stored as JSONB (JsonNode array of strings).
 */
public class KruizeDataSourceEntryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Helper: build a JsonNode array from varargs strings */
    private static ArrayNode jsonArray(String... values) {
        ArrayNode arr = MAPPER.createArrayNode();
        for (String v : values) arr.add(v);
        return arr;
    }

    @Test
    @DisplayName("Set and get cluster list with multiple clusters")
    public void testSetAndGetClusterList() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        entry.setClusters(jsonArray("default", "stage", "prod"));

        List<String> result = entry.getClusterList();
        assertEquals(3, result.size());
        assertTrue(result.contains("default"));
        assertTrue(result.contains("stage"));
        assertTrue(result.contains("prod"));
    }

    @Test
    @DisplayName("Set and get single cluster")
    public void testSingleCluster() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        entry.setClusters(jsonArray("default"));

        List<String> result = entry.getClusterList();
        assertEquals(1, result.size());
        assertEquals("default", result.get(0));
    }

    @Test
    @DisplayName("Null clusters JsonNode returns empty list")
    public void testNullClustersNode() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        entry.setClusters(null);

        assertNull(entry.getClusters());
        assertTrue(entry.getClusterList().isEmpty());
    }

    @Test
    @DisplayName("Empty JsonNode array returns empty list")
    public void testEmptyArrayNode() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        entry.setClusters(MAPPER.createArrayNode());

        assertNotNull(entry.getClusters());
        assertTrue(entry.getClusterList().isEmpty());
    }

    @Test
    @DisplayName("getClusterList when clusters field is never set")
    public void testGetClusterListWhenNeverSet() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();

        List<String> result = entry.getClusterList();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Blank/whitespace-only elements are skipped by parseClusterList")
    public void testBlankElementsAreSkipped() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        ArrayNode arr = MAPPER.createArrayNode();
        arr.add("valid-cluster");
        arr.add("  ");          // whitespace-only — should be skipped
        arr.add("");            // empty string — should be skipped
        arr.add("another-valid");
        entry.setClusters(arr);

        List<String> result = entry.getClusterList();
        assertEquals(2, result.size());
        assertTrue(result.contains("valid-cluster"));
        assertTrue(result.contains("another-valid"));
    }

    @Test
    @DisplayName("Names exceeding 253 characters are skipped by parseClusterList")
    public void testTooLongNamesAreSkipped() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        String tooLong = "a".repeat(254);
        String atLimit  = "a".repeat(253);
        entry.setClusters(jsonArray("short-name", tooLong, atLimit));

        List<String> result = entry.getClusterList();
        assertEquals(2, result.size());
        assertTrue(result.contains("short-name"));
        assertTrue(result.contains(atLimit));
        assertFalse(result.contains(tooLong));
    }

    @Test
    @DisplayName("Mixed valid and invalid cluster names - only valid ones returned")
    public void testMixedValidAndInvalid() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        String tooLong = "x".repeat(300);
        ArrayNode arr = MAPPER.createArrayNode();
        arr.add("cluster1");
        arr.add("");
        arr.add("cluster2");
        arr.add("   ");
        arr.add(tooLong);
        arr.add("cluster3");
        entry.setClusters(arr);

        List<String> result = entry.getClusterList();
        assertEquals(3, result.size());
        assertEquals("cluster1", result.get(0));
        assertEquals("cluster2", result.get(1));
        assertEquals("cluster3", result.get(2));
    }

    @Test
    @DisplayName("Cluster names with special characters are preserved")
    public void testClustersWithSpecialCharacters() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        entry.setClusters(jsonArray("cluster-1", "cluster_2", "cluster.3"));

        List<String> result = entry.getClusterList();
        assertEquals(3, result.size());
        assertTrue(result.contains("cluster-1"));
        assertTrue(result.contains("cluster_2"));
        assertTrue(result.contains("cluster.3"));
    }

    @Test
    @DisplayName("Backward compatibility - datasource without clusters")
    public void testBackwardCompatibility() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        entry.setName("prometheus-1");
        entry.setProvider("prometheus");
        entry.setServiceName("prometheus-k8s");
        entry.setNamespace("openshift-monitoring");
        entry.setUrl("https://prometheus:9090");

        // No clusters set - simulating old datasource row (NULL column)
        assertNull(entry.getClusters());
        assertTrue(entry.getClusterList().isEmpty());
        assertEquals("prometheus-1", entry.getName());
    }

    @Test
    @DisplayName("Complete datasource entry with clusters")
    public void testCompleteDatasourceEntry() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        entry.setVersion("v1.0");
        entry.setName("prometheus-1");
        entry.setProvider("prometheus");
        entry.setServiceName("prometheus-k8s");
        entry.setNamespace("openshift-monitoring");
        entry.setUrl("https://prometheus:9090");
        entry.setClusters(jsonArray("default", "stage", "prod"));

        assertEquals("v1.0", entry.getVersion());
        assertEquals("prometheus-1", entry.getName());
        assertEquals(3, entry.getClusterList().size());
        assertTrue(entry.getClusterList().contains("default"));
        assertTrue(entry.getClusterList().contains("stage"));
        assertTrue(entry.getClusterList().contains("prod"));
    }
}
