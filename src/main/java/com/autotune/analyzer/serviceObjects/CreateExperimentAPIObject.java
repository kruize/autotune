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

import com.autotune.analyzer.kruizeObject.RecommendationSettings;
import com.autotune.analyzer.kruizeObject.SloInfo;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.k8sObjects.TrialSettings;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Simulating the KruizeObject class for the CreateExperiment API
 */
public class CreateExperimentAPIObject extends BaseSO {
    @SerializedName(KruizeConstants.JSONKeys.CLUSTER_NAME)
    private String clusterName;
    @SerializedName(KruizeConstants.JSONKeys.PERFORMANCE_PROFILE)
    private String performanceProfile;
    @SerializedName("slo")
    private SloInfo sloInfo;
    @SerializedName(KruizeConstants.JSONKeys.MODE)
    private String mode;
    @SerializedName(KruizeConstants.JSONKeys.TARGET_CLUSTER)
    private String targetCluster;
    @SerializedName(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS)
    private List<KubernetesAPIObject> kubernetesAPIObjects;
    @SerializedName(KruizeConstants.JSONKeys.TRIAL_SETTINGS)
    private TrialSettings trialSettings;
    @SerializedName(KruizeConstants.JSONKeys.RECOMMENDATION_SETTINGS)
    private RecommendationSettings recommendationSettings;
    @SerializedName(KruizeConstants.JSONKeys.DATASOURCE) //TODO: to be used in future
    private String datasource;
    private AnalyzerConstants.ExperimentStatus status;
    private String experiment_id;   // this id is UUID and getting set at createExperiment API
    private ValidationOutputData validationData;  // This object indicates if this API object is valid or invalid

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

    public List<KubernetesAPIObject> getKubernetesObjects() {
        return kubernetesAPIObjects;
    }

    public void setKubernetesObjects(List<KubernetesAPIObject> kubernetesAPIObjects) {
        this.kubernetesAPIObjects = kubernetesAPIObjects;
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

    public SloInfo getSloInfo() {
        return sloInfo;
    }

    public void setSloInfo(SloInfo sloInfo) {
        this.sloInfo = sloInfo;
    }

    public AnalyzerConstants.ExperimentStatus getStatus() {
        return status;
    }

    public void setStatus(AnalyzerConstants.ExperimentStatus status) {
        this.status = status;
    }

    public String getExperiment_id() {
        return experiment_id;
    }

    public void setExperiment_id(String experiment_id) {
        this.experiment_id = experiment_id;
    }

    public ValidationOutputData getValidationData() {
        return validationData;
    }

    public void setValidationData(ValidationOutputData validationData) {
        this.validationData = validationData;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    @Override
    public String toString() {
        return "CreateExperimentAPIObject{" +
                "experimentName='" + getExperimentName() + '\'' +
                "apiVersion='" + getApiVersion() + '\'' +
                "clusterName='" + clusterName + '\'' +
                ", performanceProfile='" + performanceProfile + '\'' +
                ", sloInfo=" + sloInfo +
                ", mode='" + mode + '\'' +
                ", targetCluster='" + targetCluster + '\'' +
                ", kubernetesAPIObjects=" + kubernetesAPIObjects.toString() +
                ", trialSettings=" + trialSettings +
                ", recommendationSettings=" + recommendationSettings +
                '}';
    }
}

