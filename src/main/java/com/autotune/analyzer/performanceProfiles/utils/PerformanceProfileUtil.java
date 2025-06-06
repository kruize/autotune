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
package com.autotune.analyzer.performanceProfiles.utils;

import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.performanceProfiles.PerformanceProfileValidation;
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.KubernetesAPIObject;
import com.autotune.analyzer.serviceObjects.NamespaceAPIObject;
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
import java.util.stream.Collectors;

public class PerformanceProfileUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceProfileUtil.class);
    private PerformanceProfileUtil() {

    }
    // List of metrics that do NOT require an explicit format check
    private static final List<String> METRICS_WITHOUT_EXPLICIT_FORMAT = Arrays.asList(
            AnalyzerConstants.MetricName.namespaceTotalPods.toString(),
            AnalyzerConstants.MetricName.namespaceRunningPods.toString()
    );

    /**
     * validates the performance profile fields and the data and then adds it to the map
     * @param performanceProfile
     * @return
     */
    public static ValidationOutputData validateAndAddProfile(Map<String, PerformanceProfile> performanceProfilesMap, PerformanceProfile performanceProfile) {
        ValidationOutputData validationOutputData;
        try {
            validationOutputData = new PerformanceProfileValidation(performanceProfilesMap).validate(performanceProfile);
            if (validationOutputData.isSuccess()) {
                addPerformanceProfile(performanceProfilesMap, performanceProfile);
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
     * validates the metric profile fields and the data and then adds it to the map
     * @param metricProfilesMap
     * @param metricProfile     Metric profile to be validated
     * @return ValidationOutputData object
     */
    public static ValidationOutputData validateAndAddMetricProfile(Map<String, PerformanceProfile> metricProfilesMap, PerformanceProfile metricProfile) {
        ValidationOutputData validationOutputData;
        try {
            validationOutputData = new PerformanceProfileValidation(metricProfilesMap).validateMetricProfile(metricProfile);
            if (validationOutputData.isSuccess()) {
                addMetricProfile(metricProfilesMap, metricProfile);
            } else {
                validationOutputData.setMessage("Validation failed: " + validationOutputData.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Validate and add metric profile failed: {}", e.getMessage());
            validationOutputData = new ValidationOutputData(false, "Validation failed: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return validationOutputData;
    }

    /**
     * @param performanceProfile
     * @param updateResultsAPIObject
     * @return
     */
    public static List<String> validateResults(PerformanceProfile performanceProfile, UpdateResultsAPIObject updateResultsAPIObject) {

        List<String> errorReasons = new ArrayList<>();
        String errorMsg = "";

        // Get the metrics data from the Performance Profile
        List<String> perfProfileAggrFunctions = new ArrayList<>();
        List<String> queryList = new ArrayList<>();
        List<String> perfProfileFunctionVariablesList = new ArrayList<>();
        for (Metric metric : performanceProfile.getSloInfo().getFunctionVariables()) {
            perfProfileFunctionVariablesList.add(metric.getName());
            if (null != metric.getAggregationFunctionsMap()) {
                metric.getAggregationFunctionsMap().values().forEach(aggregationFunctions ->
                        perfProfileAggrFunctions.add(aggregationFunctions.getFunction()));
            }
            if (null != metric.getQuery())
                queryList.add(metric.getQuery());
        }

        // Get the metrics data from the Kruize Object and validate it
        for (KubernetesAPIObject kubernetesAPIObject : updateResultsAPIObject.getKubernetesObjects()) {

            if (kubernetesAPIObject.getContainerAPIObjects() != null) {
                List<AnalyzerConstants.MetricName> mandatoryFields = Arrays.asList(
                        AnalyzerConstants.MetricName.cpuUsage ,
                        AnalyzerConstants.MetricName.memoryUsage ,
                        AnalyzerConstants.MetricName.memoryRSS
                );
                List<ContainerAPIObject> containerAPIObjects = kubernetesAPIObject.getContainerAPIObjects();
                for (ContainerAPIObject containerAPIObject : containerAPIObjects) {
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
                            errorMsg = PerformanceProfileUtil.validateMetricsValues(metric.getName(), metric.getMetricResult());
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
                            if (!perfProfileAggrFunctions.isEmpty()) {
                                try {
                                    aggrInfoClassAsMap = convertObjectToMap(metricResults.getAggregationInfoResult());
                                    errorMsg = validateAggFunction(aggrInfoClassAsMap, perfProfileAggrFunctions);
                                    if (!errorMsg.isBlank()) {
                                        errorReasons.add(errorMsg.concat(String.format(
                                                AnalyzerErrorConstants.AutotuneObjectErrors.CONTAINER_AND_EXPERIMENT,
                                                containerAPIObject.getContainer_name(),
                                                updateResultsAPIObject.getExperimentName())));
                                        break;
                                    }
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
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
                    LOGGER.debug("perfProfileFunctionVariablesList: {}", perfProfileFunctionVariablesList);
                    LOGGER.debug("kruizeFunctionVariablesList: {}", kruizeFunctionVariablesList);
                    if (!new HashSet<>(kruizeFunctionVariablesList).containsAll(mandatoryFields)) {
                        errorReasons.add(errorMsg.concat(String.format("Missing one of the following mandatory parameters for experiment - %s : %s",
                                updateResultsAPIObject.getExperimentName(), mandatoryFields)));
                        break;
                    } else {
                        LOGGER.info("All mandatory fields are present for experiment: {}", updateResultsAPIObject.getExperimentName());
                        List<String> illegalMetrics = kruizeFunctionVariablesList.stream()
                                .map(AnalyzerConstants.MetricName::toString) // Convert MetricName to its String representation
                                .collect(Collectors.toList());

                        illegalMetrics.removeAll(perfProfileFunctionVariablesList); // Remove all expected metrics

                        if (!illegalMetrics.isEmpty()) {
                            // rare/impossible case as validations as validateMetricsValues function takes care of this
                            errorReasons.add(errorMsg.concat(String.format("Illegal/Invalid metrics found for experiment - %s: %s",
                                    updateResultsAPIObject.getExperimentName(), illegalMetrics)));
                        }
                    }
                }
            } else if (kubernetesAPIObject.getNamespaceAPIObject() != null) {
                List<AnalyzerConstants.MetricName> mandatoryFields = Arrays.asList(
                        AnalyzerConstants.MetricName.namespaceCpuUsage,
                        AnalyzerConstants.MetricName.namespaceMemoryUsage,
                        AnalyzerConstants.MetricName.namespaceMemoryRSS
                );
                NamespaceAPIObject namespaceAPIObject = kubernetesAPIObject.getNamespaceAPIObject();
                    // if the metrics data is not present, set corresponding validation message and skip adding the current namespace data
                    if (namespaceAPIObject.getMetrics() == null) {
                        errorReasons.add(String.format(
                                AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_METRICS,
                                namespaceAPIObject.getNamespace(),
                                updateResultsAPIObject.getExperimentName()
                        ));
                        continue;
                    }
                    List<Metric> metrics = namespaceAPIObject.getMetrics();
                    List<AnalyzerConstants.MetricName> kruizeFunctionVariablesList = new ArrayList<>();
                    for (Metric metric : metrics) {
                        try {
                            // validate the metric values
                            errorMsg = PerformanceProfileUtil.validateMetricsValues(metric.getName(), metric.getMetricResult());
                            if (!errorMsg.isBlank()) {
                                errorReasons.add(errorMsg.concat(String.format(
                                        AnalyzerErrorConstants.AutotuneObjectErrors.NAMESPACE_AND_EXPERIMENT,
                                        namespaceAPIObject.getNamespace(),
                                        updateResultsAPIObject.getExperimentName())));
                                break;
                            }
                            AnalyzerConstants.MetricName metricName = AnalyzerConstants.MetricName.valueOf(metric.getName());
                            kruizeFunctionVariablesList.add(metricName);
                            MetricResults metricResults = metric.getMetricResult();
                            Map<String, Object> aggrInfoClassAsMap;
                            if (!perfProfileAggrFunctions.isEmpty()) {
                                try {
                                    aggrInfoClassAsMap = convertObjectToMap(metricResults.getAggregationInfoResult());
                                    errorMsg = validateAggFunction(aggrInfoClassAsMap, perfProfileAggrFunctions);
                                    if (!errorMsg.isBlank()) {
                                        errorReasons.add(errorMsg.concat(String.format(
                                                AnalyzerErrorConstants.AutotuneObjectErrors.NAMESPACE_AND_EXPERIMENT,
                                                namespaceAPIObject.getNamespace(),
                                                updateResultsAPIObject.getExperimentName())));
                                        break;
                                    }
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
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

                    LOGGER.debug("perfProfileFunctionVariablesList: {}", perfProfileFunctionVariablesList);
                    LOGGER.debug("kruizeFunctionVariablesList: {}", kruizeFunctionVariablesList);
                    if (!new HashSet<>(kruizeFunctionVariablesList).containsAll(mandatoryFields)) {
                        errorReasons.add(errorMsg.concat(String.format("Missing one of the following mandatory parameters for experiment - %s : %s",
                                updateResultsAPIObject.getExperimentName(), mandatoryFields)));
                        break;
                    } else {
                        LOGGER.info("All mandatory fields are present for experiment: {}", updateResultsAPIObject.getExperimentName());
                        List<String> illegalMetrics = kruizeFunctionVariablesList.stream()
                                .map(AnalyzerConstants.MetricName::toString) // Convert MetricName to its String representation
                                .collect(Collectors.toList());

                        illegalMetrics.removeAll(perfProfileFunctionVariablesList); // Remove all expected metrics

                        if (!illegalMetrics.isEmpty()) {
                            // rare/impossible case as validations as validateMetricsValues function takes care of this
                            errorReasons.add(errorMsg.concat(String.format("Illegal/Invalid metrics found for experiment - %s: %s",
                                    updateResultsAPIObject.getExperimentName(), illegalMetrics)));
                        }
                    }
                }
            }
        return errorReasons;
    }

    public static void addPerformanceProfile(Map<String, PerformanceProfile> performanceProfileMap, PerformanceProfile performanceProfile) {
        performanceProfileMap.put(performanceProfile.getName(), performanceProfile);
        LOGGER.debug("Added PerformanceProfile: {} ",performanceProfile.getName());
    }

    public static void addMetricProfile(Map<String, PerformanceProfile> performanceProfileMap, PerformanceProfile performanceProfile) {
        performanceProfileMap.put(performanceProfile.getMetadata().get("name").asText(), performanceProfile);
        LOGGER.debug("Added MetricProfile: {} ",performanceProfile.getMetadata().get("name"));
    }

    /**
     * Validates the aggregation function objects against the aggregationInfoResult metrics
     *
     * @param aggrInfoClassAsMap
     * @param perfProfileAggrFunctions
     * @return
     */
    public static String validateAggFunction(Map<String, Object> aggrInfoClassAsMap, List<String> perfProfileAggrFunctions) {

        List<String> resultDataAggrFuncObjects = aggrInfoClassAsMap.keySet().stream().toList();
        String errorMsg = "";
        // check if none of the aggrfunctions are present in the aggrInfoObjects List
        if (resultDataAggrFuncObjects.stream().noneMatch(perfProfileAggrFunctions::contains)) {
            LOGGER.error(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_AGG_FUNCTION);
            errorMsg = errorMsg.concat(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_AGG_FUNCTION);
        } else if (!resultDataAggrFuncObjects.containsAll(perfProfileAggrFunctions)) {
            List<String> missingObjects = new ArrayList<>(perfProfileAggrFunctions);
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
                // Only validate format if the metric name is NOT in our skip list
                if (!METRICS_WITHOUT_EXPLICIT_FORMAT.contains(metricVariableName)) {
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
