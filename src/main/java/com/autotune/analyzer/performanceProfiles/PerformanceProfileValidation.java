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
package com.autotune.analyzer.performanceProfiles;

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.utils.EvalExParser;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.metrics.AggregationFunctions;
import com.autotune.common.data.metrics.Metric;
import com.autotune.analyzer.kruizeObject.SloInfo;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.KruizeSupportedTypes;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * create Experiment input validation
 */
public class PerformanceProfileValidation {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceProfileValidation.class);
    private boolean success;
    private String errorMessage;
    private final Map<String, PerformanceProfile> performanceProfilesMap;

    //Mandatory fields for PerformanceProfile
    private final List<String> mandatoryFields = new ArrayList<>(Arrays.asList(
            AnalyzerConstants.PerformanceProfileConstants.PERF_PROFILE_NAME,
            AnalyzerConstants.PROFILE_VERSION,
            AnalyzerConstants.SLO
    ));

    //Mandatory fields for MetricProfile
    private final List<String> mandatoryMetricFields = new ArrayList<>(Arrays.asList(
            AnalyzerConstants.API_VERSION,
            AnalyzerConstants.KIND,
            AnalyzerConstants.AutotuneObjectConstants.METADATA,
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
     * @param operationType Type of API call (Create, Update, etc)
     * @return Returns the ValidationOutputData containing the response based on the validation
     */
    public ValidationOutputData validate(PerformanceProfile performanceProfile, AnalyzerConstants.OperationType operationType) {

        return validatePerformanceProfileData(performanceProfile, operationType);
    }

    /**
     * Validates function variables
     *
     * @param metricProfile Metric Profile Object to be validated
     * @return Returns the ValidationOutputData containing the response based on the validation
     */
    public ValidationOutputData validateMetricProfile(PerformanceProfile metricProfile) {

        return validateMetricProfileData(metricProfile);
    }

    /**
     * Validates the data present in the performance profile object before adding it to the map
     * @param performanceProfile
     * @param operationType Type of API call (Create, Update, etc)
     * @return
     */
    private ValidationOutputData validatePerformanceProfileData(PerformanceProfile performanceProfile, AnalyzerConstants.OperationType operationType) {
        // validate the mandatory values first
        ValidationOutputData validationOutputData = validateMandatoryFieldsAndData(performanceProfile);

        // If the mandatory values are present,proceed for further validation else return the validation object directly
        if (validationOutputData.isSuccess()) {
            try {
                new ExperimentDBService().loadAllPerformanceProfiles(performanceProfilesMap);
            } catch (Exception e) {
                LOGGER.error("Loading saved performance profiles failed: {} ", e.getMessage());
            }
            StringBuilder errorString = new StringBuilder();
            // check if the performance profile already exists
            PerformanceProfile existingPerformanceProfile = performanceProfilesMap.get(performanceProfile.getName());
            switch (operationType) {
                case CREATE:
                    if (existingPerformanceProfile != null) {
                        if (existingPerformanceProfile.getProfile_version() != performanceProfile.getProfile_version()) {
                            errorString.append(String.format(
                                    AnalyzerErrorConstants.AutotuneObjectErrors.PERF_PROFILE_VERSION_MISMATCH,
                                    existingPerformanceProfile.getName()));
                        } else {
                            errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.DUPLICATE_PERF_PROFILE)
                                    .append(performanceProfile.getName());
                        }
                        return new ValidationOutputData(false, errorString.toString(), HttpServletResponse.SC_CONFLICT);
                    }
                    break;

                case UPDATE:
                    if (existingPerformanceProfile == null) {
                        errorString.append(String.format(
                                AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_PERFORMANCE_PROFILE,
                                performanceProfile.getName()));
                        return new ValidationOutputData(false, errorString.toString(), HttpServletResponse.SC_NOT_FOUND);
                    } else if (performanceProfile.getProfile_version() != KruizeDeploymentInfo.perf_profile_supported_version) {
                        errorString.append(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_PERFORMANCE_PROFILE_VERSION,
                                performanceProfile.getProfile_version(), KruizeDeploymentInfo.perf_profile_supported_version,
                                performanceProfile.getName()));
                        return new ValidationOutputData(false, errorString.toString(), HttpServletResponse.SC_CONFLICT);
                    } else if (existingPerformanceProfile.getName().equals(performanceProfile.getName()) &&
                            existingPerformanceProfile.getProfile_version() == performanceProfile.getProfile_version()) {
                        errorString.append(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.PERF_PROFILE_ALREADY_UPDATED,
                                performanceProfile.getName(), performanceProfile.getProfile_version()));
                        return new ValidationOutputData(false, errorString.toString(), HttpServletResponse.SC_CONFLICT);
                    }
                    break;
            }

            // Validates fields like k8s_type and slo object
            validateCommonProfileFields(performanceProfile, errorString, validationOutputData);

            if (!errorString.toString().isEmpty()) {
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(errorString.toString());
                validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                validationOutputData.setSuccess(true);
            }
        }
        return validationOutputData;
    }

    /**
     * Check if all mandatory values are present.
     *
     * @param perfObj Mandatory fields of this Performance Profile Object will be validated
     * @return ValidationOutputData object containing status of the validations
     */
    public ValidationOutputData validateMandatoryFieldsAndData(PerformanceProfile perfObj) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        List<String> missingMandatoryFields = new ArrayList<>();

        try {
            // Validate top-level mandatory fields
            if (checkMandatoryFields(perfObj, mandatoryFields, missingMandatoryFields)) {
                return buildValidationFailure(validationOutputData, missingMandatoryFields);
            }
            // Validate SLO fields
            if (checkMandatoryFields(perfObj.getSloInfo(), mandatorySLOPerf, missingMandatoryFields)) {
                return buildValidationFailure(validationOutputData, missingMandatoryFields);
            }

            // Validate function variables
            for (Metric metric : perfObj.getSloInfo().getFunctionVariables()) {
                if (checkMandatoryFields(metric, mandatoryFuncVariables, missingMandatoryFields)) {
                    return buildValidationFailure(validationOutputData, missingMandatoryFields);
                }
            }
            // Validate aggregationFunction/query objects
            if (checkFunctionVariables(perfObj.getSloInfo().getFunctionVariables(), missingMandatoryFields)) {
                return buildValidationFailure(validationOutputData, missingMandatoryFields);
            }

            // Validate objective function
            if (perfObj.getSloInfo().getObjectiveFunction() == null) {
                return buildValidationFailure(validationOutputData, List.of(AnalyzerConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION));
            }

            String mandatoryObjFuncData = AnalyzerConstants.AutotuneObjectConstants.OBJ_FUNCTION_TYPE;
            if (checkMandatoryFields(perfObj.getSloInfo().getObjectiveFunction(), List.of(mandatoryObjFuncData),
                    missingMandatoryFields)) {
                return buildValidationFailure(validationOutputData, missingMandatoryFields);
            }

            // All validations passed
            validationOutputData.setSuccess(true);

        } catch (Exception e) {
            setValidationError(validationOutputData, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        return validationOutputData;
    }

    /**
     * Checks that all specified fields in the given object are non-null and non-empty.
     * <p>
     * Uses reflection to invoke JavaBean-style getters (e.g., field name → getName() ).
     * Returns true if all fields are valid; otherwise, adds the first missing field to
     * missingFields and returns false.
     * </p>
     *
     * @param obj            the object instance to inspect; must not be {@code null}
     * @param fields         list of field names (without "get" prefix) to validate
     * @param missingMandatoryFields  the list where the first missing field name will be added
     * @return true if all fields are valid;  false otherwise
     */
    private boolean checkMandatoryFields(Object obj, List<String> fields, List<String> missingMandatoryFields) {
        for (String field : fields) {
            String methodName = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
            try {
                LOGGER.debug("MethodName = {}", methodName);
                Method method = obj.getClass().getMethod(methodName);
                Object value = method.invoke(obj);
                if (isNullOrEmpty(value)) {
                    missingMandatoryFields.add(field);
                    LOGGER.warn("Field '{}' is missing or empty on {}", field, obj.getClass().getSimpleName());
                    return true;
                }

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.error("Method {} doesn't exist on class {}", methodName, obj.getClass().getSimpleName());
                missingMandatoryFields.add(field);
                return true;
            }
        }
        return false;
    }


    /**
     * Populates the given ValidationOutputData object with error information and logs the message.
     * <p>
     * This helper method centralizes the logic for setting validation failure details, including
     * the error message, HTTP error code, and success flag.
     * </p>
     *
     * @param output     the ValidationOutputData object to populate with error details
     * @param message    the error message to set; may be null or empty
     * @param errorCode  the HTTP error code corresponding to the failure (e.g.SC_BAD_REQUEST)
     */
    private void setValidationError(ValidationOutputData output, String message, int errorCode) {
        output.setSuccess(false);
        output.setMessage(message);
        output.setErrorCode(errorCode);
        LOGGER.error("Validation error message: {}", message);
    }

    /**
     * Populates the givenValidationOutputData with failure details for missing mandatory fields.
     * @param output ValidationOutputData object to populate
     * @param missingFields list of missing field names
     * @return the same ValidationOutputData object populated with error info
     */
    private ValidationOutputData buildValidationFailure(ValidationOutputData output, List<String> missingFields) {
        String errorMsg = String.format("Missing mandatory parameters: %s", missingFields);
        setValidationError(output, errorMsg, HttpServletResponse.SC_BAD_REQUEST);
        return output;
    }

    /**
     * Checks whether the given value is null or considered "empty".
     * <p>
     * The following are treated as empty:
     * <ul>
     *   <li>null</li>
     *   <li>Empty or blank String</li>
     *   <li>Empty Collection, Map or array</li>
     * </ul>
     *
     * @param value the object to check
     * @return true if the value is null or empty; false otherwise
     */
    private boolean isNullOrEmpty(Object value) {
        if (value == null)
            return true;
        if (value instanceof String) {
            return ((String) value).isEmpty();
        } else if (value instanceof Collection<?>) {
            return ((Collection<?>) value).isEmpty();
        } else if (value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        }
        return false;
    }

    private boolean checkFunctionVariables(List<Metric> functionVariables, List<String> missingFields) {

        for (Metric metric : functionVariables) {
            boolean hasQuery = metric.getQuery() != null && !metric.getQuery().isEmpty();
            boolean hasAggFunctions = metric.getAggregationFunctionsMap() != null && !metric.getAggregationFunctionsMap().isEmpty();
            // At least one of them must be present
            if (!hasQuery && !hasAggFunctions) {
                missingFields.add(AnalyzerConstants.AutotuneObjectConstants.QUERY);
                missingFields.add(AnalyzerConstants.AGGREGATION_FUNCTIONS);
                return true;
            }
            // If aggregation_functions map present, validate inner fields
            if (hasAggFunctions) {
                for (Map.Entry<String, AggregationFunctions> entry : metric.getAggregationFunctionsMap().entrySet()) {
                    AggregationFunctions af = entry.getValue();
                    if (af.getFunction() == null || af.getFunction().isEmpty()) {
                        missingFields.add(AnalyzerConstants.FUNCTION);
                        LOGGER.error("Missing 'function' in aggregation_functions for '{}' key '{}'", metric.getName(), entry.getKey());
                        return true;
                    }
                    if (af.getQuery() == null || af.getQuery().isEmpty()) {
                        missingFields.add(AnalyzerConstants.AutotuneObjectConstants.QUERY);
                        LOGGER.error("Missing 'query' in aggregation_functions for '{}' key '{}'", metric.getName(), entry.getKey());
                        return true;
                    }
                }
            }
        }
        return false;
    }



    /**
     * Validates the data present in the metric profile object before adding it to the map
     * @param metricProfile Metric Profile Object to be validated
     * @return Returns the ValidationOutputData containing the response based on the validation
     */
    private ValidationOutputData validateMetricProfileData(PerformanceProfile metricProfile) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        StringBuilder errorString = new StringBuilder();
        try {
            // validate the mandatory values first
            validationOutputData = validateMandatoryMetricProfileFieldsAndData(metricProfile);

            // If the mandatory values are present,proceed for further validation else return the validation object directly
            if (validationOutputData.isSuccess()) {
                try {
                    new ExperimentDBService().loadAllMetricProfiles(performanceProfilesMap);
                } catch (Exception e) {
                    LOGGER.error("Loading saved metric profiles failed: {} ", e.getMessage());
                }

                // Check if metadata exists
                JsonNode metadata = metricProfile.getMetadata();
                if (null == metadata) {
                    errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_METRIC_PROFILE_METADATA);
                }
                // check if the performance profile already exists
                if (null != performanceProfilesMap.get(metricProfile.getMetadata().get("name").asText())) {
                    errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.DUPLICATE_METRIC_PROFILE).append(metricProfile.getMetadata().get("name").asText());
                    return new ValidationOutputData(false, errorString.toString(), HttpServletResponse.SC_CONFLICT);
                }

                // Validates fields like k8s_type and slo object
                validateCommonProfileFields(metricProfile, errorString, validationOutputData);

                if (!errorString.toString().isEmpty()) {
                    validationOutputData.setSuccess(false);
                    validationOutputData.setMessage(errorString.toString());
                    validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
                } else {
                    validationOutputData.setSuccess(true);
                }
            }
        } catch (Exception e){
            validationOutputData.setSuccess(false);
            validationOutputData.setMessage(errorString.toString());
            validationOutputData.setErrorCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return validationOutputData;
    }

    /**
     * Check if all mandatory values are present.
     *
     * @param metricObj Mandatory fields of this Metric Profile Object will be validated
     * @return ValidationOutputData object containing status of the validations
     */
    public ValidationOutputData validateMandatoryMetricProfileFieldsAndData(PerformanceProfile metricObj) {
        List<String> missingMandatoryFields = new ArrayList<>();
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        String errorMsg;
        errorMsg = "";
        mandatoryMetricFields.forEach(
                mField -> {
                    String methodName = "get" + mField.substring(0, 1).toUpperCase() + mField.substring(1);
                    try {
                        LOGGER.debug("MethodName = {}",methodName);
                        Method getNameMethod = metricObj.getClass().getMethod(methodName);
                        if (null == getNameMethod.invoke(metricObj) || getNameMethod.invoke(metricObj).toString().isEmpty())
                            missingMandatoryFields.add(mField);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        LOGGER.error("Method name {} doesn't exist!", mField);
                    }
                }
        );
        if (missingMandatoryFields.size() == 0) {
            try {
                String mandatoryMetadataPerf = AnalyzerConstants.PerformanceProfileConstants.PERF_PROFILE_NAME;
                try {
                    JsonNode metadata = metricObj.getMetadata();
                    String metricProfileName = metadata.get(mandatoryMetadataPerf).asText();
                    if (null == metricProfileName || metricProfileName.isEmpty() || metricProfileName.equals("null")) {
                        missingMandatoryFields.add(mandatoryMetadataPerf);
                    }
                } catch (Exception e) {
                    LOGGER.error("Method name doesn't exist for: {}!", mandatoryMetadataPerf);
                }

                if (missingMandatoryFields.size() == 0) {
                    mandatorySLOPerf.forEach(
                            mField -> {
                                String methodName = "get" + mField.substring(0, 1).toUpperCase() + mField.substring(1);
                                try {
                                    LOGGER.debug("MethodName = {}", methodName);
                                    Method getNameMethod = metricObj.getSloInfo().getClass().getMethod(methodName);
                                    if (getNameMethod.invoke(metricObj.getSloInfo()) == null)
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
                                        LOGGER.debug("MethodName = {}", methodName);
                                        Method getNameMethod = metricObj.getSloInfo().getFunctionVariables().get(0)
                                                .getClass().getMethod(methodName);
                                        if (getNameMethod.invoke(metricObj.getSloInfo().getFunctionVariables().get(0)) == null)
                                            missingMandatoryFields.add(mField);
                                    } catch (NoSuchMethodException | IllegalAccessException |
                                             InvocationTargetException e) {
                                        LOGGER.error("Method name {} doesn't exist!", mField);
                                    }

                                });
                        String mandatoryObjFuncData = AnalyzerConstants.AutotuneObjectConstants.OBJ_FUNCTION_TYPE;
                        String methodName = "get" + mandatoryObjFuncData.substring(0, 1).toUpperCase() +
                                mandatoryObjFuncData.substring(1);
                        try {
                            LOGGER.debug("MethodName = {}", methodName);
                            Method getNameMethod = metricObj.getSloInfo().getObjectiveFunction()
                                    .getClass().getMethod(methodName);
                            if (getNameMethod.invoke(metricObj.getSloInfo().getObjectiveFunction()) == null)
                                missingMandatoryFields.add(mandatoryObjFuncData);
                        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                            LOGGER.error("Method name {} doesn't exist!", mandatoryObjFuncData);
                        }
                        validationOutputData.setSuccess(true);
                    }
                }

                if (!missingMandatoryFields.isEmpty()) {
                    errorMsg = errorMsg.concat(String.format("Missing mandatory parameters: %s ", missingMandatoryFields));
                    validationOutputData.setSuccess(false);
                    validationOutputData.setMessage(errorMsg);
                    validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
                    LOGGER.error("Validation error message :{}", errorMsg);
                }
            } catch (Exception e) {
                validationOutputData.setSuccess(false);
                errorMsg = errorMsg.concat(e.getMessage());
                validationOutputData.setMessage(errorMsg);
                validationOutputData.setErrorCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            errorMsg = errorMsg.concat(String.format("Missing mandatory parameters: %s ", missingMandatoryFields));
            validationOutputData.setSuccess(false);
            validationOutputData.setMessage(errorMsg);
            validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
            LOGGER.error("Validation error message :{}", errorMsg);
        }

        return validationOutputData;
    }

    /**
     *  Validates fields like k8s_type and slo object common to both Metric and Performance Profile
     * @param metricProfile Metric/Performance Profile object to be validated
     * @param errorString   StringBuilder to collect error messages during validation of multiple fields
     * @param validationOutputData ValidationOutputData containing the response based on the validation
     */
    private void validateCommonProfileFields(PerformanceProfile metricProfile, StringBuilder errorString, ValidationOutputData validationOutputData){
        // Check if k8s type is supported
        String k8sType = metricProfile.getK8S_TYPE();
        if (!KruizeSupportedTypes.K8S_TYPES_SUPPORTED.contains(k8sType)) {
            errorString.append(AnalyzerConstants.PerformanceProfileConstants.K8S_TYPE).append(k8sType)
                    .append(AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED);
        }

        SloInfo sloInfo = metricProfile.getSloInfo();
        // Check if direction is supported
        if (!KruizeSupportedTypes.DIRECTIONS_SUPPORTED.contains(sloInfo.getDirection()))
            errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.DIRECTION_NOT_SUPPORTED);
        // if slo_class is present, do further validations
        if (sloInfo.getSloClass() != null) {
            // Check if slo_class is supported
            if (!KruizeSupportedTypes.SLO_CLASSES_SUPPORTED.contains(sloInfo.getSloClass()))
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
            if (!KruizeSupportedTypes.MONITORING_AGENTS_SUPPORTED.contains(functionVariable.getDatasource().toLowerCase())) {
                errorString.append(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLE)
                        .append(functionVariable.getName())
                        .append(AnalyzerErrorConstants.AutotuneObjectErrors.DATASOURCE_NOT_SUPPORTED);
            }

            // Check if value_type is supported
            if (!KruizeSupportedTypes.VALUE_TYPES_SUPPORTED.contains(functionVariable.getValueType().toLowerCase())) {
                errorString.append(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLE)
                        .append(functionVariable.getName())
                        .append(AnalyzerErrorConstants.AutotuneObjectErrors.VALUE_TYPE_NOT_SUPPORTED);
            }

            // Check if kubernetes_object type is supported, set default to 'container' if it's absent.
            String kubernetes_object = functionVariable.getKubernetesObject();
            if (null == kubernetes_object)
                functionVariable.setKubernetesObject(KruizeConstants.JSONKeys.CONTAINER);
            else {
                if (!KruizeSupportedTypes.KUBERNETES_OBJECTS_SUPPORTED.contains(kubernetes_object.toLowerCase()))
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
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(errorString.toString());
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
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
    @Override
    public String toString() {
        return "PerformanceProfileValidation{" +
                "success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
