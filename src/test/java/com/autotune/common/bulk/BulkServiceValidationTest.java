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

import com.autotune.analyzer.kruizeObject.ModelSettings;
import com.autotune.analyzer.kruizeObject.TermSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

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
        @DisplayName("Should accept cluster name with uppercase letters")
        void shouldAcceptClusterNameWithUppercase() {
            // When
            String result = BulkServiceValidation.validateClusterName("Cluster-A");

            // Then
            assertEquals("", result,
                "Uppercase letters should be accepted (format validation removed)");
        }

        @Test
        @DisplayName("Should accept cluster name starting with hyphen")
        void shouldAcceptClusterNameStartingWithHyphen() {
            // When
            String result = BulkServiceValidation.validateClusterName("-cluster");

            // Then
            assertEquals("", result,
                "Cluster name starting with hyphen should be accepted (format validation removed)");
        }

        @Test
        @DisplayName("Should accept cluster name ending with hyphen")
        void shouldAcceptClusterNameEndingWithHyphen() {
            // When
            String result = BulkServiceValidation.validateClusterName("cluster-");

            // Then
            assertEquals("", result,
                "Cluster name ending with hyphen should be accepted (format validation removed)");
        }

        @Test
        @DisplayName("Should accept cluster name with special characters")
        void shouldAcceptClusterNameWithSpecialCharacters() {
            // When
            String result = BulkServiceValidation.validateClusterName("cluster_name");

            // Then
            assertEquals("", result,
                "Underscore should be accepted (format validation removed)");
        }

        @Test
        @DisplayName("Should reject cluster name exceeding 253 characters")
        void shouldRejectClusterNameExceeding253Characters() {
            // Given - Create a 254 character string
            String longName = "a".repeat(254);

            // When
            String result = BulkServiceValidation.validateClusterName(longName);

            // Then
            assertTrue(result.contains("must not exceed 253 characters"),
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
        @DisplayName("Should accept cluster name with spaces")
        void shouldAcceptClusterNameWithSpaces() {
            // When
            String result = BulkServiceValidation.validateClusterName("cluster name");

            // Then
            assertEquals("", result,
                "Spaces should be accepted (format validation removed)");
        }

        @Test
        @DisplayName("Should accept single character cluster name")
        void shouldAcceptSingleCharacterClusterName() {
            // When
            String result = BulkServiceValidation.validateClusterName("a");

            // Then
            assertEquals("", result, "Single character cluster name should be accepted");
        }
    }

    @Nested
    @DisplayName("Model Settings Validation Tests")
    class ModelSettingsValidationTests {

        @Test
        @DisplayName("Should accept null model settings")
        void shouldAcceptNullModelSettings() {
            // When
            String result = BulkServiceValidation.validateModelSettings(null);

            // Then
            assertEquals("", result, "Null model settings should be valid");
        }

        @Test
        @DisplayName("Should reject model settings with null models list")
        void shouldRejectModelSettingsWithNullModelsList() {
            // Given
            ModelSettings settings = new ModelSettings();
            settings.setModels(null);

            // When
            String result = BulkServiceValidation.validateModelSettings(settings);

            // Then
            assertTrue(result.contains("cannot be null or empty"), 
                "Null models list should be rejected");
        }

        @Test
        @DisplayName("Should reject model settings with empty models list")
        void shouldRejectModelSettingsWithEmptyModelsList() {
            // Given
            ModelSettings settings = new ModelSettings();
            settings.setModels(Collections.emptyList());

            // When
            String result = BulkServiceValidation.validateModelSettings(settings);

            // Then
            assertTrue(result.contains("cannot be null or empty"), 
                "Empty models list should be rejected");
        }

        @Test
        @DisplayName("Should accept valid performance model")
        void shouldAcceptValidPerformanceModel() {
            // Given
            ModelSettings settings = new ModelSettings();
            settings.setModels(Arrays.asList("performance"));

            // When
            String result = BulkServiceValidation.validateModelSettings(settings);

            // Then
            assertEquals("", result, "Performance model should be accepted");
        }

        @Test
        @DisplayName("Should accept valid cost model")
        void shouldAcceptValidCostModel() {
            // Given
            ModelSettings settings = new ModelSettings();
            settings.setModels(Arrays.asList("cost"));

            // When
            String result = BulkServiceValidation.validateModelSettings(settings);

            // Then
            assertEquals("", result, "Cost model should be accepted");
        }

        @Test
        @DisplayName("Should accept multiple valid models")
        void shouldAcceptMultipleValidModels() {
            // Given
            ModelSettings settings = new ModelSettings();
            settings.setModels(Arrays.asList("performance", "cost"));

            // When
            String result = BulkServiceValidation.validateModelSettings(settings);

            // Then
            assertEquals("", result, "Multiple valid models should be accepted");
        }

        @Test
        @DisplayName("Should accept models with different case")
        void shouldAcceptModelsWithDifferentCase() {
            // Given
            ModelSettings settings = new ModelSettings();
            settings.setModels(Arrays.asList("Performance", "COST"));

            // When
            String result = BulkServiceValidation.validateModelSettings(settings);

            // Then
            assertEquals("", result, "Models with different case should be accepted");
        }

        @Test
        @DisplayName("Should reject invalid model name")
        void shouldRejectInvalidModelName() {
            // Given
            ModelSettings settings = new ModelSettings();
            settings.setModels(Arrays.asList("invalid-model"));

            // When
            String result = BulkServiceValidation.validateModelSettings(settings);

            // Then
            assertTrue(result.contains("Invalid model name"), 
                "Invalid model name should be rejected");
        }

        @Test
        @DisplayName("Should reject models list with null element")
        void shouldRejectModelsListWithNullElement() {
            // Given
            ModelSettings settings = new ModelSettings();
            settings.setModels(Arrays.asList("performance", null));

            // When
            String result = BulkServiceValidation.validateModelSettings(settings);

            // Then
            assertTrue(result.contains("null or empty model name"), 
                "Null model element should be rejected");
        }

        @Test
        @DisplayName("Should reject models list with empty string element")
        void shouldRejectModelsListWithEmptyStringElement() {
            // Given
            ModelSettings settings = new ModelSettings();
            settings.setModels(Arrays.asList("performance", ""));

            // When
            String result = BulkServiceValidation.validateModelSettings(settings);

            // Then
            assertTrue(result.contains("null or empty model name"), 
                "Empty string model element should be rejected");
        }

        @Test
        @DisplayName("Should reject models list with whitespace-only element")
        void shouldRejectModelsListWithWhitespaceOnlyElement() {
            // Given
            ModelSettings settings = new ModelSettings();
            settings.setModels(Arrays.asList("performance", "   "));

            // When
            String result = BulkServiceValidation.validateModelSettings(settings);

            // Then
            assertTrue(result.contains("null or empty model name"), 
                "Whitespace-only model element should be rejected");
        }

        @Test
        @DisplayName("Should accept models with leading/trailing whitespace")
        void shouldAcceptModelsWithLeadingTrailingWhitespace() {
            // Given
            ModelSettings settings = new ModelSettings();
            settings.setModels(Arrays.asList(" performance ", " cost "));

            // When
            String result = BulkServiceValidation.validateModelSettings(settings);

            // Then
            assertEquals("", result, "Models with whitespace should be trimmed and accepted");
        }
    }

    @Nested
    @DisplayName("Term Settings Validation Tests")
    class TermSettingsValidationTests {

        @Test
        @DisplayName("Should accept null term settings")
        void shouldAcceptNullTermSettings() {
            // When
            String result = BulkServiceValidation.validateTermSettings(null);

            // Then
            assertEquals("", result, "Null term settings should be valid");
        }

        @Test
        @DisplayName("Should reject term settings with null terms list")
        void shouldRejectTermSettingsWithNullTermsList() {
            // Given
            TermSettings settings = new TermSettings();
            settings.setTerms(null);

            // When
            String result = BulkServiceValidation.validateTermSettings(settings);

            // Then
            assertTrue(result.contains("cannot be null or empty"), 
                "Null terms list should be rejected");
        }

        @Test
        @DisplayName("Should reject term settings with empty terms list")
        void shouldRejectTermSettingsWithEmptyTermsList() {
            // Given
            TermSettings settings = new TermSettings();
            settings.setTerms(Collections.emptyList());

            // When
            String result = BulkServiceValidation.validateTermSettings(settings);

            // Then
            assertTrue(result.contains("cannot be null or empty"), 
                "Empty terms list should be rejected");
        }

        @Test
        @DisplayName("Should accept valid short term")
        void shouldAcceptValidShortTerm() {
            // Given
            TermSettings settings = new TermSettings();
            settings.setTerms(Arrays.asList("short"));

            // When
            String result = BulkServiceValidation.validateTermSettings(settings);

            // Then
            assertEquals("", result, "Short term should be accepted");
        }

        @Test
        @DisplayName("Should accept valid medium term")
        void shouldAcceptValidMediumTerm() {
            // Given
            TermSettings settings = new TermSettings();
            settings.setTerms(Arrays.asList("medium"));

            // When
            String result = BulkServiceValidation.validateTermSettings(settings);

            // Then
            assertEquals("", result, "Medium term should be accepted");
        }

        @Test
        @DisplayName("Should accept valid long term")
        void shouldAcceptValidLongTerm() {
            // Given
            TermSettings settings = new TermSettings();
            settings.setTerms(Arrays.asList("long"));

            // When
            String result = BulkServiceValidation.validateTermSettings(settings);

            // Then
            assertEquals("", result, "Long term should be accepted");
        }

        @Test
        @DisplayName("Should accept multiple valid terms")
        void shouldAcceptMultipleValidTerms() {
            // Given
            TermSettings settings = new TermSettings();
            settings.setTerms(Arrays.asList("short", "medium", "long"));

            // When
            String result = BulkServiceValidation.validateTermSettings(settings);

            // Then
            assertEquals("", result, "Multiple valid terms should be accepted");
        }

        @Test
        @DisplayName("Should accept terms with different case")
        void shouldAcceptTermsWithDifferentCase() {
            // Given
            TermSettings settings = new TermSettings();
            settings.setTerms(Arrays.asList("Short", "MEDIUM", "Long"));

            // When
            String result = BulkServiceValidation.validateTermSettings(settings);

            // Then
            assertEquals("", result, "Terms with different case should be accepted");
        }

        @Test
        @DisplayName("Should reject invalid term name")
        void shouldRejectInvalidTermName() {
            // Given
            TermSettings settings = new TermSettings();
            settings.setTerms(Arrays.asList("invalid-term"));

            // When
            String result = BulkServiceValidation.validateTermSettings(settings);

            // Then
            assertTrue(result.contains("Invalid term name"), 
                "Invalid term name should be rejected");
        }

        @Test
        @DisplayName("Should reject terms list with null element")
        void shouldRejectTermsListWithNullElement() {
            // Given
            TermSettings settings = new TermSettings();
            settings.setTerms(Arrays.asList("short", null));

            // When
            String result = BulkServiceValidation.validateTermSettings(settings);

            // Then
            assertTrue(result.contains("null or empty term name"), 
                "Null term element should be rejected");
        }

        @Test
        @DisplayName("Should reject terms list with empty string element")
        void shouldRejectTermsListWithEmptyStringElement() {
            // Given
            TermSettings settings = new TermSettings();
            settings.setTerms(Arrays.asList("short", ""));

            // When
            String result = BulkServiceValidation.validateTermSettings(settings);

            // Then
            assertTrue(result.contains("null or empty term name"), 
                "Empty string term element should be rejected");
        }

        @Test
        @DisplayName("Should reject terms list with whitespace-only element")
        void shouldRejectTermsListWithWhitespaceOnlyElement() {
            // Given
            TermSettings settings = new TermSettings();
            settings.setTerms(Arrays.asList("short", "   "));

            // When
            String result = BulkServiceValidation.validateTermSettings(settings);

            // Then
            assertTrue(result.contains("null or empty term name"), 
                "Whitespace-only term element should be rejected");
        }

        @Test
        @DisplayName("Should accept terms with leading/trailing whitespace")
        void shouldAcceptTermsWithLeadingTrailingWhitespace() {
            // Given
            TermSettings settings = new TermSettings();
            settings.setTerms(Arrays.asList(" short ", " long "));

            // When
            String result = BulkServiceValidation.validateTermSettings(settings);

            // Then
            assertEquals("", result, "Terms with whitespace should be trimmed and accepted");
        }

        @Test
        @DisplayName("Should accept subset of valid terms")
        void shouldAcceptSubsetOfValidTerms() {
            // Given
            TermSettings settings = new TermSettings();
            settings.setTerms(Arrays.asList("short", "long"));

            // When
            String result = BulkServiceValidation.validateTermSettings(settings);

            // Then
            assertEquals("", result, "Subset of valid terms should be accepted");
        }
    }
}
