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

package com.autotune.analyzer.recommendations;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Variation class
 */
public class VariationTest {

    private Variation variation;
    private Gson gson;

    @BeforeEach
    public void setUp() {
        variation = new Variation();
        gson = new GsonBuilder().create();
    }

    @Test
    public void testDefaultConstructor() {
        // Given & When
        Variation newVariation = new Variation();

        // Then
        assertNotNull(newVariation);
        assertNull(newVariation.getReplicas());
        assertNull(newVariation.getResources());
        assertNull(newVariation.getRequests());
        assertNull(newVariation.getLimits());
    }

    @Test
    public void testSetAndGetReplicas() {
        // Given
        Integer replicas = 3;

        // When
        variation.setReplicas(replicas);

        // Then
        assertEquals(replicas, variation.getReplicas());
    }

    @Test
    public void testSetAndGetReplicasWithNull() {
        // Given
        variation.setReplicas(5);

        // When
        variation.setReplicas(null);

        // Then
        assertNull(variation.getReplicas());
    }

    @Test
    public void testSetAndGetReplicasWithZero() {
        // Given
        Integer replicas = 0;

        // When
        variation.setReplicas(replicas);

        // Then
        assertEquals(0, variation.getReplicas());
    }

    @Test
    public void testSetAndGetRequests() {
        // Given
        Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requests = new HashMap<>();
        RecommendationConfigItem cpuItem = new RecommendationConfigItem(2.0, "cores");
        requests.put(AnalyzerConstants.RecommendationItem.CPU, cpuItem);

        // When
        variation.setRequests(requests);

        // Then
        assertNotNull(variation.getRequests());
        assertEquals(1, variation.getRequests().size());
        assertTrue(variation.getRequests().containsKey(AnalyzerConstants.RecommendationItem.CPU));
        assertEquals(2.0, variation.getRequests().get(AnalyzerConstants.RecommendationItem.CPU).getAmount());
    }

    @Test
    public void testSetAndGetRequestsWithMultipleItems() {
        // Given
        Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requests = new HashMap<>();
        RecommendationConfigItem cpuItem = new RecommendationConfigItem(2.0, "cores");
        RecommendationConfigItem memoryItem = new RecommendationConfigItem(4096.0, "MiB");
        requests.put(AnalyzerConstants.RecommendationItem.CPU, cpuItem);
        requests.put(AnalyzerConstants.RecommendationItem.MEMORY, memoryItem);

        // When
        variation.setRequests(requests);

        // Then
        assertNotNull(variation.getRequests());
        assertEquals(2, variation.getRequests().size());
        assertTrue(variation.getRequests().containsKey(AnalyzerConstants.RecommendationItem.CPU));
        assertTrue(variation.getRequests().containsKey(AnalyzerConstants.RecommendationItem.MEMORY));
    }

    @Test
    public void testSetAndGetRequestsWithNull() {
        // Given
        variation.setRequests(new HashMap<>());

        // When
        variation.setRequests(null);

        // Then
        assertNull(variation.getRequests());
    }

    @Test
    public void testSetAndGetLimits() {
        // Given
        Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limits = new HashMap<>();
        RecommendationConfigItem cpuItem = new RecommendationConfigItem(4.0, "cores");
        limits.put(AnalyzerConstants.RecommendationItem.CPU, cpuItem);

        // When
        variation.setLimits(limits);

        // Then
        assertNotNull(variation.getLimits());
        assertEquals(1, variation.getLimits().size());
        assertTrue(variation.getLimits().containsKey(AnalyzerConstants.RecommendationItem.CPU));
        assertEquals(4.0, variation.getLimits().get(AnalyzerConstants.RecommendationItem.CPU).getAmount());
    }

    @Test
    public void testSetAndGetLimitsWithMultipleItems() {
        // Given
        Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limits = new HashMap<>();
        RecommendationConfigItem cpuItem = new RecommendationConfigItem(4.0, "cores");
        RecommendationConfigItem memoryItem = new RecommendationConfigItem(8192.0, "MiB");
        limits.put(AnalyzerConstants.RecommendationItem.CPU, cpuItem);
        limits.put(AnalyzerConstants.RecommendationItem.MEMORY, memoryItem);

        // When
        variation.setLimits(limits);

        // Then
        assertNotNull(variation.getLimits());
        assertEquals(2, variation.getLimits().size());
        assertTrue(variation.getLimits().containsKey(AnalyzerConstants.RecommendationItem.CPU));
        assertTrue(variation.getLimits().containsKey(AnalyzerConstants.RecommendationItem.MEMORY));
    }

    @Test
    public void testSetAndGetLimitsWithNull() {
        // Given
        variation.setLimits(new HashMap<>());

        // When
        variation.setLimits(null);

        // Then
        assertNull(variation.getLimits());
    }

