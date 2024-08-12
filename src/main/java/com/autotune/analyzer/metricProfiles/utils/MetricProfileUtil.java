/*******************************************************************************
 * Copyright (c) 2023, 2024 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.metricProfiles.utils;

import com.autotune.analyzer.metricProfiles.MetricProfile;
import com.autotune.analyzer.metricProfiles.MetricProfileValidation;
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.KubernetesAPIObject;
import com.autotune.analyzer.serviceObjects.UpdateResultsAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.utils.KruizeSupportedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class MetricProfileUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricProfileUtil.class);
    private MetricProfileUtil() {

    }

    /**
     * validates the metric profile fields and the data and then adds it to the map
     * @param metricProfile
     * @return
     */
    public static ValidationOutputData validateAndAddProfile(Map<String, MetricProfile> metricProfilesMap, MetricProfile metricProfile) {
        ValidationOutputData validationOutputData;
        try {
            validationOutputData = new MetricProfileValidation(metricProfilesMap).validate(metricProfile);
            if (validationOutputData.isSuccess()) {
                addMetricProfile(metricProfilesMap, metricProfile);
            } else {
                validationOutputData.setMessage("Validation failed: " + validationOutputData.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Validate and add profile failed: " + e.getMessage());
            validationOutputData = new ValidationOutputData(false, "Validation failed: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return validationOutputData;
    }

    /**
     * @param metricProfile
     * @param updateResultsAPIObject
     * @return
     */
    public static List<String> validateResults(MetricProfile metricProfile, UpdateResultsAPIObject updateResultsAPIObject) {

        List<String> errorReasons = new ArrayList<>();
        String errorMsg = "";
        List<AnalyzerConstants.MetricName> mandatoryFields = Arrays.asList(
                AnalyzerConstants.MetricName.cpuUsage,
                AnalyzerConstants.MetricName.memoryUsage,
                AnalyzerConstants.MetricName.memoryRSS);
        // Get the metrics data from the Metric Profile
        List<String> metricProfileAggrFunctions = new ArrayList<>();
        List<String> queryList = new ArrayList<>();
        List<String> metricProfileFunctionVariablesList = new ArrayList<>();
        for (Metric metric : metricProfile.getSloInfo().getFunctionVariables()) {
            metricProfileFunctionVariablesList.add(metric.getName());
            if (null != metric.getAggregationFunctionsMap()) {
                metric.getAggregationFunctionsMap().values().forEach(aggregationFunctions ->
                        metricProfileAggrFunctions.add(aggregationFunctions.getFunction()));
            }
            if (null != metric.getQuery())
                queryList.add(metric.getQuery());
        }

        // Get the metrics data from the Kruize Object and validate it
        for (KubernetesAPIObject kubernetesAPIObject : updateResultsAPIObject.getKubernetesObjects()) {
            for (ContainerAPIObject containerAPIObject : kubernetesAPIObject.getContainerAPIObjects()) {
                // if the metrics data is not present, set corresponding validation message and skip adding the current container data
                if (containerAPIObject.getMetrics() == null) {
                    errorReasons.add(String.format(
                            AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_METRICS,
                            containerAPIObject.getContainer_name(),
                            updateResultsAPIObject.getExperimentName()
                    ));
                    continue;
                }
                List<Metric> metrics = containerAPIObject.getMetrics();
                List<AnalyzerConstants.MetricName> kruizeFunctionVariablesList = new ArrayList<>();
                for (Metric metric : metrics) {
                    try {
                        // validate the metric values
                        errorMsg = MetricProfileUtil.validateMetricsValues(metric.getName(), metric.getMetricResult());
                        if (!errorMsg.isBlank()) {
                            errorReasons.add(errorMsg.concat(String.format(
                                    AnalyzerErrorConstants.AutotuneObjectErrors.CONTAINER_AND_EXPERIMENT,
                                    containerAPIObject.getContainer_name(),
                                    updateResultsAPIObject.getExperimentName())));
                            break;
                        }
                        AnalyzerConstants.MetricName metricName = AnalyzerConstants.MetricName.valueOf(metric.getName());
                        kruizeFunctionVariablesList.add(metricName);
                        MetricResults metricResults = metric.getMetricResult();
                        Map<String, Object> aggrInfoClassAsMap;
                        if (!metricProfileAggrFunctions.isEmpty()) {
                            try {
                                aggrInfoClassAsMap = convertObjectToMap(metricResults.getAggregationInfoResult());
                                errorMsg = validateAggFunction(aggrInfoClassAsMap, metricProfileAggrFunctions);
                                if (!errorMsg.isBlank()) {
                                    errorReasons.add(errorMsg.concat(String.format(
                                            AnalyzerErrorConstants.AutotuneObjectErrors.CONTAINER_AND_EXPERIMENT,
                                            containerAPIObject.getContainer_name(),
                                            updateResultsAPIObject.getExperimentName())));
                                    break;
                                }
                            } catch(IllegalAccessException | InvocationTargetException e){
                                throw new RuntimeException(e);
                            }
                        } else{
                            // check if query is also absent
                            if (queryList.isEmpty()) {
                                errorReasons.add(AnalyzerErrorConstants.AutotuneObjectErrors.QUERY_FUNCTION_MISSING);
                                break;
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Error occurred in metrics validation: " + errorMsg);
                    }
                }
                if (!errorReasons.isEmpty())
                    break;

                LOGGER.debug("metricProfileFunctionVariablesList: {}", metricProfileFunctionVariablesList);
                LOGGER.debug("kruizeFunctionVariablesList: {}", kruizeFunctionVariablesList);
                if (!new HashSet<>(kruizeFunctionVariablesList).containsAll(mandatoryFields)) {
                    errorReasons.add(errorMsg.concat(String.format("Missing one of the following mandatory parameters for experiment - %s : %s",
                            updateResultsAPIObject.getExperimentName(), mandatoryFields)));
                    break;
                }
            }
        }
        return errorReasons;
    }

    public static void addMetricProfile(Map<String, MetricProfile> metricProfileMapProfileMap, MetricProfile metricProfile) {
        metricProfileMapProfileMap.put(metricProfile.getName(), metricProfile);
        LOGGER.debug("Added MetricProfile: {} ", metricProfile.getName());
    }

    /**
     * Validates the aggregation function objects against the aggregationInfoResult metrics
     *
     * @param aggrInfoClassAsMap
     * @param metricProfileAggrFunctions
     * @return
     */
    public static String validateAggFunction(Map<String, Object> aggrInfoClassAsMap, List<String> metricProfileAggrFunctions) {

        List<String> resultDataAggrFuncObjects = aggrInfoClassAsMap.keySet().stream().toList();
        String errorMsg = "";
        // check if none of the aggrfunctions are present in the aggrInfoObjects List
        if (resultDataAggrFuncObjects.stream().noneMatch(metricProfileAggrFunctions::contains)) {
            LOGGER.error(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_AGG_FUNCTION);
            errorMsg = errorMsg.concat(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_AGG_FUNCTION);
        } else if (!resultDataAggrFuncObjects.containsAll(metricProfileAggrFunctions)) {
            List<String> missingObjects = new ArrayList<>(metricProfileAggrFunctions);
            missingObjects.removeAll(resultDataAggrFuncObjects);
            LOGGER.error(AnalyzerErrorConstants.AutotuneObjectErrors.AGG_FUNCTION_MISMATCH.concat(missingObjects.toString()));
            errorMsg = errorMsg.concat(AnalyzerErrorConstants.AutotuneObjectErrors.AGG_FUNCTION_MISMATCH).concat(": ")
                    .concat(missingObjects.toString());
        }
        return errorMsg;
    }
    public static String validateMetricsValues(String metricVariableName, MetricResults metricResults) {

        String errorMsg = "";
        // validate the metric variable name
        try {
            AnalyzerConstants.MetricName.valueOf(metricVariableName);
        } catch (Exception e) {
            errorMsg = AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_METRIC;
            LOGGER.error(errorMsg);
            return errorMsg;
        }
        // check if the 'value' is present in the result JSON
        if (null == metricResults.getValue()) {
            LOGGER.debug(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_VALUE.concat(metricVariableName));
        }
        // validate the aggregation info values
        Map<String, Object> aggrInfoClassAsMap;
        try {
            aggrInfoClassAsMap = convertObjectToMap(metricResults.getAggregationInfoResult());
        } catch(IllegalAccessException | InvocationTargetException e){
            errorMsg = "Exception occurred while aggregationInfo conversion to map: ".concat(e.getMessage());
            LOGGER.error(errorMsg);
            return errorMsg;
        }
        for (Map.Entry<String, Object> entry : aggrInfoClassAsMap.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();

            if (value instanceof Number && !key.equals("format")) {
                double doubleValue = ((Number) value).doubleValue();
                if (doubleValue < 0 || Double.isNaN(doubleValue)) {
                    LOGGER.error(key.concat(AnalyzerErrorConstants.AutotuneObjectErrors.BLANK_AGGREGATION_INFO_VALUE).concat(metricVariableName));
                    errorMsg = errorMsg.concat(key.concat(AnalyzerErrorConstants.AutotuneObjectErrors.BLANK_AGGREGATION_INFO_VALUE).concat(metricVariableName));
                    break;
                }
            } else if (key.equals("format")) {
                String stringValue = null;
                if (value instanceof String) {
                    stringValue = (String) value;
                }
                // TODO: handle the conversions for additional supported formats
                if (!KruizeSupportedTypes.SUPPORTED_FORMATS.contains(stringValue)) {
                    LOGGER.error(AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_FORMAT);
                    errorMsg = errorMsg.concat(AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_FORMAT);
                    break;
                }
            }
        }
        return errorMsg;
    }

    /**
     * Converts the aggregationInfoResult class into Map to extract values for validation
     *
     * @param obj
     * @return
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static Map<String, Object> convertObjectToMap(Object obj) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        Method[] methods = obj.getClass().getMethods();
        Map<String, Object> map = new HashMap<>();
        for (Method m : methods) {
            if (m.getName().startsWith("get") && !m.getName().startsWith("getClass")) {
                Object value = m.invoke(obj);
                map.put(m.getName().substring(3).toLowerCase(), value);
            }
        }
        return map;
    }
}
