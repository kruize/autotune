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
package com.autotune.analyzer.kruizeObject;

import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.CreateExperimentAPIObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for multi-datasource support in Kruize experiments.
 * 
 * Tests cover:
 * 1. Prometheus-only experiments
 * 2. Cryostat-only experiments
 * 3. Combined Prometheus + Cryostat experiments
 * 4. Backward compatibility with single datasource field
 */
class MultiDatasourceTest {

    @Test
    @DisplayName("KruizeObject should support Prometheus-only datasource")
    void shouldSupportPrometheusOnlyDatasource() {
        // Given
        KruizeObject kruizeObject = new KruizeObject();
        List<String> datasources = Collections.singletonList("prometheus");
        
        // When
        kruizeObject.setDatasources(datasources);
        
        // Then
        assertNotNull(kruizeObject.getDatasources());
        assertEquals(1, kruizeObject.getDatasources().size());
        assertEquals("prometheus", kruizeObject.getDatasources().get(0));
    }

    @Test
    @DisplayName("KruizeObject should support Cryostat-only datasource")
    void shouldSupportCryostatOnlyDatasource() {
        // Given
        KruizeObject kruizeObject = new KruizeObject();
        List<String> datasources = Collections.singletonList("cryostat");
        
        // When
        kruizeObject.setDatasources(datasources);
        
        // Then
        assertNotNull(kruizeObject.getDatasources());
        assertEquals(1, kruizeObject.getDatasources().size());
        assertEquals("cryostat", kruizeObject.getDatasources().get(0));
    }

    @Test
    @DisplayName("KruizeObject should support multiple datasources (Prometheus + Cryostat)")
    void shouldSupportMultipleDatasources() {
        // Given
        KruizeObject kruizeObject = new KruizeObject();
        List<String> datasources = Arrays.asList("prometheus", "cryostat");
        
        // When
        kruizeObject.setDatasources(datasources);
        
        // Then
        assertNotNull(kruizeObject.getDatasources());
        assertEquals(2, kruizeObject.getDatasources().size());
        assertTrue(kruizeObject.getDatasources().contains("prometheus"));
        assertTrue(kruizeObject.getDatasources().contains("cryostat"));
    }

    @Test
    @DisplayName("KruizeObject should maintain backward compatibility with single datasource field")
    void shouldMaintainBackwardCompatibilityWithSingleDatasource() {
        // Given
        KruizeObject kruizeObject = new KruizeObject();
        
        // When - set using old single datasource field
        kruizeObject.setDataSource("prometheus");
        
        // Then - getDatasources() should return a list with single element
        List<String> datasources = kruizeObject.getDatasources();
        assertNotNull(datasources);
        assertEquals(1, datasources.size());
        assertEquals("prometheus", datasources.get(0));
    }

    @Test
    @DisplayName("KruizeObject getDatasources should prioritize datasources list over single datasource")
    void shouldPrioritizeDatasourcesListOverSingleDatasource() {
        // Given
        KruizeObject kruizeObject = new KruizeObject();
        
        // When - set both single and list
        kruizeObject.setDataSource("prometheus");
        kruizeObject.setDatasources(Arrays.asList("cryostat", "prometheus"));
        
        // Then - getDatasources() should return the list
        List<String> datasources = kruizeObject.getDatasources();
        assertNotNull(datasources);
        assertEquals(2, datasources.size());
        assertTrue(datasources.contains("prometheus"));
        assertTrue(datasources.contains("cryostat"));
    }

    @Test
    @DisplayName("CreateExperimentAPIObject should support Prometheus-only datasource")
    void createExperimentShouldSupportPrometheusOnly() {
        // Given
        CreateExperimentAPIObject apiObject = new CreateExperimentAPIObject();
        List<String> datasources = Collections.singletonList("prometheus");
        
        // When
        apiObject.setDatasources(datasources);
        
        // Then
        assertNotNull(apiObject.getDatasources());
        assertEquals(1, apiObject.getDatasources().size());
        assertEquals("prometheus", apiObject.getDatasources().get(0));
    }

