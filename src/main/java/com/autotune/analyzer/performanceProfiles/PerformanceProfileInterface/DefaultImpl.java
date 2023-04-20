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

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.performanceProfiles.utils.PerformanceProfileUtil;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.*;
import com.autotune.common.data.result.ContainerData;
import com.autotune.analyzer.recommendations.engine.KruizeRecommendationEngine;
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

    @Override
    public void generateRecommendation(KruizeObject kruizeObject, ExperimentResultData experimentResultData) {

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
