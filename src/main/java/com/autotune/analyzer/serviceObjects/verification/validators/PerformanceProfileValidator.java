/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.serviceObjects.verification.validators;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.performanceProfiles.utils.PerformanceProfileUtil;
import com.autotune.analyzer.serviceObjects.UpdateResultsAPIObject;
import com.autotune.analyzer.serviceObjects.verification.annotators.PerformanceProfileCheck;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.cache.PerformanceProfileCache;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.autotune.analyzer.utils.AnalyzerErrorConstants.AutotuneObjectErrors.INVALID_METRICS_ERROR_PREFIX;
import static com.autotune.analyzer.utils.AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_PERF_PROFILE;

public class PerformanceProfileValidator implements ConstraintValidator<PerformanceProfileCheck, UpdateResultsAPIObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceProfileValidator.class);
    private final ExperimentDBService experimentDBService = new ExperimentDBService();

    @Override
    public void initialize(PerformanceProfileCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(UpdateResultsAPIObject updateResultsAPIObject, ConstraintValidatorContext context) {
        LOGGER.debug("PerformanceProfileValidator expName - {} - {} - {}", updateResultsAPIObject.getExperimentName(), updateResultsAPIObject.getStartTimestamp(), updateResultsAPIObject.getEndTimestamp());
        boolean success = false;
        /*
         Fetch the performance profile from the Map corresponding to the name in the kruize object,
         and then validate the Performance Profile data
        */
        try {
            Map<String, PerformanceProfile> performanceProfilesMap = new HashMap<>();
            KruizeObject kruizeObject = updateResultsAPIObject.getKruizeObject();
            String performanceProfileName = kruizeObject.getPerformanceProfile();
            try {
                experimentDBService.loadPerformanceProfileFromDBByName(performanceProfilesMap, performanceProfileName);
            } catch (Exception e) {
                LOGGER.error("Loading saved performance profiles failed: {}", e.getMessage());
                throw e;
            }

            PerformanceProfile performanceProfile = performanceProfilesMap.get(performanceProfileName);
            if (performanceProfile == null) {
                throw new Exception(String.format("%s%s", MISSING_PERF_PROFILE, performanceProfileName));
            }

            // validate the results value present in the updateResultsAPIObject
            List<String> errorMsg = PerformanceProfileUtil.validateResults(performanceProfile, updateResultsAPIObject);
            if (errorMsg.isEmpty()) {
                success = true;
            } else {
                // Check if the error is related to invalid metrics using the constant
                boolean hasInvalidMetricsError = errorMsg.stream()
                        .anyMatch(msg -> msg.startsWith(INVALID_METRICS_ERROR_PREFIX));
                
                if (hasInvalidMetricsError) {
                    // Clear the cache and reload from database
                    LOGGER.debug("Invalid metrics error detected. Clearing cache and reloading from DB for profile: {}", performanceProfileName);
                    PerformanceProfileCache.remove(performanceProfileName);
                    try {
                        experimentDBService.loadPerformanceProfileFromDBByName(performanceProfilesMap, performanceProfileName);
                    } catch (Exception e) {
                        LOGGER.error("Loading saved performance profiles failed during refresh", e);
                        throw e;
                    }
                    
                    performanceProfile = performanceProfilesMap.get(performanceProfileName);
                    if (performanceProfile == null) {
                        throw new Exception(String.format("%s%s", MISSING_PERF_PROFILE, performanceProfileName));
                    }
                    
                    // validate the result value present in the updateResultsAPIObject
                    errorMsg = PerformanceProfileUtil.validateResults(performanceProfile, updateResultsAPIObject);
                    if (errorMsg.isEmpty()) {
                        success = true;
                    } else {
                        addConstraintViolation(context, errorMsg.toString(), "Performance profile");
                    }
                } else {
                    // For other validation errors, fail immediately without cache pruning
                    addConstraintViolation(context, errorMsg.toString(), "Performance profile");
                }
            }

        } catch (Exception e) {
            LOGGER.error(e.toString());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String stackTrace = sw.toString();
            LOGGER.debug(stackTrace);
            if (null != e.getMessage()) {
                addConstraintViolation(context, e.getMessage(), "");
            } else {
                addConstraintViolation(context, "Null value found", "");
            }

        }
        LOGGER.debug("PerformanceProfileValidator success : {}", success);
        return success;
    }

    /**
     * Helper method to add a constraint violation with a custom message.
     * Centralizes the logic for disabling default violations and building custom ones.
     *
     * @param context the constraint validator context
     * @param message the error message to include in the violation
     * @param propertyNode the property node name for the violation
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message, String propertyNode) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(propertyNode)
                .addConstraintViolation();
    }
}
