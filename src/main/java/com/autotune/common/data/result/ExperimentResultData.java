/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.data.result;

import com.autotune.utils.AnalyzerConstants;

import java.util.List;
import java.util.Objects;

/**
 * Experiment result storage object.
 */
public class ExperimentResultData {
    private String experiment_name;
    private String trialNumber;
    private String trial_timestamp;
    private List<DeploymentResultData> deployments;
    private AnalyzerConstants.ExperimentStatus status;

    public String getExperiment_name() {
        return experiment_name;
    }

    public void setExperiment_name(String experiment_name) {
        this.experiment_name = experiment_name;
    }

    public String getTrialNumber() {
        return trialNumber;
    }

    public void setTrialNumber(String trialNumber) {
        this.trialNumber = trialNumber;
    }

    public String getTrial_timestamp() {
        return trial_timestamp;
    }

    public void setTrial_timestamp(String trial_timestamp) {
        this.trial_timestamp = trial_timestamp;
    }

    public List<DeploymentResultData> getDeployments() {
        return deployments;
    }

    public void setDeployments(List<DeploymentResultData> deployments) {
        this.deployments = deployments;
    }

    public AnalyzerConstants.ExperimentStatus getStatus() {
        return status;
    }

    public void setStatus(AnalyzerConstants.ExperimentStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ExperimentResultData{" +
                "experiment_name='" + experiment_name + '\'' +
                ", trialNumber='" + trialNumber + '\'' +
                ", trial_timestamp='" + trial_timestamp + '\'' +
                ", deployments=" + deployments +
                ", status=" + status +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExperimentResultData that = (ExperimentResultData) o;
        return experiment_name.equals(that.experiment_name) && trial_timestamp.equals(that.trial_timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(experiment_name, trial_timestamp);
    }
}
