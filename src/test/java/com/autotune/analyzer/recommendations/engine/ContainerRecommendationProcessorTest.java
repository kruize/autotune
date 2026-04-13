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

package com.autotune.analyzer.recommendations.engine;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.IntervalResults;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ContainerRecommendationProcessor#getPodCountAggrInfo method
 */
public class ContainerRecommendationProcessorTest {

    /**
     * Helper method to invoke the private static getPodCountAggrInfo method using reflection
     */
    private MetricAggregationInfoResults invokePodCountAggrInfo(Map<Timestamp, IntervalResults> filteredResultsMap) throws Exception {
        Method method = ContainerRecommendationProcessor.class.getDeclaredMethod("getPodCountAggrInfo", Map.class);
        method.setAccessible(true);
        return (MetricAggregationInfoResults) method.invoke(null, filteredResultsMap);
    }

    /**
     * Helper method to create IntervalResults with metric data
     */
    private IntervalResults createIntervalResults(AnalyzerConstants.MetricName metricName, Double avg, Double min, Double max, Double sum) {
        IntervalResults intervalResults = new IntervalResults();
        HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap = new HashMap<>();
        
        MetricResults metricResults = new MetricResults();
        MetricAggregationInfoResults aggInfo = new MetricAggregationInfoResults();
        aggInfo.setAvg(avg);
        aggInfo.setMin(min);
        aggInfo.setMax(max);
        aggInfo.setSum(sum);
        metricResults.setAggregationInfoResult(aggInfo);
        
        metricResultsMap.put(metricName, metricResults);
        intervalResults.setMetricResultsMap(metricResultsMap);
        
        return intervalResults;
    }

    @Test
    public void testGetPodCountAggrInfo_WithPodCountMetric() throws Exception {
        // Given - Pod count metric data available
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();
        
        Timestamp ts1 = new Timestamp(System.currentTimeMillis());
        Timestamp ts2 = new Timestamp(System.currentTimeMillis() + 60000);
        Timestamp ts3 = new Timestamp(System.currentTimeMillis() + 120000);
        
        filteredResultsMap.put(ts1, createIntervalResults(AnalyzerConstants.MetricName.podCount, 3.0, 2.0, 4.0, null));
        filteredResultsMap.put(ts2, createIntervalResults(AnalyzerConstants.MetricName.podCount, 4.0, 3.0, 5.0, null));
        filteredResultsMap.put(ts3, createIntervalResults(AnalyzerConstants.MetricName.podCount, 5.0, 4.0, 6.0, null));

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then
        assertNotNull(result);
        assertEquals(4.0, result.getAvg()); // ceil((3+4+5)/3) = ceil(4.0) = 4.0
        assertEquals(2.0, result.getMin()); // ceil(min(2,3,4)) = ceil(2.0) = 2.0
        assertEquals(6.0, result.getMax()); // ceil(max(4,5,6)) = ceil(6.0) = 6.0
    }

    @Test
    public void testGetPodCountAggrInfo_WithPodCountMetric_SingleDataPoint() throws Exception {
        // Given - Single pod count data point
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        filteredResultsMap.put(ts, createIntervalResults(AnalyzerConstants.MetricName.podCount, 3.5, 3.0, 4.0, null));

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then
        assertNotNull(result);
        assertEquals(4.0, result.getAvg()); // ceil(3.5) = 4.0
        assertEquals(3.0, result.getMin()); // ceil(3.0) = 3.0
        assertEquals(4.0, result.getMax()); // ceil(4.0) = 4.0
    }

