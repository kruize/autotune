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
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.UpdateResultsAPIObject;
import com.autotune.analyzer.serviceObjects.verification.annotators.PerformanceProfileCheck;
import com.autotune.analyzer.services.UpdateResults;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.database.service.ExperimentDBService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PerformanceProfileValidator implements ConstraintValidator<PerformanceProfileCheck, UpdateResultsAPIObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceProfileValidator.class);

    @Override
    public void initialize(PerformanceProfileCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(UpdateResultsAPIObject updateResultsAPIObject, ConstraintValidatorContext context) {
        boolean success = false;
        /*
         Fetch the performance profile from the Map corresponding to the name in the kruize object,
         and then validate the Performance Profile data
        */
        try {
            KruizeObject kruizeObject = ExperimentNameExistValidator.mainKruizeExperimentMAP.get(updateResultsAPIObject.getExperimentName());
            if(UpdateResults.performanceProfilesMap.isEmpty() || !UpdateResults.performanceProfilesMap.containsKey(kruizeObject.getPerformanceProfile())) {
                ConcurrentHashMap<String, PerformanceProfile> tempPerformanceProfilesMap = new ConcurrentHashMap<>();
                new ExperimentDBService().loadAllPerformanceProfiles(tempPerformanceProfilesMap);
                UpdateResults.performanceProfilesMap.putAll(tempPerformanceProfilesMap);
            }
            PerformanceProfile performanceProfile = UpdateResults.performanceProfilesMap.get(kruizeObject.getPerformanceProfile());
            ExperimentResultData resultData = Converters.KruizeObjectConverters.convertUpdateResultsAPIObjToExperimentResultData(updateResultsAPIObject);
            // validate the 'resultdata' with the performance profile
            String errorMsg = PerformanceProfileUtil.validateResults(performanceProfile, resultData);
            if (null == errorMsg || errorMsg.isEmpty()) {
                success = true;
            } else {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(errorMsg)
                        .addPropertyNode("Performance profile")
                        .addConstraintViolation();
            }
        } catch (Exception e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(e.getMessage())
                    .addPropertyNode("Performance profile")
                    .addConstraintViolation();
        }
        return success;
    }
}
