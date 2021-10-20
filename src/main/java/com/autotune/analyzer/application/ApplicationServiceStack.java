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

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.k8sObjects.AutotuneConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * An ApplicationServiceStack represents a microservice, and can have multiple
 * stack layers on different levels. (runtime, container, framework, etc)
 *
 * application: "petclinic",
 * namespace: "defaut"
 * serviceStackLayers:
 * -  layer_name: container
 *    layer_level: 0
 *    details: generic container tunables
 *    tunables:
 *    - name: memoryLimit
 *      value_type: double
 *      upper_bound: 300M
 *      lower_bound: 150M
 *      queries:
 *        datasource:
 *        - name: 'prometheus'
 *          query: 'container_memory_working_set_bytes{$CONTAINER_LABEL$="", $POD_LABEL$="$POD$"}'
 *      slo_class:
 *      - response_time
 *      - throughput
 */
public class ApplicationServiceStack
{
	private String applicationServiceName;
	private String namespace;
	private String status;
	private Map<String, AutotuneConfig> applicationServiceStackLayers;

	public ApplicationServiceStack(String applicationServiceName, String namespace) {
		this.applicationServiceName = applicationServiceName;
		this.namespace = namespace;

		this.applicationServiceStackLayers = new HashMap<>();
	}

	public ApplicationServiceStack(String applicationServiceName, String namespace, String status) {
		this.applicationServiceName = applicationServiceName;
		this.namespace = namespace;
		this.status = status;

		this.applicationServiceStackLayers = new HashMap<>();
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) throws InvalidValueException {
		if (namespace != null)
			this.namespace = namespace;
		else
			throw new InvalidValueException("Namespace cannot be null");
	}

	public String getApplicationServiceName() {
		return applicationServiceName;
	}

	public void setApplicationServiceName(String applicationServiceName) throws InvalidValueException {
		if (applicationServiceName != null)
			this.applicationServiceName = applicationServiceName;
		else
			throw new InvalidValueException("Application service name cannot be null");
	}

	public Map<String, AutotuneConfig> getStackLayers() {
		return applicationServiceStackLayers;
	}

	public void setStackLayers(Map<String, AutotuneConfig> applicationServiceStackLayers) {
		this.applicationServiceStackLayers = applicationServiceStackLayers;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "ApplicationTunables{" +
				"applicationName='" + applicationServiceName + '\'' +
				", namespace='" + namespace + '\'' +
				", status='" + status + '\'' +
				", serviceStackLayers=" + applicationServiceStackLayers +
				'}';
	}
}
