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
package com.autotune.analyzer.experiments;

import com.autotune.analyzer.k8sObjects.Metric;

import java.util.ArrayList;

/**
 *
 */
public class PodContainer {
	private final String stackName;
	private final String containerName;
	private Resources requests;
	private Resources limits;
	private String runtimeOptions;
	private ArrayList<Metric> containerMetrics;

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

	public ArrayList<Metric> getContainerMetrics() { return containerMetrics; }

	public void setContainerMetrics(ArrayList<Metric> containerMetrics) {
		this.containerMetrics = containerMetrics;
	}
}
