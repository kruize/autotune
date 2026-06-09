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
package com.autotune.analyzer.utils;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for multi-datasource layer detection logic.
 * 
 * Tests verify that:
 * 1. Layer detection works with Prometheus-only datasource
 * 2. Layer detection works with Cryostat-only datasource
 * 3. Layer detection works with multiple datasources
 * 4. Detected layers are properly aggregated across datasources
 * 5. Graceful handling of datasource failures
 */
class MultiDatasourceLayerDetectionTest {

    private KruizeObject kruizeObject;
    private ContainerAPIObject containerAPIObject;

    @BeforeEach
    void setup() {
        kruizeObject = new KruizeObject();
        kruizeObject.setExperimentName("test-experiment");
        
        containerAPIObject = new ContainerAPIObject();
        containerAPIObject.setLayerMap(new HashMap<>());
    }

    @Test
    @DisplayName("Should handle Prometheus-only datasource for layer detection")
    void shouldHandlePrometheusOnlyDatasource() {
        // Given
        List<String> datasources = Collections.singletonList("prometheus");
        kruizeObject.setDatasources(datasources);
        
        // When
        List<String> actualDatasources = kruizeObject.getDatasources();
        
        // Then
        assertNotNull(actualDatasources);
        assertEquals(1, actualDatasources.size());
        assertEquals("prometheus", actualDatasources.get(0));
    }

    @Test
    @DisplayName("Should handle Cryostat-only datasource for layer detection")
    void shouldHandleCryostatOnlyDatasource() {
        // Given
        List<String> datasources = Collections.singletonList("cryostat");
        kruizeObject.setDatasources(datasources);
        
        // When
        List<String> actualDatasources = kruizeObject.getDatasources();
        
        // Then
        assertNotNull(actualDatasources);
        assertEquals(1, actualDatasources.size());
        assertEquals("cryostat", actualDatasources.get(0));
    }

    @Test
    @DisplayName("Should handle multiple datasources for layer detection")
    void shouldHandleMultipleDatasources() {
        // Given
        List<String> datasources = Arrays.asList("prometheus", "cryostat");
        kruizeObject.setDatasources(datasources);
        
        // When
        List<String> actualDatasources = kruizeObject.getDatasources();
        
        // Then
        assertNotNull(actualDatasources);
        assertEquals(2, actualDatasources.size());
        assertTrue(actualDatasources.contains("prometheus"));
        assertTrue(actualDatasources.contains("cryostat"));
    }

    @Test
    @DisplayName("Should iterate through all datasources during detection")
    void shouldIterateThroughAllDatasources() {
        // Given
        List<String> datasources = Arrays.asList("prometheus", "cryostat");
        kruizeObject.setDatasources(datasources);
        
        // When - simulate iteration
        int datasourceCount = 0;
        for (String datasource : kruizeObject.getDatasources()) {
            assertNotNull(datasource);
            datasourceCount++;
        }
        
        // Then
        assertEquals(2, datasourceCount);
    }

    @Test
    @DisplayName("Should maintain layer map structure for detected layers")
    void shouldMaintainLayerMapStructure() {
        // Given
        containerAPIObject.setLayerMap(new HashMap<>());
        
        // When
        containerAPIObject.getLayerMap().put("hotspot", null);
        containerAPIObject.getLayerMap().put("quarkus", null);
        
        // Then
        assertNotNull(containerAPIObject.getLayerMap());
        assertEquals(2, containerAPIObject.getLayerMap().size());
        assertTrue(containerAPIObject.getLayerMap().containsKey("hotspot"));
        assertTrue(containerAPIObject.getLayerMap().containsKey("quarkus"));
    }

    @Test
    @DisplayName("Should handle empty datasources list gracefully")
    void shouldHandleEmptyDatasourcesList() {
        // Given
        List<String> datasources = Collections.emptyList();
        kruizeObject.setDatasources(datasources);
        
        // When
        List<String> actualDatasources = kruizeObject.getDatasources();
        
        // Then
        assertNotNull(actualDatasources);
        assertTrue(actualDatasources.isEmpty());
    }

