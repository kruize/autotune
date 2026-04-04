/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

package com.autotune.analyzer.recommendations.v1;

import com.autotune.analyzer.recommendations.Config;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

/**
 * ConfigV1 extends Config to add v1-specific fields for the new API endpoint.
 * Adds replicas field and provides resources as a nested structure.
 */
public class ConfigV1 extends Config {
    
    @SerializedName(KruizeConstants.JSONKeys.REPLICAS)
    private Integer replicas;
    
    @SerializedName(KruizeConstants.JSONKeys.RESOURCES)
    private HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> resources;

    public ConfigV1() {
        super();
        this.resources = new HashMap<>();
    }

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    public HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> getResources() {
        return resources;
    }

    public void setResources(HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> resources) {
        this.resources = resources;
    }
}
