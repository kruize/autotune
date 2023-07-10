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
public class KruizeConstants {
    public static final String AUTH_MOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/";
    public static final String MINIKUBE = "minikube";
    public static final String OPENSHIFT = "openshift";
    public static final String CONFIG_FILE = "KRUIZE_CONFIG_FILE";

    private KruizeConstants() {
    }

    /**
     * Holds the constants of env vars and values to start Autotune in different Modes
     */
    public static final class StartUpMode {
        public static final String AUTOTUNE_MODE = "AUTOTUNE_MODE";
        public static final String EM_ONLY_MODE = "EM_ONLY";

        private StartUpMode() {
        }
    }

    public static final class HpoOperations {
        public static final String EXP_TRIAL_GENERATE_NEW = "EXP_TRIAL_GENERATE_NEW";
        public static final String EXP_TRIAL_GENERATE_SUBSEQUENT = "EXP_TRIAL_GENERATE_SUBSEQUENT";
        public static final String EXP_TRIAL_RESULT = "EXP_TRIAL_RESULT";
        public static final String ID = "id";
        public static final String URL = "url";
        public static final String OPERATION = "operation";
        public static final String SEARCHSPACE = "search_space";

        private HpoOperations() {
        }
    }

    public static final class ExponentialBackOff {
        public static final int MAX_ELAPSED_TIME_MILLIS = 10 * 1000;   //10 Seconds;
        public static final int TIME_TO_WAIT = 1000;
        public static final double RANDOM_FACTOR = 0.5;
        public static final long INITIAL_TIME_MILLIS = 0;
        public static final double multiplier = 0.5;

        private ExponentialBackOff() {
        }
    }

    public static final class JSONKeys {
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
        public static final String METRICS = "metrics";
        public static final String CONFIG = "config";
        public static final String CURRENT = "current";
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
        public static final String AGGREGATION_INFO = "aggregation_info";
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
        public static final String FORMAT = "format";
        public static final String SUM = "sum";
        public static final String COUNT = "count";
        public static final String MEDIAN = "median";
        public static final String RANGE = "range";
        // UI support JSON keys
        public static final String DATA = "data";
        public static final String NAMESPACES = "namespaces";

        public static final String CLUSTER_NAME = "cluster_name";
        public static final String PERFORMANCE_PROFILE = "performance_profile";
        public static final String TARGET_CLUSTER = "target_cluster";
        public static final String KUBERNETES_OBJECTS = "kubernetes_objects";
        public static final String VERSION = "version";
        public static final String CONTAINER_IMAGE_NAME = "container_image_name";
        public static final String RECOMMENDATION_SETTINGS = "recommendation_settings";
        public static final String INTERVAL_START_TIME = "interval_start_time";
        public static final String INTERVAL_END_TIME = "interval_end_time";
        public static final String DURATION_IN_MINUTES = "duration_in_minutes";
        public static final String MONITORING_START_TIME = "monitoring_start_time";
        public static final String MONITORING_END_TIME = "monitoring_end_time";
        public static final String PODS_COUNT = "pods_count";
        public static final String ERROR_MSG = "error_msg";
        public static final String SHORT_TERM = "short_term";
        public static final String MEDIUM_TERM = "medium_term";
        public static final String LONG_TERM = "long_term";
        public static final String RECOMMENDATIONS = "recommendations";
        public static final String VARIATION = "variation";
        public static final String NOTIFICATIONS = "notifications";
        public static final String DURATION_BASED = "duration_based";
        public static final String PROFILE_BASED = "profile_based";

        private JSONKeys() {
        }
    }