    @Test
    @DisplayName("CreateExperimentAPIObject should support Cryostat-only datasource")
    void createExperimentShouldSupportCryostatOnly() {
        // Given
        CreateExperimentAPIObject apiObject = new CreateExperimentAPIObject();
        List<String> datasources = Collections.singletonList("cryostat");
        
        // When
        apiObject.setDatasources(datasources);
        
        // Then
        assertNotNull(apiObject.getDatasources());
        assertEquals(1, apiObject.getDatasources().size());
        assertEquals("cryostat", apiObject.getDatasources().get(0));
    }

    @Test
    @DisplayName("CreateExperimentAPIObject should support multiple datasources")
    void createExperimentShouldSupportMultipleDatasources() {
        // Given
        CreateExperimentAPIObject apiObject = new CreateExperimentAPIObject();
        List<String> datasources = Arrays.asList("prometheus", "cryostat");
        
        // When
        apiObject.setDatasources(datasources);
        
        // Then
        assertNotNull(apiObject.getDatasources());
        assertEquals(2, apiObject.getDatasources().size());
        assertTrue(apiObject.getDatasources().contains("prometheus"));
        assertTrue(apiObject.getDatasources().contains("cryostat"));
    }

    @Test
    @DisplayName("CreateExperimentAPIObject should maintain backward compatibility")
    void createExperimentShouldMaintainBackwardCompatibility() {
        // Given
        CreateExperimentAPIObject apiObject = new CreateExperimentAPIObject();
        
        // When - set using old single datasource field
        apiObject.setDatasource("prometheus");
        
        // Then - getDatasources() should return a list with single element
        List<String> datasources = apiObject.getDatasources();
        assertNotNull(datasources);
        assertEquals(1, datasources.size());
        assertEquals("prometheus", datasources.get(0));
    }

    @Test
    @DisplayName("Empty datasources list should be handled gracefully")
    void shouldHandleEmptyDatasourcesList() {
        // Given
        KruizeObject kruizeObject = new KruizeObject();
        List<String> datasources = Collections.emptyList();
        
        // When
        kruizeObject.setDatasources(datasources);
        
        // Then
        assertNotNull(kruizeObject.getDatasources());
        assertTrue(kruizeObject.getDatasources().isEmpty());
    }

    @Test
    @DisplayName("Null datasources should return empty list when no single datasource is set")
    void shouldReturnEmptyListWhenNoDatasourceSet() {
        // Given
        KruizeObject kruizeObject = new KruizeObject();
        
        // When - no datasource set
        List<String> datasources = kruizeObject.getDatasources();
        
        // Then
        assertNotNull(datasources);
        assertTrue(datasources.isEmpty());
    }

    @Test
    @DisplayName("Datasources order should be preserved")
    void shouldPreserveDatasourcesOrder() {
        // Given
        KruizeObject kruizeObject = new KruizeObject();
        List<String> datasources = Arrays.asList("cryostat", "prometheus");
        
        // When
        kruizeObject.setDatasources(datasources);
        
        // Then
        List<String> result = kruizeObject.getDatasources();
        assertEquals("cryostat", result.get(0));
        assertEquals("prometheus", result.get(1));
    }

    @Test
    @DisplayName("Duplicate datasources should be allowed in the list")
    void shouldAllowDuplicateDatasources() {
        // Given
        KruizeObject kruizeObject = new KruizeObject();
        List<String> datasources = Arrays.asList("prometheus", "prometheus");
        
        // When
        kruizeObject.setDatasources(datasources);
        
        // Then
        assertNotNull(kruizeObject.getDatasources());
        assertEquals(2, kruizeObject.getDatasources().size());
    }

    @Test
    @DisplayName("Case sensitivity should be preserved in datasource names")
    void shouldPreserveCaseSensitivity() {
        // Given
        KruizeObject kruizeObject = new KruizeObject();
        List<String> datasources = Arrays.asList("Prometheus", "CRYOSTAT");
        
        // When
        kruizeObject.setDatasources(datasources);
        
        // Then
        assertEquals("Prometheus", kruizeObject.getDatasources().get(0));
        assertEquals("CRYOSTAT", kruizeObject.getDatasources().get(1));
    }
}
