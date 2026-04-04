/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

package com.autotune.analyzer.recommendations;

import com.autotune.analyzer.utils.AnalyzerConstants;

import java.util.List;
import java.util.Map;

public class Config {
    private Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requests;
    private Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limits;
    private List<RecommendationConfigEnv> env;

    public Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> getRequests() {
        return requests;
    }

    public void setRequests(Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requests) {
        this.requests = requests;
    }

    public Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> getLimits() {
        return limits;
    }

    public void setLimits(Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limits) {
        this.limits = limits;
    }

    public List<RecommendationConfigEnv> getEnv() {
        return env;
    }

    public void setEnv(List<RecommendationConfigEnv> env) {
        this.env = env;
    }
}
