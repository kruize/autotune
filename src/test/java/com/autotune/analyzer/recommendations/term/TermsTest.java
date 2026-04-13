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

package com.autotune.analyzer.recommendations.term;

import com.autotune.common.data.result.IntervalResults;
import com.autotune.utils.KruizeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Terms#checkIfMinDataAvailableForTerm and 
 * Terms#checkIfMinDataAvailableForTermForNamespace methods
 */
public class TermsTest {

    private Map<Timestamp, IntervalResults> metricsData;
    private Terms shortTerm;
    private Terms mediumTerm;
    private Terms longTerm;
    private Terms customTerm;

    @BeforeEach
    public void setUp() {
        metricsData = new HashMap<>();
        
        // Create terms with different thresholds
        // Short term: 1 day threshold
        shortTerm = new Terms("short_term", 1, 0.5, 10, 0.1);
        
        // Medium term: 3 days threshold
        mediumTerm = new Terms("medium_term", 3, 1.5, 20, 0.2);
        
        // Long term: 7 days threshold
        longTerm = new Terms("long_term", 7, 3.5, 30, 0.3);
        
        // Custom term for specific testing
        customTerm = new Terms("custom_term", 2, 1.0, 15, 0.15);
    }

    /**
     * Helper method to populate metrics data with specified number of data points
     */
    private void populateMetricsData(int dataPoints) {
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < dataPoints; i++) {
            Timestamp timestamp = new Timestamp(currentTime + (i * 60000)); // 1 minute apart
            metricsData.put(timestamp, new IntervalResults());
        }
    }

    // ==================== Tests for checkIfMinDataAvailableForTerm ====================

    @Test
    public void testCheckIfMinDataAvailableForTerm_NullMetricsData() {
        // Given
        double measurementDuration = 15.0; // 15 minutes

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(null, shortTerm, measurementDuration);

        // Then
        assertFalse(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTerm_EmptyMetricsData() {
        // Given
        double measurementDuration = 15.0;

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(metricsData, shortTerm, measurementDuration);

        // Then
        assertFalse(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTerm_ShortTerm_SufficientData() {
        // Given
        double measurementDuration = 15.0; // 15 minutes
        // For short term: threshold = 0.5 days
        // Min datapoints = (0.5 * 24 * 60) / 15 = 48 datapoints
        populateMetricsData(50); // More than required

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(metricsData, shortTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTerm_ShortTerm_InsufficientData() {
        // Given
        double measurementDuration = 15.0;
        // Min datapoints = (0.5 * 24 * 60) / 15 = 48 datapoints
        populateMetricsData(40); // Less than required

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(metricsData, shortTerm, measurementDuration);

        // Then
        assertFalse(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTerm_ShortTerm_ExactMinimum() {
        // Given
        double measurementDuration = 15.0;
        // Min datapoints = (0.5 * 24 * 60) / 15 = 48 datapoints
        populateMetricsData(48); // Exactly the minimum

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(metricsData, shortTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTerm_MediumTerm_SufficientData() {
        // Given
        double measurementDuration = 15.0;
        // For medium term: threshold = 1.5 days
        // Min datapoints = (1.5 * 24 * 60) / 15 = 144 datapoints
        populateMetricsData(150);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(metricsData, mediumTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTerm_MediumTerm_InsufficientData() {
        // Given
        double measurementDuration = 15.0;
        // Min datapoints = (1.5 * 24 * 60) / 15 = 144 datapoints
        populateMetricsData(100);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(metricsData, mediumTerm, measurementDuration);

        // Then
        assertFalse(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTerm_LongTerm_SufficientData() {
        // Given
        double measurementDuration = 15.0;
        // For long term: threshold = 3.5 days
        // Min datapoints = (3.5 * 24 * 60) / 15 = 336 datapoints
        populateMetricsData(350);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(metricsData, longTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTerm_LongTerm_InsufficientData() {
        // Given
        double measurementDuration = 15.0;
        // Min datapoints = (3.5 * 24 * 60) / 15 = 336 datapoints
        populateMetricsData(300);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(metricsData, longTerm, measurementDuration);

        // Then
        assertFalse(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTerm_DifferentMeasurementDuration_5Min() {
        // Given
        double measurementDuration = 5.0; // 5 minutes
        // For custom term: threshold = 1.0 day
        // Min datapoints = (1.0 * 24 * 60) / 5 = 288 datapoints
        populateMetricsData(300);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(metricsData, customTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTerm_DifferentMeasurementDuration_30Min() {
        // Given
        double measurementDuration = 30.0; // 30 minutes
        // For custom term: threshold = 1.0 day
        // Min datapoints = (1.0 * 24 * 60) / 30 = 48 datapoints
        populateMetricsData(50);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(metricsData, customTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTerm_DifferentMeasurementDuration_60Min() {
        // Given
        double measurementDuration = 60.0; // 60 minutes (1 hour)
        // For custom term: threshold = 1.0 day
        // Min datapoints = (1.0 * 24 * 60) / 60 = 24 datapoints
        populateMetricsData(25);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(metricsData, customTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTerm_SingleDataPoint() {
        // Given
        double measurementDuration = 15.0;
        populateMetricsData(1);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(metricsData, shortTerm, measurementDuration);

        // Then
        assertFalse(result); // 1 datapoint is not enough for any term
    }

    @Test
    public void testCheckIfMinDataAvailableForTerm_VerySmallThreshold() {
        // Given
        Terms veryShortTerm = new Terms("very_short", 1, 0.1, 5, 0.05); // 0.1 day threshold
        double measurementDuration = 15.0;
        // Min datapoints = (0.1 * 24 * 60) / 15 = 9.6 = 9 datapoints (integer cast)
        populateMetricsData(10);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(metricsData, veryShortTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTerm_LargeDataset() {
        // Given
        double measurementDuration = 15.0;
        populateMetricsData(10000); // Very large dataset

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(metricsData, longTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    // ==================== Tests for checkIfMinDataAvailableForTermForNamespace ====================

    @Test
    public void testCheckIfMinDataAvailableForTermForNamespace_NullMetricsData() {
        // Given
        double measurementDuration = 15.0;

        // When
        boolean result = Terms.checkIfMinDataAvailableForTermForNamespace(null, shortTerm, measurementDuration);

        // Then
        assertFalse(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTermForNamespace_EmptyMetricsData() {
        // Given
        double measurementDuration = 15.0;

        // When
        boolean result = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, shortTerm, measurementDuration);

        // Then
        assertFalse(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTermForNamespace_ShortTerm_SufficientData() {
        // Given
        double measurementDuration = 15.0;
        // Min datapoints = (0.5 * 24 * 60) / 15 = 48 datapoints
        populateMetricsData(50);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, shortTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTermForNamespace_ShortTerm_InsufficientData() {
        // Given
        double measurementDuration = 15.0;
        // Min datapoints = (0.5 * 24 * 60) / 15 = 48 datapoints
        populateMetricsData(40);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, shortTerm, measurementDuration);

        // Then
        assertFalse(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTermForNamespace_ShortTerm_ExactMinimum() {
        // Given
        double measurementDuration = 15.0;
        // Min datapoints = (0.5 * 24 * 60) / 15 = 48 datapoints
        populateMetricsData(48);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, shortTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTermForNamespace_MediumTerm_SufficientData() {
        // Given
        double measurementDuration = 15.0;
        // Min datapoints = (1.5 * 24 * 60) / 15 = 144 datapoints
        populateMetricsData(150);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, mediumTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTermForNamespace_MediumTerm_InsufficientData() {
        // Given
        double measurementDuration = 15.0;
        // Min datapoints = (1.5 * 24 * 60) / 15 = 144 datapoints
        populateMetricsData(100);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, mediumTerm, measurementDuration);

        // Then
        assertFalse(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTermForNamespace_LongTerm_SufficientData() {
        // Given
        double measurementDuration = 15.0;
        // Min datapoints = (3.5 * 24 * 60) / 15 = 336 datapoints
        populateMetricsData(350);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, longTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTermForNamespace_LongTerm_InsufficientData() {
        // Given
        double measurementDuration = 15.0;
        // Min datapoints = (3.5 * 24 * 60) / 15 = 336 datapoints
        populateMetricsData(300);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, longTerm, measurementDuration);

        // Then
        assertFalse(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTermForNamespace_DifferentMeasurementDuration_5Min() {
        // Given
        double measurementDuration = 5.0;
        // Min datapoints = (1.0 * 24 * 60) / 5 = 288 datapoints
        populateMetricsData(300);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, customTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTermForNamespace_DifferentMeasurementDuration_30Min() {
        // Given
        double measurementDuration = 30.0;
        // Min datapoints = (1.0 * 24 * 60) / 30 = 48 datapoints
        populateMetricsData(50);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, customTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTermForNamespace_DifferentMeasurementDuration_60Min() {
        // Given
        double measurementDuration = 60.0;
        // Min datapoints = (1.0 * 24 * 60) / 60 = 24 datapoints
        populateMetricsData(25);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, customTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTermForNamespace_SingleDataPoint() {
        // Given
        double measurementDuration = 15.0;
        populateMetricsData(1);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, shortTerm, measurementDuration);

        // Then
        assertFalse(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTermForNamespace_VerySmallThreshold() {
        // Given
        Terms veryShortTerm = new Terms("very_short", 1, 0.1, 5, 0.05);
        double measurementDuration = 15.0;
        // Min datapoints = (0.1 * 24 * 60) / 15 = 9.6 = 9 datapoints
        populateMetricsData(10);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, veryShortTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTermForNamespace_LargeDataset() {
        // Given
        double measurementDuration = 15.0;
        populateMetricsData(10000);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, longTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    // ==================== Comparison Tests ====================

    @Test
    public void testBothMethods_ProduceSameResult_SufficientData() {
        // Given
        double measurementDuration = 15.0;
        populateMetricsData(100);

        // When
        boolean containerResult = Terms.checkIfMinDataAvailableForTerm(metricsData, shortTerm, measurementDuration);
        boolean namespaceResult = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, shortTerm, measurementDuration);

        // Then
        assertEquals(containerResult, namespaceResult);
        assertTrue(containerResult);
    }

    @Test
    public void testBothMethods_ProduceSameResult_InsufficientData() {
        // Given
        double measurementDuration = 15.0;
        populateMetricsData(10);

        // When
        boolean containerResult = Terms.checkIfMinDataAvailableForTerm(metricsData, shortTerm, measurementDuration);
        boolean namespaceResult = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, shortTerm, measurementDuration);

        // Then
        assertEquals(containerResult, namespaceResult);
        assertFalse(containerResult);
    }

    @Test
    public void testBothMethods_ProduceSameResult_NullData() {
        // Given
        double measurementDuration = 15.0;

        // When
        boolean containerResult = Terms.checkIfMinDataAvailableForTerm(null, shortTerm, measurementDuration);
        boolean namespaceResult = Terms.checkIfMinDataAvailableForTermForNamespace(null, shortTerm, measurementDuration);

        // Then
        assertEquals(containerResult, namespaceResult);
        assertFalse(containerResult);
    }

    @Test
    public void testBothMethods_ProduceSameResult_EmptyData() {
        // Given
        double measurementDuration = 15.0;

        // When
        boolean containerResult = Terms.checkIfMinDataAvailableForTerm(metricsData, shortTerm, measurementDuration);
        boolean namespaceResult = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, shortTerm, measurementDuration);

        // Then
        assertEquals(containerResult, namespaceResult);
        assertFalse(containerResult);
    }

    // ==================== Edge Case Tests ====================

    @Test
    public void testCheckIfMinDataAvailableForTerm_ZeroThreshold() {
        // Given
        Terms zeroThresholdTerm = new Terms("zero", 1, 0.0, 5, 0.05);
        double measurementDuration = 15.0;
        populateMetricsData(1);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(metricsData, zeroThresholdTerm, measurementDuration);

        // Then
        assertTrue(result); // 0 datapoints required, so any data is sufficient
    }

    @Test
    public void testCheckIfMinDataAvailableForTermForNamespace_ZeroThreshold() {
        // Given
        Terms zeroThresholdTerm = new Terms("zero", 1, 0.0, 5, 0.05);
        double measurementDuration = 15.0;
        populateMetricsData(1);

        // When
        boolean result = Terms.checkIfMinDataAvailableForTermForNamespace(metricsData, zeroThresholdTerm, measurementDuration);

        // Then
        assertTrue(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTerm_BoundaryCondition_JustBelowMinimum() {
        // Given
        double measurementDuration = 15.0;
        // Min datapoints = (0.5 * 24 * 60) / 15 = 48 datapoints
        populateMetricsData(47); // Just below minimum

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(metricsData, shortTerm, measurementDuration);

        // Then
        assertFalse(result);
    }

    @Test
    public void testCheckIfMinDataAvailableForTerm_BoundaryCondition_JustAboveMinimum() {
        // Given
        double measurementDuration = 15.0;
        // Min datapoints = (0.5 * 24 * 60) / 15 = 48 datapoints
        populateMetricsData(49); // Just above minimum

        // When
        boolean result = Terms.checkIfMinDataAvailableForTerm(metricsData, shortTerm, measurementDuration);

        // Then
        assertTrue(result);
    }
}