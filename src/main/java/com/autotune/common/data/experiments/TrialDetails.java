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
package com.autotune.common.data.experiments;

import com.autotune.analyzer.k8sObjects.Metric;

import java.util.ArrayList;

/**
 *
 */
public class TrialDetails {
    private String deploymentType;
    private final String deploymentName;
    private final String deploymentNameSpace;
    private String state;
    private String result;
    private String resultInfo;
    private String resultError;
    private ArrayList<Metric> podMetrics;
    private ArrayList<PodContainer> podContainers;

    public TrialDetails(String deploymentType,
						String deploymentName,
						String deploymentNameSpace,
						String state,
						String result,
						String resultInfo,
						String resultError,
						ArrayList<Metric> podMetrics,
						ArrayList<PodContainer> podContainers) {
        this.deploymentType = deploymentType;
        this.deploymentName = deploymentName;
        this.deploymentNameSpace = deploymentNameSpace;
        this.state = state;
        this.result = result;
        this.resultInfo = resultInfo;
        this.resultError = resultError;
        this.podMetrics = podMetrics;
        this.podContainers = podContainers;
    }

    public String getDeploymentType() {
        return deploymentType;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getDeploymentNameSpace() {
        return deploymentNameSpace;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResultInfo() { return resultInfo; }

    public void setResultInfo(String resultInfo) { this.resultInfo = resultInfo; }

    public String getResultError() { return resultError; }

    public ArrayList<Metric> getPodMetrics() {
        return podMetrics;
    }

    public ArrayList<PodContainer> getPodContainers() {
        return podContainers;
    }
}
