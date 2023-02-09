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
package com.autotune.common.performanceProfiles.PerformanceProfileInterface;

import com.autotune.common.data.result.Containers;
import com.autotune.common.data.result.DeploymentResultData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.data.result.Results;
import com.autotune.common.k8sObjects.KruizeObject;
import com.autotune.common.k8sObjects.Metric;
import com.autotune.common.performanceProfiles.PerformanceProfile;
import com.autotune.common.performanceProfiles.PerformanceProfilesDeployment;
import com.autotune.utils.AnalyzerConstants;
import com.autotune.utils.AnalyzerErrorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Util class to validate the performance profile metrics with the experiment results metrics.
 */
public class RemoteMonitoringOpenShiftImpl implements PerfProfileInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteMonitoringOpenShiftImpl.class);

    /**
     * Converts the generalInfo class into Map to extract values for validation
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

    @Override
    public String validate(KruizeObject kruizeObject, ExperimentResultData experimentResultData) {

        String errorMsg = "";
        // Get the metrics data from the Performance Profile
        PerformanceProfile performanceProfile = PerformanceProfilesDeployment.performanceProfilesMap
                .get(kruizeObject.getPerformanceProfile());
        List<String> aggrFunctionsObjects = new ArrayList<>();
        List<String> perfProfileFunctionVariablesList = new ArrayList<>();
        for (Metric metric : performanceProfile.getSloInfo().getFunctionVariables()) {
            perfProfileFunctionVariablesList.add(metric.getName());
            metric.getAggregationFunctions().forEach(aggregationFunctions ->
                    aggrFunctionsObjects.add(aggregationFunctions.getFunction()));
        }
        LOGGER.debug(String.format("List of functionVariables: %s", perfProfileFunctionVariablesList));
        LOGGER.debug(String.format("List of agg func objects: %s", aggrFunctionsObjects));

        // Get the metrics data from the Kruize Object
        for (DeploymentResultData deploymentResultData : experimentResultData.getDeployments()) {
            for (Containers containers : deploymentResultData.getContainers()) {
                HashMap<AnalyzerConstants.MetricName, HashMap<String, Results>> containerMetricsMap =
                        containers.getContainer_metrics();
                List<String> kruizeFunctionVariablesList = containerMetricsMap.keySet().stream().toList().stream().map(Enum::name).toList();
                if (!(perfProfileFunctionVariablesList.size() == kruizeFunctionVariablesList.size() &&
                        new HashSet<>(perfProfileFunctionVariablesList).containsAll(kruizeFunctionVariablesList) &&
                        new HashSet<>(kruizeFunctionVariablesList).containsAll(perfProfileFunctionVariablesList))) {
                    LOGGER.debug("perfProfileFunctionVariablesList: {}", perfProfileFunctionVariablesList);
                    LOGGER.debug("kruizeFunctionVariablesList: {}", kruizeFunctionVariablesList);
                    perfProfileFunctionVariablesList.removeAll(kruizeFunctionVariablesList);
                    errorMsg = errorMsg.concat(String.format("Following Performance Profile parameters are missing for experiment - %s : %s", experimentResultData.getExperiment_name(), perfProfileFunctionVariablesList));
                    break;
                } else {
                    for (HashMap<String, Results> funcVar : containerMetricsMap.values()) {
                        Map<String, Object> genInfoClassAsMap;
                        try {
                            genInfoClassAsMap = RemoteMonitoringOpenShiftImpl.convertObjectToMap(funcVar.get("results").getAggregation_info());
                            errorMsg = validateAggFunction(genInfoClassAsMap.keySet(), aggrFunctionsObjects);
                            if (!errorMsg.isBlank()) {
                                errorMsg = errorMsg.concat(String.format("for the experiment : %s"
                                        , experimentResultData.getExperiment_name()));
                                return errorMsg;
                            }
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        return errorMsg;
    }

    /**
     * Validates the aggregation function objects against the generalInfo metrics
     *
     * @param keySet
     * @param aggrFunctionsObjects
     * @return
     */
    private String validateAggFunction(Set<String> keySet, List<String> aggrFunctionsObjects) {

        List<String> genInfoObjects = keySet.stream().toList();
        List<String> missingAggFunction = new ArrayList<>();
        String errorMsg = "";
        // check if none of the aggrfunctions are present in the genInfo List
        if (genInfoObjects.stream().noneMatch(aggrFunctionsObjects::contains)) {
            LOGGER.error(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_AGG_FUNCTION);
            errorMsg = errorMsg.concat(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_AGG_FUNCTION);
        } else {
            // check if some or all the values are present or not and respond accordingly
            for (String aggFuncObj : aggrFunctionsObjects) {
                if (!genInfoObjects.contains(aggFuncObj)) {
                    missingAggFunction.add(aggFuncObj);
                }
            }
            if (!missingAggFunction.isEmpty()) {
                LOGGER.warn("Missing Aggregation Functions: {}", missingAggFunction);
            }
        }
        return errorMsg;
    }

    @Override
    public void recommend() {

        //TODO: Will be updated once algo is completed

    }
}