    public static final class TimeUnitsExt {
        public static final String SECOND_LC_SINGULAR = "second";
        public static final String SECOND_LC_PLURAL = SECOND_LC_SINGULAR + "s";
        public static final String SECOND_UC_PLURAL = SECOND_LC_PLURAL.toUpperCase();
        public static final String SECOND_UC_SINGULAR = SECOND_LC_SINGULAR.toUpperCase();
        public static final String SECOND_SHORT_LC_SINGULAR = "sec";
        public static final String SECOND_SHORT_LC_PLURAL = SECOND_SHORT_LC_SINGULAR + "s";
        public static final String SECOND_SHORT_UC_PLURAL = SECOND_SHORT_LC_PLURAL.toUpperCase();
        public static final String SECOND_SHORT_UC_SINGULAR = SECOND_SHORT_LC_SINGULAR.toUpperCase();
        public static final String SECOND_SINGLE_LC = "s";
        public static final String SECOND_SINGLE_UC = SECOND_SINGLE_LC.toUpperCase();
        public static final String MINUTE_LC_SINGULAR = "minute";
        public static final String MINUTE_LC_PLURAL = MINUTE_LC_SINGULAR + "s";
        public static final String MINUTE_UC_PLURAL = MINUTE_LC_PLURAL.toUpperCase();
        public static final String MINUTE_UC_SINGULAR = MINUTE_LC_SINGULAR.toUpperCase();
        public static final String MINUTE_SHORT_LC_SINGULAR = "min";
        public static final String MINUTE_SHORT_LC_PLURAL = MINUTE_SHORT_LC_SINGULAR + "s";
        public static final String MINUTE_SHORT_UC_PLURAL = MINUTE_SHORT_LC_PLURAL.toUpperCase();
        public static final String MINUTE_SHORT_UC_SINGULAR = MINUTE_SHORT_LC_SINGULAR.toUpperCase();
        public static final String MINUTE_SINGLE_LC = "m";
        public static final String MINUTE_SINGLE_UC = MINUTE_SINGLE_LC.toUpperCase();
        public static final String HOUR_LC_SINGULAR = "hour";
        public static final String HOUR_LC_PLURAL = HOUR_LC_SINGULAR + "s";
        public static final String HOUR_UC_PLURAL = HOUR_LC_PLURAL.toUpperCase();
        public static final String HOUR_UC_SINGULAR = HOUR_LC_SINGULAR.toUpperCase();
        public static final String HOUR_SHORT_LC_SINGULAR = "hr";
        public static final String HOUR_SHORT_LC_PLURAL = HOUR_SHORT_LC_SINGULAR + "s";
        public static final String HOUR_SHORT_UC_PLURAL = HOUR_SHORT_LC_PLURAL.toUpperCase();
        public static final String HOUR_SHORT_UC_SINGULAR = HOUR_SHORT_LC_SINGULAR.toUpperCase();
        public static final String HOUR_SINGLE_LC = "h";
        public static final String HOUR_SINGLE_UC = HOUR_SINGLE_LC.toUpperCase();

        private TimeUnitsExt() {
        }
    }

    public static class TimeConv {
        public static final int NO_OF_MSECS_IN_SEC = 1000;
        public static int NO_OF_SECONDS_PER_MINUTE = 60;
        public static int NO_OF_MINUTES_PER_HOUR = 60;
        public static int NO_OF_HOURS_PER_DAY = 24;
        public static int MEASUREMENT_DURATION_THRESHOLD_SECONDS = 30;

        private TimeConv() {
        }
    }

    public static class Patterns {
        public static final String DURATION_PATTERN = "(\\d+)([a-zA-Z]+)";
        public static final String WHITESPACE_PATTERN = "\\s";
        public static final String QUERY_WITH_TIME_RANGE_PATTERN = ".*\\[(\\d+)([a-zA-Z]+)\\].*";

        private Patterns() {
        }
    }

    public static class SupportedDatasources {
        public static final String PROMETHEUS = "prometheus";

        private SupportedDatasources() {
        }
    }

    public static class HttpConstants {
        private HttpConstants() {
        }

        public static class MethodType {
            public static final String GET = "GET";

            private MethodType() {
            }
        }
    }

    public static class CycleTypes {
        public static final String WARMUP = "WarmUpCycle";
        public static final String MEASUREMENT = "MeasurementCycle";

        private CycleTypes() {
        }
    }

    public static class ConvUnits {
        private ConvUnits() {
        }

