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
package com.autotune.analyzer.application;

import java.util.HashMap;
import java.util.Map;

/**
 * ApplicationDeployment contains the details of a deployment associated with an AutotuneObject
 */
public class ApplicationDeployment {
	private final String deploymentName;
	private final String experimentName;
	private final String namespace;
	private String status;
	private Map<String, ApplicationServiceStack> applicationServiceStackMap;

	public ApplicationDeployment(String deploymentName, String experimentName, String namespace, String status) {
		this.deploymentName = deploymentName;
		this.experimentName = experimentName;
		this.namespace = namespace;
		this.status = status;

		this.applicationServiceStackMap = new HashMap<>();
	}

	public String getDeploymentName() {
		return deploymentName;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Map<String, ApplicationServiceStack> getApplicationServiceStackMap() {
		return applicationServiceStackMap;
	}
}
