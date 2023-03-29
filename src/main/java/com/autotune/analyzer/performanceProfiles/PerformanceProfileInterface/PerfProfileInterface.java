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
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.recommendation.engine.KruizeRecommendationEngine;

import java.util.List;
import java.util.Map;

/**
 * Abstraction layer containing methods for the validation of the Performance Profiles with respect to the updated experiment.
 * and to parse the objective function data.
 */
public interface PerfProfileInterface {
// name, validateResults, validateProfile, recommend
    String getName(PerformanceProfile profile);
    String recommend(PerformanceProfile performanceProfile, ExperimentResultData experimentResultData);
    void recommend(KruizeObject kruizeObject);
}
