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

package com.autotune.analyzer.kruizeLayer;

import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Validation helper for KruizeLayer objects
 */
public class LayerValidation {
    private static final Logger LOGGER = LoggerFactory.getLogger(LayerValidation.class);

    /**
     * Validates a KruizeLayer object
     *
     * @param layer The layer to validate
     * @return ValidationOutputData with success flag and error messages
     */
    public ValidationOutputData validate(KruizeLayer layer) {
        List<String> errors = new ArrayList<>();

        // 1. Validate mandatory fields
        if (layer.getMetadata() == null || layer.getMetadata().getName() == null ||
            layer.getMetadata().getName().trim().isEmpty()) {
            errors.add(AnalyzerErrorConstants.APIErrors.CreateLayerAPI.LAYER_METADATA_NAME_NULL);
        }

        if (layer.getLayerName() == null || layer.getLayerName().trim().isEmpty()) {
            errors.add(AnalyzerErrorConstants.APIErrors.CreateLayerAPI.LAYER_NAME_NULL);
        }

        if (layer.getLayerPresence() == null) {
            errors.add(AnalyzerErrorConstants.APIErrors.CreateLayerAPI.LAYER_PRESENCE_NULL);
        }

        if (layer.getTunables() == null || layer.getTunables().isEmpty()) {
            errors.add(AnalyzerErrorConstants.APIErrors.CreateLayerAPI.LAYER_TUNABLES_NULL_OR_EMPTY);
        }

        // 2. Validate layer level
        if (layer.getLayerLevel() < 0) {
            errors.add(String.format(AnalyzerErrorConstants.APIErrors.CreateLayerAPI.LAYER_LEVEL_NEGATIVE, layer.getLayerLevel()));
        }

        // 3. Validate layer presence mutual exclusivity
        if (layer.getLayerPresence() != null) {
            ValidationOutputData presenceValidation = validateLayerPresence(layer.getLayerPresence());
            if (!presenceValidation.isSuccess()) {
                errors.add(presenceValidation.getMessage());
            }
        }

        // 4. Validate tunables
        if (layer.getTunables() != null && !layer.getTunables().isEmpty()) {
            ValidationOutputData tunableValidation = validateTunables(layer.getTunables());
            if (!tunableValidation.isSuccess()) {
                errors.add(tunableValidation.getMessage());
            }
        }

        // Return result
        if (!errors.isEmpty()) {
            String errorMsg = String.join("; ", errors);
            LOGGER.error("Layer validation failed: {}", errorMsg);
            return new ValidationOutputData(false, errorMsg, HttpServletResponse.SC_BAD_REQUEST);
        }

        return new ValidationOutputData(true, null, null);
    }

    /**
     * Validates LayerPresence mutual exclusivity
     * Business rule: Must have EXACTLY ONE of: presence='always', queries, or label
     */
    private ValidationOutputData validateLayerPresence(LayerPresence presence) {
        int presenceTypeCount = 0;

        if (presence.getPresence() != null && !presence.getPresence().trim().isEmpty()) {
            presenceTypeCount++;
        }
        if (presence.getQueries() != null && !presence.getQueries().isEmpty()) {
            presenceTypeCount++;
        }
        if (presence.getLabel() != null && !presence.getLabel().isEmpty()) {
            presenceTypeCount++;
        }

        if (presenceTypeCount == 0) {
            return new ValidationOutputData(false,
                AnalyzerErrorConstants.APIErrors.CreateLayerAPI.LAYER_PRESENCE_MISSING,
                HttpServletResponse.SC_BAD_REQUEST);
        }

        if (presenceTypeCount > 1) {
            return new ValidationOutputData(false,
                AnalyzerErrorConstants.APIErrors.CreateLayerAPI.LAYER_PRESENCE_MULTIPLE_TYPES,
                HttpServletResponse.SC_BAD_REQUEST);
        }

        return new ValidationOutputData(true, null, null);
    }

    /**
     * Validates tunables collection
     * - Checks for duplicate tunable names
     * - Validates each tunable using Tunable.validate()
     */
    private ValidationOutputData validateTunables(ArrayList<Tunable> tunables) {
        List<String> errors = new ArrayList<>();

        // Check for duplicate tunable names
        Set<String> tunableNames = new HashSet<>();
        List<String> duplicates = new ArrayList<>();

        for (Tunable tunable : tunables) {
            if (tunable == null) {
                continue;
            }

            // Check for duplicate names
            if (tunable.getName() != null) {
                if (tunableNames.contains(tunable.getName())) {
                    duplicates.add(tunable.getName());
                } else {
                    tunableNames.add(tunable.getName());
                }
            }

            // Validate individual tunable using existing Tunable.validate() method
            try {
                tunable.validate();
            } catch (Exception e) {
                errors.add(e.getMessage());
            }
        }

        if (!duplicates.isEmpty()) {
            errors.add(String.format(AnalyzerErrorConstants.APIErrors.CreateLayerAPI.LAYER_DUPLICATE_TUNABLE_NAMES,
                    String.join(", ", duplicates)));
        }

        if (!errors.isEmpty()) {
            return new ValidationOutputData(false, String.join("; ", errors), HttpServletResponse.SC_BAD_REQUEST);
        }

        return new ValidationOutputData(true, null, null);
    }
}
