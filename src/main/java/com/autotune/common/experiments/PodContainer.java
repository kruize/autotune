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
 *                     "config": {
 *                         "0": {
 *                             "update env": {.....
 */
public class PodContainer {
    @SerializedName("image_name")
    private final String stackName;
    @SerializedName("container_name")
    private final String containerName;
    /**
     * Set of prometheus queries used to fetch metrics at container level.
     */
    @SerializedName("container_metrics")
    private HashMap<String, Metric> containerMetrics;
    /**
     * Contains set of instructions for containers to update runtime Environment, Resources like Cpu or Memory etc.
     * These instructions are tagged to TrialNumber.
     * Example:
     * "0123": {
     *                             "update env": {
     *                                 "JAVA_OPTIONS": " -server -XX:MaxRAMPercentage=70 -XX:+AllowParallelDefineClass -XX:MaxInlineLevel=23 -XX:+UseG1GC -XX:+TieredCompilation -Dquarkus.thread-pool.queue-size=78 -Dquarkus.thread-pool.core-threads=2",
     *                                 "JDK_JAVA_OPTIONS": " -server -XX:MaxRAMPercentage=70 -XX:+AllowParallelDefineClass -XX:MaxInlineLevel=23 -XX:+UseG1GC -XX:+TieredCompilation -Dquarkus.thread-pool.queue-size=78 -Dquarkus.thread-pool.core-threads=2"
     *                             },
     *                             "update requests and limits": {
     *                                 "requests": {
     *                                     "memory": 229,
     *                                     "cpu": "1.39"
     *                                 },
     *                                 "limits": {
     *                                     "memory": 229,
     *                                     "cpu": "1.39"
     *                                 }
     *                             }
     * }
     *
     * Here 0123 is the TrialNumber, and "update env" and "update requests and limits" are keys under TrialNumber:0123
     */
    @SerializedName("config")
    private HashMap<String, HashMap<String, JsonObject>> trailConfigs;

    public PodContainer(String stackName, String containerName) {
        this.stackName = stackName;
        this.containerName = containerName;
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
                ", trailConfigs=" + trailConfigs +
                '}';
    }
}
