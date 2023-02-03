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
package com.autotune.analyzer.utils;

import com.autotune.common.data.ValidationResultData;
import com.autotune.common.performanceProfiles.PerformanceProfile;
import com.autotune.utils.AnalyzerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * create Experiment input validation
 */
public class PerformanceProfileValidation {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceProfileValidation.class);
    private boolean success;
    private String errorMessage;
    private final Map<String, PerformanceProfile> performanceProfilesMap;

    //Mandatory fields
    private final List<String> mandatoryFields = new ArrayList<>(Arrays.asList(
            AnalyzerConstants.PerformanceProfileConstants.PERF_PROFILE_NAME,
            AnalyzerConstants.SLO
    ));

    private final List<String> mandatorySLOPerf = new ArrayList<>(Arrays.asList(
            AnalyzerConstants.AutotuneObjectConstants.DIRECTION,
            AnalyzerConstants.PerformanceProfileConstants.OBJECTIVE_FUNCTION,
            AnalyzerConstants.PerformanceProfileConstants.FUNCTION_VARIABLES
    ));

    private final List<String> mandatoryFuncVariables = new ArrayList<>(Arrays.asList(
            AnalyzerConstants.AutotuneObjectConstants.NAME,
            AnalyzerConstants.AutotuneObjectConstants.DATASOURCE,
            AnalyzerConstants.PerformanceProfileConstants.VALUE_TYPE
    ));

    public PerformanceProfileValidation(Map<String, PerformanceProfile> performanceProfilesMap) {
        this.performanceProfilesMap = performanceProfilesMap;
    }


    /**
     * Validates function variables
     *
     * @param performanceProfilesMap Map which contains the profiles and in which new profiles will be added post validation.
     * @param performanceProfile Performance Profile Object to be validated
     * @return Returns the ValidationResultData containing the response based on the validation
     */
    public ValidationResultData validate(Map<String, PerformanceProfile> performanceProfilesMap, PerformanceProfile performanceProfile) {

        ValidationResultData validationResultData = validateMandatoryFields(performanceProfile);
        String errorMsg = "";
        if (validationResultData.isSuccess()) {
            String perfProfileName = performanceProfile.getName();
            if (null == performanceProfilesMap.get(perfProfileName)) {
                LOGGER.debug("Performance Profile {} doesn't exist, Proceeding to create a new one..",perfProfileName);
                addPerformanceProfile(performanceProfilesMap, performanceProfile);
                setSuccess(true);
            }else {
                errorMsg = errorMsg.concat(String.format("Performance Profile name : %s is duplicate", perfProfileName));
                validationResultData.setMessage(errorMsg);
                validationResultData.setSuccess(false);
            }
        }
        else
            markFailed(validationResultData.getMessage());
        return validationResultData;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void markFailed(String message) {
        setSuccess(false);
        setErrorMessage(message);
    }

    /**
     * Check if all mandatory values are present.
     *
     * @param perfObj Mandatory fields of this Performance Profile Object will be validated
     * @return ValidationResultData object containing status of the validations
     */
    public ValidationResultData validateMandatoryFields(PerformanceProfile perfObj) {
        List<String> missingMandatoryFields = new ArrayList<>();
        ValidationResultData validationResultData = new ValidationResultData(false, null);
        String errorMsg;
        String perfProfileName = perfObj.getName();
        errorMsg = String.format("Performance Profile Name : %s \n", perfProfileName);
        mandatoryFields.forEach(
                mField -> {
                    String methodName = "get" + mField.substring(0, 1).toUpperCase() + mField.substring(1);
                    try {
                        LOGGER.debug("MethodName = {}",methodName);
                        Method getNameMethod = perfObj.getClass().getMethod(methodName);
                        if (getNameMethod.invoke(perfObj) == null)
                            missingMandatoryFields.add(mField);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        LOGGER.error("Method name {} doesn't exist!", mField);
                    }
                }
        );
        if (missingMandatoryFields.size() == 0) {
            try {
                mandatorySLOPerf.forEach(
                        mField -> {
                            String methodName = "get" + mField.substring(0, 1).toUpperCase() + mField.substring(1);
                            try {
                                LOGGER.debug("MethodName = {}",methodName);
                                Method getNameMethod = perfObj.getSloInfo().getClass().getMethod(methodName);
                                if (getNameMethod.invoke(perfObj.getSloInfo()) == null)
                                    missingMandatoryFields.add(mField);
                            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                                LOGGER.error("Method name {} doesn't exist!", mField);
                            }

                        });
                if (missingMandatoryFields.size() == 0) {
                    mandatoryFuncVariables.forEach(
                            mField -> {
                                String methodName = "get" + mField.substring(0, 1).toUpperCase() + mField.substring(1);
                                try {
                                    LOGGER.debug("MethodName = {}",methodName);
                                    Method getNameMethod = perfObj.getSloInfo().getFunctionVariables().get(0)
                                            .getClass().getMethod(methodName);
                                    if (getNameMethod.invoke(perfObj.getSloInfo().getFunctionVariables().get(0)) == null)
                                        missingMandatoryFields.add(mField);
                                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                                    LOGGER.error("Method name {} doesn't exist!", mField);
                                }

                            });
                    String mandatoryObjFuncData = AnalyzerConstants.AutotuneObjectConstants.OBJ_FUNCTION_TYPE;
                    String methodName = "get" + mandatoryObjFuncData.substring(0, 1).toUpperCase() +
                    mandatoryObjFuncData.substring(1);
                    try {
                        LOGGER.debug("MethodName = {}",methodName);
                        Method getNameMethod = perfObj.getSloInfo().getObjectiveFunction()
                                .getClass().getMethod(methodName);
                        if (getNameMethod.invoke(perfObj.getSloInfo().getObjectiveFunction()) == null)
                            missingMandatoryFields.add(mandatoryObjFuncData);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        LOGGER.error("Method name {} doesn't exist!", mandatoryObjFuncData);
                    }
                }
                validationResultData.setSuccess(true);
            } catch (Exception e) {
                validationResultData.setSuccess(false);
                errorMsg = errorMsg.concat(e.getMessage());
                validationResultData.setMessage(errorMsg);
            }
        } else {
            errorMsg = errorMsg.concat(String.format("Missing following Mandatory parameters %s \n", missingMandatoryFields));
            validationResultData.setSuccess(false);
            validationResultData.setMessage(errorMsg);
            LOGGER.debug("Validation error message :{}", errorMsg);
        }
        LOGGER.debug("{}", validationResultData);
        return validationResultData;
    }

    public static void addPerformanceProfile(Map<String, PerformanceProfile> performanceProfileMap, PerformanceProfile performanceProfile) {
        performanceProfileMap.put(performanceProfile.getName(), performanceProfile);
        LOGGER.debug("Added PerformanceProfile {}: ",performanceProfile.getName());
    }

    @Override
    public String toString() {
        return "PerformanceProfileValidation{" +
                "success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
