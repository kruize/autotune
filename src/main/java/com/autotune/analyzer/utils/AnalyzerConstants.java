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
package com.autotune.analyzer.utils;

import com.autotune.utils.KruizeConstants;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Holds constants used in other parts of the codebase
 */
public class AnalyzerConstants {
    public static final String MODE = "mode";
    public static final String TARGET_CLUSTER = "target_cluster";
    public static final String MONITOR = "monitor";
    public static final String EXPERIMENT = "experiment";
    public static final String LOCAL = "local";
    public static final String REMOTE = "remote";


    // Used to parse autotune configmaps


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
    public static final String DEFAULT_K8S_TYPE = "openshift";
    public static final String PROFILE_VERSION = "profile_version";
    public static final Double DEFAULT_PROFILE_VERSION = 1.0;
    public static final String AGGREGATION_FUNCTIONS = "aggregation_functions";
    public static final String FUNCTION = "function";
    public static final String VERSIONS = "versions";
    public static final String KUBERNETES_OBJECT = "kubernetes_object";
    public static final String KUBERNETES_OBJECTS = "kubernetes_objects";
    public static final String AUTOTUNE_CONFIG_PLURALS = "autotuneconfigs";
    public static final String AUTOTUNE_CONFIG_RESOURCE_NAME = AUTOTUNE_CONFIG_PLURALS + GROUP;
    public static final String AUTOTUNE_VARIABLE_PLURALS = "autotunequeryvariables";
    public static final String AUTOTUNE_VARIABLE_RESOURCE_NAME = AUTOTUNE_VARIABLE_PLURALS + GROUP;
    public static final String PRESENCE_ALWAYS = "always";
    public static final String NONE = "none";
    public static final String POD_VARIABLE = "$POD$";
    public static final String NAMESPACE_VARIABLE = "$NAMESPACE$";
    public static final String CONTAINER_VARIABLE = "$CONTAINER_NAME$";
    public static final String MEASUREMENT_DURATION_IN_MIN_VARAIBLE = "$MEASUREMENT_DURATION_IN_MIN$";
    public static final String WORKLOAD_VARIABLE = "$WORKLOAD$";
    public static final String WORKLOAD_TYPE_VARIABLE = "$WORKLOAD_TYPE$";
    public static final String API_VERSION = "apiVersion";
    public static final String KIND = "kind";
    public static final String RESOURCE_VERSION = "resourceVersion";
    public static final String UID = "uid";
    public static final String REASON_NORMAL = "Normal";
    public static final String AUTOTUNE = "Autotune";
    public static final String EXPERIMENT_MAP = "MainExperimentsMAP";
    public static final String NAME = "experimentName";
    public static final String SLO = "sloInfo";
    public static final String NAMESPACE = "namespace";
    public static final String RECOMMENDATION_SETTINGS = "recommendation_settings";
    public static final String DEPLOYMENT_NAME = "name";
    public static final String SELECTOR = "selectorInfo";
    public static final String NULL = "null";
    public static final String BULKUPLOAD_CREATEEXPERIMENT_LIMIT = "bulkupload_createexperiment_limit";
    public static final String PERSISTANCE_STORAGE = "persistance_storage";
    public static final String RESULTS_COUNT = "results_count";
    public static final int GC_THRESHOLD_COUNT = 100;
    public static final String TARGET = "target/bin";
    public static final String MIGRATIONS = "migrations";
    public static final String ROS_DDL_SQL = "kruize_experiments_ddl.sql";
    public static final String KRUIZE_LOCAL_DDL_SQL = "kruize_local_ddl.sql";
    public static final String VERSION = "version";
    public static final String DATASOURCE_NAME = "dataSourceName";

    private AnalyzerConstants() {
    }

    public enum MODEType {
        MONITORING,
        EXPERIMENT;

    }

    public enum TargetType {
        LOCAL,
        REMOTE;

    }

    public enum ExperimentStatus {
        QUEUED,
        IN_PROGRESS,
        STALE,
        PAUSE,
        RESUME,
        DELETE,
        COMPLETED,
        FAILED;
    }

    public enum RecommendationItem {
        cpu,
        memory
    }

    public enum CapacityMax {
        capacity,
        max
    }

    public enum ResourceSetting {
        requests,
        limits
    }

    public enum PersistenceType {
        LOCAL,              //Store only local  , Default
        HYBRID,             //Store data both in db and local
        DB                  //Store only DB
    }

    public enum RecommendationSection {
        CURRENT_CONFIG(KruizeConstants.JSONKeys.CURRENT),
        RECOMMENDATION_CONFIG(KruizeConstants.JSONKeys.CONFIG),
        VARIATION(KruizeConstants.JSONKeys.VARIATION);

