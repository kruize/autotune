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
    public static class StartUpMode {
        private StartUpMode() { }
        public static final String AUTOTUNE_MODE = "AUTOTUNE_MODE";
        public static final String EM_ONLY_MODE = "EM_ONLY";
    }

    public static class JSONKeys {
        private JSONKeys() { }
        public static final String CONTAINERS = "containers";
        public static final String IMAGE_NAME = "image_name";
        public static final String CONTAINER_NAME = "container_name";
        public static final String ITERATIONS = "iterations";

        // Info section
        public static String INFO = "info";
        public static String TRIAL_INFO = "trial_info";
        public static String DATASOURCE_INFO = "datasource_info";
        public static String TRIAL_ID = "trial_id";
        public static String TRIAL_NUM = "trial_num";
        public static String TRIAL_RESULT_URL = "trial_result_url";
        public static String URL = "url";
        // Settings section
        public static String TRACKERS = "trackers";
        public static String SETTINGS = "settings";
        public static String DEPLOYMENT_TRACKING = "deployment_tracking";
        public static String DEPLOYMENT_POLICY = "deployment_policy";
        public static String DEPLOYMENT_SETTINGS = "deployment_settings";
        public static String TYPE = "type";
        public static String DEPLOYMENT_INFO = "deployment_info";
        public static String DEPLOYMENT_NAME = "deployment_name";
        public static String TARGET_ENV = "target_env";
        public static String TRIAL_SETTINGS = "trial_settings";
        public static String TOTAL_DURATION = "total_duration";
        public static String WARMUP_CYCLES = "warmup_cycles";
        public static String WARMUP_DURATION = "warmup_duration";
        public static String MEASUREMENT_CYCLES = "measurement_cycles";
        public static String MEASUREMENT_DURATION = "measurement_duration";
        // Metadata Section
        public static String EXPERIMENT_ID = "experiment_id";
        public static String EXPERIMENT_NAME = "experiment_name";

        // Deployments Section
        public static String DEPLOYMENTS = "deployments";
        public static String NAMESPACE = "namespace";
        public static String POD_METRICS = "pod_metrics";
        public static String CONTAINER_METRICS = "container_metrics";
        public static String CONFIG = "config";
        public static String NAME = "name";
        public static String QUERY = "query";
        public static String DATASOURCE = "datasource";
        public static String METRIC_INFO = "metric_info";
        public static String METRICS_RESULTS = "metrics_results";
        public static String WARMUP_RESULTS = "warmup_results";
        public static String MEASUREMENT_RESULTS = "measurement_results";
        public static String ITERATION_RESULT = "iteration_result";
        public static String GENERAL_INFO = "general_info";
        public static String RESULTS = "results";
        public static String SCORE = "score";
        public static String ERROR = "error";
        public static String MEAN = "mean";
        public static String MODE = "mode";
        public static String SPIKE = "spike";
        public static String P_50_0 = "50p";
        public static String P_95_0 = "95p";
        public static String P_97_0 = "97p";
        public static String P_99_0 = "99p";
        public static String P_99_9 = "99.9p";
        public static String P_99_99 = "99.99p";
        public static String P_99_999 = "99.999p";
        public static String P_99_9999 = "99.9999p";
        public static String P_100_0 = "100p";
        public static String CYCLES = "cycles";
        public static String DURATION = "duration";
        public static String PERCENTILE_INFO = "percentile_info";
    }
}
