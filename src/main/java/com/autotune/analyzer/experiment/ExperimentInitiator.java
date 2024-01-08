/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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

import com.autotune.analyzer.exceptions.KruizeResponse;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.performanceProfiles.PerformanceProfileInterface.PerfProfileInterface;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.UpdateResultsAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.database.service.ExperimentDBService;
import com.google.gson.annotations.SerializedName;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.autotune.analyzer.utils.AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_EXPERIMENT;
import static com.autotune.analyzer.utils.AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_EXPERIMENT_NAME;

/**
 * Initiates new experiment data validations and push into queue for worker to
 * execute task.
 */
public class ExperimentInitiator {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentInitiator.class);
    List<UpdateResultsAPIObject> successUpdateResultsAPIObjects = new ArrayList<>();
    List<UpdateResultsAPIObject> failedUpdateResultsAPIObjects = new ArrayList<>();
    private ValidationOutputData validationOutputData;

    public static List<KruizeResponse> getErrorMap(List<String> errorMessages) {
        List<KruizeResponse> responses;
        if (null != errorMessages) {
            HashMap<Integer, String> groupSimilarMap = new HashMap<Integer, String>();
            errorMessages.forEach(
                    (errorText) -> {
                        if (AnalyzerErrorConstants.APIErrors.updateResultsAPI.ERROR_CODE_MAP.containsKey(errorText)) {
                            if (groupSimilarMap.containsKey(AnalyzerErrorConstants.APIErrors.updateResultsAPI.ERROR_CODE_MAP.get(errorText))) {
                                String errorMsg = groupSimilarMap.get(AnalyzerErrorConstants.APIErrors.updateResultsAPI.ERROR_CODE_MAP.get(errorText));
                                errorMsg = errorMsg + " , " + errorText;
                                groupSimilarMap.put(AnalyzerErrorConstants.APIErrors.updateResultsAPI.ERROR_CODE_MAP.get(errorText), errorMsg);
                            } else {
                                groupSimilarMap.put(AnalyzerErrorConstants.APIErrors.updateResultsAPI.ERROR_CODE_MAP.get(errorText), errorText);
                            }
                        } else {
                            if (groupSimilarMap.containsKey(HttpServletResponse.SC_BAD_REQUEST)) {
                                String errorMsg = groupSimilarMap.get(HttpServletResponse.SC_BAD_REQUEST);
                                errorMsg = errorMsg + " , " + errorText;
                                groupSimilarMap.put(HttpServletResponse.SC_BAD_REQUEST, errorMsg);
                            } else {
                                groupSimilarMap.put(HttpServletResponse.SC_BAD_REQUEST, errorText);
                            }
                        }
                    }
            );
            responses = new ArrayList<>();
            groupSimilarMap.forEach((httpCode, errorText) ->
                    {
                        responses.add(
                                new KruizeResponse(errorText, httpCode, "", "ERROR", null)
                        );
                    }
            );
        } else {
            responses = null;
        }
        return responses;
    }

    /**
     * Initiate Experiment validation
     *
     * @param mainKruizeExperimentMap
     * @param kruizeExpList
     */
    public void validateAndAddNewExperiments(
            Map<String, KruizeObject> mainKruizeExperimentMap,
            List<KruizeObject> kruizeExpList
    ) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        try {
            ExperimentValidation validationObject = new ExperimentValidation(mainKruizeExperimentMap);
            validationObject.validate(kruizeExpList);
            if (validationObject.isSuccess()) {
                ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
                experimentInterface.addExperimentToLocalStorage(mainKruizeExperimentMap, kruizeExpList);
                validationOutputData.setSuccess(true);
            } else {
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage("Validation failed: " + validationObject.getErrorMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Validate and push experiment falied: " + e.getMessage());
            validationOutputData.setSuccess(false);
            validationOutputData.setMessage("Validation failed: " + e.getMessage());
        }
    }

    // Generate recommendations and add it to the kruize object
    public void generateAndAddRecommendations(KruizeObject kruizeObject, List<ExperimentResultData> experimentResultDataList, Timestamp interval_start_time, Timestamp interval_end_time) throws Exception {
        if (AnalyzerConstants.PerformanceProfileConstants.perfProfileInstances.containsKey(kruizeObject.getPerformanceProfile())) {
            PerfProfileInterface perfProfileInstance =
                    (PerfProfileInterface) AnalyzerConstants.PerformanceProfileConstants
                            .perfProfileInstances.get(kruizeObject.getPerformanceProfile())
                            .getDeclaredConstructor().newInstance();
            perfProfileInstance.generateRecommendation(kruizeObject, experimentResultDataList, interval_start_time, interval_end_time);
        } else {
            throw new Exception("No Recommendation Engine mapping found for performance profile: " +
                    kruizeObject.getPerformanceProfile() + ". Cannot process recommendations for the experiment");
        }
    }

    public void validateAndAddExperimentResults(List<UpdateResultsAPIObject> updateResultsAPIObjects) {
        List<UpdateResultsAPIObject> failedDBObjects;
        Validator validator = Validation.byProvider(HibernateValidator.class)
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .failFast(true)
                .buildValidatorFactory()
                .getValidator();
        Map<String, KruizeObject> mainKruizeExperimentMAP = new ConcurrentHashMap<>();
        List<String> errorReasons = new ArrayList<>();
        for (UpdateResultsAPIObject object : updateResultsAPIObjects) {
            String experimentName = object.getExperimentName();
            if (experimentName == null) {
                errorReasons.add(String.format("%s%s", MISSING_EXPERIMENT_NAME, null));
                object.setErrors(getErrorMap(errorReasons));
                failedUpdateResultsAPIObjects.add(object);
                continue;
            }
            if (!mainKruizeExperimentMAP.containsKey(experimentName)) {
                try {
                    new ExperimentDBService().loadExperimentFromDBByName(mainKruizeExperimentMAP, experimentName); // TODO try to avoid DB
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }
            if (mainKruizeExperimentMAP.containsKey(experimentName)) {

                // check version
                String errorMsg = checkVersion(object, mainKruizeExperimentMAP);
                if (errorMsg != null) {
                    errorReasons.add(errorMsg);
                    object.setErrors(getErrorMap(errorReasons));
                    failedUpdateResultsAPIObjects.add(object);
                    continue;
                }
                object.setKruizeObject(mainKruizeExperimentMAP.get(object.getExperimentName()));
                Set<ConstraintViolation<UpdateResultsAPIObject>> violations = new HashSet<>();
                try {
                    violations = validator.validate(object, UpdateResultsAPIObject.FullValidationSequence.class);
                    if (violations.isEmpty()) {
                        successUpdateResultsAPIObjects.add(object);
                    } else {
                        for (ConstraintViolation<UpdateResultsAPIObject> violation : violations) {
                            String propertyPath = violation.getPropertyPath().toString();
                            if (null != propertyPath && !propertyPath.isEmpty()) {
                                errorReasons.add(getSerializedName(propertyPath, UpdateResultsAPIObject.class) + ": " + violation.getMessage());
                            } else {
                                errorReasons.add(violation.getMessage());
                            }
                        }
                        object.setErrors(getErrorMap(errorReasons));
                        failedUpdateResultsAPIObjects.add(object);
                    }
                } catch (Exception e) {
                    LOGGER.debug(e.getMessage());
                    e.printStackTrace();
                    errorReasons.add(String.format("%s%s", e.getMessage(), experimentName));
                    object.setErrors(getErrorMap(errorReasons));
                    failedUpdateResultsAPIObjects.add(object);
                }
            } else {
                errorReasons.add(String.format("%s%s", MISSING_EXPERIMENT_NAME, experimentName));
                object.setErrors(getErrorMap(errorReasons));
                failedUpdateResultsAPIObjects.add(object);
            }
        }
        List<ExperimentResultData> resultDataList = new ArrayList<>();
        successUpdateResultsAPIObjects.forEach(
                (successObj) -> {
                    resultDataList.add(Converters.KruizeObjectConverters.convertUpdateResultsAPIObjToExperimentResultData(successObj));
                }
        );

        if (successUpdateResultsAPIObjects.size() > 0) {
            failedDBObjects = new ExperimentDBService().addResultsToDB(resultDataList);
            failedUpdateResultsAPIObjects.addAll(failedDBObjects);
        }
    }

    private String checkVersion(UpdateResultsAPIObject object, Map<String, KruizeObject> mainKruizeExperimentMAP) {
        try {
            KruizeObject kruizeObject = mainKruizeExperimentMAP.get(object.getExperimentName());
            if (!object.getApiVersion().equals(kruizeObject.getApiVersion())) {
                return String.format(AnalyzerErrorConstants.AutotuneObjectErrors.VERSION_MISMATCH,
                        kruizeObject.getApiVersion(), object.getApiVersion());
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred while checking version: {}", e.getMessage());
            return null;
        }
        return null;
    }

    public String getSerializedName(String fieldName, Class<?> targetClass) {
        Class<?> currentClass = targetClass;
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                SerializedName annotation = field.getAnnotation(SerializedName.class);
                if (annotation != null) {
                    fieldName = annotation.value();
                }
            } catch (NoSuchFieldException e) {
                // Field not found in the current class
                // Move up to the superclass
                currentClass = currentClass.getSuperclass();
            }
        }
        return fieldName;
    }

    public List<UpdateResultsAPIObject> getSuccessUpdateResultsAPIObjects() {
        return successUpdateResultsAPIObjects;
    }

    public List<UpdateResultsAPIObject> getFailedUpdateResultsAPIObjects() {
        return failedUpdateResultsAPIObjects;
    }


}
