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

import com.autotune.analyzer.serviceObjects.ContainerMetricsHelper;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.*;
import com.autotune.common.k8sObjects.ContainerObject;
import com.autotune.common.performanceProfiles.PerformanceProfile;
import com.autotune.utils.AnalyzerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Util class to validate the performance profile metrics with the experiment results metrics.
 */
public class DefaultImpl extends PerfProfileImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultImpl.class);

    private String validateValues(HashMap<String, MetricResults> funcVar, List<String> funcVarValueTypes) {
        LOGGER.info("Func variables : {}", funcVar);
        LOGGER.info("Func variable value types : {}", funcVarValueTypes);
        return "";
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
            for (ContainerObject containerObject : deploymentResultData.getContainerObjects()) {
                List<ContainerMetricsHelper> containerMetricsHelpers = containerObject.getMetrics();
                List<String> kruizeFunctionVariablesList = containerMetricsHelpers.stream().map(ContainerMetricsHelper::getName)
                        .collect(Collectors.toCollection(ArrayList::new));
                for (ContainerMetricsHelper containerMetricsHelper : containerMetricsHelpers) {
                    Map<String, Object> aggrInfoClassAsMap;
                    try {
                        // TODO: Need to update the below code
                        aggrInfoClassAsMap = DefaultImpl.convertObjectToMap(containerMetricsHelper.getMetricResults().getAggregationInfoResult());
                       LOGGER.info("aggrInfoClassAsMap: {}", aggrInfoClassAsMap);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return "";
    }
}