    @Test
    public void testSetAndGetResources() {
        // Given
        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> resources = new HashMap<>();
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requests = new HashMap<>();
        RecommendationConfigItem cpuItem = new RecommendationConfigItem(2.0, "cores");
        requests.put(AnalyzerConstants.RecommendationItem.CPU, cpuItem);
        resources.put(AnalyzerConstants.ResourceSetting.requests, requests);

        // When
        variation.setResources(resources);

        // Then
        assertNotNull(variation.getResources());
        assertEquals(1, variation.getResources().size());
        assertTrue(variation.getResources().containsKey(AnalyzerConstants.ResourceSetting.requests));
    }

    @Test
    public void testSetAndGetResourcesWithRequestsAndLimits() {
        // Given
        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> resources = new HashMap<>();
        
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requests = new HashMap<>();
        RecommendationConfigItem cpuRequestItem = new RecommendationConfigItem(2.0, "cores");
        RecommendationConfigItem memoryRequestItem = new RecommendationConfigItem(4096.0, "MiB");
        requests.put(AnalyzerConstants.RecommendationItem.CPU, cpuRequestItem);
        requests.put(AnalyzerConstants.RecommendationItem.MEMORY, memoryRequestItem);
        
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limits = new HashMap<>();
        RecommendationConfigItem cpuLimitItem = new RecommendationConfigItem(4.0, "cores");
        RecommendationConfigItem memoryLimitItem = new RecommendationConfigItem(8192.0, "MiB");
        limits.put(AnalyzerConstants.RecommendationItem.CPU, cpuLimitItem);
        limits.put(AnalyzerConstants.RecommendationItem.MEMORY, memoryLimitItem);
        
        resources.put(AnalyzerConstants.ResourceSetting.requests, requests);
        resources.put(AnalyzerConstants.ResourceSetting.limits, limits);

        // When
        variation.setResources(resources);

        // Then
        assertNotNull(variation.getResources());
        assertEquals(2, variation.getResources().size());
        assertTrue(variation.getResources().containsKey(AnalyzerConstants.ResourceSetting.requests));
        assertTrue(variation.getResources().containsKey(AnalyzerConstants.ResourceSetting.limits));
        assertEquals(2, variation.getResources().get(AnalyzerConstants.ResourceSetting.requests).size());
        assertEquals(2, variation.getResources().get(AnalyzerConstants.ResourceSetting.limits).size());
    }

    @Test
    public void testSetAndGetResourcesWithNull() {
        // Given
        variation.setResources(new HashMap<>());

        // When
        variation.setResources(null);

        // Then
        assertNull(variation.getResources());
    }

    @Test
    public void testSetAndGetResourcesWithGPU() {
        // Given
        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> resources = new HashMap<>();
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requests = new HashMap<>();
        RecommendationConfigItem gpuItem = new RecommendationConfigItem(1.0, "gpu");
        requests.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU, gpuItem);
        resources.put(AnalyzerConstants.ResourceSetting.requests, requests);

        // When
        variation.setResources(resources);

