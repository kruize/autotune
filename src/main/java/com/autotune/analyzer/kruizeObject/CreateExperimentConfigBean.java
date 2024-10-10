/*******************************************************************************
 * Copyright (c) 2022, 2022 Red Hat, IBM Corporation and others.
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

import com.autotune.analyzer.serviceObjects.KubernetesAPIObject;
import com.autotune.common.k8sObjects.TrialSettings;
import com.fasterxml.jackson.annotation.JsonGetter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * THis is a placeholder class for bulkAPI createExperiment template to store defaults
 */
public class CreateExperimentConfigBean {

    // Private fields
    private String experiment_name;
    private String cluster_name;
    private List<KubernetesAPIObject> kubernetes_objects;
    private String mode;
    private String target_cluster;
    private String version;
    private String datasource;
    private String performance_profile;
    private double threshold;
    private String measurementDurationStr;
    private int measurementDuration;
    private TrialSettings trial_settings;
    private RecommendationSettings recommendation_settings;

    // Getters and Setters
    public String getExperiment_name() {
        return experiment_name;
    }

    public void setExperiment_name(String experiment_name) {
        this.experiment_name = experiment_name;
    }

    public String getCluster_name() {
        return cluster_name;
    }

    public void setCluster_name(String cluster_name) {
        this.cluster_name = cluster_name;
    }

    public List<KubernetesAPIObject> getKubernetes_objects() {
        return kubernetes_objects;
    }

    public void setKubernetes_objects(List<KubernetesAPIObject> kubernetes_objects) {
        this.kubernetes_objects = kubernetes_objects;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getTarget_cluster() {
        return target_cluster;
    }

    public void setTarget_cluster(String target_cluster) {
        this.target_cluster = target_cluster;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public String getPerformance_profile() {
        return performance_profile;
    }

    public void setPerformance_profile(String performance_profile) {
        this.performance_profile = performance_profile;
    }

    public void setTrial_settings(TrialSettings trial_settings) {
        this.trial_settings = trial_settings;
    }

    public void setRecommendation_settings(RecommendationSettings recommendation_settings) {
        this.recommendation_settings = recommendation_settings;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getMeasurementDurationStr() {
        return measurementDurationStr;
    }

    public void setMeasurementDurationStr(String measurementDurationStr) {
        this.measurementDurationStr = measurementDurationStr;
    }

    public int getMeasurementDuration() {
        return measurementDuration;
    }

    public void setMeasurementDuration(int measurementDuration) {
        this.measurementDuration = measurementDuration;
    }

    @JsonGetter("trial_settings")
    public Map<String, String> getTrialSettings() {
        Map<String, String> trialSettingsMap = new HashMap<>();
        trialSettingsMap.put("measurement_duration", this.measurementDurationStr);
        return trialSettingsMap;
    }

    @JsonGetter("recommendation_settings")
    public Map<String, Double> getRecommendationSettings() {
        Map<String, Double> recommendationSettingsMap = new HashMap<>();
        recommendationSettingsMap.put("threshold", this.threshold);
        return recommendationSettingsMap;
    }

    @Override
    public String toString() {
        return "CreateExperimentConfigBean{" +
                "experiment_name='" + experiment_name + '\'' +
                ", cluster_name='" + cluster_name + '\'' +
                ", kubernetes_objects=" + kubernetes_objects +
                ", mode='" + mode + '\'' +
                ", target_cluster='" + target_cluster + '\'' +
                ", version='" + version + '\'' +
                ", datasource='" + datasource + '\'' +
                ", performance_profile='" + performance_profile + '\'' +
                ", measurementDurationStr='" + measurementDurationStr + '\'' +
                ", trial_settings=" + trial_settings +
                ", recommendation_settings=" + recommendation_settings +
                '}';
    }
}
