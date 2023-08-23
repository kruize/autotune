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
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.k8sObjects.K8sObject;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class PerformanceProfileUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceProfileUtil.class);
    private PerformanceProfileUtil() {

    }

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
     * @param performanceProfile
     * @param experimentResultData
     * @return
     */
    public static String validateResults(PerformanceProfile performanceProfile, ExperimentResultData experimentResultData) {
        String errorMsg = "";
        List<AnalyzerConstants.MetricName> mandatoryFields = Arrays.asList(
                AnalyzerConstants.MetricName.cpuUsage,
                AnalyzerConstants.MetricName.memoryUsage,
                AnalyzerConstants.MetricName.memoryRSS);
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

        // Get the metrics data from the Kruize Object
        for (K8sObject k8sObject : experimentResultData.getKubernetes_objects()) {
            for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
                HashMap<AnalyzerConstants.MetricName, Metric> metrics = containerData.getMetrics();
                List<AnalyzerConstants.MetricName> kruizeFunctionVariablesList = metrics.keySet().stream().toList();
                LOGGER.debug("perfProfileFunctionVariablesList: {}", perfProfileFunctionVariablesList);
                LOGGER.debug("kruizeFunctionVariablesList: {}", kruizeFunctionVariablesList);
                if (!kruizeFunctionVariablesList.containsAll(mandatoryFields)) {
                    errorMsg = errorMsg.concat(String.format("Missing one of the following mandatory parameters for experiment - %s : %s", experimentResultData.getExperiment_name(), mandatoryFields));
                    break;
                }
                for (IntervalResults intervalResults : containerData.getResults().values()) {
                    for (MetricResults metricResults : intervalResults.getMetricResultsMap().values()) {
                        Map<String, Object> aggrInfoClassAsMap;
                        if (!perfProfileAggrFunctions.isEmpty()) {
                            try {
                                aggrInfoClassAsMap = convertObjectToMap(metricResults.getAggregationInfoResult());
                                errorMsg = validateAggFunction(aggrInfoClassAsMap.keySet(), perfProfileAggrFunctions);
                                if (!errorMsg.isBlank()) {
                                    errorMsg = errorMsg.concat(String.format("for the experiment : %s"
                                            , experimentResultData.getExperiment_name()));
                                    break;
                                }
                            } catch(IllegalAccessException | InvocationTargetException e){
                                throw new RuntimeException(e);
                            }
                        } else{
                            // check if query is also absent
                            if (queryList.isEmpty()) {
                                errorMsg = AnalyzerErrorConstants.AutotuneObjectErrors.QUERY_FUNCTION_MISSING;
                                break;
                            }
                        }
                        // check if the 'value' is present in the result JSON
                        if (null == metricResults.getValue()) {
                            LOGGER.debug(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_VALUE.concat(metricResults.getName()));
                        }
                    }
                    if (!errorMsg.isBlank())
                        break;
                }
                if (!errorMsg.isBlank())
                    break;
            }
        }
        return errorMsg;
    }

    public static void addPerformanceProfile(Map<String, PerformanceProfile> performanceProfileMap, PerformanceProfile performanceProfile) {
        performanceProfileMap.put(performanceProfile.getName(), performanceProfile);
        LOGGER.debug("Added PerformanceProfile: {} ",performanceProfile.getName());
    }

    /**
     * Validates the aggregation function objects against the aggregationInfoResult metrics
     *
     * @param keySet
     * @param perfProfileAggrFunctions
     * @return
     */
    private static String validateAggFunction(Set<String> keySet, List<String> perfProfileAggrFunctions) {

        List<String> resultDataAggrFuncObjects = keySet.stream().toList();
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
                if (value instanceof Double)
                    map.put(m.getName().substring(3).toLowerCase(), value);
            }
        }
        return map;
    }
}
