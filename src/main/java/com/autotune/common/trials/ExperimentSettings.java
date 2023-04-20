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
package com.autotune.common.trials;

import com.google.gson.annotations.SerializedName;

/**
 *  Example
 *  "settings": {
 *         "trial_settings": {
 *             "measurement_cycles": "3",
 *             "warmup_duration": "1min",
 *             "warmup_cycles": "3",
 *             "measurement_duration": "1min",
 *             "iterations": "3"
 *         },
 *         "deployment_settings": {
 *             "deployment_tracking": {
 *                 "trackers": [
 *                     "training"
 *                 ]
 *             },
 *             "deployment_policy": {
 *                 "type": "rollingUpdate"
 *             }
 *         }
 *     },
 *
 */
public class ExperimentSettings {
    @SerializedName("trial_settings")
    private final TrialSettings trialSettings;
    @SerializedName("deployment_settings")
    private final DeploymentSettings deploymentSettings;
    @SerializedName("do_experiment")
    private final boolean do_experiment;
    @SerializedName("do_monitoring")
    private final boolean do_monitoring;
    @SerializedName("wait_for_load")
    private final boolean wait_for_load;

    public ExperimentSettings(TrialSettings trialSettings,
                              DeploymentSettings deploymentSettings,
                              boolean do_experiment,
                              boolean do_monitoring,
                              boolean wait_for_load) {
        this.trialSettings = trialSettings;
        this.deploymentSettings = deploymentSettings;
        this.do_experiment = do_experiment;
        this.do_monitoring = do_monitoring;
        this.wait_for_load = wait_for_load;
    }

    public TrialSettings getTrialSettings() {
        return trialSettings;
    }

    public DeploymentSettings getDeploymentSettings() {
        return deploymentSettings;
    }

    public boolean isDo_experiment() {
        return do_experiment;
    }

    public boolean isDo_monitoring() {
        return do_monitoring;
    }

    public boolean isWait_for_load() {
        return wait_for_load;
    }

    @Override
    public String toString() {
        return "ExperimentSettings{" +
                "trialSettings=" + trialSettings +
                ", deploymentSettings=" + deploymentSettings +
                '}';
    }
}
