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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for KruizeDataSourceEntry cluster functionality
 */
public class KruizeDataSourceEntryTest {

    @Test
    @DisplayName("Test setting and getting cluster list with multiple clusters")
    public void testSetAndGetClusterList() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        List<String> clusters = Arrays.asList("default", "stage", "prod");
        
        entry.setClusterList(clusters);
        
        assertEquals("default,stage,prod", entry.getClusters());
        assertEquals(clusters, entry.getClusterList());
    }

    @Test
    @DisplayName("Test setting and getting single cluster")
    public void testSingleCluster() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        List<String> clusters = Arrays.asList("default");
        
        entry.setClusterList(clusters);
        
        assertEquals("default", entry.getClusters());
        assertEquals(1, entry.getClusterList().size());
        assertEquals("default", entry.getClusterList().get(0));
    }

    @Test
    @DisplayName("Test null cluster list handling")
    public void testNullClusterList() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        
        entry.setClusterList(null);
        
        assertNull(entry.getClusters());
        assertTrue(entry.getClusterList().isEmpty());
    }

    @Test
    @DisplayName("Test empty cluster list handling")
    public void testEmptyClusterList() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        
        entry.setClusterList(new ArrayList<>());
        
        assertNull(entry.getClusters());
        assertTrue(entry.getClusterList().isEmpty());
    }

    @Test
    @DisplayName("Test getting cluster list when clusters field is null")
    public void testGetClusterListWhenNull() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        
        // Don't set any clusters
        List<String> result = entry.getClusterList();
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test getting cluster list when clusters field is empty string")
    public void testGetClusterListWhenEmptyString() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        entry.setClusters("");
        
        List<String> result = entry.getClusterList();
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test getting cluster list when clusters field has whitespace")
    public void testGetClusterListWithWhitespace() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        entry.setClusters("   ");
        
        List<String> result = entry.getClusterList();
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test comma-separated string conversion")
    public void testCommaSeparatedStringConversion() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        entry.setClusters("cluster1,cluster2,cluster3");
        
        List<String> result = entry.getClusterList();
        
        assertEquals(3, result.size());
        assertEquals("cluster1", result.get(0));
        assertEquals("cluster2", result.get(1));
        assertEquals("cluster3", result.get(2));
    }

    @Test
    @DisplayName("Test cluster list immutability")
    public void testClusterListImmutability() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        List<String> originalClusters = new ArrayList<>(Arrays.asList("default", "stage"));
        
        entry.setClusterList(originalClusters);
        
        // Modify the original list after setting
        originalClusters.add("prod");
        
        // Entry should not be affected by modification of original list
        assertEquals(2, entry.getClusterList().size());
        assertFalse(entry.getClusterList().contains("prod"));
        
        // Now test that retrieved list is also independent
        List<String> retrievedClusters = entry.getClusterList();
        retrievedClusters.add("test");
        
        // Entry should not be affected by modification of retrieved list
        assertEquals(2, entry.getClusterList().size());
        assertFalse(entry.getClusterList().contains("test"));
    }

    @Test
    @DisplayName("Test setting clusters with special characters")
    public void testClustersWithSpecialCharacters() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        List<String> clusters = Arrays.asList("cluster-1", "cluster_2", "cluster.3");
        
        entry.setClusterList(clusters);
        
        assertEquals("cluster-1,cluster_2,cluster.3", entry.getClusters());
        assertEquals(clusters, entry.getClusterList());
    }

    @Test
    @DisplayName("Test backward compatibility - datasource without clusters")
    public void testBackwardCompatibility() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        entry.setName("prometheus-1");
        entry.setProvider("prometheus");
        entry.setServiceName("prometheus-k8s");
        entry.setNamespace("openshift-monitoring");
        entry.setUrl("https://prometheus:9090");
        
        // Don't set clusters - simulating old datasource
        assertNull(entry.getClusters());
        assertTrue(entry.getClusterList().isEmpty());
        
        // Should still work fine
        assertEquals("prometheus-1", entry.getName());
        assertEquals("prometheus", entry.getProvider());
    }

    @Test
    @DisplayName("Test complete datasource entry with clusters")
    public void testCompleteDatasourceEntry() {
        KruizeDataSourceEntry entry = new KruizeDataSourceEntry();
        entry.setVersion("v1.0");
        entry.setName("prometheus-1");
        entry.setProvider("prometheus");
        entry.setServiceName("prometheus-k8s");
        entry.setNamespace("openshift-monitoring");
        entry.setUrl("https://prometheus:9090");
        entry.setClusterList(Arrays.asList("default", "stage", "prod"));
        
        assertEquals("v1.0", entry.getVersion());
        assertEquals("prometheus-1", entry.getName());
        assertEquals("prometheus", entry.getProvider());
        assertEquals("prometheus-k8s", entry.getServiceName());
        assertEquals("openshift-monitoring", entry.getNamespace());
        assertEquals("https://prometheus:9090", entry.getUrl());
        assertEquals(3, entry.getClusterList().size());
        assertTrue(entry.getClusterList().contains("default"));
        assertTrue(entry.getClusterList().contains("stage"));
        assertTrue(entry.getClusterList().contains("prod"));
    }
}
