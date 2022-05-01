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
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

/**
 *
 */
public class PodContainer {
	@SerializedName("image_name")
	private final String stackName;
	@SerializedName("container_name")
	private final String containerName;
	private Resources requests;
	private Resources limits;
	private String runtimeOptions;
	@SerializedName("container_metrics")
	private HashMap<String, Metric> containerMetrics;
	@SerializedName("config")
	private HashMap<String, HashMap<String, JsonObject> > trailConfigs;

	public PodContainer(String stackName, String containerName) {
		this.stackName = stackName;
		this.containerName = containerName;
	}

	public String getStackName() { return stackName; }

	public String getContainerName() { return containerName; }

	public Resources getRequests() { return requests; }

	public Resources getLimits() { return limits; }

	public String getRuntimeOptions() {	return runtimeOptions; }

	public void setRequests(Resources requests) {
		this.requests = requests;
	}

	public void setLimits(Resources limits) {
		this.limits = limits;
	}

	public void setRuntimeOptions(String runtimeOptions) {
		this.runtimeOptions = runtimeOptions;
	}

	public HashMap<String, Metric> getContainerMetrics() { return containerMetrics; }

	public void setContainerMetrics(HashMap<String, Metric> containerMetrics) {
		this.containerMetrics = containerMetrics;
	}

	@Override
	public String toString() {
		return "PodContainer{" +
				"stackName='" + stackName + '\'' +
				", containerName='" + containerName + '\'' +
				", requests=" + requests +
				", limits=" + limits +
				", runtimeOptions='" + runtimeOptions + '\'' +
				", containerMetrics=" + containerMetrics +
				", trailConfigs=" + trailConfigs +
				'}';
	}
}
