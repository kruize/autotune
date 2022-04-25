/*******************************************************************************
 * Copyright (c) 2020, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.utils;

import com.autotune.utils.EventLogger;

import java.util.regex.Pattern;

/**
 * Holds constants used in other parts of the codebase
 */
public class AnalyzerConstants {
	private AnalyzerConstants() {
	}

	// Used to parse autotune configmaps
	public static final String K8S_TYPE = "K8S_TYPE";
	public static final String AUTH_TYPE = "AUTH_TYPE";
	public static final String AUTH_TOKEN = "AUTH_TOKEN";
	public static final String CLUSTER_TYPE = "CLUSTER_TYPE";
	public static final String LOGGING_LEVEL = "LOGGING_LEVEL";
	public static final String ROOT_LOGGING_LEVEL = "ROOT_LOGGING_LEVEL";
	public static final String MONITORING_AGENT = "MONITORING_AGENT";
	public static final String MONITORING_SERVICE = "MONITORING_SERVICE";
	public static final String MONITORING_AGENT_ENDPOINT = "MONITORING_AGENT_ENDPOINT";

	public static final String PROMETHEUS_DATA_SOURCE = "prometheus";
	public static final String PROMETHEUS_API = "/api/v1/query?query=";

	public static final String HTTP_PROTOCOL = "http";
	public static final String HTTPS_PROTOCOL = "https";

	// Used in Configuration for accessing the autotune objects from kubernetes
	public static final String SCOPE = "Namespaced";
	public static final String GROUP = "recommender.com";
	public static final String API_VERSION_V1 = "v1";
	public static final String POD_TEMPLATE_HASH = "pod-template-hash";
	public static final String AUTOTUNE_PLURALS = "autotunes";
	public static final String AUTOTUNE_RESOURCE_NAME = AUTOTUNE_PLURALS + GROUP;
	public static final String AUTOTUNE_CONFIG_PLURALS = "autotuneconfigs";
	public static final String AUTOTUNE_CONFIG_RESOURCE_NAME = AUTOTUNE_CONFIG_PLURALS + GROUP;
	public static final String AUTOTUNE_VARIABLE_PLURALS = "autotunequeryvariables";
	public static final String AUTOTUNE_VARIABLE_RESOURCE_NAME = AUTOTUNE_VARIABLE_PLURALS + GROUP;

	public static final String PRESENCE_ALWAYS = "always";
	public static final String NONE = "none";

	public static final String POD_VARIABLE = "$POD$";
	public static final String NAMESPACE_VARIABLE = "$NAMESPACE$";

	public static final String API_VERSION = "apiVersion";
	public static final String KIND = "kind";
	public static final String RESOURCE_VERSION = "resourceVersion";
	public static final String UID = "uid";

	public static final String REASON_NORMAL = "Normal";
	public static final String AUTOTUNE = "Autotune";

	/**
	 * Used to parse the Autotune kind resource
	 */
	public static class AutotuneObjectConstants {
		private AutotuneObjectConstants() {
		}

		public static final String SPEC = "spec";
		public static final String SLO = "slo";
		public static final String SLO_CLASS = "slo_class";
		public static final String DIRECTION = "direction";
		public static final String OBJECTIVE_FUNCTION = "objective_function";

		public static final String FUNCTION_VARIABLES = "function_variables";
		public static final String NAME = "name";
		public static final String QUERY = "query";
		public static final String VALUE_TYPE = "value_type";
		public static final String DATASOURCE = "datasource";

		public static final String MINIMIZE = "minimize";
		public static final String MAXIMIZE = "maximize";

		public static final String SELECTOR = "selector";
		public static final String MATCH_LABEL = "matchLabel";
		public static final String MATCH_LABEL_VALUE = "matchLabelValue";
		public static final String MATCH_ROUTE = "matchRoute";
		public static final String MATCH_URI = "matchURI";
		public static final String MATCH_SERVICE = "matchService";

		public static final String MODE = "mode";
		public static final String METADATA = "metadata";
		public static final String NAMESPACE = "namespace";
		public static final String EXPERIMENT_ID = "experiment_id";
		public static final String HPO_ALGO_IMPL = "hpo_algo_impl";

		public static final String DEFAULT_HPO_ALGO_IMPL = "optuna_tpe";


	}

	/**
	 * Used to parse the AutotuneConfig resource
	 */
	public static class AutotuneConfigConstants {
		private AutotuneConfigConstants() {
		}

		public static final String METADATA = "metadata";
		public static final String NAMESPACE = "namespace";

		public static final String DATASOURCE = "datasource";
		public static final String LAYER_PRESENCE = "layer_presence";
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
		public static final String TUNABLE_NAME = "tunable_name";
		public static final String TUNABLE_VALUE = "tunable_value";
		public static final String QUERY = "query";
		public static final String KEY = "key";
		public static final String VALUE_TYPE = "value_type";
		public static final String UPPER_BOUND = "upper_bound";
		public static final String LOWER_BOUND = "lower_bound";
		public static final String CATEGORICAL_TYPE = "categorical";
		public static final String TUNABLE_CHOICES = "choices";
		public static final String TRUE = "true";
		public static final String FALSE = "false";

		public static final String DOUBLE = "double";
		public static final String LONG = "long";
		public static final String INTEGER = "integer";

		public static final Pattern BOUND_CHARS = Pattern.compile("[\\sa-zA-Z]");
		public static final Pattern BOUND_DIGITS = Pattern.compile("[\\s0-9\\.]");
		public static final String SLO_CLASS = "slo_class";

