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

import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ListRecommendationsAPIObject extends BaseSO{
    @SerializedName(KruizeConstants.JSONKeys.CLUSTER_NAME)
    private String clusterName;
    @SerializedName(KruizeConstants.JSONKeys.EXPERIMENT_TYPE)
    private String experiment_type;

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

    public String getExperimentType() {
        return experiment_type;
    }

    public void setExperimentType(String experiment_type) {
        this.experiment_type = experiment_type;
    }
}
