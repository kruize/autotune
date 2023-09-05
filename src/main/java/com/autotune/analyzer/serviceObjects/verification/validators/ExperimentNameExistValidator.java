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
import com.autotune.analyzer.serviceObjects.verification.annotators.ExperimentNameExist;
import com.autotune.database.service.ExperimentDBService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExperimentNameExistValidator implements ConstraintValidator<ExperimentNameExist, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentNameExistValidator.class);
    public static ConcurrentHashMap<String, KruizeObject> mainKruizeExperimentMAP = new ConcurrentHashMap<>();

    // You can inject your database access/repository here to fetch the data

    @Override
    public boolean isValid(String experimentName, ConstraintValidatorContext context) {
        boolean success = false;
        String errorMessage = "";
        if (!mainKruizeExperimentMAP.containsKey(experimentName)) {
            // Retrieve the data from the database
            try {
                new ExperimentDBService().loadExperimentFromDBByName(mainKruizeExperimentMAP, experimentName);
            } catch (Exception e) {
                LOGGER.error("Loading saved experiment {} failed: {} ", experimentName, e.getMessage());
                errorMessage = String.format("failed to load from DB due to %s", e.getMessage());
            }
        }

        if (mainKruizeExperimentMAP.containsKey(experimentName)) {
            success = true;
        } else {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(String.format("%s not found %s", experimentName, errorMessage))
                    .addPropertyNode("")
                    .addConstraintViolation();
        }
        return success;
    }
}

