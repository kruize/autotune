/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
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

package com.autotune.analyzer.recommendations.utils;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.IntervalResults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RecommendationUtils#getCurrentValue and getCurrentValueForNamespace methods
 */
public class RecommendationUtilsTest {

    private Map<Timestamp, IntervalResults> filteredResultsMap;
    private Timestamp testTimestamp;
    private ArrayList<RecommendationConstants.RecommendationNotification> notifications;

    @BeforeEach
    public void setUp() {
        filteredResultsMap = new HashMap<>();
        testTimestamp = new Timestamp(System.currentTimeMillis());
        notifications = new ArrayList<>();
    }

    /**
     * Helper method to create IntervalResults with metric data
     */
    private IntervalResults createIntervalResults(AnalyzerConstants.MetricName metricName, Double avg, Double sum, String format) {
        IntervalResults intervalResults = new IntervalResults();
        HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap = new HashMap<>();
        
        MetricResults metricResults = new MetricResults();
        MetricAggregationInfoResults aggInfo = new MetricAggregationInfoResults();
        aggInfo.setAvg(avg);
        aggInfo.setSum(sum);
        aggInfo.setFormat(format);
        metricResults.setAggregationInfoResult(aggInfo);
        
        metricResultsMap.put(metricName, metricResults);
        intervalResults.setMetricResultsMap(metricResultsMap);
        
        return intervalResults;
    }

    // ==================== Tests for getCurrentValue ====================

