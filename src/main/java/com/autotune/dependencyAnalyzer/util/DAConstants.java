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
	// Used to parse autotune configmaps
	public static final String AUTH_TOKEN = "auth_token";
	public static final String MONITORING_SERVICE = "monitoring_service";
	public static final String MONITORING_AGENT_ENDPOINT = "monitoring_agent_endpoint";
	public static final String PROMETHEUS_DATA_SOURCE = "prometheus";
	public static final String HTTP_PROTOCOL = "http";

	// Used in Configuration for accessing the autotune objects from kubernetes
	public static final String SCOPE = "Namespaced";
	public static final String API_VERSION = "v1beta1";
	public static final String GROUP = "recommender.com";
	public static final String AUTOTUNE_PLURALS = "autotunes";
	public static final String AUTOTUNE_RESOURCE_NAME = AUTOTUNE_PLURALS + GROUP;
	public static final String AUTOTUNE_CONFIG_PLURALS = "autotuneconfigs";
	public static final String AUTOTUNE_CONFIG_RESOURCE_NAME = AUTOTUNE_CONFIG_PLURALS + GROUP;
	public static final String AUTOTUNE_VARIABLE_PLURALS = "autotunequeryvariables";
	public static final String AUTOTUNE_VARIABLE_RESOURCE_NAME = AUTOTUNE_VARIABLE_PLURALS + GROUP;

	public static final String PROMETHEUS_ENDPOINT = "/api/v1/query?query=";

	public static final String PRESENCE_ALWAYS = "always";

	public static class AutotuneObjectConstants {
		public static final String SPEC = "spec";
		public static final String SLA = "sla";
		public static final String SLA_CLASS = "sla_class";
		public static final String DIRECTION = "direction";
		public static final String OBJECTIVE_FUNCTION = "objective_function";

		public static final String FUNCTION_VARIABLES = "function_variables";
		public static final String NAME = "name";
		public static final String QUERY = "query";
		public static final String VALUE_TYPE = "value_type";
		public static final String DATASOURCE = "datasource";

		public static final String SELECTOR = "selector";
		public static final String MATCH_LABEL = "matchLabel";
		public static final String MATCH_LABEL_VALUE = "matchLabelValue";
		public static final String MATCH_ROUTE = "matchRoute";
		public static final String MATCH_URI = "matchURI";
		public static final String MATCH_SERVICE = "matchService";

		public static final String MODE = "mode";
		public static final String METADATA = "metadata";
		public static final String NAMESPACE = "namespace";
	}

	public static class AutotuneConfigConstants {
		public static final String METADATA = "metadata";
		public static final String NAMESPACE = "namespace";

		public static final String DATASOURCE = "datasource";
		public static final String LAYER_PRESENCE = "layerPresence";
		public static final String PRESENCE = "presence";
		public static final String LABEL = "label";
		public static final String QUERY_VARIABLES = "query_variables";
		public static final String VALUE = "value";

		public static final String LAYER_NAME = "layer_name";
		public static final String DETAILS = "details";
		public static final String LAYER_DETAILS = "layer_details";
		public static final String LAYER_LEVEL = "layer_level";

		public static final String TUNABLES = "tunables";
		public static final String QUERIES = "queries";

		public static final String NAME = "name";
		public static final String QUERY = "query";
		public static final String KEY = "key";
		public static final String VALUE_TYPE = "value_type";
		public static final String UPPER_BOUND = "upper_bound";
		public static final String LOWER_BOUND = "lower_bound";
		public static final String SLA_CLASS = "sla_class";

	}

	public static class ServiceConstants {
		public static final String APPLICATION_NAME = "application_name";
		public static final String LAYER_DETAILS = "layer_details";
		public static final String LAYERS = "layers";
		public static final String QUERY_URL = "query_url";
	}
}
