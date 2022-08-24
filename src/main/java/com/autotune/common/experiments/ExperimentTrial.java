/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.experiments;

import com.autotune.common.k8sObjects.Metric;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * A storage object, which is used to feed information about trial details
 * for Experiment manager to perform experiments suggested by Analyser.
 */
public class ExperimentTrial {
    @SerializedName("experiment_name")
    private final String experimentName;
    /**
     * Mode is used to tell how Experiment should be performed using following values
     * Apply -> THis is will apply config and collect Metrics.
     * CollectMetrics -> This will just collect metrics without applying config.
     */
    @SerializedName("mode")
    private final String mode;
    /**
     * Environment is used to indicate QA or PROD
     * By default PRODUCTION environed doest not wait for load to applied
     * Whereas in QA environment EM waits for USER to apply load.
     */
    @SerializedName("environment")
    private final String environment;
    /**
     * Namespace and Deployment Name will be mentioned here.
     */
    @SerializedName("resource")
    private final ResourceDetails resourceDetails;
    @SerializedName("experiment_id")
    private final String experimentId;
    /**
     * Key : metric name
     * Value : Query
     */
    @SerializedName("pod_metrics")
    private final HashMap<String, Metric> podMetricsHashMap;
    /**
     * Key : Container Name
     * Value : hashmap of Metrics having
     * Key : metric name
     * Value : Query
     */
    @SerializedName("container_metrics")
    private final HashMap<String, HashMap<String, Metric>> containerMetricsHashMap;
    /**
     * Current active Trial number for tracking purpose.
     */
    @SerializedName("trial_info")
    private final TrialInfo trialInfo;
    /**
     * Key : Metric collection name which will be used in metrics attribute for reference.
     * Value : Metric collection utility details like prometheus, cAdvisor, fluent
     */
    @SerializedName("datasource_info")
    private final HashMap<String, DatasourceInfo> datasourceInfoHashMap;
    /**
     * Detailed settings for Trial run and deployment
     */
    @SerializedName("settings")
    private final ExperimentSettings experimentSettings;
    /**
     * Key : Trial number/ or Any alphanumeric
     * Value : Configs containing Requests,Limits,Env etc
     */
    @SerializedName("trials")
    private final HashMap<String, TrialDetails> trialDetails;
    // HashMap of parallel trials being monitored for this trial
    // Eg. training and production
    // uses tracker as key. tracker = "training" or "production"

    public ExperimentTrial(String experimentName,
                           String mode, String environment, ResourceDetails resourceDetails, String experimentId,
                           HashMap<String, Metric> podMetricsHashMap, HashMap<String, HashMap<String, Metric>> containerMetricsHashMap, TrialInfo trialInfo,
                           HashMap<String, DatasourceInfo> datasourceInfoHashMap,
                           ExperimentSettings experimentSettings,
                           HashMap<String, TrialDetails> trialDetails) {
        this.mode = mode;
        this.environment = environment;
        this.resourceDetails = resourceDetails;
        this.experimentId = experimentId;
        this.experimentName = experimentName;
        this.podMetricsHashMap = podMetricsHashMap;
        this.containerMetricsHashMap = containerMetricsHashMap;
        this.trialInfo = trialInfo;
        this.datasourceInfoHashMap = datasourceInfoHashMap;
        this.experimentSettings = experimentSettings;
        this.trialDetails = trialDetails;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public ResourceDetails getResourceDetails() {
        return resourceDetails;
    }

    public HashMap<String, DatasourceInfo> getDatasourceInfoHashMap() {
        return datasourceInfoHashMap;
    }

    public TrialInfo getTrialInfo() {
        return trialInfo;
    }

    public ExperimentSettings getExperimentSettings() {
        return experimentSettings;
    }

    public HashMap<String, TrialDetails> getTrialDetails() {
        return trialDetails;
    }

    public HashMap<String, Metric> getPodMetricsHashMap() {
        return podMetricsHashMap;
    }

    public HashMap<String, HashMap<String, Metric>> getContainerMetricsHashMap() {
        return containerMetricsHashMap;
    }

    public String getMode() {
        return mode;
    }

    public String getEnvironment() {
        return environment;
    }

    @Override
    public String toString() {
        return "ExperimentTrial{" +
                "experimentName='" + experimentName + '\'' +
                ", mode='" + mode + '\'' +
                ", environment='" + environment + '\'' +
                ", resourceDetails=" + resourceDetails +
                ", experimentId='" + experimentId + '\'' +
                ", podMetricsHashMap=" + podMetricsHashMap +
                ", containerMetricsHashMap=" + containerMetricsHashMap +
                ", trialDetails=" + trialDetails +
                ", trialInfo=" + trialInfo +
                ", datasourceInfoHashMap=" + datasourceInfoHashMap +
                ", experimentSettings=" + experimentSettings +
                '}';
    }


}



