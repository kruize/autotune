/*******************************************************************************
 * Copyright (c) 2022, 2022 Red Hat, IBM Corporation and others.
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

/**
 * Constants for Autotune module
 */
public class AutotuneConstants {
    private AutotuneConstants() { }

    /**
     * Holds the constants of env vars and values to start Autotune in different Modes
     */
    public static final class StartUpMode {
        private StartUpMode() { }
        public static final String AUTOTUNE_MODE = "AUTOTUNE_MODE";
        public static final String EM_ONLY_MODE = "EM_ONLY";
    }

    public static final class HpoOperations {
        private HpoOperations() { }
        public static final String EXP_TRIAL_GENERATE_NEW = "EXP_TRIAL_GENERATE_NEW";
        public static final String EXP_TRIAL_GENERATE_SUBSEQUENT = "EXP_TRIAL_GENERATE_SUBSEQUENT";
        public static final String EXP_TRIAL_RESULT = "EXP_TRIAL_RESULT";
        public static final String ID = "id";
        public static final String URL = "url";
        public static final String OPERATION = "operation";
        public static final String SEARCHSPACE = "search_space";
    }

    public static final class JSONKeys {
        private JSONKeys() { }

        public static final String QUESTION_MARK = "?";
        public static final String AMPERSAND = "&";
        public static final String EQUALS = "=";
        public static final String PLUS = "+";
        public static final String MINUS = "-";

        public static final String REQUEST_SUM = "request_sum";
        public static final String REQUEST_COUNT = "request_count";

        public static final String CONTAINERS = "containers";
        public static final String IMAGE_NAME = "image_name";
        public static final String CONTAINER_NAME = "container_name";
        public static final String ITERATIONS = "iterations";

        // Info section
        public static final String INFO = "info";
        public static final String TRIAL_INFO = "trial_info";
        public static final String DATASOURCE_INFO = "datasource_info";
        public static final String TRIAL_ID = "trial_id";
        public static final String TRIAL_NUM = "trial_num";
        public static final String TRIAL_RESULT_URL = "trial_result_url";
        public static final String URL = "url";

        // Settings section
        public static final String TRACKERS = "trackers";
        public static final String SETTINGS = "settings";
        public static final String DEPLOYMENT_TRACKING = "deployment_tracking";
        public static final String DEPLOYMENT_POLICY = "deployment_policy";
        public static final String DEPLOYMENT_SETTINGS = "deployment_settings";
        public static final String TYPE = "type";
        public static final String DEPLOYMENT_INFO = "deployment_info";
        public static final String DEPLOYMENT_NAME = "deployment_name";
        public static final String TARGET_ENV = "target_env";
        public static final String TRIAL_SETTINGS = "trial_settings";
        public static final String TOTAL_DURATION = "total_duration";
        public static final String WARMUP_CYCLES = "warmup_cycles";
        public static final String WARMUP_DURATION = "warmup_duration";
        public static final String MEASUREMENT_CYCLES = "measurement_cycles";
        public static final String MEASUREMENT_DURATION = "measurement_duration";
        // Metadata Section
        public static final String EXPERIMENT_ID = "experiment_id";
        public static final String EXPERIMENT_NAME = "experiment_name";

        // Deployments Section
        public static final String DEPLOYMENTS = "deployments";
        public static final String NAMESPACE = "namespace";
        public static final String POD_METRICS = "pod_metrics";
        public static final String CONTAINER_METRICS = "container_metrics";
        public static final String CONFIG = "config";
        public static final String NAME = "name";
        public static final String QUERY = "query";
        public static final String DATASOURCE = "datasource";

        public static final String CPU = "cpu";
        public static final String MEMORY = "memory";
        public static final String REQUESTS = "requests";
        public static final String LIMITS = "limits";
        public static final String RESOURCES = "resources";
        public static final String CONTAINER = "container";
        public static final String TEMPLATE = "template";

        public static final String JAVA_OPTIONS = "JAVA_OPTIONS";
        public static final String JDK_JAVA_OPTIONS = "JDK_JAVA_OPTIONS";
        public static final String ENV = "ENV";
        public static final String TRIAL_RUNNING = "--Trial Running--";

        public static final String RESULT_VALUE = "result_value";
        public static final String RESULT_VALUE_TYPE = "result_value_type";

        public static final String METRIC_INFO = "metric_info";
        public static final String METRICS_RESULTS = "metrics_results";
        public static final String WARMUP_RESULTS = "warmup_results";
        public static final String MEASUREMENT_RESULTS = "measurement_results";
        public static final String SUMMARY_RESULTS = "summary_results";
        public static final String ITERATION_RESULT = "iteration_result";
        public static final String GENERAL_INFO = "general_info";
        public static final String RESULTS = "results";
        public static final String SCORE = "score";
        public static final String ERROR = "error";
        public static final String MEAN = "mean";
        public static final String MODE = "mode";
        public static final String SPIKE = "spike";
        public static final String P_50_0 = "50p";
        public static final String P_95_0 = "95p";
        public static final String P_97_0 = "97p";
        public static final String P_99_0 = "99p";
        public static final String P_99_9 = "99.9p";
        public static final String P_99_99 = "99.99p";
        public static final String P_99_999 = "99.999p";
        public static final String P_99_9999 = "99.9999p";
        public static final String P_100_0 = "100p";
        public static final String MAX = "max";
        public static final String MIN = "min";
        public static final String CYCLES = "cycles";
        public static final String DURATION = "duration";
        public static final String PERCENTILE_INFO = "percentile_info";
    }

    public static final String AUTH_MOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/";
    public static final String MINIKUBE = "minikube";
    public static final String OPENSHIFT = "openshift";
}
