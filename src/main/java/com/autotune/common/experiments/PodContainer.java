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
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

/**
 * Data utilities for storing information about containers.
 * Example
 *      "containers": {
 *                 "kruize/tfb-qrh:1.13.2.F_mm.v1": {
 *                     "image_name": "kruize/tfb-qrh:1.13.2.F_mm.v1",
 *                     "container_name": "tfb-server",
 *                     "container_metrics":....,
 *                     "config": { ......
 */
public class PodContainer {
    @SerializedName("image_name")
    private final String stackName;
    @SerializedName("container_name")
    private final String containerName;
    // Hashmap of prometheus queries used to fetch metrics at container level.
    // Key will be MaxInlineLevel,memoryRequest or cpuRequest etc
    /**
     * Example
     *      "container_metrics": {
     *          "MaxInlineLevel": {
     *              "datasource": "prometheus",
     *              "query": "jvm_memory_used_bytes{area=\"heap\", $CONTAINER_LABEL$=\"\", $POD_LABEL$=\"$POD$\"}",
     *              "name": "MaxInlineLevel"
     *          },
     *          "memoryRequest": {
     *              "datasource": "prometheus",
     *              "query": "container_memory_working_set_bytes{$CONTAINER_LABEL$=\"\", $POD_LABEL$=\"$POD$\"}",
     *              "name": "memoryRequest"
     *          },
     * }
     */
    @SerializedName("container_metrics")
    private HashMap<String, Metric> containerMetrics;
    // Hashmap of instructions for containers to update runtime Environment, Resources like Cpu or Memory etc.
    // Key will be trialNumber
    /**
     * Contains set of instructions for containers to update runtime Environment, Resources like Cpu or Memory etc.
     * These instructions are tagged to TrialNumber.
     */
    @SerializedName("config")
    private HashMap<String, ContainerConfigData> trialConfigs;

    public PodContainer(String stackName, String containerName) {
        this.stackName = stackName;
        this.containerName = containerName;
    }

    public HashMap<String, ContainerConfigData> getTrialConfigs() {
        return trialConfigs;
    }

    public void setTrialConfigs(HashMap<String, ContainerConfigData> trialConfigs) {
        this.trialConfigs = trialConfigs;
    }

    public String getStackName() {
        return stackName;
    }

    public String getContainerName() {
        return containerName;
    }

    public HashMap<String, Metric> getContainerMetrics() {
        return containerMetrics;
    }

    public void setContainerMetrics(HashMap<String, Metric> containerMetrics) {
        this.containerMetrics = containerMetrics;
    }

    @Override
    public String toString() {
        return "PodContainer{" +
                "stackName='" + stackName + '\'' +
                ", containerName='" + containerName + '\'' +
                ", containerMetrics=" + containerMetrics +
                ", trialConfigs=" + trialConfigs +
                '}';
    }

}
