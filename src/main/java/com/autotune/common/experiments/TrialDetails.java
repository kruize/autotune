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

import com.autotune.common.k8sObjects.Metric;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.HashMap;

/**
 *
 */
public class TrialDetails {
    @SerializedName("type")
    private String deploymentType;
    @SerializedName("deployment_name")
    private final String deploymentName;
    @SerializedName("namespace")
    private final String deploymentNameSpace;
    private String state;
    private String result;
    private String resultInfo;
    private String resultError;
    private Timestamp startTime;
    private Timestamp endTime;
    // Hashmap of metrics associated with the Pod
    // Uses metric name as key
    @SerializedName("pod_metrics")
    private HashMap<String, Metric> podMetrics;
    // Hashmap of containers being tracked for this trial
    // Uses stack name (docker image name) as key
    @SerializedName("containers")
    private HashMap<String, PodContainer> podContainers;

    /**
     *
     * @param deploymentType
     * @param deploymentName
     * @param deploymentNameSpace
     * @param state
     * @param result
     * @param resultInfo
     * @param resultError
     * @param podMetrics
     * @param podContainers
     */
    public TrialDetails(String deploymentType,
						String deploymentName,
						String deploymentNameSpace,
						String state,
						String result,
						String resultInfo,
						String resultError,
						HashMap<String, Metric> podMetrics,
						HashMap<String, PodContainer> podContainers) {
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

    public String getDeploymentType() { return deploymentType; }

    public String getDeploymentName() { return deploymentName; }

    public String getDeploymentNameSpace() { return deploymentNameSpace; }

    public String getState() { return state; }

    public void setState(String state) { this.state = state; }

    public String getResult() { return result; }

    public void setResult(String result) { this.result = result; }

    public String getResultInfo() { return resultInfo; }

    public void setResultInfo(String resultInfo) { this.resultInfo = resultInfo; }

    public String getResultError() { return resultError; }

    public void setResultError(String resultError) { this.resultError = resultError; }

    public Timestamp getStartTime() { return startTime; }

    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }

    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public HashMap<String, Metric> getPodMetrics() { return podMetrics; }

    public HashMap<String, PodContainer> getPodContainers() { return podContainers; }

    @Override
    public String toString() {
        return "TrialDetails{" +
                "deploymentType='" + deploymentType + '\'' +
                ", deploymentName='" + deploymentName + '\'' +
                ", deploymentNameSpace='" + deploymentNameSpace + '\'' +
                ", state='" + state + '\'' +
                ", result='" + result + '\'' +
                ", resultInfo='" + resultInfo + '\'' +
                ", resultError='" + resultError + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", podMetrics=" + podMetrics +
                ", podContainers=" + podContainers +
                '}';
    }
}
