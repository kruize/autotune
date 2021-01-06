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
package com.autotune.dependencyAnalyzer.util;

/**
 * Holds constants used in other parts of the codebase
 */
public class DAConstants
{
	//Used to parse autotune configmaps
	public static final String AUTH_TOKEN = "auth_token";
	public static final String MONITORING_SERVICE = "monitoring_service";
	public static final String MONITORING_AGENT_ENDPOINT = "monitoring_agent_endpoint";
	public static final String PROMETHEUS_DATA_SOURCE = "prometheus";
	public static final String HTTP_PROTOCOL = "http";

	//Used in Configuration for accessing the autotune objects from kubernetes
	public static final String SCOPE = "Namespaced";
	public static final String API_VERSION = "v1beta1";
	public static final String GROUP = "recommender.com";
	public static final String AUTOTUNE_PLURALS = "autotunes";
	public static final String AUTOTUNE_RESOURCE_NAME = AUTOTUNE_PLURALS + GROUP;
	public static final String AUTOTUNE_CONFIG_PLURALS = "autotuneconfigs";
	public static final String AUTOTUNE_CONFIG_RESOURCE_NAME = AUTOTUNE_CONFIG_PLURALS + GROUP;
	public static final String AUTOTUNE_VARIABLE_PLURALS = "autotunequeryvariables";
	public static final String AUTOTUNE_VARIABLE_RESOURCE_NAME = AUTOTUNE_VARIABLE_PLURALS + GROUP;
	public static final String LAYER_PRESENCE = "layerPresence";

	public static final String PROMETHEUS_ENDPOINT = "/api/v1/query?query=";
}
