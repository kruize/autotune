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

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

/**
 * A storage object, which is used to feed information about trial details for Experiment manager to perform experiments suggested by Analyser.
 */
public class ExperimentTrial {
    @SerializedName("experiment_id")
    private final String experimentId;
    private final String namespace;
    @SerializedName("experiment_name")
    private final String experimentName;
    @SerializedName("trial_info")
    private final TrialInfo trialInfo;
    @SerializedName("datasource_info")
    private final DatasourceInfo datasourceInfo;
    @SerializedName("settings")
    private final ExperimentSettings experimentSettings;
    // HashMap of parallel trials being monitored for this trial
    // uses tracker as key. tracker = "training" or "production"
    /**
     * Example
     * "deployments": {
     *      "training": {
     *                      "pod_metrics": {...},
     *                      "deployment_name": "tfb-qrh-sample",
     *                      "namespace": "default",
     *                      "containers": {...},
     *                      "type": "training"
     *      }
     * }
     */
    @SerializedName("deployments")
    private final HashMap<String, TrialDetails> trialDetails;

    public ExperimentTrial(String experimentName,
                           String experimentId,
                           String namespace,
                           TrialInfo trialInfo,
                           DatasourceInfo datasourceInfo,
                           ExperimentSettings experimentSettings,
                           HashMap<String, TrialDetails> trialDetails) {
        this.experimentId = experimentId;
        this.namespace = namespace;
        this.experimentName = experimentName;
        this.trialInfo = trialInfo;
        this.datasourceInfo = datasourceInfo;
        this.experimentSettings = experimentSettings;
        this.trialDetails = trialDetails;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public DatasourceInfo getDatasourceInfo() {
        return datasourceInfo;
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

    @Override
    public String toString() {
        return "ExperimentTrial{" +
                "experimentId='" + experimentId + '\'' +
                ", namespace='" + namespace + '\'' +
                ", experimentName='" + experimentName + '\'' +
                ", trialInfo=" + trialInfo +
                ", datasourceInfo=" + datasourceInfo +
                ", experimentSettings=" + experimentSettings +
                ", trialDetails=" + trialDetails +
                '}';
    }
}
