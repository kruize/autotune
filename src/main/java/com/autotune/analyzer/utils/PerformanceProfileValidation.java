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

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.common.data.ValidationResultData;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.k8sObjects.SloInfo;
import com.autotune.common.performanceProfiles.PerformanceProfile;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.utils.AnalyzerConstants;
import com.autotune.utils.AnalyzerErrorConstants;
import com.autotune.utils.AutotuneConstants;
import com.autotune.utils.AutotuneSupportedTypes;
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
     * @param performanceProfile Performance Profile Object to be validated
     * @return Returns the ValidationResultData containing the response based on the validation
     */
    public ValidationResultData validate(PerformanceProfile performanceProfile) {

        ValidationResultData validationResultData = validateMandatoryFieldsAndData(performanceProfile);
        if (validationResultData.isSuccess())
            setSuccess(true);
        else {
            markFailed(validationResultData.getMessage());
            LOGGER.debug("Performance profile validation failed due to : {}",validationResultData.getMessage());
        }
        return validationResultData;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
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
    public ValidationResultData validateMandatoryFieldsAndData(PerformanceProfile perfObj) {
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

        if (validationResultData.isSuccess())
            validationResultData = validatePerformanceProfileData(perfObj);

        return validationResultData;
    }

    /**
     * Validates the data present in the performance profile object before adding it to the map
     * @param performanceProfile
     * @return
     */
    private ValidationResultData validatePerformanceProfileData(PerformanceProfile performanceProfile) {
        ValidationResultData validationResultData = new ValidationResultData(false, null);
        StringBuilder errorString = new StringBuilder();
        // check if the performance profile is duplicate
        if (performanceProfilesMap.get(performanceProfile.getName()) != null) {
            errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.DUPLICATE_PERF_PROFILE);
            validationResultData.setMessage(errorString.toString());
            return validationResultData;
        }
        // Check if k8s type is supported
        String k8sType = performanceProfile.getK8S_TYPE();
        if (!AutotuneSupportedTypes.K8S_TYPES_SUPPORTED.contains(k8sType)) {
            errorString.append(AnalyzerConstants.PerformanceProfileConstants.K8S_TYPE).append(k8sType)
                    .append(AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED);
        }

        SloInfo sloInfo = performanceProfile.getSloInfo();
        // Check if direction is supported
        if (!AutotuneSupportedTypes.DIRECTIONS_SUPPORTED.contains(sloInfo.getDirection()))
            errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.DIRECTION_NOT_SUPPORTED);
        // if slo_class is present, do further validations
        if (sloInfo.getSloClass() != null) {
            // Check if slo_class is supported
            if (!AutotuneSupportedTypes.SLO_CLASSES_SUPPORTED.contains(sloInfo.getSloClass()))
                errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.SLO_CLASS_NOT_SUPPORTED);

            //check if slo_class is 'response_time' and direction is 'minimize'
            if (sloInfo.getSloClass().equalsIgnoreCase(EMConstants.StandardDefaults.RESPONSE_TIME) && !sloInfo.getDirection()
                    .equalsIgnoreCase(AnalyzerConstants.AutotuneObjectConstants.MINIMIZE)) {
                errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.INVALID_DIRECTION_FOR_SLO_CLASS);
            }

            //check if slo_class is 'throughput' and direction is 'maximize'
            if (sloInfo.getSloClass().equalsIgnoreCase(EMConstants.StandardDefaults.THROUGHPUT) && !sloInfo.getDirection()
                    .equalsIgnoreCase(AnalyzerConstants.AutotuneObjectConstants.MAXIMIZE)) {
                errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.INVALID_DIRECTION_FOR_SLO_CLASS);
            }
        }
        // Check if function_variables is empty
        if (sloInfo.getFunctionVariables().isEmpty())
            errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.FUNCTION_VARIABLES_EMPTY);

        // Check if objective_function and it's type exists
        if (sloInfo.getObjectiveFunction() == null)
            errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.OBJECTIVE_FUNCTION_MISSING);

        // Get the objective_function type
        String objFunctionType = sloInfo.getObjectiveFunction().getFunction_type();
        String expression = null;
        for (Metric functionVariable : sloInfo.getFunctionVariables()) {
            // Check if datasource is supported
            if (!AutotuneSupportedTypes.MONITORING_AGENTS_SUPPORTED.contains(functionVariable.getDatasource().toLowerCase())) {
                errorString.append(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLE)
                        .append(functionVariable.getName())
                        .append(AnalyzerErrorConstants.AutotuneObjectErrors.DATASOURCE_NOT_SUPPORTED);
            }

            // Check if value_type is supported
            if (!AutotuneSupportedTypes.VALUE_TYPES_SUPPORTED.contains(functionVariable.getValueType().toLowerCase())) {
                errorString.append(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLE)
                        .append(functionVariable.getName())
                        .append(AnalyzerErrorConstants.AutotuneObjectErrors.VALUE_TYPE_NOT_SUPPORTED);
            }

            // Check if kubernetes_object type is supported, set default to 'container' if it's absent.
            String kubernetes_object = functionVariable.getKubernetesObject();
            if (null == kubernetes_object)
                functionVariable.setKubernetesObject(AutotuneConstants.JSONKeys.CONTAINER);
            else {
                if (!AutotuneSupportedTypes.KUBERNETES_OBJECTS_SUPPORTED.contains(kubernetes_object.toLowerCase()))
                    errorString.append(AnalyzerConstants.KUBERNETES_OBJECTS).append(kubernetes_object)
                            .append(AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED);
            }

            // Validate Objective Function
            try {
                if (objFunctionType.equals(AnalyzerConstants.AutotuneObjectConstants.EXPRESSION)) {

                    expression = sloInfo.getObjectiveFunction().getExpression();
                    if (null == expression || expression.equals(AnalyzerConstants.NULL)) {
                        throw new NullPointerException(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_EXPRESSION);
                    }

                } else if (objFunctionType.equals(AnalyzerConstants.PerformanceProfileConstants.SOURCE)) {
                    if (null != sloInfo.getObjectiveFunction().getExpression()) {
                        errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.MISPLACED_EXPRESSION);
                        throw new InvalidValueException(errorString.toString());
                    }
                } else {
                    errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.INVALID_TYPE);
                    throw new InvalidValueException(errorString.toString());
                }
            } catch (NullPointerException | InvalidValueException npe) {
                errorString.append(npe.getMessage());
                validationResultData.setSuccess(false);
                validationResultData.setMessage(errorString.toString());
            }

            // Check if function_variable is part of objective_function
            if (objFunctionType.equals(AnalyzerConstants.AutotuneObjectConstants.EXPRESSION)) {
                if (!expression.contains(functionVariable.getName())) {
                    errorString.append(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLE)
                            .append(functionVariable.getName()).append(" ")
                            .append(AnalyzerErrorConstants.AutotuneObjectErrors.FUNCTION_VARIABLE_ERROR);
                }
            }
        }

        // Check if objective_function is correctly formatted
        if (objFunctionType.equals(AnalyzerConstants.AutotuneObjectConstants.EXPRESSION)) {
            if (expression.equals(AnalyzerConstants.NULL) || !new EvalExParser().validate(sloInfo.getObjectiveFunction().getExpression(), sloInfo.getFunctionVariables())) {
                errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.INVALID_OBJECTIVE_FUNCTION);
            }
        }

        if (!errorString.isEmpty()) {
            validationResultData.setSuccess(false);
            validationResultData.setMessage(errorString.toString());
        } else
            validationResultData.setSuccess(true);

        return validationResultData;
    }

    @Override
    public String toString() {
        return "PerformanceProfileValidation{" +
                "success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
