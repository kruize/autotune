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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
            // When
            String result = BulkServiceValidation.validateClusterName(null);

            // Then
            assertEquals("", result, "Null cluster name should be valid");
        }

        @Test
        @DisplayName("Should reject empty string cluster name")
        void shouldRejectEmptyStringClusterName() {
            // When
            String result = BulkServiceValidation.validateClusterName("");

            // Then
            assertTrue(result.contains("cannot be an empty string"), 
                "Empty string should be rejected");
        }

        @Test
        @DisplayName("Should accept valid lowercase cluster name")
        void shouldAcceptValidLowercaseClusterName() {
            // When
            String result = BulkServiceValidation.validateClusterName("cluster-a");

            // Then
            assertEquals("", result, "Valid lowercase cluster name should be accepted");
        }

        @Test
        @DisplayName("Should accept cluster name with dots")
        void shouldAcceptClusterNameWithDots() {
            // When
            String result = BulkServiceValidation.validateClusterName("cluster.prod.us-east");

            // Then
            assertEquals("", result, "Cluster name with dots should be accepted");
        }

        @Test
        @DisplayName("Should accept cluster name with numbers")
        void shouldAcceptClusterNameWithNumbers() {
            // When
            String result = BulkServiceValidation.validateClusterName("cluster-123");

            // Then
            assertEquals("", result, "Cluster name with numbers should be accepted");
        }

        @Test
        @DisplayName("Should reject cluster name with uppercase letters")
        void shouldRejectClusterNameWithUppercase() {
            // When
            String result = BulkServiceValidation.validateClusterName("Cluster-A");

            // Then
            assertTrue(result.contains("Invalid cluster_name format"), 
                "Uppercase letters should be rejected");
        }

        @Test
        @DisplayName("Should reject cluster name starting with hyphen")
        void shouldRejectClusterNameStartingWithHyphen() {
            // When
            String result = BulkServiceValidation.validateClusterName("-cluster");

            // Then
            assertTrue(result.contains("Invalid cluster_name format"), 
                "Cluster name starting with hyphen should be rejected");
        }

        @Test
        @DisplayName("Should reject cluster name ending with hyphen")
        void shouldRejectClusterNameEndingWithHyphen() {
            // When
            String result = BulkServiceValidation.validateClusterName("cluster-");

            // Then
            assertTrue(result.contains("Invalid cluster_name format"), 
                "Cluster name ending with hyphen should be rejected");
        }

        @Test
        @DisplayName("Should reject cluster name with special characters")
        void shouldRejectClusterNameWithSpecialCharacters() {
            // When
            String result = BulkServiceValidation.validateClusterName("cluster_name");

            // Then
            assertTrue(result.contains("Invalid cluster_name format"), 
                "Underscore should be rejected");
        }

        @Test
        @DisplayName("Should reject cluster name exceeding 253 characters")
        void shouldRejectClusterNameExceeding253Characters() {
            // Given - Create a 254 character string
            String longName = "a".repeat(254);

            // When
            String result = BulkServiceValidation.validateClusterName(longName);

            // Then
            assertTrue(result.contains("too long"), 
                "Cluster name exceeding 253 characters should be rejected");
        }

        @Test
        @DisplayName("Should accept cluster name with exactly 253 characters")
        void shouldAcceptClusterNameWith253Characters() {
            // Given - Create a valid 253 character string
            String maxLengthName = "a" + "-".repeat(251) + "b";

            // When
            String result = BulkServiceValidation.validateClusterName(maxLengthName);

            // Then
            assertEquals("", result, "253 character cluster name should be accepted");
        }

        @Test
        @DisplayName("Should reject cluster name with spaces")
        void shouldRejectClusterNameWithSpaces() {
            // When
            String result = BulkServiceValidation.validateClusterName("cluster name");

            // Then
            assertTrue(result.contains("Invalid cluster_name format"), 
                "Spaces should be rejected");
        }

        @Test
        @DisplayName("Should accept single character cluster name")
        void shouldAcceptSingleCharacterClusterName() {
            // When
            String result = BulkServiceValidation.validateClusterName("a");

            // Then
            assertEquals("", result, "Single character cluster name should be accepted");
        }

        @Test
        @DisplayName("Should accept cluster name with leading and trailing whitespace after trimming")
        void shouldAcceptClusterNameWithWhitespaceAfterTrimming() {
            // When
            String result = BulkServiceValidation.validateClusterName("  prod-cluster  ");

            // Then
            assertEquals("", result, "Cluster name with leading/trailing spaces should be accepted after trimming");
        }

        @Test
        @DisplayName("Should reject whitespace-only cluster name")
        void shouldRejectWhitespaceOnlyClusterName() {
            // When
            String result = BulkServiceValidation.validateClusterName("   ");

            // Then
            assertTrue(result.contains("cannot be an empty string"),
                "Whitespace-only cluster name should be rejected as empty");
        }
    }


}
