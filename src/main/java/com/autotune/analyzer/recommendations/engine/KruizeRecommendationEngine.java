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
package com.autotune.analyzer.recommendations.engine;

import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.result.ContainerData;

import java.sql.Timestamp;
import java.util.HashMap;

public interface KruizeRecommendationEngine {
    public String getEngineName();
    public String getEngineKey();
    public AnalyzerConstants.RecommendationCategory getEngineCategory();
    public HashMap<String, Recommendation> generateRecommendation(ContainerData containerData, Timestamp monitoringEndTime);
    public boolean checkIfMinDataAvailable(ContainerData containerData);
}