    @Test
    public void testGetCurrentValue_CpuRequest_Success() {
        // Given
        filteredResultsMap.put(testTimestamp, createIntervalResults(AnalyzerConstants.MetricName.cpuRequest, 2.5, null, "cores"));

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValue(
                AnalyzerConstants.MetricName.cpuRequest, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNotNull(result);
        assertEquals(2.5, result.getAmount());
        assertEquals("cores", result.getFormat());
        assertTrue(notifications.isEmpty());
    }

    @Test
    public void testGetCurrentValue_MemoryRequest_Success() {
        // Given
        filteredResultsMap.put(testTimestamp, createIntervalResults(AnalyzerConstants.MetricName.memoryRequest, 4096.0, null, "MiB"));

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValue(
                AnalyzerConstants.MetricName.memoryRequest, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNotNull(result);
        assertEquals(4096.0, result.getAmount());
        assertEquals("MiB", result.getFormat());
        assertTrue(notifications.isEmpty());
    }

    @Test
    public void testGetCurrentValue_CpuLimit_Success() {
        // Given
        filteredResultsMap.put(testTimestamp, createIntervalResults(AnalyzerConstants.MetricName.cpuLimit, 4.0, null, "cores"));

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValue(
                AnalyzerConstants.MetricName.cpuLimit, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNotNull(result);
        assertEquals(4.0, result.getAmount());
        assertEquals("cores", result.getFormat());
        assertTrue(notifications.isEmpty());
    }

    @Test
    public void testGetCurrentValue_MemoryLimit_Success() {
        // Given
        filteredResultsMap.put(testTimestamp, createIntervalResults(AnalyzerConstants.MetricName.memoryLimit, 8192.0, null, "MiB"));

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValue(
                AnalyzerConstants.MetricName.memoryLimit, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNotNull(result);
        assertEquals(8192.0, result.getAmount());
        assertEquals("MiB", result.getFormat());
        assertTrue(notifications.isEmpty());
    }

    @Test
    public void testGetCurrentValue_PodCount_FromPodCountMetric() {
        // Given
        filteredResultsMap.put(testTimestamp, createIntervalResults(AnalyzerConstants.MetricName.podCount, 3.0, 5.0, null));

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValue(
                AnalyzerConstants.MetricName.podCount, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNotNull(result);
        assertEquals(5.0, result.getAmount()); // Uses sum for podCount
        assertNull(result.getFormat());
        assertTrue(notifications.isEmpty());
    }

    @Test
    public void testGetCurrentValue_PodCount_FallbackToCpuUsage() {
        // Given - No podCount metric, but cpuUsage available
        IntervalResults intervalResults = new IntervalResults();
        HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap = new HashMap<>();
        
        // CPU usage with sum=6.0, avg=2.0 => pod count = 3.0
        MetricResults cpuMetric = new MetricResults();
        MetricAggregationInfoResults cpuAggInfo = new MetricAggregationInfoResults();
        cpuAggInfo.setAvg(2.0);
        cpuAggInfo.setSum(6.0);
        cpuMetric.setAggregationInfoResult(cpuAggInfo);
        metricResultsMap.put(AnalyzerConstants.MetricName.cpuUsage, cpuMetric);
        
        intervalResults.setMetricResultsMap(metricResultsMap);
        filteredResultsMap.put(testTimestamp, intervalResults);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValue(
                AnalyzerConstants.MetricName.podCount, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNotNull(result);
        assertEquals(3.0, result.getAmount()); // ceil(6.0/2.0) = ceil(3.0) = 3.0
        assertTrue(notifications.isEmpty());
    }

    @Test
    public void testGetCurrentValue_PodCount_FallbackToMemoryUsage() {
        // Given - No podCount or cpuUsage, but memoryUsage available
        IntervalResults intervalResults = new IntervalResults();
        HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap = new HashMap<>();
        
        // Memory usage with sum=2048.0, avg=512.0 => pod count = 4.0
        MetricResults memMetric = new MetricResults();
        MetricAggregationInfoResults memAggInfo = new MetricAggregationInfoResults();
        memAggInfo.setAvg(512.0);
        memAggInfo.setSum(2048.0);
        memMetric.setAggregationInfoResult(memAggInfo);
        metricResultsMap.put(AnalyzerConstants.MetricName.memoryUsage, memMetric);
        
        intervalResults.setMetricResultsMap(metricResultsMap);
        filteredResultsMap.put(testTimestamp, intervalResults);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValue(
                AnalyzerConstants.MetricName.podCount, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNotNull(result);
        assertEquals(4.0, result.getAmount()); // ceil(2048.0/512.0) = ceil(4.0) = 4.0
        assertTrue(notifications.isEmpty());
    }

    @Test
    public void testGetCurrentValue_PodCount_WithDecimalValue() {
        // Given - CPU usage that results in decimal pod count
        IntervalResults intervalResults = new IntervalResults();
        HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap = new HashMap<>();
        
        MetricResults cpuMetric = new MetricResults();
        MetricAggregationInfoResults cpuAggInfo = new MetricAggregationInfoResults();
        cpuAggInfo.setAvg(2.0);
        cpuAggInfo.setSum(7.0); // 7.0/2.0 = 3.5
        cpuMetric.setAggregationInfoResult(cpuAggInfo);
        metricResultsMap.put(AnalyzerConstants.MetricName.cpuUsage, cpuMetric);
        
        intervalResults.setMetricResultsMap(metricResultsMap);
        filteredResultsMap.put(testTimestamp, intervalResults);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValue(
                AnalyzerConstants.MetricName.podCount, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNotNull(result);
        assertEquals(4.0, result.getAmount()); // ceil(3.5) = 4.0
    }

    @Test
    public void testGetCurrentValue_PodCount_ZeroValue() {
        // Given - Pod count is zero
        filteredResultsMap.put(testTimestamp, createIntervalResults(AnalyzerConstants.MetricName.podCount, 0.0, 0.0, null));

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValue(
                AnalyzerConstants.MetricName.podCount, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNull(result); // Should return null for zero pod count
    }

    @Test
    public void testGetCurrentValue_CpuRequest_NotSet() {
        // Given - Empty metric results map
        IntervalResults intervalResults = new IntervalResults();
        intervalResults.setMetricResultsMap(new HashMap<>());
        filteredResultsMap.put(testTimestamp, intervalResults);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValue(
                AnalyzerConstants.MetricName.cpuRequest, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNull(result);
        assertEquals(1, notifications.size());
        assertEquals(RecommendationConstants.RecommendationNotification.CRITICAL_CPU_REQUEST_NOT_SET, notifications.get(0));
    }

    @Test
    public void testGetCurrentValue_MemoryRequest_NotSet() {
        // Given - Empty metric results map
        IntervalResults intervalResults = new IntervalResults();
        intervalResults.setMetricResultsMap(new HashMap<>());
        filteredResultsMap.put(testTimestamp, intervalResults);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValue(
                AnalyzerConstants.MetricName.memoryRequest, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNull(result);
        assertEquals(1, notifications.size());
        assertEquals(RecommendationConstants.RecommendationNotification.CRITICAL_MEMORY_REQUEST_NOT_SET, notifications.get(0));
    }

    @Test
    public void testGetCurrentValue_CpuLimit_NotSet() {
        // Given - Empty metric results map
        IntervalResults intervalResults = new IntervalResults();
        intervalResults.setMetricResultsMap(new HashMap<>());
        filteredResultsMap.put(testTimestamp, intervalResults);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValue(
                AnalyzerConstants.MetricName.cpuLimit, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNull(result);
        assertEquals(1, notifications.size());
        assertEquals(RecommendationConstants.RecommendationNotification.WARNING_CPU_LIMIT_NOT_SET, notifications.get(0));
    }

    @Test
    public void testGetCurrentValue_MemoryLimit_NotSet() {
        // Given - Empty metric results map
        IntervalResults intervalResults = new IntervalResults();
        intervalResults.setMetricResultsMap(new HashMap<>());
        filteredResultsMap.put(testTimestamp, intervalResults);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValue(
                AnalyzerConstants.MetricName.memoryLimit, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNull(result);
        assertEquals(1, notifications.size());
        assertEquals(RecommendationConstants.RecommendationNotification.CRITICAL_MEMORY_LIMIT_NOT_SET, notifications.get(0));
    }

    @Test
    public void testGetCurrentValue_NullNotifications() {
        // Given - Null notifications list
        IntervalResults intervalResults = new IntervalResults();
        intervalResults.setMetricResultsMap(new HashMap<>());
        filteredResultsMap.put(testTimestamp, intervalResults);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValue(
                AnalyzerConstants.MetricName.cpuRequest, filteredResultsMap, testTimestamp, null);

        // Then
        assertNull(result);
        // Should not throw exception with null notifications
    }

    @Test
    public void testGetCurrentValue_TimestampNotInMap() {
        // Given - Timestamp not in the map
        Timestamp differentTimestamp = new Timestamp(System.currentTimeMillis() + 10000);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValue(
                AnalyzerConstants.MetricName.cpuRequest, filteredResultsMap, differentTimestamp, notifications);

        // Then
        assertNull(result);
        assertEquals(1, notifications.size());
    }

    @Test
    public void testGetCurrentValue_NullAggregationInfo() {
        // Given - Metric with null aggregation info
        IntervalResults intervalResults = new IntervalResults();
        HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap = new HashMap<>();
        MetricResults metricResults = new MetricResults();
        metricResults.setAggregationInfoResult(null);
        metricResultsMap.put(AnalyzerConstants.MetricName.cpuRequest, metricResults);
        intervalResults.setMetricResultsMap(metricResultsMap);
        filteredResultsMap.put(testTimestamp, intervalResults);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValue(
                AnalyzerConstants.MetricName.cpuRequest, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNull(result);
        assertEquals(1, notifications.size());
    }

    @Test
    public void testGetCurrentValue_PodCount_CpuUsageWithZeroAvg() {
        // Given - CPU usage with zero avg (should be skipped)
        IntervalResults intervalResults = new IntervalResults();
        HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap = new HashMap<>();
        
        MetricResults cpuMetric = new MetricResults();
        MetricAggregationInfoResults cpuAggInfo = new MetricAggregationInfoResults();
        cpuAggInfo.setAvg(0.0);
        cpuAggInfo.setSum(6.0);
        cpuMetric.setAggregationInfoResult(cpuAggInfo);
        metricResultsMap.put(AnalyzerConstants.MetricName.cpuUsage, cpuMetric);
        
        intervalResults.setMetricResultsMap(metricResultsMap);
        filteredResultsMap.put(testTimestamp, intervalResults);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValue(
                AnalyzerConstants.MetricName.podCount, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNull(result); // Should skip zero avg
    }

    // ==================== Tests for getCurrentValueForNamespace ====================

    @Test
    public void testGetCurrentValueForNamespace_CpuRequest_Success() {
        // Given
        filteredResultsMap.put(testTimestamp, createIntervalResults(AnalyzerConstants.MetricName.namespaceCpuRequest, 10.0, 50.0, "cores"));

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValueForNamespace(
                AnalyzerConstants.MetricName.namespaceCpuRequest, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNotNull(result);
        assertEquals(50.0, result.getAmount()); // Uses sum for namespace
        assertEquals("cores", result.getFormat());
        assertTrue(notifications.isEmpty());
    }

    @Test
    public void testGetCurrentValueForNamespace_MemoryRequest_Success() {
        // Given
        filteredResultsMap.put(testTimestamp, createIntervalResults(AnalyzerConstants.MetricName.namespaceMemoryRequest, 2048.0, 10240.0, "MiB"));

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValueForNamespace(
                AnalyzerConstants.MetricName.namespaceMemoryRequest, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNotNull(result);
        assertEquals(10240.0, result.getAmount());
        assertEquals("MiB", result.getFormat());
        assertTrue(notifications.isEmpty());
    }

    @Test
    public void testGetCurrentValueForNamespace_CpuLimit_Success() {
        // Given
        filteredResultsMap.put(testTimestamp, createIntervalResults(AnalyzerConstants.MetricName.namespaceCpuLimit, 20.0, 100.0, "cores"));

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValueForNamespace(
                AnalyzerConstants.MetricName.namespaceCpuLimit, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNotNull(result);
        assertEquals(100.0, result.getAmount());
        assertEquals("cores", result.getFormat());
        assertTrue(notifications.isEmpty());
    }

    @Test
    public void testGetCurrentValueForNamespace_MemoryLimit_Success() {
        // Given
        filteredResultsMap.put(testTimestamp, createIntervalResults(AnalyzerConstants.MetricName.namespaceMemoryLimit, 4096.0, 20480.0, "MiB"));

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValueForNamespace(
                AnalyzerConstants.MetricName.namespaceMemoryLimit, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNotNull(result);
        assertEquals(20480.0, result.getAmount());
        assertEquals("MiB", result.getFormat());
        assertTrue(notifications.isEmpty());
    }

    @Test
    public void testGetCurrentValueForNamespace_CpuRequest_NotSet() {
        // Given - Empty metric results map
        IntervalResults intervalResults = new IntervalResults();
        intervalResults.setMetricResultsMap(new HashMap<>());
        filteredResultsMap.put(testTimestamp, intervalResults);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValueForNamespace(
                AnalyzerConstants.MetricName.namespaceCpuRequest, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNull(result);
        assertEquals(1, notifications.size());
        assertEquals(RecommendationConstants.RecommendationNotification.CRITICAL_CPU_REQUEST_NOT_SET, notifications.get(0));
    }

    @Test
    public void testGetCurrentValueForNamespace_MemoryRequest_NotSet() {
        // Given - Empty metric results map
        IntervalResults intervalResults = new IntervalResults();
        intervalResults.setMetricResultsMap(new HashMap<>());
        filteredResultsMap.put(testTimestamp, intervalResults);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValueForNamespace(
                AnalyzerConstants.MetricName.namespaceMemoryRequest, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNull(result);
        assertEquals(1, notifications.size());
        assertEquals(RecommendationConstants.RecommendationNotification.CRITICAL_MEMORY_REQUEST_NOT_SET, notifications.get(0));
    }

    @Test
    public void testGetCurrentValueForNamespace_CpuLimit_NotSet() {
        // Given - Empty metric results map
        IntervalResults intervalResults = new IntervalResults();
        intervalResults.setMetricResultsMap(new HashMap<>());
        filteredResultsMap.put(testTimestamp, intervalResults);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValueForNamespace(
                AnalyzerConstants.MetricName.namespaceCpuLimit, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNull(result);
        assertEquals(1, notifications.size());
        assertEquals(RecommendationConstants.RecommendationNotification.WARNING_CPU_LIMIT_NOT_SET, notifications.get(0));
    }

    @Test
    public void testGetCurrentValueForNamespace_MemoryLimit_NotSet() {
        // Given - Empty metric results map
        IntervalResults intervalResults = new IntervalResults();
        intervalResults.setMetricResultsMap(new HashMap<>());
        filteredResultsMap.put(testTimestamp, intervalResults);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValueForNamespace(
                AnalyzerConstants.MetricName.namespaceMemoryLimit, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNull(result);
        assertEquals(1, notifications.size());
        assertEquals(RecommendationConstants.RecommendationNotification.CRITICAL_MEMORY_LIMIT_NOT_SET, notifications.get(0));
    }

    @Test
    public void testGetCurrentValueForNamespace_NullNotifications() {
        // Given - Null notifications list
        IntervalResults intervalResults = new IntervalResults();
        intervalResults.setMetricResultsMap(new HashMap<>());
        filteredResultsMap.put(testTimestamp, intervalResults);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValueForNamespace(
                AnalyzerConstants.MetricName.namespaceCpuRequest, filteredResultsMap, testTimestamp, null);

        // Then
        assertNull(result);
        // Should not throw exception with null notifications
    }

    @Test
    public void testGetCurrentValueForNamespace_TimestampNotInMap() {
        // Given - Timestamp not in the map
        Timestamp differentTimestamp = new Timestamp(System.currentTimeMillis() + 10000);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValueForNamespace(
                AnalyzerConstants.MetricName.namespaceCpuRequest, filteredResultsMap, differentTimestamp, notifications);

        // Then
        assertNull(result);
        assertEquals(1, notifications.size());
    }

    @Test
    public void testGetCurrentValueForNamespace_NullAggregationInfo() {
        // Given - Metric with null aggregation info
        IntervalResults intervalResults = new IntervalResults();
        HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap = new HashMap<>();
        MetricResults metricResults = new MetricResults();
        metricResults.setAggregationInfoResult(null);
        metricResultsMap.put(AnalyzerConstants.MetricName.namespaceCpuRequest, metricResults);
        intervalResults.setMetricResultsMap(metricResultsMap);
        filteredResultsMap.put(testTimestamp, intervalResults);

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValueForNamespace(
                AnalyzerConstants.MetricName.namespaceCpuRequest, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNull(result);
        assertEquals(1, notifications.size());
    }

    @Test
    public void testGetCurrentValueForNamespace_ZeroSum() {
        // Given - Namespace metric with zero sum
        filteredResultsMap.put(testTimestamp, createIntervalResults(AnalyzerConstants.MetricName.namespaceCpuRequest, 5.0, 0.0, "cores"));

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValueForNamespace(
                AnalyzerConstants.MetricName.namespaceCpuRequest, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNotNull(result);
        assertEquals(0.0, result.getAmount()); // Should return zero sum
        assertEquals("cores", result.getFormat());
    }

    @Test
    public void testGetCurrentValueForNamespace_LargeValues() {
        // Given - Large namespace values
        filteredResultsMap.put(testTimestamp, createIntervalResults(AnalyzerConstants.MetricName.namespaceMemoryRequest, 10000.0, 100000.0, "MiB"));

        // When
        RecommendationConfigItem result = RecommendationUtils.getCurrentValueForNamespace(
                AnalyzerConstants.MetricName.namespaceMemoryRequest, filteredResultsMap, testTimestamp, notifications);

        // Then
        assertNotNull(result);
        assertEquals(100000.0, result.getAmount());
        assertEquals("MiB", result.getFormat());
        assertTrue(notifications.isEmpty());
    }
}