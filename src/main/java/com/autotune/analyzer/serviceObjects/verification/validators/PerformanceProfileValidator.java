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
import com.autotune.analyzer.services.UpdateResults;
import com.autotune.database.service.ExperimentDBService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.autotune.analyzer.utils.AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_PERF_PROFILE;

public class PerformanceProfileValidator implements ConstraintValidator<PerformanceProfileCheck, UpdateResultsAPIObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceProfileValidator.class);

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
            KruizeObject kruizeObject = updateResultsAPIObject.getKruizeObject();
            if (UpdateResults.performanceProfilesMap.isEmpty() || !UpdateResults.performanceProfilesMap.containsKey(kruizeObject.getPerformanceProfile())) {
                ConcurrentHashMap<String, PerformanceProfile> tempPerformanceProfilesMap = new ConcurrentHashMap<>();
                new ExperimentDBService().loadAllPerformanceProfiles(tempPerformanceProfilesMap);
                UpdateResults.performanceProfilesMap.putAll(tempPerformanceProfilesMap);
            }
            PerformanceProfile performanceProfile = null;
            if (UpdateResults.performanceProfilesMap.containsKey(kruizeObject.getPerformanceProfile())) {
                performanceProfile = UpdateResults.performanceProfilesMap.get(kruizeObject.getPerformanceProfile());
            } else {
                throw new Exception(String.format("%s%s", MISSING_PERF_PROFILE, kruizeObject.getPerformanceProfile()));
            }

            // validate the results value present in the updateResultsAPIObject
            List<String> errorMsg = PerformanceProfileUtil.validateResults(performanceProfile, updateResultsAPIObject);
            if (errorMsg.isEmpty()) {
                success = true;
            } else {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(errorMsg.toString())
                        .addPropertyNode("Performance profile")
                        .addConstraintViolation();
            }

        } catch (Exception e) {
            LOGGER.error(e.toString());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String stackTrace = sw.toString();
            LOGGER.debug(stackTrace);
            if (null != e.getMessage()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(e.getMessage())
                        .addPropertyNode("")
                        .addConstraintViolation();
            } else {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Null value found")
                        .addPropertyNode("")
                        .addConstraintViolation();
            }

        }
        LOGGER.debug("PerformanceProfileValidator success : {}", success);
        return success;
    }
}
