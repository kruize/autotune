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
package com.autotune.analyzer.experiment;

import com.autotune.analyzer.kruizeObject.ExperimentUseCaseType;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.kruizeObject.ModelSettings;
import com.autotune.analyzer.kruizeObject.RecommendationSettings;
import com.autotune.analyzer.kruizeObject.TermSettings;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.utils.KruizeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for term/model cross-validation logic in ExperimentValidation.
 *
 * Focuses exclusively on the recommendation_settings term↔model rules added
 * for stability and flex. Uses Mockito to stub a KruizeObject so only the
 * validation logic under test is exercised.
 *
 * Test naming convention: <scenario>_<expectedOutcome>
 * Structure: Given / When / Then
 */
class ExperimentValidationTermModelTest {

    private ExperimentValidation validation;
    private KruizeObject kruizeObject;
    private ExperimentUseCaseType useCaseType;

    // -------------------------------------------------------------------------
    // Setup helpers
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        // ExperimentValidation constructor only uses the map to pre-populate
        // namespace:name pairs — an empty map is sufficient for these tests.
        validation = new ExperimentValidation(new HashMap<>());

        kruizeObject = mock(KruizeObject.class);
        useCaseType  = mock(ExperimentUseCaseType.class);

        // Stub mandatory fields so they pass the initial mandatory-field checks
        when(kruizeObject.getExperimentName()).thenReturn("test-exp");
        when(kruizeObject.getMode()).thenReturn("monitor");
        when(kruizeObject.getTarget_cluster()).thenReturn("local");
        when(kruizeObject.getExperiment_usecase_type()).thenReturn(useCaseType);

        // Stub experiment type as CONTAINER — avoids the namespace data check
        when(kruizeObject.getExperimentType()).thenReturn(AnalyzerConstants.ExperimentType.CONTAINER);

        // Return a non-null list for getKubernetes_objects so the
        // mandatoryDeploymentSelector loop marks missingDeploySelector=false
        when(kruizeObject.getKubernetes_objects()).thenReturn(Collections.emptyList());