        public static class Memory {
            public static final int BITS_IN_BYTE = 8;
            private static final int PHYSICAL_STANDARD = 1000;
            public static final int BYTES_IN_KILOBYTES = PHYSICAL_STANDARD;
            public static final int KILOBYTES_IN_MEGABYTES = PHYSICAL_STANDARD;
            public static final int MEGABYTES_IN_GIGABYTES = PHYSICAL_STANDARD;
            private static final int BINARY_STANDARD = 1024;
            public static final int BYTES_IN_KIBIBYTES = BINARY_STANDARD;
            public static final int KIBIBYTES_IN_MEBIBYTES = BINARY_STANDARD;
            public static final int MEBIBYTES_IN_GIBIBYTES = BINARY_STANDARD;
            private static final double INVERSE_PHYSICAL_STANDARD = 0.001;
            // Inverse
            public static final double BYTES_TO_KILOBYTES = INVERSE_PHYSICAL_STANDARD;
            public static final double KILOBYTES_TO_MEGABYTES = INVERSE_PHYSICAL_STANDARD;
            public static final double MEGABYTES_TO_GIGABYTES = INVERSE_PHYSICAL_STANDARD;
            private static final double INVERSE_BINARY_STANDARD = 0.0009765625;
            public static final double BYTES_TO_KIBIBYTES = INVERSE_BINARY_STANDARD;
            public static final double KIBIBYTES_TO_MEBIBYTES = INVERSE_BINARY_STANDARD;
            public static final double MEBIBYTES_TO_GIBIBYTES = INVERSE_BINARY_STANDARD;

            private Memory() {
            }
        }
    }

    public static class ErrorMsgs {
        private ErrorMsgs() {
        }

        public static class APIErrorMsgs {
            private APIErrorMsgs() {
            }

            public static class ListDeploymentsInNamespace {
                public static final String INVALID_NAMESPACE = "Given Namespace is invalid";
                public static final String NO_NAMESPACE_SENT = "Please pass a namespace to get the deployments";
                public static final String EMPTY_NAMESPACE = "Namespace cannot be empty";

                private ListDeploymentsInNamespace() {
                }
            }
        }
    }

    /**
     * This class contains Constants used for CORS
     */
    public static class CORSConstants {
        /**
         * Wildcard to match all the Paths (Endpoints)
         */
        public static final String PATH_WILDCARD = "/*";
        public static final String ALLOWED_ORIGINS = "*";
        public static final String ALLOWED_METHODS = "POST, GET";
        public static final String ALLOWED_HEADERS = "X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept";
        public static final String MAX_AGE = "1728000";

        private CORSConstants() {

        }
    }

    public static final class DBConstants {
        public static final String CONFIG_FILE = "DB_CONFIG_FILE";

        private DBConstants() {
        }
    }

    public static final class DateFormats {
        public static final String STANDARD_JSON_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        public static final String DB_EXTRACTION_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

        private DateFormats() {

        }
    }

    /**
     * In order to assign values to the static variables of KruizeDeploymentInfo
     * using Java reflection, the class variables are utilized, and therefore,
     * if any new variables are added, their corresponding declaration is necessary.
     */
    public static final class DATABASE_ENV_NAME {
        public static final String DATABASE_ADMIN_USERNAME = "database_adminusername";
        public static final String DATABASE_ADMIN_PASSWORD = "database_adminpassword";
        public static final String DATABASE_USERNAME = "database_username";
        public static final String DATABASE_PASSWORD = "database_password";
        public static final String DATABASE_HOSTNAME = "database_hostname";
        public static final String DATABASE_DBNAME = "database_name";
        public static final String DATABASE_PORT = "database_port";
        public static final String DATABASE_SSL_MODE = "database_sslmode";
    }

