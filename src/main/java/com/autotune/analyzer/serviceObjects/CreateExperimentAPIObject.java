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

import com.autotune.analyzer.kruizeObject.SloInfo;
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.common.data.metrics.Metric;
import com.autotune.analyzer.kruizeObject.RecommendationSettings;
import com.autotune.common.k8sObjects.TrialSettings;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.List;

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
    private List<KubernetesObject> kubernetesObjects;
    @SerializedName(KruizeConstants.JSONKeys.TRIAL_SETTINGS)
    private TrialSettings trialSettings;
    @SerializedName(KruizeConstants.JSONKeys.RECOMMENDATION_SETTINGS)
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

    public List<KubernetesObject> getKubernetesObjects() {
        return kubernetesObjects;
    }

    public void setKubernetesObjects(List<KubernetesObject> kubernetesObjects) {
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

    public SloInfo getSloInfo() {
        return sloInfo;
    }

    public void setSloInfo(SloInfo sloInfo) {
        this.sloInfo = sloInfo;
    }

    @Override
    public String toString() {
        return "CreateExperimentSO{" +
                "clusterName='" + clusterName + '\'' +
                ", performanceProfile='" + performanceProfile + '\'' +
                ", mode='" + mode + '\'' +
                ", targetCluster='" + targetCluster + '\'' +
                ", kubernetesObjects=" + kubernetesObjects +
                ", trialSettings=" + trialSettings +
                ", recommendationSettings=" + recommendationSettings +
                '}';
    }
}
class KubernetesObject {
    private String type;
    private String name;
    private String namespace;
    private List<Container> containers;

    public KubernetesObject(String name, String type, String namespace) {
        this.name = name;
        this.type = type;
        this.namespace = namespace;
    }

    // getters and setters

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public List<Container> getContainers() {
        return containers;
    }

    public void setContainers(List<Container> containers) {
        this.containers = containers;
    }

    @Override
    public String toString() {
        return "KubernetesObject{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", containers=" + containers +
                '}';
    }
}

class Container {
    private String container_image_name;
    private String container_name;
    private ContainerRecommendations containerRecommendations;
    private List<Metric> metrics;

    public Container(String container_name, String container_image_name, ContainerRecommendations containerRecommendations, List<Metric> metrics) {
        this.container_name = container_name;
        this.containerRecommendations = containerRecommendations;
        this.container_image_name = container_image_name;
        this.metrics = metrics;
    }

    public String getContainer_image_name() {
        return container_image_name;
    }

    public String getContainer_name() {
        return container_name;
    }

    public ContainerRecommendations getContainerRecommendations() {
        return containerRecommendations;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        return "Container{" +
                "container_image_name='" + container_image_name + '\'' +
                ", container_name='" + container_name + '\'' +
                ", containerRecommendations=" + containerRecommendations +
                ", metrics=" + metrics +
                '}';
    }
}