        // Not remote or local monitoring → skips datasource/metadata profile checks
        when(useCaseType.isRemote_monitoring()).thenReturn(false);
        when(useCaseType.isLocal_monitoring()).thenReturn(false);
    }

    /**
     * Builds a RecommendationSettings with the given term list and model list.
     * Pass null for either to leave that setting absent.
     */
    private RecommendationSettings buildSettings(List<String> terms, List<String> models) {
        RecommendationSettings settings = new RecommendationSettings();

        if (terms != null) {
            TermSettings termSettings = new TermSettings();
            termSettings.setTerms(terms);
            settings.setTermSettings(termSettings);
        }

        if (models != null) {
            ModelSettings modelSettings = new ModelSettings();
            modelSettings.setModels(models);
            settings.setModelSettings(modelSettings);
        }

        return settings;
    }

    // -------------------------------------------------------------------------
    // Auto-default: stability-only (no term) → valid
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("stability model with no term → valid (engine auto-defaults to flex)")
    void stabilityModelNoTerm_shouldBeValid() {
        // Given
        RecommendationSettings settings = buildSettings(null, List.of(KruizeConstants.JSONKeys.STABILITY));
        when(kruizeObject.getRecommendation_settings()).thenReturn(settings);

        // When
        ValidationOutputData result = validation.validateMandatoryFields(kruizeObject);

        // Then
        assertTrue(result.isSuccess(),
                "stability-only with no term should be valid — engine defaults to flex");
    }

    // -------------------------------------------------------------------------
    // Auto-default: flex-only (no model) → valid
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("flex term with no model → valid (engine auto-defaults to stability)")
    void flexTermNoModel_shouldBeValid() {
        // Given
        RecommendationSettings settings = buildSettings(List.of(KruizeConstants.JSONKeys.FLEX), null);
        when(kruizeObject.getRecommendation_settings()).thenReturn(settings);

        // When
        ValidationOutputData result = validation.validateMandatoryFields(kruizeObject);

        // Then
        assertTrue(result.isSuccess(),
                "flex-only with no model should be valid — engine defaults to stability");
    }

    // -------------------------------------------------------------------------
    // Explicit pairing: stability + flex → valid
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("stability model + flex term explicitly specified → valid")
    void stabilityModelWithFlexTerm_shouldBeValid() {
        // Given
        RecommendationSettings settings = buildSettings(
                List.of(KruizeConstants.JSONKeys.FLEX),
                List.of(KruizeConstants.JSONKeys.STABILITY));
        when(kruizeObject.getRecommendation_settings()).thenReturn(settings);

        // When
        ValidationOutputData result = validation.validateMandatoryFields(kruizeObject);

        // Then
        assertTrue(result.isSuccess(),
                "stability + flex explicit pairing should always be valid");
    }

    // -------------------------------------------------------------------------
    // Mixed stability + cost/perf: no term → valid
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("stability + cost models with no term → valid (mixed-stability case)")
    void stabilityAndCostNoTerm_shouldBeValid() {
        // Given
        RecommendationSettings settings = buildSettings(
                null,
                List.of(KruizeConstants.JSONKeys.STABILITY, KruizeConstants.JSONKeys.COST));
        when(kruizeObject.getRecommendation_settings()).thenReturn(settings);

        // When
        ValidationOutputData result = validation.validateMandatoryFields(kruizeObject);

        // Then
        assertTrue(result.isSuccess(),
                "stability + cost with no term should be valid — engine assigns terms per model");
    }

    @Test
    @DisplayName("stability + performance models with no term → valid (mixed-stability case)")
    void stabilityAndPerformanceNoTerm_shouldBeValid() {
        // Given
        RecommendationSettings settings = buildSettings(
                null,
                List.of(KruizeConstants.JSONKeys.STABILITY, KruizeConstants.JSONKeys.PERFORMANCE));
        when(kruizeObject.getRecommendation_settings()).thenReturn(settings);

        // When
        ValidationOutputData result = validation.validateMandatoryFields(kruizeObject);

        // Then
        assertTrue(result.isSuccess(),
                "stability + performance with no term should be valid");
    }

    @Test
    @DisplayName("stability + cost + performance models with no term → valid")
    void allThreeModelsNoTerm_shouldBeValid() {
        // Given
        RecommendationSettings settings = buildSettings(
                null,
                List.of(KruizeConstants.JSONKeys.STABILITY,
                        KruizeConstants.JSONKeys.COST,
                        KruizeConstants.JSONKeys.PERFORMANCE));
        when(kruizeObject.getRecommendation_settings()).thenReturn(settings);

        // When
        ValidationOutputData result = validation.validateMandatoryFields(kruizeObject);

        // Then
        assertTrue(result.isSuccess(),
                "all three models with no term should be valid");
    }

    // -------------------------------------------------------------------------
    // Error: stability + other models + any term → rejected
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("stability + cost + flex term → error (mixed models must not have a term)")
    void stabilityAndCostWithFlexTerm_shouldError() {
        // Given
        RecommendationSettings settings = buildSettings(
                List.of(KruizeConstants.JSONKeys.FLEX),
                List.of(KruizeConstants.JSONKeys.STABILITY, KruizeConstants.JSONKeys.COST));
        when(kruizeObject.getRecommendation_settings()).thenReturn(settings);

        // When
        ValidationOutputData result = validation.validateMandatoryFields(kruizeObject);

        // Then
        assertFalse(result.isSuccess());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getErrorCode());
        assertEquals(AnalyzerErrorConstants.APIErrors.CreateExperimentAPI.STABILITY_WITH_OTHER_MODELS_NO_TERM_ALLOWED,
                result.getMessage());
    }

    @Test
    @DisplayName("stability + cost + short term → error")
    void stabilityAndCostWithShortTerm_shouldError() {
        // Given
        RecommendationSettings settings = buildSettings(
                List.of(KruizeConstants.JSONKeys.SHORT),
                List.of(KruizeConstants.JSONKeys.STABILITY, KruizeConstants.JSONKeys.COST));
        when(kruizeObject.getRecommendation_settings()).thenReturn(settings);

        // When
        ValidationOutputData result = validation.validateMandatoryFields(kruizeObject);

        // Then
        assertFalse(result.isSuccess());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getErrorCode());
        assertEquals(AnalyzerErrorConstants.APIErrors.CreateExperimentAPI.STABILITY_WITH_OTHER_MODELS_NO_TERM_ALLOWED,
                result.getMessage());
    }

    // -------------------------------------------------------------------------
    // Error: flex + other terms + a model → rejected (existing flex rule)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("flex + short terms + stability model → error (flex+other-terms must not have a model)")
    void flexWithOtherTermsAndModel_shouldError() {
        // Given
        RecommendationSettings settings = buildSettings(
                List.of(KruizeConstants.JSONKeys.FLEX, KruizeConstants.JSONKeys.SHORT),
                List.of(KruizeConstants.JSONKeys.STABILITY));
        when(kruizeObject.getRecommendation_settings()).thenReturn(settings);

        // When
        ValidationOutputData result = validation.validateMandatoryFields(kruizeObject);

        // Then
        assertFalse(result.isSuccess());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getErrorCode());
        assertEquals(AnalyzerErrorConstants.APIErrors.CreateExperimentAPI.FLEX_WITH_OTHER_TERMS_NO_MODEL_ALLOWED,
                result.getMessage());
    }

    // -------------------------------------------------------------------------
    // Valid: flex + other terms, no model → valid (existing mixed-flex rule)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("flex + short terms with no model → valid (mixed-flex case)")
    void flexWithOtherTermsNoModel_shouldBeValid() {
        // Given
        RecommendationSettings settings = buildSettings(
                List.of(KruizeConstants.JSONKeys.FLEX, KruizeConstants.JSONKeys.SHORT),
                null);
        when(kruizeObject.getRecommendation_settings()).thenReturn(settings);

        // When
        ValidationOutputData result = validation.validateMandatoryFields(kruizeObject);

        // Then
        assertTrue(result.isSuccess(),
                "flex + other terms with no model should be valid — engine assigns models per term");
    }

    // -------------------------------------------------------------------------
    // Error: invalid term name
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("unrecognised term name → error")
    void invalidTermName_shouldError() {
        // Given
        RecommendationSettings settings = buildSettings(List.of("quarterly"), null);
        when(kruizeObject.getRecommendation_settings()).thenReturn(settings);

        // When
        ValidationOutputData result = validation.validateMandatoryFields(kruizeObject);

        // Then
        assertFalse(result.isSuccess());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getErrorCode());
        assertEquals(AnalyzerErrorConstants.APIErrors.CreateExperimentAPI.INVALID_TERM_NAME,
                result.getMessage());
    }

    // -------------------------------------------------------------------------
    // Error: invalid model name
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("unrecognised model name → error")
    void invalidModelName_shouldError() {
        // Given
        RecommendationSettings settings = buildSettings(null, List.of("turbo"));
        when(kruizeObject.getRecommendation_settings()).thenReturn(settings);

        // When
        ValidationOutputData result = validation.validateMandatoryFields(kruizeObject);

        // Then
        assertFalse(result.isSuccess());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getErrorCode());
        assertEquals(AnalyzerErrorConstants.APIErrors.CreateExperimentAPI.INVALID_MODEL_NAME,
                result.getMessage());
    }

    // -------------------------------------------------------------------------
    // Error: stability + performance + any term → rejected
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("stability + performance + medium term → error")
    void stabilityAndPerformanceWithTerm_shouldError() {
        // Given
        RecommendationSettings settings = buildSettings(
                List.of(KruizeConstants.JSONKeys.MEDIUM),
                List.of(KruizeConstants.JSONKeys.STABILITY, KruizeConstants.JSONKeys.PERFORMANCE));
        when(kruizeObject.getRecommendation_settings()).thenReturn(settings);

        // When
        ValidationOutputData result = validation.validateMandatoryFields(kruizeObject);

        // Then
        assertFalse(result.isSuccess());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getErrorCode());
        assertEquals(AnalyzerErrorConstants.APIErrors.CreateExperimentAPI.STABILITY_WITH_OTHER_MODELS_NO_TERM_ALLOWED,
                result.getMessage());
    }

    // -------------------------------------------------------------------------
    // Positive: flex + multiple other terms, no model → valid
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("flex + short + medium terms with no model → valid")
    void flexWithMultipleOtherTermsNoModel_shouldBeValid() {
        // Given
        RecommendationSettings settings = buildSettings(
                List.of(KruizeConstants.JSONKeys.FLEX,
                        KruizeConstants.JSONKeys.SHORT,
                        KruizeConstants.JSONKeys.MEDIUM),
                null);
        when(kruizeObject.getRecommendation_settings()).thenReturn(settings);

        // When
        ValidationOutputData result = validation.validateMandatoryFields(kruizeObject);

        // Then
        assertTrue(result.isSuccess(),
                "flex + short + medium with no model should be valid — engine assigns models per term");
    }

    // -------------------------------------------------------------------------
    // Error: blank term value → rejected
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("blank term value → error")
    void blankTermValue_shouldError() {
        // Given
        RecommendationSettings settings = buildSettings(List.of(""), null);
        when(kruizeObject.getRecommendation_settings()).thenReturn(settings);

        // When
        ValidationOutputData result = validation.validateMandatoryFields(kruizeObject);

        // Then
        assertFalse(result.isSuccess());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getErrorCode());
        assertEquals(AnalyzerErrorConstants.APIErrors.CreateExperimentAPI.EMPTY_NOT_ALLOWED,
                result.getMessage());
    }

    // -------------------------------------------------------------------------
    // Error: blank model value → rejected
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("blank model value → error")
    void blankModelValue_shouldError() {
        // Given
        RecommendationSettings settings = buildSettings(null, List.of(""));
        when(kruizeObject.getRecommendation_settings()).thenReturn(settings);

        // When
        ValidationOutputData result = validation.validateMandatoryFields(kruizeObject);

        // Then
        assertFalse(result.isSuccess());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getErrorCode());
        assertEquals(AnalyzerErrorConstants.APIErrors.CreateExperimentAPI.EMPTY_NOT_ALLOWED,
                result.getMessage());
    }
}
