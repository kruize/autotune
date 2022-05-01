/*******************************************************************************
 * Copyright (c) 2021, 2021 Red Hat, IBM Corporation and others.
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

/**
 *
 */
public class ExperimentSettings {
    @SerializedName("trial_settings")
    private final TrialSettings trialSettings;
    @SerializedName("deployment_settings")
    private final DeploymentSettings deploymentSettings;

    public ExperimentSettings(TrialSettings trialSettings, DeploymentSettings deploymentSettings) {
        this.trialSettings = trialSettings;
        this.deploymentSettings = deploymentSettings;
    }

    public TrialSettings getTrialSettings() {
        return trialSettings;
    }

    public DeploymentSettings getDeploymentSettings() {
        return deploymentSettings;
    }

    @Override
    public String toString() {
        return "ExperimentSettings{" +
                "trialSettings=" + trialSettings +
                ", deploymentSettings=" + deploymentSettings +
                '}';
    }
}
