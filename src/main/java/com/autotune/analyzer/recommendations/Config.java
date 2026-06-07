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
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class Config {
    @SerializedName(KruizeConstants.JSONKeys.REPLICAS)
    private Integer replicas;

    // resources is a Map of map which wraps requests and limits.
    // New API endpoint make use of this to nest requests and limits under resources in its response.
    // In such scenario, requests and limits are set to null
    @SerializedName(KruizeConstants.JSONKeys.RESOURCES)
    private Map<AnalyzerConstants.ResourceSetting, Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> resources;

    // Existing API endpoints use requests and limits as-is
    // In such scenario, resources is set to null.
    private Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requests;
    private Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limits;
    private List<RecommendationConfigEnv> env;

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    public Map<AnalyzerConstants.ResourceSetting, Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> getResources() {
        return resources;
    }

    public void setResources(Map<AnalyzerConstants.ResourceSetting, Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> resources) {
        this.resources = resources;
        if (resources != null) {
            // When using the nested "resources" representation, keep the flat representation unset
            this.requests = null;
            this.limits = null;
        }
    }

    public Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> getRequests() {
        return requests;
    }

    public void setRequests(Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requests) {
        this.requests = requests;
        if (requests != null) {
            // When using the flat "requests"/"limits" representation, keep the nested "resources" unset
            this.resources = null;
        }
    }

    public Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> getLimits() {
        return limits;
    }

    public void setLimits(Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limits) {
        this.limits = limits;
        if (limits != null) {
            // When using the flat "requests"/"limits" representation, keep the nested "resources" unset
            this.resources = null;
        }
    }

    public List<RecommendationConfigEnv> getEnv() {
        return env;
    }

    public void setEnv(List<RecommendationConfigEnv> env) {
        this.env = env;
    }
}