    @Test
    public void testGetPodCountAggrInfo_WithCpuUsageMetric() throws Exception {
        // Given - No pod count, but CPU usage data available
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();
        
        Timestamp ts1 = new Timestamp(System.currentTimeMillis());
        Timestamp ts2 = new Timestamp(System.currentTimeMillis() + 60000);
        
        // sum/avg = pod count: 6.0/2.0 = 3.0, 8.0/2.0 = 4.0
        filteredResultsMap.put(ts1, createIntervalResults(AnalyzerConstants.MetricName.cpuUsage, 2.0, 1.5, 2.5, 6.0));
        filteredResultsMap.put(ts2, createIntervalResults(AnalyzerConstants.MetricName.cpuUsage, 2.0, 1.5, 2.5, 8.0));

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then
        assertNotNull(result);
        assertEquals(4.0, result.getAvg()); // ceil((3.0+4.0)/2) = ceil(3.5) = 4.0
        assertEquals(3.0, result.getMin()); // ceil(min(3.0, 4.0)) = ceil(3.0) = 3.0
        assertEquals(4.0, result.getMax()); // ceil(max(3.0, 4.0)) = ceil(4.0) = 4.0
    }

    @Test
    public void testGetPodCountAggrInfo_WithMemoryUsageMetric() throws Exception {
        // Given - No pod count or CPU usage, but memory usage data available
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();
        
        Timestamp ts1 = new Timestamp(System.currentTimeMillis());
        Timestamp ts2 = new Timestamp(System.currentTimeMillis() + 60000);
        
        // sum/avg = pod count: 1024.0/512.0 = 2.0, 2048.0/512.0 = 4.0
        filteredResultsMap.put(ts1, createIntervalResults(AnalyzerConstants.MetricName.memoryUsage, 512.0, 400.0, 600.0, 1024.0));
        filteredResultsMap.put(ts2, createIntervalResults(AnalyzerConstants.MetricName.memoryUsage, 512.0, 400.0, 600.0, 2048.0));

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then
        assertNotNull(result);
        assertEquals(3.0, result.getAvg()); // ceil((2.0+4.0)/2) = ceil(3.0) = 3.0
        assertEquals(2.0, result.getMin()); // ceil(min(2.0, 4.0)) = ceil(2.0) = 2.0
        assertEquals(4.0, result.getMax()); // ceil(max(2.0, 4.0)) = ceil(4.0) = 4.0
    }

    @Test
    public void testGetPodCountAggrInfo_EmptyResultsMap() throws Exception {
        // Given - Empty results map
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then
        assertNull(result);
    }

    @Test
    public void testGetPodCountAggrInfo_NullMetricResultsMap() throws Exception {
        // Given - IntervalResults with null metric results map
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        IntervalResults intervalResults = new IntervalResults();
        intervalResults.setMetricResultsMap(null);
        filteredResultsMap.put(ts, intervalResults);

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then
        assertNull(result);
    }

    @Test
    public void testGetPodCountAggrInfo_NullAggregationInfo() throws Exception {
        // Given - Metric results with null aggregation info
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        
        IntervalResults intervalResults = new IntervalResults();
        HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap = new HashMap<>();
        MetricResults metricResults = new MetricResults();
        metricResults.setAggregationInfoResult(null);
        metricResultsMap.put(AnalyzerConstants.MetricName.podCount, metricResults);
        intervalResults.setMetricResultsMap(metricResultsMap);
        
        filteredResultsMap.put(ts, intervalResults);

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then
        assertNull(result);
    }

    @Test
    public void testGetPodCountAggrInfo_NullAvgInPodCount() throws Exception {
        // Given - Pod count with null avg
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        
        IntervalResults intervalResults = new IntervalResults();
        HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap = new HashMap<>();
        MetricResults metricResults = new MetricResults();
        MetricAggregationInfoResults aggInfo = new MetricAggregationInfoResults();
        aggInfo.setAvg(null);
        metricResults.setAggregationInfoResult(aggInfo);
        metricResultsMap.put(AnalyzerConstants.MetricName.podCount, metricResults);
        intervalResults.setMetricResultsMap(metricResultsMap);
        
        filteredResultsMap.put(ts, intervalResults);

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then
        assertNull(result);
    }

    @Test
    public void testGetPodCountAggrInfo_ZeroAvgInCpuUsage() throws Exception {
        // Given - CPU usage with zero avg (should be filtered out)
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        filteredResultsMap.put(ts, createIntervalResults(AnalyzerConstants.MetricName.cpuUsage, 0.0, 0.0, 0.0, 10.0));

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then
        assertNull(result);
    }

