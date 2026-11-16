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
package com.autotune.common.bulk;

import com.autotune.analyzer.kruizeObject.TermSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BulkServiceValidation class.
 * Tests validation logic for cluster_name, model_settings, and term_settings fields.
 * 
 * Test Structure:
 * - Nested test classes for each validation method
 * - Positive and negative test cases
 * - Edge cases and boundary conditions
 */
class BulkServiceValidationTest {

    @Nested
    @DisplayName("Cluster Name Validation Tests")
    class ClusterNameValidationTests {

        @Test
        @DisplayName("Should accept null cluster name")
        void shouldAcceptNullClusterName() {
            // Given
            String[] errorOut = new String[1];

            // When
            String result = BulkServiceValidation.validateClusterName(null, errorOut);

            // Then
            assertNull(result, "Null cluster name should return null (optional field, not supplied)");
            assertNull(errorOut[0], "No error should be set for null cluster name");
        }

        @Test
        @DisplayName("Should reject empty string cluster name")
        void shouldRejectEmptyStringClusterName() {
            // Given
            String[] errorOut = new String[1];

            // When
            String result = BulkServiceValidation.validateClusterName("", errorOut);

            // Then
            assertNull(result, "Empty string should return null");
            assertNotNull(errorOut[0], "Error should be set for empty cluster name");
            assertTrue(errorOut[0].contains("cannot be an empty string"),
                "Empty string should be rejected");
        }

        @Test
        @DisplayName("Should accept valid lowercase cluster name")
        void shouldAcceptValidLowercaseClusterName() {
            // Given
            String[] errorOut = new String[1];

            // When
            String result = BulkServiceValidation.validateClusterName("cluster-a", errorOut);

            // Then
            assertEquals("cluster-a", result, "Valid lowercase cluster name should be returned");
            assertNull(errorOut[0], "No error should be set for valid cluster name");
        }

        @Test
        @DisplayName("Should accept cluster name with dots")
        void shouldAcceptClusterNameWithDots() {
            // Given
            String[] errorOut = new String[1];

            // When
            String result = BulkServiceValidation.validateClusterName("cluster.prod.us-east", errorOut);

            // Then
            assertEquals("cluster.prod.us-east", result, "Cluster name with dots should be accepted");
            assertNull(errorOut[0], "No error should be set for valid cluster name");
        }

        @Test
        @DisplayName("Should accept cluster name with numbers")
        void shouldAcceptClusterNameWithNumbers() {
            // Given
            String[] errorOut = new String[1];

            // When
            String result = BulkServiceValidation.validateClusterName("cluster-123", errorOut);

            // Then
            assertEquals("cluster-123", result, "Cluster name with numbers should be accepted");
            assertNull(errorOut[0], "No error should be set for valid cluster name");
        }

        @Test
        @DisplayName("Should reject cluster name exceeding 253 characters")
        void shouldRejectClusterNameExceeding253Characters() {
            // Given - Create a 254 character string
            String longName = "a".repeat(254);
            String[] errorOut = new String[1];

            // When
            String result = BulkServiceValidation.validateClusterName(longName, errorOut);

            // Then
            assertNull(result, "Cluster name exceeding 253 characters should return null");
            assertNotNull(errorOut[0], "Error should be set for too-long cluster name");
            assertTrue(errorOut[0].contains("too long"),
                "Cluster name exceeding 253 characters should be rejected");
        }

        @Test
        @DisplayName("Should accept cluster name with exactly 253 characters")
        void shouldAcceptClusterNameWith253Characters() {
            // Given - Create a valid 253 character string
            String maxLengthName = "a" + "-".repeat(251) + "b";
            String[] errorOut = new String[1];

            // When
            String result = BulkServiceValidation.validateClusterName(maxLengthName, errorOut);

            // Then
            assertEquals(maxLengthName, result, "253 character cluster name should be accepted");
            assertNull(errorOut[0], "No error should be set for 253-character cluster name");
        }

        @Test
        @DisplayName("Should accept single character cluster name")
        void shouldAcceptSingleCharacterClusterName() {
            // Given
            String[] errorOut = new String[1];

            // When
            String result = BulkServiceValidation.validateClusterName("a", errorOut);

            // Then
            assertEquals("a", result, "Single character cluster name should be accepted");
            assertNull(errorOut[0], "No error should be set for valid cluster name");
        }

        @Test
        @DisplayName("Should return trimmed cluster name when leading and trailing whitespace present")
        void shouldAcceptClusterNameWithWhitespaceAfterTrimming() {
            // Given
            String[] errorOut = new String[1];

            // When
            String result = BulkServiceValidation.validateClusterName("  prod-cluster  ", errorOut);

            // Then
            assertEquals("prod-cluster", result, "Cluster name should be trimmed");
            assertNull(errorOut[0], "No error should be set for valid cluster name after trimming");
        }

        @Test
        @DisplayName("Should reject whitespace-only cluster name")
        void shouldRejectWhitespaceOnlyClusterName() {
            // Given
            String[] errorOut = new String[1];

            // When
            String result = BulkServiceValidation.validateClusterName("   ", errorOut);

            // Then
            assertNull(result, "Whitespace-only cluster name should return null");
            assertNotNull(errorOut[0], "Error should be set for whitespace-only cluster name");
            assertTrue(errorOut[0].contains("cannot be an empty string"),
                "Whitespace-only cluster name should be rejected as empty");
        }
    }

    @Nested
    @DisplayName("Term Settings Validation Tests")
    class TermSettingsValidationTests {

        @Test
        @DisplayName("Should accept null term settings")
        void shouldAcceptNullTermSettings() {
            String result = BulkServiceValidation.validateTermSettings(null);
            assertEquals("", result, "Null term settings should be valid (optional field)");
        }

        @Test
        @DisplayName("Should accept flex term")
        void shouldAcceptFlexTerm() {
            TermSettings termSettings = new TermSettings();
            termSettings.setTerms(List.of("flex"));
            String result = BulkServiceValidation.validateTermSettings(termSettings);
            assertEquals("", result, "Flex term should be accepted");
        }

        @Test
        @DisplayName("Should accept all valid terms including flex case-insensitively")
        void shouldAcceptAllValidTerms() {
            TermSettings termSettings = new TermSettings();
            termSettings.setTerms(Arrays.asList("Short", "MEDIUM", "Long", "FLEX"));
            String result = BulkServiceValidation.validateTermSettings(termSettings);
            assertEquals("", result, "All valid terms including flex should be accepted case-insensitively");
        }

        @Test
        @DisplayName("Should reject invalid term name")
        void shouldRejectInvalidTermName() {
            TermSettings termSettings = new TermSettings();
            termSettings.setTerms(List.of("invalid_term"));
            String result = BulkServiceValidation.validateTermSettings(termSettings);
            assertTrue(result.contains("Invalid term name: invalid_term"), "Invalid term name should be rejected");
        }

        @Test
        @DisplayName("Should reject empty terms list")
        void shouldRejectEmptyTermsList() {
            TermSettings termSettings = new TermSettings();
            termSettings.setTerms(Collections.emptyList());
            String result = BulkServiceValidation.validateTermSettings(termSettings);
            assertTrue(result.contains("cannot be null or empty"), "Empty terms list should be rejected");
        }
    }

}
