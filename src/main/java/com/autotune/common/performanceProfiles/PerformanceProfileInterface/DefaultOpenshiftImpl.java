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

import com.autotune.common.data.result.AggregationInfoResult;
import com.autotune.common.data.result.ContainerResultData;
import com.autotune.common.data.result.DeploymentResultData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.k8sObjects.Metric;
import com.autotune.common.performanceProfiles.PerformanceProfile;
import com.autotune.utils.AnalyzerErrorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Util class to validate the performance profile metrics with the experiment results metrics.
 */
public class DefaultOpenshiftImpl implements PerfProfileInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOpenshiftImpl.class);
    List<String> perfProfileFunctionVariablesList = new ArrayList<>();

    @Override
    public String validate(PerformanceProfile performanceProfile, ExperimentResultData experimentResultData) {

        String errorMsg = "";
        // Get the metrics data from the Performance Profile
        List<String> aggrFunctionsObjects = new ArrayList<>();
        for (Metric metric:performanceProfile.getSloInfo().getFunctionVariables()) {
            perfProfileFunctionVariablesList.add(metric.getName());
            metric.getAggregationFunctions().forEach(aggregationFunctions ->
                    aggrFunctionsObjects.add(aggregationFunctions.getFunction()));
        }
        LOGGER.debug(String.format("List of functionVariables: %s", perfProfileFunctionVariablesList));
        LOGGER.debug(String.format("List of agg func objects: %s", aggrFunctionsObjects));

        // Get the metrics data from the Kruize Object
        for (DeploymentResultData deploymentResultData : experimentResultData.getDeployments()) {
            for (ContainerResultData containerResultData : deploymentResultData.getContainers()) {
                HashMap<String, HashMap<String, HashMap<String, AggregationInfoResult>>> containerMetricsMap =
                        containerResultData.getContainer_metrics();
                List<String> kruizeFunctionVariablesList = containerMetricsMap.keySet().stream().toList();
                if (!(perfProfileFunctionVariablesList.size() == kruizeFunctionVariablesList.size() &&
                    new HashSet<>(perfProfileFunctionVariablesList).containsAll(kruizeFunctionVariablesList) &&
                    new HashSet<>(kruizeFunctionVariablesList).containsAll(perfProfileFunctionVariablesList))) {
                    LOGGER.debug("perfProfileFunctionVariablesList: {}",perfProfileFunctionVariablesList);
                    LOGGER.debug("kruizeFunctionVariablesList: {}",kruizeFunctionVariablesList);
                    perfProfileFunctionVariablesList.removeAll(kruizeFunctionVariablesList);
                    errorMsg = errorMsg.concat(String.format("Following Performance Profile parameters are missing for experiment - %s : %s", experimentResultData.getExperiment_name(), perfProfileFunctionVariablesList));
                    break;
                } else  {
                    for(HashMap<String, HashMap<String, AggregationInfoResult>> funcVar:containerMetricsMap.values()){
                        for(HashMap<String, AggregationInfoResult> genInfo:funcVar.values()){
                          Map<String, Object> genInfoClassAsMap;
                          for(AggregationInfoResult genInfoObj:genInfo.values()){
                              try {
                                genInfoClassAsMap = DefaultOpenshiftImpl.convertObjectToMap(genInfoObj);
                                errorMsg = validateAggFunction(genInfoClassAsMap.keySet(), aggrFunctionsObjects);
                                if (!errorMsg.isBlank()) {
                                    errorMsg = errorMsg.concat(String.format("for the experiment : %s"
                                            ,experimentResultData.getExperiment_name()));
                                    return errorMsg;
                                }
                              } catch (IllegalAccessException | InvocationTargetException e) {
                                  throw new RuntimeException(e);
                              }
                          }
                      }
                    }
                }
            }
        }

        return errorMsg;
    }

    /**
     * Calculates the objective function by calling the algebraic parser library. The result is then sent to HPO.
     * @param performanceProfile
     * @param experimentResultData
     * @return
     */
    @Override
    public String recommend(PerformanceProfile performanceProfile, ExperimentResultData experimentResultData) {

        String objectiveFunction = performanceProfile.getSloInfo().getObjectiveFunction().getExpression();
        Map<String, String> objFunctionMap = new HashMap<>();
        String errorMsg = "";

        // Get the metrics data from the Kruize Object
        for (DeploymentResultData deploymentResultData : experimentResultData.getDeployments()) {
            for (ContainerResultData containers : deploymentResultData.getContainers()) {
                HashMap<String, HashMap<String, HashMap<String, AggregationInfoResult>>> containerMetricsMap =
                        containers.getContainer_metrics();
                List<String> kruizeFunctionVariablesList = containerMetricsMap.keySet().stream().toList();
                for(HashMap<String, HashMap<String, AggregationInfoResult>> funcVar:containerMetricsMap.values()){
                    for(HashMap<String, AggregationInfoResult> aggregationInfoResultMap:funcVar.values()){
                        Map<String, Object> aggrInfoClassAsMap;
                        for(AggregationInfoResult aggregationInfoResult:aggregationInfoResultMap.values()){
                            try {
                                // TODO: Need to update the below code
                                aggrInfoClassAsMap = DefaultOpenshiftImpl.convertObjectToMap(aggregationInfoResult);
                               LOGGER.info("aggrInfoClassAsMap: {}", aggrInfoClassAsMap);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }

            }
        }

        return objectiveFunction;
    }
    /**
     * Validates the aggregation function objects against the generalInfo metrics
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

    /**
     *  Converts the generalInfo class into Map to extract values for validation
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
