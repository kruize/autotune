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

public class ProfileBasedRecommendationEngine implements KruizeRecommendationEngine{
    private String name;
    private String key;
    private AnalyzerConstants.RecommendationCategory category;

    public ProfileBasedRecommendationEngine() {
        this.name = AnalyzerConstants.RecommendationEngine.EngineNames.PROFILE_BASED;
        this.key = AnalyzerConstants.RecommendationEngine.EngineKeys.PROFILE_BASED_KEY;
        this.category = AnalyzerConstants.RecommendationCategory.PROFILE_BASED;
    }

    public ProfileBasedRecommendationEngine(String name) {
        this.name = name;
    }

    @Override
    public String getEngineName() {
        return this.name;
    }

    @Override
    public String getEngineKey() {
        return this.key;
    }

    @Override
    public AnalyzerConstants.RecommendationCategory getEngineCategory() {
        return this.category;
    }

    @Override
    public HashMap<String, Recommendation> getRecommendations(ContainerData containerData, Timestamp monitoringEndTime) {
        // TODO: Needs to be implemented
        return null;
    }
}