    @Test
    public void testGetPodCountAggrInfo_NullSumInCpuUsage() throws Exception {
        // Given - CPU usage with null sum (should be filtered out)
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        filteredResultsMap.put(ts, createIntervalResults(AnalyzerConstants.MetricName.cpuUsage, 2.0, 1.5, 2.5, null));

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then
        assertNull(result);
    }

    @Test
    public void testGetPodCountAggrInfo_PodCountTakesPrecedenceOverCpu() throws Exception {
        // Given - Both pod count and CPU usage available
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        
        IntervalResults intervalResults = new IntervalResults();
        HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap = new HashMap<>();
        
        // Pod count metric
        MetricResults podCountMetric = new MetricResults();
        MetricAggregationInfoResults podCountAggInfo = new MetricAggregationInfoResults();
        podCountAggInfo.setAvg(5.0);
        podCountAggInfo.setMin(4.0);
        podCountAggInfo.setMax(6.0);
        podCountMetric.setAggregationInfoResult(podCountAggInfo);
        metricResultsMap.put(AnalyzerConstants.MetricName.podCount, podCountMetric);
        
        // CPU usage metric
        MetricResults cpuMetric = new MetricResults();
        MetricAggregationInfoResults cpuAggInfo = new MetricAggregationInfoResults();
        cpuAggInfo.setAvg(2.0);
        cpuAggInfo.setSum(6.0); // Would give pod count of 3
        cpuMetric.setAggregationInfoResult(cpuAggInfo);
        metricResultsMap.put(AnalyzerConstants.MetricName.cpuUsage, cpuMetric);
        
        intervalResults.setMetricResultsMap(metricResultsMap);
        filteredResultsMap.put(ts, intervalResults);

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then - Should use pod count, not CPU
        assertNotNull(result);
        assertEquals(5.0, result.getAvg());
        assertEquals(4.0, result.getMin());
        assertEquals(6.0, result.getMax());
    }

    @Test
    public void testGetPodCountAggrInfo_CpuTakesPrecedenceOverMemory() throws Exception {
        // Given - Both CPU and memory usage available, but no pod count
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        
        IntervalResults intervalResults = new IntervalResults();
        HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap = new HashMap<>();
        
        // CPU usage metric
        MetricResults cpuMetric = new MetricResults();
        MetricAggregationInfoResults cpuAggInfo = new MetricAggregationInfoResults();
        cpuAggInfo.setAvg(2.0);
        cpuAggInfo.setSum(8.0); // Would give pod count of 4
        cpuMetric.setAggregationInfoResult(cpuAggInfo);
        metricResultsMap.put(AnalyzerConstants.MetricName.cpuUsage, cpuMetric);
        
        // Memory usage metric
        MetricResults memMetric = new MetricResults();
        MetricAggregationInfoResults memAggInfo = new MetricAggregationInfoResults();
        memAggInfo.setAvg(512.0);
        memAggInfo.setSum(1024.0); // Would give pod count of 2
        memMetric.setAggregationInfoResult(memAggInfo);
        metricResultsMap.put(AnalyzerConstants.MetricName.memoryUsage, memMetric);
        
        intervalResults.setMetricResultsMap(metricResultsMap);
        filteredResultsMap.put(ts, intervalResults);

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then - Should use CPU, not memory
        assertNotNull(result);
        assertEquals(4.0, result.getAvg());
    }

    @Test
    public void testGetPodCountAggrInfo_WithDecimalPodCounts() throws Exception {
        // Given - Pod count with decimal values
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        filteredResultsMap.put(ts, createIntervalResults(AnalyzerConstants.MetricName.podCount, 3.7, 2.3, 4.9, null));

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then - Should ceil the values
        assertNotNull(result);
        assertEquals(4.0, result.getAvg()); // ceil(3.7) = 4.0
        assertEquals(3.0, result.getMin()); // ceil(2.3) = 3.0
        assertEquals(5.0, result.getMax()); // ceil(4.9) = 5.0
    }

