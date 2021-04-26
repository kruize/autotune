/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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

package com.autotune.experimentManager.finiteStateMachine.objects;

import com.autotune.experimentManager.finiteStateMachine.api.EMFiniteStateMachine;
import io.fabric8.kubernetes.api.model.apps.Deployment;

import java.util.List;

/**
 * ExperimentTrialObject holds data for a experiment, we are using this class as a data traversing object(DTO)
 * between different state of the finite state machine.
 */

public class ExperimentTrialObject {
    private long id;
    private String appVersion;
    private String deploymentName;
    private String trialRun;
    private int trialNumber;
    private String trialMeasurementTime;
    private String trialResult;
    private String trialResultInfo;
    private String trialResultError;
    private Deployment productionDeployment;
    private Deployment trialDeployment;
    private String URL;
    private List<MetricObject> metricsObjects;
    private EMFiniteStateMachine emFSM;

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getTrialMeasurementTime() {
        return trialMeasurementTime;
    }
    public void setTrialMeasurementTime(String trialMeasurementTime) {
        this.trialMeasurementTime = trialMeasurementTime;
    }
    public String getTrialResult() {
        return trialResult;
    }
    public void setTrialResult(String trialResult) {
        this.trialResult = trialResult;
    }
    public String getTrialResultInfo() {
        return trialResultInfo;
    }
    public void setTrialResultInfo(String trialResultInfo) {
        this.trialResultInfo = trialResultInfo;
    }
    public String getTrialResultError() {
        return trialResultError;
    }
    public void setTrialResultError(String trialResultError) {
        this.trialResultError = trialResultError;
    }
    public String getAppVersion() {
        return appVersion;
    }
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }
    public String getDeploymentName() {
        return deploymentName;
    }
    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }
    public String getTrialRun() {
        return trialRun;
    }
    public void setTrialRun(String trialRun) {
        this.trialRun = trialRun;
    }
    public int getTrialNumber() {
        return trialNumber;
    }
    public void setTrialNumber(int trialNumber) {
        this.trialNumber = trialNumber;
    }
    public Deployment getProductionDeployment() {
        return productionDeployment;
    }
    public void setProductionDeployment(Deployment productionDeployment) {
        this.productionDeployment = productionDeployment;
    }
    public Deployment getTrialDeployment() {
        return trialDeployment;
    }
    public void setTrialDeployment(Deployment trialDeployment) {
        this.trialDeployment = trialDeployment;
    }
    public List<MetricObject> getMetricsObjects() {
        return metricsObjects;
    }
    public void setMetricsObjects(List<MetricObject> metricsObjects) {
        this.metricsObjects = metricsObjects;
    }
    public EMFiniteStateMachine getEmFSM() {
        return emFSM;
    }
    public void setEmFSM(EMFiniteStateMachine emFSM) {
        this.emFSM = emFSM;
    }
    public String getURL() {
        return URL;
    }
    public void setURL(String uRL) {
        URL = uRL;
    }
}