        // Then
        assertNotNull(variation.getResources());
        assertTrue(variation.getResources().get(AnalyzerConstants.ResourceSetting.requests)
                .containsKey(AnalyzerConstants.RecommendationItem.NVIDIA_GPU));
    }

    @Test
    public void testCompleteVariationSetup() {
        // Given
        Integer replicas = 5;
        
        Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requests = new HashMap<>();
        requests.put(AnalyzerConstants.RecommendationItem.CPU, new RecommendationConfigItem(2.0, "cores"));
        requests.put(AnalyzerConstants.RecommendationItem.MEMORY, new RecommendationConfigItem(4096.0, "MiB"));
        
        Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limits = new HashMap<>();
        limits.put(AnalyzerConstants.RecommendationItem.CPU, new RecommendationConfigItem(4.0, "cores"));
        limits.put(AnalyzerConstants.RecommendationItem.MEMORY, new RecommendationConfigItem(8192.0, "MiB"));
        
        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> resources = new HashMap<>();
        resources.put(AnalyzerConstants.ResourceSetting.requests, new HashMap<>(requests));
        resources.put(AnalyzerConstants.ResourceSetting.limits, new HashMap<>(limits));

        // When
        variation.setReplicas(replicas);
        variation.setRequests(requests);
        variation.setLimits(limits);
        variation.setResources(resources);

        // Then
        assertEquals(5, variation.getReplicas());
        assertNotNull(variation.getRequests());
        assertNotNull(variation.getLimits());
        assertNotNull(variation.getResources());
        assertEquals(2, variation.getRequests().size());
        assertEquals(2, variation.getLimits().size());
        assertEquals(2, variation.getResources().size());
    }

    @Test
    public void testJsonSerializationWithReplicas() {
        // Given
        variation.setReplicas(3);

        // When
        String json = gson.toJson(variation);
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        // Then
        assertNotNull(jsonObject);
        assertTrue(jsonObject.has(KruizeConstants.JSONKeys.REPLICAS));
        assertEquals(3, jsonObject.get(KruizeConstants.JSONKeys.REPLICAS).getAsInt());
    }

    @Test
    public void testJsonSerializationWithResources() {
        // Given
        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> resources = new HashMap<>();
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requests = new HashMap<>();
        requests.put(AnalyzerConstants.RecommendationItem.CPU, new RecommendationConfigItem(2.0, "cores"));
        resources.put(AnalyzerConstants.ResourceSetting.requests, requests);
        variation.setResources(resources);

        // When
        String json = gson.toJson(variation);
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        // Then
        assertNotNull(jsonObject);
        assertTrue(jsonObject.has(KruizeConstants.JSONKeys.RESOURCES));
    }

    @Test
    public void testJsonDeserializationWithReplicas() {
        // Given
        String json = "{\"replicas\":5}";

        // When
        Variation deserializedVariation = gson.fromJson(json, Variation.class);

        // Then
        assertNotNull(deserializedVariation);
        assertEquals(5, deserializedVariation.getReplicas());
    }

    @Test
    public void testEmptyVariation() {
        // Given & When
        Variation emptyVariation = new Variation();

        // Then
        assertNull(emptyVariation.getReplicas());
        assertNull(emptyVariation.getRequests());
        assertNull(emptyVariation.getLimits());
        assertNull(emptyVariation.getResources());
    }

    @Test
    public void testVariationWithOnlyRequests() {
        // Given
        Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requests = new HashMap<>();
        requests.put(AnalyzerConstants.RecommendationItem.CPU, new RecommendationConfigItem(2.0, "cores"));

        // When
        variation.setRequests(requests);

        // Then
        assertNotNull(variation.getRequests());
        assertNull(variation.getLimits());
        assertNull(variation.getResources());
        assertNull(variation.getReplicas());
    }

    @Test
    public void testVariationWithOnlyLimits() {
        // Given
        Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limits = new HashMap<>();
        limits.put(AnalyzerConstants.RecommendationItem.MEMORY, new RecommendationConfigItem(8192.0, "MiB"));

        // When
        variation.setLimits(limits);

        // Then
        assertNotNull(variation.getLimits());
        assertNull(variation.getRequests());
        assertNull(variation.getResources());
        assertNull(variation.getReplicas());
    }

    @Test
    public void testVariationWithErrorMessage() {
        // Given
        Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requests = new HashMap<>();
        RecommendationConfigItem errorItem = new RecommendationConfigItem("Error: Unable to calculate recommendation");
        requests.put(AnalyzerConstants.RecommendationItem.CPU, errorItem);

        // When
        variation.setRequests(requests);

        // Then
        assertNotNull(variation.getRequests());
        assertEquals("Error: Unable to calculate recommendation", 
                variation.getRequests().get(AnalyzerConstants.RecommendationItem.CPU).getErrorMsg());
    }

    @Test
    public void testVariationWithLargeReplicaCount() {
        // Given
        Integer largeReplicas = 1000;

        // When
        variation.setReplicas(largeReplicas);

        // Then
        assertEquals(1000, variation.getReplicas());
    }

    @Test
    public void testVariationWithNegativeReplicas() {
        // Given
        Integer negativeReplicas = -1;

        // When
        variation.setReplicas(negativeReplicas);

        // Then
        assertEquals(-1, variation.getReplicas());
    }

    @Test
    public void testModifyRequestsAfterSetting() {
        // Given
        Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requests = new HashMap<>();
        requests.put(AnalyzerConstants.RecommendationItem.CPU, new RecommendationConfigItem(2.0, "cores"));
        variation.setRequests(requests);

        // When
        variation.getRequests().put(AnalyzerConstants.RecommendationItem.MEMORY, 
                new RecommendationConfigItem(4096.0, "MiB"));

        // Then
        assertEquals(2, variation.getRequests().size());
        assertTrue(variation.getRequests().containsKey(AnalyzerConstants.RecommendationItem.MEMORY));
    }

    @Test
    public void testModifyLimitsAfterSetting() {
        // Given
        Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limits = new HashMap<>();
        limits.put(AnalyzerConstants.RecommendationItem.CPU, new RecommendationConfigItem(4.0, "cores"));
        variation.setLimits(limits);

        // When
        variation.getLimits().put(AnalyzerConstants.RecommendationItem.MEMORY, 
                new RecommendationConfigItem(8192.0, "MiB"));

        // Then
        assertEquals(2, variation.getLimits().size());
        assertTrue(variation.getLimits().containsKey(AnalyzerConstants.RecommendationItem.MEMORY));
    }

    @Test
    public void testVariationIndependence() {
        // Given
        Variation variation1 = new Variation();
        Variation variation2 = new Variation();
        
        variation1.setReplicas(3);
        variation2.setReplicas(5);

        // Then
        assertEquals(3, variation1.getReplicas());
        assertEquals(5, variation2.getReplicas());
        assertNotEquals(variation1.getReplicas(), variation2.getReplicas());
    }
}