    @Test
    public void testGetPodCountAggrInfo_WithMultiplePodCountDataPoints() throws Exception {
        // Given - Multiple pod count data points
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();
        
        for (int i = 0; i < 5; i++) {
            Timestamp ts = new Timestamp(System.currentTimeMillis() + (i * 60000));
            double avg = 3.0 + i * 0.5; // 3.0, 3.5, 4.0, 4.5, 5.0
            filteredResultsMap.put(ts, createIntervalResults(AnalyzerConstants.MetricName.podCount, avg, avg - 0.5, avg + 0.5, null));
        }

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then
        assertNotNull(result);
        assertEquals(4.0, result.getAvg()); // ceil((3.0+3.5+4.0+4.5+5.0)/5) = ceil(4.0) = 4.0
        assertEquals(3.0, result.getMin()); // ceil(min(2.5, 3.0, 3.5, 4.0, 4.5)) = ceil(2.5) = 3.0
        assertEquals(6.0, result.getMax()); // ceil(max(3.5, 4.0, 4.5, 5.0, 5.5)) = ceil(5.5) = 6.0
    }

    @Test
    public void testGetPodCountAggrInfo_MixedValidAndInvalidCpuData() throws Exception {
        // Given - Mix of valid and invalid CPU usage data
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();
        
        Timestamp ts1 = new Timestamp(System.currentTimeMillis());
        Timestamp ts2 = new Timestamp(System.currentTimeMillis() + 60000);
        Timestamp ts3 = new Timestamp(System.currentTimeMillis() + 120000);
        
        // Valid data
        filteredResultsMap.put(ts1, createIntervalResults(AnalyzerConstants.MetricName.cpuUsage, 2.0, 1.5, 2.5, 6.0));
        // Invalid - zero avg
        filteredResultsMap.put(ts2, createIntervalResults(AnalyzerConstants.MetricName.cpuUsage, 0.0, 0.0, 0.0, 8.0));
        // Valid data
        filteredResultsMap.put(ts3, createIntervalResults(AnalyzerConstants.MetricName.cpuUsage, 2.0, 1.5, 2.5, 8.0));

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then - Should only use valid data points
        assertNotNull(result);
        assertEquals(4.0, result.getAvg()); // ceil((3.0+4.0)/2) = ceil(3.5) = 4.0
    }

    @Test
    public void testGetPodCountAggrInfo_LargePodCounts() throws Exception {
        // Given - Large pod count values
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        filteredResultsMap.put(ts, createIntervalResults(AnalyzerConstants.MetricName.podCount, 100.5, 50.2, 150.8, null));

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then
        assertNotNull(result);
        assertEquals(101.0, result.getAvg()); // ceil(100.5) = 101.0
        assertEquals(51.0, result.getMin()); // ceil(50.2) = 51.0
        assertEquals(151.0, result.getMax()); // ceil(150.8) = 151.0
    }

    @Test
    public void testGetPodCountAggrInfo_AllMetricsUnavailable() throws Exception {
        // Given - Results with metrics other than podCount, cpuUsage, memoryUsage
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        filteredResultsMap.put(ts, createIntervalResults(AnalyzerConstants.MetricName.cpuRequest, 2.0, 1.5, 2.5, 6.0));

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then
        assertNull(result);
    }

    @Test
    public void testGetPodCountAggrInfo_CalculatedPodCountIsZero() throws Exception {
        // Given - CPU usage that would result in zero pod count
        Map<Timestamp, IntervalResults> filteredResultsMap = new HashMap<>();
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        
        IntervalResults intervalResults = new IntervalResults();
        HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap = new HashMap<>();
        MetricResults metricResults = new MetricResults();
        MetricAggregationInfoResults aggInfo = new MetricAggregationInfoResults();
        aggInfo.setAvg(2.0);
        aggInfo.setSum(0.0); // sum/avg = 0/2 = 0
        metricResults.setAggregationInfoResult(aggInfo);
        metricResultsMap.put(AnalyzerConstants.MetricName.cpuUsage, metricResults);
        intervalResults.setMetricResultsMap(metricResultsMap);
        
        filteredResultsMap.put(ts, intervalResults);

        // When
        MetricAggregationInfoResults result = invokePodCountAggrInfo(filteredResultsMap);

        // Then - Should return null as avg is 0
        assertNull(result);
    }
}