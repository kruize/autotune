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
import com.autotune.analyzer.serviceObjects.UpdateResultsAPIObject;
import com.autotune.analyzer.serviceObjects.verification.annotators.TimeDifferenceCheck;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.utils.KruizeConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TimeDifferenceValidator implements ConstraintValidator<TimeDifferenceCheck, UpdateResultsAPIObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeDifferenceValidator.class);

    @Override
    public void initialize(TimeDifferenceCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(UpdateResultsAPIObject updateResultsAPIObject, ConstraintValidatorContext context) {
        LOGGER.debug("TimeDifferenceValidator expName - {} - {} - {}", updateResultsAPIObject.getExperimentName(), updateResultsAPIObject.getStartTimestamp(), updateResultsAPIObject.getEndTimestamp());
        boolean success = false;
        try {
            KruizeObject kruizeObject = updateResultsAPIObject.getKruizeObject();
            IntervalResults intervalResults = new IntervalResults(updateResultsAPIObject.getStartTimestamp(), updateResultsAPIObject.getEndTimestamp());
            Double durationInSeconds = intervalResults.getDuration_in_seconds();
            String measurementDurationInMins = kruizeObject.getTrial_settings().getMeasurement_durationMinutes();
            Double parsedMeasurementDuration = Double.parseDouble(measurementDurationInMins.substring(0, measurementDurationInMins.length() - 3));
            // Calculate the lower and upper bounds for the acceptable range i.e. +-5 seconds
            double lowerRange = Math.abs((parsedMeasurementDuration * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE) - (KruizeConstants.TimeConv.MEASUREMENT_DURATION_THRESHOLD_SECONDS));
            double upperRange = (parsedMeasurementDuration * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE) + (KruizeConstants.TimeConv.MEASUREMENT_DURATION_THRESHOLD_SECONDS);
            if ((durationInSeconds >= lowerRange && durationInSeconds <= upperRange))
                success = true;
        } catch (Exception e) {
            if (null != e.getMessage()) {
                LOGGER.error(e.getMessage());
                e.printStackTrace();
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(e.getMessage())
                        .addPropertyNode("Time : ")
                        .addConstraintViolation();
            }else{
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Null values not allowed")
                        .addPropertyNode("Time : ")
                        .addConstraintViolation();
            }
        }
        LOGGER.debug("TimeDifferenceValidator success : {}", success);
        return success;
    }
}
