/*******************************************************************************
 * Copyright (c) 2025 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.kruizeObject;

import java.util.List;
import java.util.Map;
import com.autotune.analyzer.recommendations.model.RecommendationTunables;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

public class ModelSettings {

    private List<String> models;

    @SerializedName(KruizeConstants.JSONKeys.MODEL_TUNABLE)
    private Map<String, RecommendationTunables> modelTunable;


    public ModelSettings() {}

    public List<String> getModels() {
        return models;
    }

    public void setModels(List<String> models) {
        this.models = models;
    }

    public Map<String, RecommendationTunables> getModelTunable() {
        return modelTunable;
    }

    public void setModelTunable(Map<String, RecommendationTunables> modelTunable) {
        this.modelTunable = modelTunable;
    }

    @Override
    public String toString() {
        return "ModelSettings{" +
                "models=" + models +
                ", modelTunable=" + modelTunable +
                '}';
    }

}
