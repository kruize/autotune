/*******************************************************************************
 * Copyright (c) 2022, 2022 Red Hat, IBM Corporation and others.
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
 *
 *******************************************************************************/

package com.autotune.analyzer.adapters;

import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MetricAggregationInfoResultsIntSerializer
 */
public class MetricAggregationInfoResultsIntSerializerTest {

    private Gson gson;

    @BeforeEach
    public void setUp() {
        gson = new GsonBuilder()
                .registerTypeAdapter(MetricAggregationInfoResults.class, new MetricAggregationInfoResultsIntSerializer())
                .create();
    }

    @Test
    public void testSerializeWithAllFieldsPopulated() {
        // Given
        MetricAggregationInfoResults results = new MetricAggregationInfoResults();
        results.setAvg(100.5);
        results.setSum(500.75);
        results.setMin(50.25);
        results.setMax(200.99);
        results.setCount(10);
        results.setMedian(95.5);
        results.setMode(100.0);
        results.setRange(150.74);
        results.setFormat("decimal");

        // When
        String json = gson.toJson(results);
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        // Then
        assertNotNull(jsonObject);
        assertEquals(100, jsonObject.get("avg").getAsInt());
        assertEquals(500, jsonObject.get("sum").getAsInt());
        assertEquals(50, jsonObject.get("min").getAsInt());
        assertEquals(200, jsonObject.get("max").getAsInt());
        assertEquals(10, jsonObject.get("count").getAsInt());
        assertEquals(95, jsonObject.get("median").getAsInt());
        assertEquals(100, jsonObject.get("mode").getAsInt());
        assertEquals(150, jsonObject.get("range").getAsInt());
        assertEquals("decimal", jsonObject.get("format").getAsString());
    }

    @Test
    public void testSerializeWithAllFieldsNull() {
        // Given
        MetricAggregationInfoResults results = new MetricAggregationInfoResults();

        // When
        String json = gson.toJson(results);
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        // Then
        assertNotNull(jsonObject);
        assertFalse(jsonObject.has("avg"));
        assertFalse(jsonObject.has("sum"));
        assertFalse(jsonObject.has("min"));
        assertFalse(jsonObject.has("max"));
        assertFalse(jsonObject.has("count"));
        assertFalse(jsonObject.has("median"));
        assertFalse(jsonObject.has("mode"));
        assertFalse(jsonObject.has("range"));
        assertFalse(jsonObject.has("format"));
    }

    @Test
    public void testSerializeWithPartialFields() {
        // Given
        MetricAggregationInfoResults results = new MetricAggregationInfoResults();
        results.setAvg(75.8);
        results.setMax(150.3);
        results.setCount(5);

        // When
        String json = gson.toJson(results);
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        // Then
        assertNotNull(jsonObject);
        assertTrue(jsonObject.has("avg"));
        assertTrue(jsonObject.has("max"));
        assertTrue(jsonObject.has("count"));
        assertEquals(75, jsonObject.get("avg").getAsInt());
        assertEquals(150, jsonObject.get("max").getAsInt());
        assertEquals(5, jsonObject.get("count").getAsInt());
        
        assertFalse(jsonObject.has("sum"));
        assertFalse(jsonObject.has("min"));
        assertFalse(jsonObject.has("median"));
        assertFalse(jsonObject.has("mode"));
        assertFalse(jsonObject.has("range"));
        assertFalse(jsonObject.has("format"));
    }

    @Test
    public void testSerializeWithZeroValues() {
        // Given
        MetricAggregationInfoResults results = new MetricAggregationInfoResults();
        results.setAvg(0.0);
        results.setSum(0.0);
        results.setMin(0.0);
        results.setMax(0.0);
        results.setCount(0);
        results.setMedian(0.0);
        results.setMode(0.0);
        results.setRange(0.0);

        // When
        String json = gson.toJson(results);
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        // Then
        assertNotNull(jsonObject);
        assertEquals(0, jsonObject.get("avg").getAsInt());
        assertEquals(0, jsonObject.get("sum").getAsInt());
        assertEquals(0, jsonObject.get("min").getAsInt());
        assertEquals(0, jsonObject.get("max").getAsInt());
        assertEquals(0, jsonObject.get("count").getAsInt());
        assertEquals(0, jsonObject.get("median").getAsInt());
        assertEquals(0, jsonObject.get("mode").getAsInt());
        assertEquals(0, jsonObject.get("range").getAsInt());
    }

    @Test
    public void testSerializeWithNegativeValues() {
        // Given
        MetricAggregationInfoResults results = new MetricAggregationInfoResults();
        results.setAvg(-50.7);
        results.setSum(-200.3);
        results.setMin(-100.9);
        results.setMax(-10.1);
        results.setMedian(-45.5);

        // When
        String json = gson.toJson(results);
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        // Then
        assertNotNull(jsonObject);
        assertEquals(-50, jsonObject.get("avg").getAsInt());
        assertEquals(-200, jsonObject.get("sum").getAsInt());
        assertEquals(-100, jsonObject.get("min").getAsInt());
        assertEquals(-10, jsonObject.get("max").getAsInt());
        assertEquals(-45, jsonObject.get("median").getAsInt());
    }

