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
package com.autotune.analyzer.serviceObjects;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.ExperimentTypeAware;
import com.autotune.analyzer.utils.ExperimentTypeUtil;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ListRecommendationsAPIObject extends BaseSO implements ExperimentTypeAware {
    @SerializedName(KruizeConstants.JSONKeys.CLUSTER_NAME)
    private String clusterName;
    @SerializedName(KruizeConstants.JSONKeys.EXPERIMENT_TYPE)
    @JsonAdapter(ExperimentTypeUtil.ExperimentTypeSerializer.class)
    private AnalyzerConstants.ExperimentType experimentType;

    @SerializedName(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS)
    private List<KubernetesAPIObject> kubernetesObjects;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public List<KubernetesAPIObject> getKubernetesObjects() {
        return kubernetesObjects;
    }

    public void setKubernetesObjects(List<KubernetesAPIObject> kubernetesObjects) {
        this.kubernetesObjects = kubernetesObjects;
    }

    public AnalyzerConstants.ExperimentType getExperimentType() {
        return experimentType;
    }

    public void setExperimentType(AnalyzerConstants.ExperimentType experimentType) {
        this.experimentType = experimentType;
    }

    @Override
    public boolean isNamespaceExperiment() {
        return ExperimentTypeUtil.isNamespaceExperiment(experimentType);
    }

    @Override
    public boolean isContainerExperiment() {
        return ExperimentTypeUtil.isContainerExperiment(experimentType);
    }


}
