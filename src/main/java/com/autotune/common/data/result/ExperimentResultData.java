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

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.k8sObjects.K8sObject;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

/**
 * Experiment result storage object.
 */
public class ExperimentResultData {
    private String experiment_name;

    private String version;
    private String trialNumber;
    @SerializedName("INTERVAL_START_TIME")
    private Timestamp intervalStartTime;
    @SerializedName("INTERVAL_END_TIME")
    private Timestamp intervalEndTime;
    private List<DeploymentResultData> deployments;
    private AnalyzerConstants.ExperimentStatus status;
    private ValidationOutputData validationOutputData;
    private List<K8sObject> kubernetes_objects;


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

    public ValidationOutputData getValidationOutputData() {
        return validationOutputData;
    }

    public void setValidationOutputData(ValidationOutputData validationOutputData) {
        this.validationOutputData = validationOutputData;
    }

    public Timestamp getIntervalStartTime() {
        return intervalStartTime;
    }

    public void setIntervalStartTime(Timestamp intervalStartTime) {
        this.intervalStartTime = intervalStartTime;
    }

    public Timestamp getIntervalEndTime() {
        return intervalEndTime;
    }

    public void setIntervalEndTime(Timestamp intervalEndTime) {
        this.intervalEndTime = intervalEndTime;
    }

    public List<K8sObject> getKubernetes_objects() {
        return kubernetes_objects;
    }

    public void setKubernetes_objects(List<K8sObject> kubernetes_objects) {
        this.kubernetes_objects = kubernetes_objects;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "ExperimentResultData{" +
                "experiment_name='" + experiment_name + '\'' +
                ", trialNumber='" + trialNumber + '\'' +
                ", intervalStartTime='" + intervalStartTime + '\'' +
                ", intervalEndTime='" + intervalEndTime + '\'' +
                ", deployments=" + deployments +
                ", status=" + status +
                ", validationOutputData=" + validationOutputData +
                ", kubernetes_objects=" + kubernetes_objects +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExperimentResultData that = (ExperimentResultData) o;
        return experiment_name.equals(that.experiment_name) && intervalEndTime.equals(that.intervalEndTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(experiment_name, intervalEndTime);
    }
}
