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

import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.common.k8sObjects.RecommendationSettings;
import com.autotune.common.k8sObjects.TrialSettings;
import com.autotune.utils.AutotuneConstants;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CreateExperimentSO extends BaseSO {
    @SerializedName(AutotuneConstants.JSONKeys.CLUSTER_NAME)
    private String clusterName;
    @SerializedName(AutotuneConstants.JSONKeys.PERFORMANCE_PROFILE)
    private String performanceProfile;
    @SerializedName(AutotuneConstants.JSONKeys.MODE)
    private String mode;
    @SerializedName(AutotuneConstants.JSONKeys.TARGET_CLUSTER)
    private String targetCluster;
    @SerializedName(AutotuneConstants.JSONKeys.KUBERNETES_OBJECTS)
    private List<K8sObject> kubernetesObjects;
    @SerializedName(AutotuneConstants.JSONKeys.TRIAL_SETTINGS)
    private TrialSettings trialSettings;
    @SerializedName(AutotuneConstants.JSONKeys.RECOMMENDATION_SETTINGS)
    private RecommendationSettings recommendationSettings;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getPerformanceProfile() {
        return performanceProfile;
    }

    public void setPerformanceProfile(String performanceProfile) {
        this.performanceProfile = performanceProfile;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getTargetCluster() {
        return targetCluster;
    }

    public void setTargetCluster(String targetCluster) {
        this.targetCluster = targetCluster;
    }

    public List<K8sObject> getKubernetesObjects() {
        return kubernetesObjects;
    }

    public void setKubernetesObjects(List<K8sObject> kubernetesObjects) {
        this.kubernetesObjects = kubernetesObjects;
    }

    public TrialSettings getTrialSettings() {
        return trialSettings;
    }

    public void setTrialSettings(TrialSettings trialSettings) {
        this.trialSettings = trialSettings;
    }

    public RecommendationSettings getRecommendationSettings() {
        return recommendationSettings;
    }

    public void setRecommendationSettings(RecommendationSettings recommendationSettings) {
        this.recommendationSettings = recommendationSettings;
    }
}
