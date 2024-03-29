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

import com.autotune.analyzer.serviceObjects.UpdateResultsAPIObject;
import com.autotune.analyzer.serviceObjects.verification.annotators.CompareDate;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CompareDateValidator implements ConstraintValidator<CompareDate, UpdateResultsAPIObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompareDateValidator.class);

    @Override
    public void initialize(CompareDate constraintAnnotation) {
    }

    @Override
    public boolean isValid(UpdateResultsAPIObject updateResultsAPIObject, ConstraintValidatorContext context) {
        LOGGER.debug("CompareDateValidator expName - {} - {} - {}", updateResultsAPIObject.getExperimentName(), updateResultsAPIObject.getStartTimestamp(), updateResultsAPIObject.getEndTimestamp());
        boolean success = false;
        try {
            if (null != updateResultsAPIObject.getStartTimestamp() && null != updateResultsAPIObject.getEndTimestamp()) {
                int comparisonResult = updateResultsAPIObject.getStartTimestamp().compareTo(updateResultsAPIObject.getEndTimestamp());
                if (comparisonResult < 0) {
                    success = true;
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        LOGGER.debug("CompareDateValidator success : {}", success);
        return success;
    }
}
