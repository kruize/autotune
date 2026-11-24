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
package com.autotune.analyzer.experiment;

import com.autotune.analyzer.kruizeObject.*;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the model and resource_scope_settings checks of ExperimentValidation.validateMandatoryFields.
 */
class ExperimentValidationScopeTest {

    private static final String CPU_ERROR_VALID = "Valid: under_provisioned, over_provisioned, optimised, idle, all";

    private static KruizeObject localContainerExperiment(List<String> models, ResourceScopeSettings scopeSettings) throws Exception {
        KruizeObject kruizeObject = new KruizeObject();
        kruizeObject.setExperimentName("scope-test");
        kruizeObject.setMode(AnalyzerConstants.MONITOR);
        kruizeObject.setTarget_cluster(AnalyzerConstants.LOCAL);
        kruizeObject.setExperimentType(AnalyzerConstants.ExperimentType.CONTAINER);
        kruizeObject.setDataSource("prometheus-1");
        kruizeObject.setKubernetes_objects(new ArrayList<>());

        RecommendationSettings settings = new RecommendationSettings();
        if (models != null) {
            ModelSettings modelSettings = new ModelSettings();
            modelSettings.setModels(models);
            settings.setModelSettings(modelSettings);
        }
        settings.setResourceScopeSettings(scopeSettings);
        kruizeObject.setRecommendation_settings(settings);

        // The use case type is derived from mode and target cluster, so set it last
        kruizeObject.setExperiment_usecase_type(new ExperimentUseCaseType(kruizeObject));
        return kruizeObject;
    }

    private static ValidationOutputData validate(KruizeObject kruizeObject) {
        return new ExperimentValidation(new HashMap<>()).validateMandatoryFields(kruizeObject);
    }

    private static ResourceScopeSettings scope(List<String> cpu, List<String> memory) {
        return new ResourceScopeSettings(cpu, memory);
    }

    private static List<ResourceScopeSettings> validScopes() {
        return List.of(
                scope(List.of("under_provisioned"), null),
                scope(null, List.of("over_provisioned")),
                scope(List.of("under_provisioned", "idle"), List.of("over_provisioned")),
                scope(List.of("all"), List.of("all")),
                scope(List.of("all", "under_provisioned"), null),
                scope(List.of("idle"), null),
                scope(List.of("Under_Provisioned"), List.of("OPTIMIZED")),
                scope(List.of("ALL"), List.of("Optimized"))
        );
    }

    @Test
    @DisplayName("Should accept valid resource_scope_settings")
    void shouldAcceptValidScopes() throws Exception {
        for (ResourceScopeSettings scopeSettings : validScopes()) {
            ValidationOutputData result = validate(localContainerExperiment(null, scopeSettings));

            assertTrue(result.isSuccess(), scopeSettings + ": " + result.getMessage());
        }
    }

    @Test
    @DisplayName("Should accept an experiment without resource_scope_settings")
    void shouldAcceptAbsentScopeSettings() throws Exception {
        assertTrue(validate(localContainerExperiment(null, null)).isSuccess());
    }

    private static void assertRejected(ValidationOutputData result, String expectedMessage) {
        assertFalse(result.isSuccess());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getErrorCode());
        assertEquals(expectedMessage, result.getMessage());
    }

    @Test
    @DisplayName("Should reject an unknown model")
    void shouldRejectUnknownModel() throws Exception {
        assertRejected(validate(localContainerExperiment(List.of("bogus"), null)),
                AnalyzerErrorConstants.APIErrors.CreateExperimentAPI.INVALID_MODEL_NAME);
    }

    @Test
    @DisplayName("Should reject an empty resource_scope_settings object")
    void shouldRejectEmptyObject() throws Exception {
        assertRejected(validate(localContainerExperiment(null, scope(null, null))),
                AnalyzerErrorConstants.APIErrors.CreateExperimentAPI.RESOURCE_SCOPE_SETTINGS_EMPTY);
    }

    @Test
    @DisplayName("Should reject an empty cpu list")
    void shouldRejectEmptyCpuList() throws Exception {
        assertRejected(validate(localContainerExperiment(null, scope(List.of(), null))),
                "resource_scope_settings.cpu must not be an empty list.");
    }

    @Test
    @DisplayName("Should reject an empty memory list")
    void shouldRejectEmptyMemoryList() throws Exception {
        assertRejected(validate(localContainerExperiment(null, scope(null, List.of()))),
                "resource_scope_settings.memory must not be an empty list.");
    }

    @Test
    @DisplayName("Should reject an unknown cpu scope")
    void shouldRejectUnknownCpuScope() throws Exception {
        assertRejected(validate(localContainerExperiment(null, scope(List.of("bogus"), null))),
                "Unknown cpu scope: 'bogus'. " + CPU_ERROR_VALID);
    }

    @Test
    @DisplayName("Should reject an unknown memory scope")
    void shouldRejectUnknownMemoryScope() throws Exception {
        assertRejected(validate(localContainerExperiment(null, scope(null, List.of("bogus")))),
                "Unknown memory scope: 'bogus'. Valid: under_provisioned, over_provisioned, optimised, all");
    }

    @Test
    @DisplayName("Should reject idle for memory")
    void shouldRejectIdleForMemory() throws Exception {
        assertRejected(validate(localContainerExperiment(null, scope(List.of("under_provisioned"), List.of("idle")))),
                AnalyzerErrorConstants.APIErrors.CreateExperimentAPI.IDLE_NOT_VALID_FOR_MEMORY);
    }

    @Test
    @DisplayName("Should reject a blank scope and a null scope entry")
    void shouldRejectBlankAndNullEntries() throws Exception {
        assertRejected(validate(localContainerExperiment(null, scope(List.of(" "), null))),
                "Unknown cpu scope: ' '. " + CPU_ERROR_VALID);

        List<String> withNull = new ArrayList<>();
        withNull.add(null);
        assertRejected(validate(localContainerExperiment(null, scope(withNull, null))),
                "Unknown cpu scope: 'null'. " + CPU_ERROR_VALID);
    }
}