    @Test
    public void testSerializeWithLargeValues() {
        // Given
        MetricAggregationInfoResults results = new MetricAggregationInfoResults();
        results.setAvg(1000000.99);
        results.setSum(5000000.50);
        results.setMax(10000000.75);
        results.setCount(Integer.MAX_VALUE);

        // When
        String json = gson.toJson(results);
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        // Then
        assertNotNull(jsonObject);
        assertEquals(1000000, jsonObject.get("avg").getAsInt());
        assertEquals(5000000, jsonObject.get("sum").getAsInt());
        assertEquals(10000000, jsonObject.get("max").getAsInt());
        assertEquals(Integer.MAX_VALUE, jsonObject.get("count").getAsInt());
    }

    @Test
    public void testSerializeWithEmptyFormat() {
        // Given
        MetricAggregationInfoResults results = new MetricAggregationInfoResults();
        results.setAvg(100.0);
        results.setFormat("");

        // When
        String json = gson.toJson(results);
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        // Then
        assertNotNull(jsonObject);
        assertTrue(jsonObject.has("avg"));
        assertFalse(jsonObject.has("format")); // Empty format should not be included
    }

    @Test
    public void testSerializeWithFormatOnly() {
        // Given
        MetricAggregationInfoResults results = new MetricAggregationInfoResults();
        results.setFormat("percentage");

        // When
        String json = gson.toJson(results);
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        // Then
        assertNotNull(jsonObject);
        assertTrue(jsonObject.has("format"));
        assertEquals("percentage", jsonObject.get("format").getAsString());
    }

    @Test
    public void testSerializeRoundingBehavior() {
        // Given - Test rounding behavior for .5 values
        MetricAggregationInfoResults results = new MetricAggregationInfoResults();
        results.setAvg(100.5);
        results.setMin(50.5);
        results.setMax(200.5);

        // When
        String json = gson.toJson(results);
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        // Then - intValue() truncates towards zero
        assertNotNull(jsonObject);
        assertEquals(100, jsonObject.get("avg").getAsInt());
        assertEquals(50, jsonObject.get("min").getAsInt());
        assertEquals(200, jsonObject.get("max").getAsInt());
    }

    @Test
    public void testSerializeWithDecimalPrecision() {
        // Given - Test various decimal values
        MetricAggregationInfoResults results = new MetricAggregationInfoResults();
        results.setAvg(99.99);
        results.setSum(199.01);
        results.setMin(49.49);
        results.setMax(149.51);

        // When
        String json = gson.toJson(results);
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        // Then
        assertNotNull(jsonObject);
        assertEquals(99, jsonObject.get("avg").getAsInt());
        assertEquals(199, jsonObject.get("sum").getAsInt());
        assertEquals(49, jsonObject.get("min").getAsInt());
        assertEquals(149, jsonObject.get("max").getAsInt());
    }

    @Test
    public void testSerializeCountIsInteger() {
        // Given - Count should remain as Integer, not converted from Double
        MetricAggregationInfoResults results = new MetricAggregationInfoResults();
        results.setCount(42);

        // When
        String json = gson.toJson(results);
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        // Then
        assertNotNull(jsonObject);
        assertTrue(jsonObject.has("count"));
        assertEquals(42, jsonObject.get("count").getAsInt());
    }

    @Test
    public void testSerializeCompleteScenario() {
        // Given - A realistic scenario with mixed values
        MetricAggregationInfoResults results = new MetricAggregationInfoResults();
        results.setAvg(85.67);
        results.setSum(857.45);
        results.setMin(10.23);
        results.setMax(150.89);
        results.setCount(10);
        results.setMedian(82.34);
        results.setMode(85.0);
        results.setRange(140.66);
        results.setFormat("cores");

        // When
        String json = gson.toJson(results);
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        // Then
        assertNotNull(jsonObject);
        assertEquals(9, jsonObject.size()); // All 9 fields should be present
        assertEquals(85, jsonObject.get("avg").getAsInt());
        assertEquals(857, jsonObject.get("sum").getAsInt());
        assertEquals(10, jsonObject.get("min").getAsInt());
        assertEquals(150, jsonObject.get("max").getAsInt());
        assertEquals(10, jsonObject.get("count").getAsInt());
        assertEquals(82, jsonObject.get("median").getAsInt());
        assertEquals(85, jsonObject.get("mode").getAsInt());
        assertEquals(140, jsonObject.get("range").getAsInt());
        assertEquals("cores", jsonObject.get("format").getAsString());
    }
}