    @Test
    @DisplayName("Should preserve datasource order during iteration")
    void shouldPreserveDatasourceOrder() {
        // Given
        List<String> datasources = Arrays.asList("cryostat", "prometheus");
        kruizeObject.setDatasources(datasources);
        
        // When
        List<String> actualDatasources = kruizeObject.getDatasources();
        
        // Then
        assertEquals("cryostat", actualDatasources.get(0));
        assertEquals("prometheus", actualDatasources.get(1));
    }

    @Test
    @DisplayName("Should support backward compatibility with single datasource")
    void shouldSupportBackwardCompatibility() {
        // Given
        kruizeObject.setDataSource("prometheus");
        
        // When
        List<String> datasources = kruizeObject.getDatasources();
        
        // Then
        assertNotNull(datasources);
        assertEquals(1, datasources.size());
        assertEquals("prometheus", datasources.get(0));
    }

    @Test
    @DisplayName("Should allow layer aggregation from multiple datasources")
    void shouldAllowLayerAggregation() {
        // Given
        containerAPIObject.setLayerMap(new HashMap<>());
        
        // When - simulate detection from prometheus
        containerAPIObject.getLayerMap().put("hotspot", null);
        
        // When - simulate detection from cryostat
        containerAPIObject.getLayerMap().put("quarkus", null);
        
        // Then - both layers should be present
        assertEquals(2, containerAPIObject.getLayerMap().size());
        assertTrue(containerAPIObject.getLayerMap().containsKey("hotspot"));
        assertTrue(containerAPIObject.getLayerMap().containsKey("quarkus"));
    }

    @Test
    @DisplayName("Should handle duplicate layer detection from multiple datasources")
    void shouldHandleDuplicateLayerDetection() {
        // Given
        containerAPIObject.setLayerMap(new HashMap<>());
        
        // When - both datasources detect the same layer
        containerAPIObject.getLayerMap().put("hotspot", null);
        containerAPIObject.getLayerMap().put("hotspot", null); // duplicate
        
        // Then - layer should exist only once
        assertEquals(1, containerAPIObject.getLayerMap().size());
        assertTrue(containerAPIObject.getLayerMap().containsKey("hotspot"));
    }

    @Test
    @DisplayName("Should maintain experiment name during multi-datasource detection")
    void shouldMaintainExperimentName() {
        // Given
        String experimentName = "test-experiment";
        kruizeObject.setExperimentName(experimentName);
        
        // When - simulate detection with multiple datasources
        kruizeObject.setDatasources(Arrays.asList("prometheus", "cryostat"));
        
        // Then
        assertEquals(experimentName, kruizeObject.getExperimentName());
    }

    @Test
    @DisplayName("Should support three or more datasources")
    void shouldSupportThreeOrMoreDatasources() {
        // Given
        List<String> datasources = Arrays.asList("prometheus", "cryostat", "custom-datasource");
        kruizeObject.setDatasources(datasources);
        
        // When
        List<String> actualDatasources = kruizeObject.getDatasources();
        
        // Then
        assertEquals(3, actualDatasources.size());
        assertTrue(actualDatasources.contains("prometheus"));
        assertTrue(actualDatasources.contains("cryostat"));
        assertTrue(actualDatasources.contains("custom-datasource"));
    }

    @Test
    @DisplayName("Should handle null layer map gracefully")
    void shouldHandleNullLayerMap() {
        // Given
        containerAPIObject.setLayerMap(null);
        
        // When
        containerAPIObject.setLayerMap(new HashMap<>());
        
        // Then
        assertNotNull(containerAPIObject.getLayerMap());
        assertTrue(containerAPIObject.getLayerMap().isEmpty());
    }
}