        private String name;

        private RecommendationSection(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    public enum MetricName {
        cpuRequest,
        cpuLimit,
        cpuUsage,
        cpuThrottle,
        memoryRequest,
        memoryLimit,
        memoryUsage,
        memoryRSS,
        maxDate,
        namespaceCpuRequest,
        namespaceCpuLimit,
        namespaceCpuUsage,
        namespaceCpuThrottle,
        namespaceMemoryRequest,
        namespaceMemoryLimit,
        namespaceMemoryUsage,
        namespaceMemoryRSS,
        namespaceTotalPods,
        namespaceRunningPods,
        namespaceMaxDate,
        gpuCoreUsage,
        gpuMemoryUsage
    }

    public enum K8S_OBJECT_TYPES {
        DEPLOYMENT,
        DEPLOYMENT_CONFIG,
        STATEFULSET,
        REPLICASET,
        REPLICATION_CONTROLLER,
        DAEMONSET,
        JOB,
    }

    public enum RegisterRecommendationModelStatus {
        SUCCESS,
        ALREADY_EXISTS,
        INVALID
    }

    /**
     * Used to parse the Autotune kind resource
     */
    public static final class AutotuneObjectConstants {

        public static final String SPEC = "spec";
        public static final String SLO = "slo";
        public static final String SLO_CLASS = "slo_class";
        public static final String DIRECTION = "direction";
        public static final String OBJECTIVE_FUNCTION = "objective_function";
        public static final String OBJ_FUNCTION_TYPE = "function_type";
        public static final String EXPRESSION = "expression";
        public static final String FUNCTION_VARIABLES = "function_variables";
        public static final String NAME = "name";
        public static final String QUERY = "query";
        public static final String VALUE_TYPE = "value_type";
        public static final String DATASOURCE = "datasource";
        public static final String TOTAL_TRIALS = "total_trials";
        public static final String PARALLEL_TRIALS = "parallel_trials";
        public static final String MINIMIZE = "minimize";
        public static final String MAXIMIZE = "maximize";
        public static final String SELECTOR = "selector";
        public static final String MATCH_LABEL = "matchLabel";
        public static final String MATCH_LABEL_VALUE = "matchLabelValue";
        public static final String MATCH_ROUTE = "matchRoute";
        public static final String MATCH_URI = "matchURI";
        public static final String MATCH_SERVICE = "matchService";
        public static final String MODE = "mode";
        public static final String DEFAULT_MODE = "experiment";
        public static final String TARGET_CLUSTER = "target_cluster";
        public static final String DEFAULT_TARGET_CLUSTER = "local";
        public static final String METADATA = "metadata";
        public static final String NAMESPACE = "namespace";
        public static final String EXPERIMENT_ID = "experiment_id";
        public static final String HPO_ALGO_IMPL = "hpo_algo_impl";
        public static final String DEFAULT_HPO_ALGO_IMPL = "optuna_tpe";
        public static final String FUNCTION_VARIABLE = "function_variable: ";
        public static final String CLUSTER_NAME = "cluster_name";

        private AutotuneObjectConstants() {
        }
    }

    /**
     * Used to parse the KruizeLayer resource
     */
    public static final class AutotuneConfigConstants {

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

        private AutotuneConfigConstants() {
        }

    }

    /**
     * Contains Strings used in REST services
     */
    public static final class ServiceConstants {

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
        public static final String LATEST = "latest";
        public static final String EXPERIMENT_REGISTERED = "Registered successfully with Kruize! View registered experiments at /listExperiments";
        public static final String RESULT_SAVED = "Results added successfully! View saved results at /listExperiments.";
        public static final String DATASOURCE_NAME = "name";
        public static final String DATASOURCE = "datasource";
        public static final String DATASOURCE_PROVIDER = "provider";
        public static final String CLUSTER_NAME = "cluster_name";
        public static final String VERBOSE = "verbose";
        public static final String FALSE = "false";

        private ServiceConstants() {
        }
    }

    /**
     * Contains Strings used in the HOTSPOT Layer
     */
    public static final class HotspotConstants {

        public static final String XXOPTION = " -XX:";
        public static final String USE = "+Use";
        public static final String SERVER = " -server";
        public static final String ALLOW_PARALLEL_DEFINE_CLASS = "AllowParallelDefineClass";
        public static final String ALLOW_VECTORIZE_ON_DEMAND = "AllowVectorizeOnDemand";
        public static final String ALWAYS_COMPILE_LOOP_METHODS = "AlwaysCompileLoopMethods";
        public static final String ALWAYS_PRE_TOUCH = "AlwaysPreTouch";
        public static final String ALWAYS_TENURE = "AlwaysTenure";
        public static final String BACKGROUND_COMPILATION = "BackgroundCompilation";
        public static final String COMPILE_THRESHOLD = "CompileThreshold";
        public static final String COMPILE_THRESHOLD_SCALING = "CompileThresholdScaling";
        public static final String CONC_GC_THREADS = "ConcGCThreads";
        public static final String DO_ESCAPE_ANALYSIS = "DoEscapeAnalysis";
        public static final String FREQ_INLINE_SIZE = "FreqInlineSize";
        public static final String GC = "gc";
        public static final String INLINE_SMALL_CODE = "InlineSmallCode";
        public static final String LOOP_UNROLL_LIMIT = "LoopUnrollLimit";
        public static final String LOOP_UNROLL_MIN = "LoopUnrollMin";
        public static final String MAX_INLINE_LEVEL = "MaxInlineLevel";
        public static final String MAX_RAM_PERCENTAGE = "MaxRAMPercentage";
        public static final String MIN_INLINING_THRESHOLD = "MinInliningThreshold";
        public static final String MIN_SURVIVOR_RATIO = "MinSurvivorRatio";
        public static final String NETTY_BUFFER_CHECK = "nettyBufferCheck";
        public static final String NETTY_BUFFER_CHECKBOUNDS = "io.netty.buffer.checkBounds";
        public static final String NETTY_BUFFER_CHECKACCESSIBLE = "io.netty.buffer.checkAccessible";
        public static final String NEW_RATIO = "NewRatio";
        public static final String PARALLEL_GC_THREADS = "ParallelGCThreads";
        public static final String STACK_TRACE_IN_THROWABLE = "StackTraceInThrowable";
        public static final String TIERED_COMPILATION = "TieredCompilation";
        public static final String TIERED_STOP_AT_LEVEL = "TieredStopAtLevel";
        public static final String USE_INLINE_CACHES = "UseInlineCaches";
        public static final String USE_LOOP_PREDICATE = "UseLoopPredicate";
        public static final String USE_STRING_DEDUPLICATION = "UseStringDeduplication";
        public static final String USE_SUPER_WORD = "UseSuperWord";
        public static final String USE_TYPE_SPECULATION = "UseTypeSpeculation";

        private HotspotConstants() {
        }

    }

    /**
     * Contains Strings used in the QUARKUS Layer
     */
    public static final class QuarkusConstants {

        public static final String QUARKUS = "quarkus";
        public static final String DOPTION = " -D";

        private QuarkusConstants() {
        }

    }

    /**
     * Contains Strings used in the Container Layer
     */
    public static final class ContainerConstants {

        public static final String CPU_REQUEST = "cpuRequest";
        public static final String MEM_REQUEST = "memoryRequest";

        private ContainerConstants() {
        }

    }

    public static class createExperimentParallelEngineConfigs {
        /**
         * MAX Queue size to stack experiments
         */
        public static int QUEUE_SIZE = 20000;
        /**
         * Core pool size is the minimum number of workers to keep alive
         */
        public static int CORE_POOL_SIZE = 100;
        /**
         * Maximum number of workers limit
         */
        public static int MAX_POOL_SIZE = 1000;
        /**
         * Timeout for idle threads waiting for work. Threads use this timeout when there are more than corePoolSize present or if allowCoreThreadTimeOut. Otherwise they wait forever for new work.
         */
        public static int CORE_POOL_KEEPALIVETIME_IN_SECS = 5;
        /**
         * the time between successive executions
         */
        public static int DELAY_IN_SECS = 2;
        public static String EXECUTOR = "KRUIZE_EXECUTOR";

        private createExperimentParallelEngineConfigs() {
        }
    }

    public static class updateResultsParallelEngineConfigs {
        /**
         * MAX Queue size to stack experiments
         */
        public static int QUEUE_SIZE = 20000;
        /**
         * Core pool size is the minimum number of workers to keep alive
         */
        public static int CORE_POOL_SIZE = 100;
        /**
         * Maximum number of workers limit
         */
        public static int MAX_POOL_SIZE = 1000;
        /**
         * Timeout for idle threads waiting for work. Threads use this timeout when there are more than corePoolSize present or if allowCoreThreadTimeOut. Otherwise they wait forever for new work.
         */
        public static int CORE_POOL_KEEPALIVETIME_IN_SECS = 5;
        /**
         * the time between successive executions
         */
        public static int DELAY_IN_SECS = 2;
        public static String EXECUTOR = "KRUIZE_EXECUTOR";

    }

    public static final class PerformanceProfileConstants {

        public static final String PERFORMANCE_PROFILE_PLURALS = "kruizeperformanceprofiles";
        public static final String PERFORMANCE_PROFILE_RESOURCE_NAME = PERFORMANCE_PROFILE_PLURALS + GROUP;
        public static final String K8S_TYPE = "k8s_type";
        public static final String PERF_PROFILE = "performanceProfile";
        public static final String PERF_PROFILE_MAP = "performanceProfileMap";
        public static final String METRIC_PROFILE_MAP = "metricProfileMap";
        public static final String PERF_PROFILE_NAME = "name";
        public static final String OBJECTIVE_FUNCTION = "objectiveFunction";
        public static final String FUNCTION_VARIABLES = "functionVariables";
        public static final String METRIC_PROFILE_NAME = "name";
        public static final String VALUE_TYPE = "valueType";
        public static final String SOURCE = "source";
        public static final String PERFORMANCE_PROFILE_PKG = "com.autotune.analyzer.performanceProfiles.PerformanceProfileInterface.";
        public static final String DEFAULT_PROFILE = "default";

        //Metric profile constants
        public static final String DEFAULT_API_VERSION = "recommender.com/v1";
        public static final String DEFAULT_KIND = "KruizePerformanceProfile";

        // Perf profile names
        public static final String RESOURCE_OPT_OPENSHIFT_PROFILE = "resource-optimization-openshift";
        public static final String RESOURCE_OPT_LOCAL_MON_PROFILE = "resource-optimization-local-monitoring";

        public static final Map<String, String> PerfProfileNames = Map.of(
                RESOURCE_OPT_OPENSHIFT_PROFILE, "ResourceOptimizationOpenshiftImpl",
                RESOURCE_OPT_LOCAL_MON_PROFILE, "ResourceOptimizationOpenshiftImpl"
        );
    }

    public static final class K8sObjectConstants {
        private K8sObjectConstants() {

        }

        public static final class Types {
            public static final String DEPLOYMENT = "deployment";
            public static final String DEPLOYMENT_CONFIG = "deploymentConfig";
            public static final String STATEFULSET = "statefulset";
            public static final String REPLICASET = "replicaset";
            public static final String REPLICATION_CONTROLLER = "replicationController";
            public static final String DAEMONSET = "daemonset";
            public static final String JOB = "job";

            private Types() {

            }
        }
    }

    public static final class MetricNameConstants {
        public static final String CPU_REQUEST = "cpuRequest";
        public static final String CPU_LIMIT = "cpuLimit";
        public static final String CPU_USAGE = "cpuUsage";
        public static final String CPU_THROTTLE = "cpuThrottle";
        public static final String MEMORY_REQUEST = "memoryRequest";
        public static final String MEMORY_LIMIT = "memoryLimit";
        public static final String MEMORY_USAGE = "memoryUsage";
        public static final String MEMORY_RSS = "memoryRSS";

        private MetricNameConstants() {

        }

    }

    public static final class PercentileConstants {
        public static final Integer FIFTIETH_PERCENTILE = 50;
        public static final Integer NINETIETH_PERCENTILE = 90;
        public static final Integer NINETY_FIFTH_PERCENTILE = 95;
        public static final Integer NINETY_SIXTH_PERCENTILE = 96;
        public static final Integer NINETY_SEVENTH_PERCENTILE = 97;
        public static final Integer NINETY_EIGHTH_PERCENTILE = 98;
        public static final Integer NINETY_NINTH_PERCENTILE = 99;
        public static final Integer HUNDREDTH_PERCENTILE = 100;
    }

    public static final class BooleanString {
        public static final String TRUE_DEFAULT = "True";
        public static final String FALSE_DEFAULT = "False";
        public static final String TRUE_LOWER = TRUE_DEFAULT.toLowerCase();
        public static final String TRUE = TRUE_LOWER;
        public static final String FALSE_LOWER = FALSE_DEFAULT.toLowerCase();
        public static final String FALSE = FALSE_LOWER;
        public static final String TRUE_UPPER = TRUE_DEFAULT.toUpperCase();
        public static final String FALSE_UPPER = FALSE_DEFAULT.toUpperCase();

        private BooleanString() {

        }
    }

    public static final class VersionConstants {
        public static final String CURRENT_KRUIZE_OBJECT_VERSION = "v2.0";

        private VersionConstants() {

        }

        public static final class APIVersionConstants {
            public static final String CURRENT_CREATE_EXPERIMENT_VERSION = "v2.0";
            public static final String CURRENT_UPDATE_RESULTS_VERSION = "v2.0";
            public static final String CURRENT_LIST_RECOMMENDATIONS_VERSION = "v2.0";
            public static final String CURRENT_UPDATE_RECOMMENDATIONS_VERSION = "v2.0";

            private APIVersionConstants() {

            }
        }
    }
}
