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
package com.autotune.analyzer.performanceProfiles.PerformanceProfileInterface;

import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.performanceProfiles.utils.PerformanceProfileUtil;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.*;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.recommendation.engine.KruizeRecommendationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

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
            for (ContainerData containerData : deploymentResultData.getContainerDataMap().values()) {
                HashMap<AnalyzerConstants.MetricName, Metric> metrics = containerData.getMetrics();
                Set<AnalyzerConstants.MetricName> kruizeFunctionVariablesList = metrics.keySet();
                for (IntervalResults intervalResults : containerData.getResults().values()) {
                    for (MetricResults metricResults : intervalResults.getMetricResultsMap().values()) {
                        Map<String, Object> aggrInfoClassAsMap;
                        try {
                            // TODO: Need to update the below code
                            aggrInfoClassAsMap = PerformanceProfileUtil.convertObjectToMap(metricResults.getAggregationInfoResult());
                            LOGGER.info("aggrInfoClassAsMap: {}", aggrInfoClassAsMap);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        return "";
    }

    // TODO: Update these based on requirements, currently leaving Invalid as Default impl doesn't need engine
    @Override
    public AnalyzerConstants.RegisterRecommendationEngineStatus registerEngine(KruizeRecommendationEngine kruizeRecommendationEngine) {
        return AnalyzerConstants.RegisterRecommendationEngineStatus.INVALID;
    }


    @Override
    public List<KruizeRecommendationEngine> getEngines() {
        return null;
    }
}