    /**
     * In order to assign values to the static variables of KruizeDeploymentInfo
     * using Java reflection, the class variables are utilized, and therefore,
     * if any new variables are added, their corresponding declaration is necessary.
     */
    public static final class KRUIZE_CONFIG_ENV_NAME {
        public static final String K8S_TYPE = "k8stype";
        public static final String AUTH_TYPE = "authtype";
        public static final String AUTH_TOKEN = "authtoken";
        public static final String MONITORING_AGENT = "monitoringagent";
        public static final String MONITORING_SERVICE = "monitoringservice";
        public static final String MONITORING_AGENT_ENDPOINT = "monitoringendpoint";
        public static final String CLUSTER_TYPE = "clustertype";
        public static final String AUTOTUNE_MODE = "autotunemode";
        public static final String EM_ONLY_MODE = "emonly";
        public static final String SETTINGS_SAVE_TO_DB = "savetodb";
        public static final String SETTINGS_DB_DRIVER = "dbdriver";
        public static final String SETTINGS_HIBERNATE_DIALECT = "hibernate_dialect";
        public static final String SETTINGS_HIBERNATE_CONNECTION_DRIVER_CLASS = "hibernate_driver";
        public static final String SETTINGS_HIBERNATE_C3P0_MIN_SIZE = "hibernate_c3p0minsize";
        public static final String SETTINGS_HIBERNATE_C3P0_MAX_SIZE = "hibernate_c3p0maxsize";
        public static final String SETTINGS_HIBERNATE_C3P0_TIMEOUT = "hibernate_c3p0timeout";
        public static final String SETTINGS_HIBERNATE_C3P0_MAX_STATEMENTS = "hibernate_c3p0maxstatements";
        public static final String SETTINGS_HIBERNATE_HBM2DDL_AUTO = "hibernate_hbm2ddlauto";
        public static final String SETTINGS_HIBERNATE_SHOW_SQL = "hibernate_showsql";
        public static final String SETTINGS_HIBERNATE_TIME_ZONE = "hibernate_timezone";
    }

    public static final class RecommendationEngineConstants {
        private RecommendationEngineConstants() {

        }

        public static final class DurationBasedEngine {
            private DurationBasedEngine() {

            }

            public static final class DurationAmount {
                private DurationAmount() {

                }

                public static final int SHORT_TERM_DURATION_DAYS = 1;
                public static final int MEDIUM_TERM_DURATION_DAYS = 7;
                public static final int LONG_TERM_DURATION_DAYS = 15;
            }

            public static final class RecommendationDurationRanges {
                private RecommendationDurationRanges() {

                }

                private static final double BUFFER_VALUE_IN_MINS = (TimeConv.MEASUREMENT_DURATION_THRESHOLD_SECONDS / TimeConv.NO_OF_SECONDS_PER_MINUTE);
                /* SHORT TERM */
                public static final double SHORT_TERM_TOTAL_DURATION_UPPER_BOUND_MINS =
                        (DurationAmount.SHORT_TERM_DURATION_DAYS * TimeConv.NO_OF_HOURS_PER_DAY * TimeConv.NO_OF_MINUTES_PER_HOUR) + BUFFER_VALUE_IN_MINS;

                public static final double SHORT_TERM_TOTAL_DURATION_LOWER_BOUND_MINS =
                        (DurationAmount.SHORT_TERM_DURATION_DAYS * TimeConv.NO_OF_HOURS_PER_DAY * TimeConv.NO_OF_MINUTES_PER_HOUR) - BUFFER_VALUE_IN_MINS;

                /* MEDIUM TERM */
                public static final double MEDIUM_TERM_TOTAL_DURATION_UPPER_BOUND_MINS =
                        (DurationAmount.MEDIUM_TERM_DURATION_DAYS * TimeConv.NO_OF_HOURS_PER_DAY * TimeConv.NO_OF_MINUTES_PER_HOUR) + BUFFER_VALUE_IN_MINS;
                public static final double MEDIUM_TERM_TOTAL_DURATION_LOWER_BOUND_MINS =
                        (DurationAmount.MEDIUM_TERM_DURATION_DAYS * TimeConv.NO_OF_HOURS_PER_DAY * TimeConv.NO_OF_MINUTES_PER_HOUR) - BUFFER_VALUE_IN_MINS;

                /* LONG TERM */
                public static final double LONG_TERM_TOTAL_DURATION_UPPER_BOUND_MINS =
                        (DurationAmount.LONG_TERM_DURATION_DAYS * TimeConv.NO_OF_HOURS_PER_DAY * TimeConv.NO_OF_MINUTES_PER_HOUR) + BUFFER_VALUE_IN_MINS;
                public static final double LONG_TERM_TOTAL_DURATION_LOWER_BOUND_MINS =
                        (DurationAmount.LONG_TERM_DURATION_DAYS * TimeConv.NO_OF_HOURS_PER_DAY * TimeConv.NO_OF_MINUTES_PER_HOUR) - BUFFER_VALUE_IN_MINS;

            }
        }


    }
}