		public static final String LAYER_PRESENCE_LABEL = "layerPresenceLabel";
		public static final String LAYER_PRESENCE_LABEL_VALUE = "layerPresenceLabelValue";
		public static final String LAYER_PRESENCE_QUERIES = "layerPresenceQueries";
		public static final String LAYER_ID = "layer_id";
		public static final String STEP = "step";

		public static final String LAYER_GENERIC = "generic";
		public static final String LAYER_CONTAINER = "container";
		public static final String LAYER_HOTSPOT = "hotspot";
		public static final String LAYER_QUARKUS = "quarkus";
		public static final String LAYER_OPENJ9 = "openj9";
		public static final String LAYER_NODEJS = "nodejs";
	}

	/**
	 * Contains Strings used in REST services
	 */
	public static class ServiceConstants {
		private ServiceConstants() {
		}

		public static final String JSON_CONTENT_TYPE = "application/json";
		public static final String CHARACTER_ENCODING = "UTF-8";
		public static final String EXPERIMENT_NAME = "experiment_name";
		public static final String DEPLOYMENTS = "deployments";
		public static final String DEPLOYMENT_NAME = "deployment_name";
		public static final String NAMESPACE = "namespace";
		public static final String STACKS = "stacks";
		public static final String STACK_NAME = "stack_name";
		public static final String CONTAINER_NAME = "container_name";
		public static final String LAYER_DETAILS = "layer_details";
		public static final String LAYERS = "layers";
		public static final String QUERY_URL = "query_url";

		public static final String TRAINING = "training";
		public static final String PRODUCTION = "production";

		public static final String TOTAL_TRIALS = "total_trials";
		public static final String TRIALS_COMPLETED = "trials_completed";
		public static final String TRIALS_ONGOING = "trials_ongoing";
		public static final String TRIALS_PASSED = "trials_passed";
		public static final String TRIALS_FAILED = "trials_failed";
		public static final String BEST_TRIAL = "best_trial";
		public static final String TRIALS_SUMMARY = "trials_summary";
		public static final String TRIAL_STATUS = "status";
		public static final String TRIAL_NUMBER = "trial_number";
		public static final String TRIAL_RESULT = "trial_result";
		public static final String TRIAL_ERRORS = "trial_errors";
		public static final String TRIAL_DURATION = "trial_duration";
		public static final String EXPERIMENT_TRIALS = "experiment_trials";
		public static final String NA = "NA";
		public static final String SECONDS = " seconds";

	}

	/**
	 * Contains Strings used in the HOTSPOT Layer
	 */
	public static class HotspotConstants {
		private HotspotConstants() {
		}

		public static final String XXOPTION = " -XX:";
		public static final String USE = "+Use";
		public static final String SERVER = " -server";
		public static final String MAX_RAM_PERCENTAGE = "MaxRAMPercentage";
		public static final String GC = "gc";
		public static final String TIERED_COMPILATION = "TieredCompilation";
		public static final String ALLOW_PARALLEL_DEFINE_CLASS = "AllowParallelDefineClass";
		public static final String ALLOW_VECTORIZE_ON_DEMAND = "AllowVectorizeOnDemand";
		public static final String ALWAYS_COMPILE_LOOP_METHODS = "AlwaysCompileLoopMethods";
		public static final String ALWAYS_PRE_TOUCH = "AlwaysPreTouch";
		public static final String ALWAYS_TENURE = "AlwaysTenure";
		public static final String BACKGROUND_COMPILATION = "BackgroundCompilation";
		public static final String DO_ESCAPE_ANALYSIS = "DoEscapeAnalysis";
		public static final String USE_INLINE_CACHES = "UseInlineCaches";
		public static final String USE_LOOP_PREDICATE = "UseLoopPredicate";
		public static final String USE_STRING_DEDUPLICATION = "UseStringDeduplication";
		public static final String USE_SUPER_WORD = "UseSuperWord";
		public static final String USE_TYPE_SPECULATION = "UseTypeSpeculation";
		public static final String COMPILE_THRESHOLD_SCALING = "CompileThresholdScaling";
		public static final String MAX_INLINE_LEVEL = "MaxInlineLevel";
		public static final String FREQ_INLINE_SIZE = "FreqInlineSize";
		public static final String MIN_INLINING_THRESHOLD = "MinInliningThreshold";
		public static final String COMPILE_THRESHOLD = "CompileThreshold";
		public static final String CONC_GC_THREADS = "ConcGCThreads";
		public static final String PARALLEL_GC_THREADS = "ParallelGCThreads";
		public static final String INLINE_SMALL_CODE = "InlineSmallCode";
		public static final String LOOP_UNROLL_LIMIT = "LoopUnrollLimit";
		public static final String LOOP_UNROLL_MIN = "LoopUnrollMin";
		public static final String MIN_SURVIVOR_RATIO = "MinSurvivorRatio";
		public static final String NEW_RATIO = "NewRatio";
		public static final String TIERED_STOP_AT_LEVEL = "TieredStopAtLevel";
	}

	/**
	 * Contains Strings used in the QUARKUS Layer
	 */
	public static class QuarkusConstants {
		private QuarkusConstants() {
		}

		public static final String QUARKUS = "quarkus";
		public static final String DOPTION = " -D";
	}

	/**
	 * Contains Strings used in the Container Layer
	 */
	public static class ContainerConstants {
		private ContainerConstants() {
		}

		public static final String CPU_REQUEST = "cpuRequest";
		public static final String MEM_REQUEST = "memoryRequest";
	}
}
