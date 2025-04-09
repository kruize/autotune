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

import com.autotune.analyzer.kruizeObject.CreateExperimentConfigBean;
import com.autotune.analyzer.recommendations.model.RecommendationTunables;
import com.autotune.analyzer.serviceObjects.BulkJobStatus;
import com.autotune.analyzer.utils.AnalyzerConstants;
import org.apache.kafka.common.protocol.types.Field;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationEngine.PercentileConstants.*;

/**
 * Constants for Autotune module
 */
public class KruizeConstants {
    public static final String AUTH_MOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/";
    public static final String MINIKUBE = "minikube";
    public static final String OPENSHIFT = "openshift";
    public static final String CONFIG_FILE = "KRUIZE_CONFIG_FILE";
    public static final String SQL_EXCEPTION_HELPER_PKG = "org.hibernate.engine.jdbc.spi";

    private KruizeConstants() {
    }

    public static enum KRUIZE_RECOMMENDATION_API_VERSION {
        V1_0("1.0"),
        LATEST("1.0");
        private final String versionNumber;

        KRUIZE_RECOMMENDATION_API_VERSION(String versionNumber) {
            this.versionNumber = versionNumber;
        }

        public String getVersionNumber() {
            return versionNumber;
        }

    }

    public static class APIMessages {
        public static final String MAX_DAY = "maxDay : %s";
        public static final String SUCCESS = "success";
        public static final String FAILURE = "failure";
        public static final String GET = "get";
        public static final String SET = "set";
        public static final String CONTAINER_USAGE_INFO = "Determine the date of the last activity for the container based on its usage. ";
        public static final String NAMESPACE_USAGE_INFO = "Determine the date of the last activity for the namespace based on its usage. ";
        public static final String RECOMMENDATION_TERM = "recommendationTerm : %s";
        public static final String MONITORING_START_TIME = "monitoringStartTime : %s";
        public static final String EXPERIMENT_DATASOURCE = "Experiment: %s,  Datasource: %s";
        public static final String UPDATE_RECOMMENDATIONS_INPUT_PARAMS = "experiment_name : %s and interval_start_time : %s and interval_end_time : %s ";
        public static final String UPDATE_RECOMMENDATIONS_SUCCESS = "Update Recommendation API success response, experiment_name: %s and interval_end_time : %s";
        public static final String UPDATE_RECOMMENDATIONS_FAILURE = "UpdateRecommendations API failure response, experiment_name: %s and intervalEndTimeStr : %s";
        public static final String UPDATE_RECOMMENDATIONS_RESPONSE = "Update Recommendation API response: %s";
        public static final String UPDATE_RECOMMENDATIONS_FAILURE_MSG = "UpdateRecommendations API failed for experiment_name: %s and intervalEndTimeStr : %s due to %s";
    }

    public static class MetricProfileAPIMessages {
        public static final String CREATE_METRIC_PROFILE_SUCCESS_MSG = "Metric Profile : %s created successfully.";
        public static final String VIEW_METRIC_PROFILES_MSG = " View Metric Profiles at /listMetricProfiles";
        public static final String LOAD_METRIC_PROFILE_FAILURE = "Failed to load saved metric profile data: {}";
        public static final String ADD_METRIC_PROFILE_TO_DB_WITH_VERSION = "Added Metric Profile : {} into the DB with version: {}";
        public static final String DELETE_METRIC_PROFILE_SUCCESS_MSG = "Metric profile: %s deleted successfully.";
        public static final String DELETE_METRIC_PROFILE_FROM_DB_SUCCESS_MSG = "Metric profile deleted successfully from the DB.";
    }

    public static class MetricProfileConstants {
        public static final String CHECKING_AVAILABLE_METRIC_PROFILE_FROM_DB = "Checking available metric profiles from database: ";
        public static final String NO_METRIC_PROFILE_FOUND_IN_DB = "No metric profile found in database.";
        public static final String METRIC_PROFILE_FOUND = "MetricProfile found: ";
        public static final String ADDING_METRIC_PROFILE = "Trying to add the metric profile to collection: ";
        public static final String METRIC_PROFILE_ALREADY_EXISTS = "MetricProfile already exists: ";
        public static final String METRIC_PROFILE_ADDED = "MetricProfile added to the collection successfully: {}";
        public static final String METRIC_PROFILE_FILE_PATH = "MetricProfile file path: {}";

        public static class MetricProfileErrorMsgs {
            public static final String ADD_DEFAULT_METRIC_PROFILE_EXCEPTION = "Exception occurred while adding default Metric profile: {}";
            public static final String SET_UP_DEFAULT_METRIC_PROFILE_ERROR = "Failed to set up default MetricProfile due to: {}";
            public static final String FILE_NOT_FOUND_ERROR = "File not found: {}";
            public static final String FILE_READ_ERROR_ERROR_MESSAGE = "Failed to read the JSON file from the specified path: {}";
            public static final String ADD_METRIC_PROFILE_TO_DB_ERROR = "Failed to add Metric Profile due to {}";
            public static final String METRIC_PROFILE_VALIDATION_FAILURE = "Validation failed: {}";

            public MetricProfileErrorMsgs() {
            }
        }
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

    public static final class CostBasedRecommendationConstants {

        public static final RecommendationTunables COST_RECOMMENDATION_TUNABLES = new RecommendationTunables(COST_CPU_PERCENTILE, COST_MEMORY_PERCENTILE, COST_ACCELERATOR_PERCENTILE);

    }
    public static final class PerformanceBasedRecommendationConstants {

        public static final RecommendationTunables PERFORMANCE_RECOMMENDATION_TUNABLES = new RecommendationTunables(PERFORMANCE_CPU_PERCENTILE, PERFORMANCE_MEMORY_PERCENTILE, PERFORMANCE_ACCELERATOR_PERCENTILE);

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
        public static final String EXPERIMENT_TYPE = "experiment_type";
        // Deployments Section
        public static final String DEPLOYMENTS = "deployments";
        public static final String NAMESPACE = "namespace";
        public static final String NAMESPACE_NAME = "namespace_name";
        public static final String POD_METRICS = "pod_metrics";
        public static final String CONTAINER_METRICS = "container_metrics";
        public static final String METRICS = "metrics";
        public static final String CONFIG = "config";
        public static final String METRIC = "metric";
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
        public static final String AVG = "avg";
        public static final String COUNT = "count";
        public static final String MEDIAN = "median";
        public static final String RANGE = "range";
        public static final String CORES = "cores";
        public static final String BYTES = "bytes";

        // Datasource JSON keys
        public static final String DATASOURCES = "datasources";
        public static final String DATASOURCE_NAME = "datasource_name";

        // UI support JSON keys
        public static final String DATA = "data";
        public static final String NAMESPACES = "namespaces";

        public static final String CLUSTER_NAME = "cluster_name";
        public static final String PERFORMANCE_PROFILE = "performance_profile";
        public static final String METADATA_PROFILE = "metadata_profile";
        public static final String TARGET_CLUSTER = "target_cluster";
        public static final String KUBERNETES_OBJECTS = "kubernetes_objects";
        public static final String VERSION = "version";
        public static final String CONTAINER_IMAGE_NAME = "container_image_name";
        public static final String RECOMMENDATION_SETTINGS = "recommendation_settings";
        public static final String INTERVAL_START_TIME = "interval_start_time";

        public static final String CALCULATED_START_TIME = "calculated_start_time";
        public static final String INTERVAL_END_TIME = "interval_end_time";
        public static final String DURATION_IN_MINUTES = "duration_in_minutes";
        public static final String DURATION_IN_HOURS = "duration_in_hours";
        public static final String MONITORING_START_TIME = "monitoring_start_time";
        public static final String MONITORING_END_TIME = "monitoring_end_time";
        public static final String PODS_COUNT = "pods_count";
        public static final String ERROR_MSG = "error_msg";
        public static final String SHORT_TERM = "short_term";
        public static final String MEDIUM_TERM = "medium_term";
        public static final String LONG_TERM = "long_term";
        public static final String SHORT = "short";
        public static final String MEDIUM = "medium";
        public static final String LONG = "long";
        public static final String RECOMMENDATIONS = "recommendations";
        public static final String VARIATION = "variation";
        public static final String NOTIFICATIONS = "notifications";
        public static final String DURATION_BASED = "duration_based";
        public static final String PROFILE_BASED = "profile_based";
        public static final String COST = "cost";
        public static final String PERFORMANCE = "performance";
        public static final String RECOMMENDATION_TERMS = "recommendation_terms";
        public static final String RECOMMENDATION_ENGINES = "recommendation_engines";
        public static final String RECOMMENDATION_MODELS = "recommendation_models";
        public static final String PLOTS_DATAPOINTS = "datapoints";
        public static final String PLOTS_DATA = "plots_data";
        public static final String CONFIDENCE_LEVEL = "confidence_level";
        public static final String HOSTNAME = "Hostname";
        public static final String UUID = "UUID";
        public static final String DEVICE = "device";
        public static final String MODEL_NAME = "modelName";
        public static final String GPU_PROFILE = "GPU_I_PROFILE";

        // Config changes JSON Keys
        public static final String MODEL_SETTINGS = "model_settings";
        public static final String TERM_SETTINGS = "term_settings";

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

        public static final class TimeZones {
            public static final String UTC = "UTC";
        }
    }

    public static class TimeConv {
        public static final int NO_OF_MSECS_IN_SEC = 1000;
        public static final int MEASUREMENT_DURATION_THRESHOLD_SECONDS = 30;
        public static final int MEASUREMENT_DURATION_THRESHOLD_MINUTES = 15;
        public static int NO_OF_SECONDS_PER_MINUTE = 60;
        public static int NO_OF_MINUTES_PER_HOUR = 60;
        public static int NO_OF_HOURS_PER_DAY = 24;
        public static int NO_OF_HOURS_IN_7_DAYS = 168;
        public static int NO_OF_HOURS_15_DAYS = 360;

        private TimeConv() {
        }
    }

    public static class Patterns {
        public static final String DURATION_PATTERN = "(\\d+)([a-zA-Z]+)";
        public static final String WHITESPACE_PATTERN = "\\s";
        public static final String QUERY_WITH_TIME_RANGE_PATTERN = ".*\\[(\\d+)([a-zA-Z]+)\\].*";
        public static final String CLOUDWATCH_LOG_PATTERN = "%d{yyyy-MM-ddHH:mm:ss.SSS} %level [%t][%F(%L)]-%msg%n";

        private Patterns() {
        }
    }

    public static class SupportedDatasources {
        public static final String PROMETHEUS = "prometheus";
        public static final String THANOS = "thanos";

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

    public static class DataSourceConstants {
        public static final String DATASOURCE_NAME = "name";
        public static final String DATASOURCE_PROVIDER = "provider";
        public static final String DATASOURCE_SERVICE_NAME = "serviceName";
        public static final String DATASOURCE_SERVICE_NAMESPACE = "namespace";
        public static final String DATASOURCE_URL = "url";
        public static final String KRUIZE_DATASOURCE = "datasource";
        public static final String SERVICE_DNS = ".svc.cluster.local";
        public static final String PROMETHEUS_DEFAULT_SERVICE_PORT = "9090";
        public static final String OPENSHIFT_MONITORING_PROMETHEUS_DEFAULT_SERVICE_PORT = "9091";
        public static final String PROMETHEUS_REACHABILITY_QUERY = "up";
        public static final String DATASOURCE_ENDPOINT_WITH_QUERY_RANGE = "%s/api/v1/query_range?query=%s&start=%s&end=%s&step=%s";
        public static final String DATE_ENDPOINT_WITH_QUERY = "%s/api/v1/query?query=%s";

        private DataSourceConstants() {
        }

        public static class DataSourceDetailsInfoConstants {
            public static final String version = "v1.0";
            public static final String CLUSTER_NAME = "default";

            private DataSourceDetailsInfoConstants() {
            }
        }

        public static class DataSourceInfoMsgs {
            public static final String ADDING_DATASOURCE = "Trying to add the datasource to collection: {} - {}";
            public static final String VERIFYING_DATASOURCE_REACHABILITY = "Verifying datasource reachability status: {}";
            public static final String CHECKING_AVAILABLE_DATASOURCE = "Checking available datasources:";
            public static final String CHECKING_AVAILABLE_DATASOURCE_FROM_DB = "Checking available datasources from database:";
            public static final String NO_DATASOURCE_FOUND_IN_DB = "No datasource found in database.";
            public static final String CHECK_DATASOURCE_UPDATES = "Datasource {} already exists, Checking for updates...";
            public static final String DATASOURCE_AUTH_CHANGED = "Authentication details for datasource {} have changed. Checking if the datasource is serviceable with the new config...";
            public static final String DATASOURCE_AUTH_UNCHANGED = "No changes detected in the authentication details for datasource {}";

            private DataSourceInfoMsgs() {
            }

        }

        public static class DataSourceSuccessMsgs {

            public static final String DATASOURCE_ADDED = "Datasource added to the collection successfully.";
            public static final String DATASOURCE_ADDED_DB = "Datasource added to the database successfully.";
            public static final String DATASOURCE_FOUND = "Datasource found: ";
            public static final String DATASOURCE_SERVICEABLE = "Datasource is serviceable.";
            public static final String DATASOURCE_AUTH_ADDED_DB = "Auth details added to the DB successfully.";
            public static final String DATASOURCE_AUTH_UPDATED_DB = "Auth details updated in the DB successfully.";

            private DataSourceSuccessMsgs() {
            }
        }

        public static class DataSourceErrorMsgs {
            public static final String MISSING_DATASOURCE_NAME = "Datasource name cannot be empty.";
            public static final String MISSING_DATASOURCE_PROVIDER = "Datasource provider cannot be empty.";
            public static final String MISSING_DATASOURCE_NAMESPACE = "Datasource namespace cannot be empty.";
            public static final String DATASOURCE_URL_SERVICENAME_BOTH_SET = "Datasource url and servicename both can not be set.";
            public static final String MISSING_DATASOURCE_SERVICENAME_AND_URL = "Datasource servicename and url both cannot be empty.";
            public static final String UNSUPPORTED_DATASOURCE_PROVIDER = "Datasource provider is invalid.";
            public static final String DATASOURCE_NOT_SERVICEABLE = "Datasource is not serviceable.";
            public static final String DATASOURCE_CONNECTION_FAILED = "Datasource connection refused or timed out.";
            public static final String DATASOURCE_DB_LOAD_FAILED = "Loading saved datasource {} details from db failed: {}";
            public static final String DATASOURCE_DB_AUTH_LOAD_FAILED = "Loading datasource {} AUTH details failed: {}";
            public static final String DATASOURCE_ALREADY_EXIST = "Datasource with the name already exist.";
            public static final String DATASOURCE_NOT_EXIST = "Datasource with the name does not exist.";
            public static final String INVALID_DATASOURCE_URL = "Datasource url is not valid.";
            public static final String DATASOURCE_NOT_SUPPORTED = "Datasource is not supported: ";
            public static final String SERVICE_NOT_FOUND = "Can not find service with specified name.";
            public static final String ENDPOINT_NOT_FOUND = "Service endpoint not found.";
            public static final String MISSING_DATASOURCE_INFO = "Datasource is missing, add a valid Datasource";
            public static final String INVALID_DATASOURCE_INFO = "Datasource is either missing or is invalid: ";
            public static final String MISSING_DATASOURCE_AUTH = "Auth details are missing for datasource: {}";
            public static final String DATASOURCE_AUTH_DB_INSERTION_FAILED = "Failed to add auth details to DB: {}";
            public static final String DATASOURCE_AUTH_DB_UPDATE_FAILED = "Failed to update auth details in the DB: {}";
            public static final String DATASOURCE_AUTH_UPDATE_INVALID = "The updated authentication configuration is invalid. Reverting to the previous configuration.";

            private DataSourceErrorMsgs() {
            }
        }

        public static class DataSourceQueryJSONKeys {
            public static final String STATUS = "status";
            public static final String DATA = "data";
            public static final String RESULT = "result";
            public static final String METRIC = "metric";
            public static final String VALUE = "value";
            public static final String VALUES = "values";

            private DataSourceQueryJSONKeys() {
            }

        }

        public static class DataSourceQueryStatus {
            public static final String SUCCESS = "success";
            public static final String ERROR = "error";

            private DataSourceQueryStatus() {
            }
        }

        public static class DataSourceQueryMetricKeys {
            public static final String NAMESPACE = "namespace";
            public static final String WORKLOAD = "workload";
            public static final String WORKLOAD_TYPE = "workload_type";
            public static final String CONTAINER_NAME = "container";
            public static final String CONTAINER_IMAGE_NAME = "image";

            private DataSourceQueryMetricKeys() {
            }
        }

        public static class DataSourceMetadataInfoConstants {
            public static final String version = "v1.0";
            public static final String CLUSTER_NAME = "default";

            private DataSourceMetadataInfoConstants() {
            }
        }

        public static class DataSourceMetadataSuccessMsgs {
            public static final String METADATA_ADDED = "Metadata added to the DB successfully.";
            public static final String DATASOURCE_DELETED = "Successfully deleted datasource: ";
            public static final String DATASOURCE_FOUND = "Datasource found: ";
            public static final String DATASOURCE_SERVICEABLE = "Datasource is serviceable.";
            public static final String DATASOURCE_NOT_SERVICEABLE = "Datasource is not serviceable.";

            private DataSourceMetadataSuccessMsgs() {

            }
        }

        public static class DataSourceMetadataErrorMsgs {
            public static final String METADATA_EXIST = "Metadata already exists for datasource: {}!";
            public static final String METADATA_LOAD_FROM_DB = "Failed to load metadata for the datasource: {}: {} ";
            public static final String MISSING_DATASOURCE_METADATA_DATASOURCE_NAME = "DataSourceMetadata Datasource name cannot be empty";
            public static final String MISSING_DATASOURCE_METADATA_WORKLOAD_MAP = "DataSourceMetadata Workload data cannot be empty or null";
            public static final String MISSING_DATASOURCE_METADATA_CONTAINER_MAP = "DataSourceMetadata Container data cannot be empty or null";
            public static final String MISSING_DATASOURCE_METADATA_INFO_OBJECT = "DataSourceMetadataInfo Object cannot be null";
            public static final String MISSING_DATASOURCE_METADATA_DATASOURCE_OBJECT = "DataSourceMetadata DataSource Object cannot be empty or null";
            public static final String NAMESPACE_JSON_PARSING_ERROR = "Error parsing namespace JSON array: ";
            public static final String WORKLOAD_JSON_PARSING_ERROR = "Error parsing workload JSON array: ";
            public static final String CONTAINER_JSON_PARSING_ERROR = "Error parsing container JSON array: ";
            public static final String DATASOURCE_METADATA_INFO_CREATION_ERROR = "Error creating DataSourceMetadataInfo: ";
            public static final String WORKLOAD_METADATA_UPDATE_ERROR = "Error updating DataSourceMetadataInfo with workload metadata: ";
            public static final String CONTAINER_METADATA_UPDATE_ERROR = "Error updating DataSourceMetadataInfo with container metadata: ";
            public static final String NAMESPACE_METADATA_UPDATE_ERROR = "Error updating DataSourceMetadataInfo with namespace metadata: ";
            public static final String NAMESPACE_MAP_NOT_POPULATED = "The namespaceMap is not populated, is either null or empty.";
            public static final String NAMESPACE_WORKLOAD_MAP_NOT_POPULATED = "The namespaceWorkloadMap is not populated, is either null or empty.";
            public static final String WORKLOAD_CONTAINER_MAP_NOT_POPULATED = "The workloadContainerMap is not populated, is either null or empty.";
            public static final String INVALID_DATASOURCE_METADATA_CLUSTER = "dataSourceCluster object is null";
            public static final String INVALID_DATASOURCE_METADATA_NAMESPACE = "dataSourceNamespace object is null";
            public static final String INVALID_DATASOURCE_METADATA_NAMESPACE_DATA = "namespaceHashMap is either null or empty";
            public static final String DATASOURCE_METADATA_INFO_NOT_AVAILABLE = "DataSourceMetadataInfo is null. Metadata is not populated.";
            public static final String DATASOURCE_METADATA_DATASOURCE_NOT_AVAILABLE = "DataSource information is not available for the specified DataSource: ";
            public static final String DATASOURCE_METADATA_CLUSTER_NOT_AVAILABLE = "DataSourceCluster information is not available for the specified Cluster {} and DataSource {}";
            public static final String DATASOURCE_METADATA_NAMESPACE_NOT_AVAILABLE = "DataSourceNamespace information is not available for the specified Namespace {}, Cluster {} and DataSource {}";
            public static final String SET_CLUSTER_MAP_ERROR = "clusterHashMap is null, no clusters provided for cluster group: ";
            public static final String SET_WORKLOAD_MAP_ERROR = "workloadHashMap is null, no workloads provided for namespace: ";
            public static final String SET_CONTAINER_MAP_ERROR = "containerHashMap is null, no containers provided for workload: ";
            public static final String SET_NAMESPACE_MAP_ERROR = "namespaceHashMap is null, no namespaces provided for cluster: ";
            public static final String LOAD_DATASOURCE_FROM_DB_ERROR = "Error loading datasource - %s from DB: %s";
            public static final String LOAD_DATASOURCE_METADATA_TO_DB_ERROR = "Failed to add metadata to DB: {}";
            public static final String LOAD_DATASOURCE_METADATA_FROM_DB_ERROR = "Error loading datasource - %s from DB: %s";
            public static final String DATASOURCE_METADATA_VALIDATION_FAILURE_MSG = "Validation of imported metadata failed, mandatory fields missing: %s";
            public static final String NAMESPACE_QUERY_VALIDATION_FAILED = "Validation failed for namespace data query.";
            public static final String DATASOURCE_OPERATOR_RETRIEVAL_FAILURE = "Failed to retrieve data source operator for provider: %s";

            private DataSourceMetadataErrorMsgs() {
            }
        }

        public static class DataSourceMetadataInfoJSONKeys {
            public static final String DATASOURCES = "datasources";
            public static final String DATASOURCE_NAME = "datasource_name";
            public static final String CLUSTERS = "clusters";
            public static final String CLUSTER_NAME = "cluster_name";
            public static final String NAMESPACES = "namespaces";
            public static final String NAMESPACE = "namespace";
            public static final String WORKLOADS = "workloads";
            public static final String WORKLOAD_NAME = "workload_name";
            public static final String WORKLOAD_TYPE = "workload_type";
            public static final String CONTAINERS = "containers";
            public static final String CONTAINER_NAME = "container_name";
            public static final String CONTAINER_IMAGE_NAME = "container_image_name";

            private DataSourceMetadataInfoJSONKeys() {
            }
        }

        public static class DataSourceMetadataInfoSuccessMsgs {
            public static final String DATASOURCE_METADATA_DELETED = "Datasource metadata deleted successfully.";

            private DataSourceMetadataInfoSuccessMsgs() {
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

        public static class RecommendationErrorMsgs {
            private RecommendationErrorMsgs() {

            }

            public static final String AMT_FORMAT_IS_NULL = "Invalid input: 'amount' and 'format' cannot be null";
            public static final String CPU_UNSUPPORTED_FORMAT = "Unsupported format for CPU conversion: ";
            public static final String ACCELERATOR_UNSUPPORTED_FORMAT = "Unsupported format for Accelerator conversion: ";
            public static final String INPUT_NULL = "Input object cannot be null";
            public static final String VALUE_NEGATIVE = "Value cannot be negative";
            public static final String INVALID_MEM_FORMAT = "Invalid format: Supported formats are bytes, KB, KiB, MB, MiB, GB, GiB, etc.";
            public static final String EMPTY_NOTIFICATIONS_OBJECT ="Notifications Object passed is empty. The notifications are not sent as part of recommendation.";
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
        public static final long MILLI_SECONDS_FOR_DAY = 24 * 60 * 60 * 1000;
        public static final long MINUTES_FOR_DAY = 24 * 60;
        public static SimpleDateFormat simpleDateFormatForUTC = new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, Locale.ROOT);

        private DateFormats() {
            simpleDateFormatForUTC.setTimeZone(TimeZone.getTimeZone(KruizeConstants.TimeUnitsExt.TimeZones.UTC));
        }
    }

    /**
     * In order to assign values to the static variables of KruizeDeploymentInfo
     * using Java reflection, the class variables are utilized, and therefore,
     * if any new variables are added, their corresponding declaration is necessary.
     * Ref InitializeDeployment.setConfigValues(KruizeConstants.CONFIG_FILE, KruizeConstants.DATABASE_ENV_NAME.class);
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
     * Ref InitializeDeployment.setConfigValues(KruizeConstants.CONFIG_FILE, KruizeConstants.KRUIZE_CONFIG_ENV_NAME.class);
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
        public static final String BULK_UPDATE_RESULTS_LIMIT = "bulkresultslimit";
        public static final String DELETE_PARTITION_THRESHOLD_IN_DAYS = "deletepartitionsthreshold";
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
        public static final String PLOTS = "plots";
        public static final String log_recommendation_metrics_level = "log_recommendation_metrics_level";
        public static final String CLOUDWATCH_LOGS_ACCESS_KEY_ID = "logging_cloudwatch_accessKeyId";
        public static final String CLOUDWATCH_LOGS_SECRET_ACCESS_KEY = "logging_cloudwatch_secretAccessKey";
        public static final String CLOUDWATCH_LOGS_LOG_GROUP = "logging_cloudwatch_logGroup";
        public static final String CLOUDWATCH_LOGS_REGION = "logging_cloudwatch_region";
        public static final String CLOUDWATCH_LOGS_LOG_STREAM = "logging_cloudwatch_logStream";
        public static final String CLOUDWATCH_LOGS_LOG_LEVEL = "logging_cloudwatch_logLevel";
        public static final String LOCAL = "local";
        public static final String LOG_HTTP_REQ_RESP = "logAllHttpReqAndResp";
        public static final String RECOMMENDATIONS_URL = "recommendationsURL";
        public static final String EXPERIMENTS_URL = "experimentsURL";
        public static final String BULK_API_LIMIT = "bulkapilimit";
        public static final String TEST_USE_ONLY_CACHE_JOB_IN_MEM = "testUseOnlycacheJobInMemory";
        public static final String JOB_FILTER_TO_DB = "jobFilterToDB";
        public static final String BULK_THREAD_POOL_SIZE = "bulkThreadPoolSize";
        public static final String EXPERIMENT_NAME_FORMAT = "experimentNameFormat";
        public static final String IS_ROS_ENABLED = "isROSEnabled";
        public static final String DATASOURCE_VIA_ENV = "datasource";
        public static final String METADATA_PROFILE_FILE_PATH = "metadataProfileFilePath";
        public static final String METRIC_PROFILE_FILE_PATH = "metricProfileFilePath";
        public static final String IS_KAFKA_ENABLED = "isKafkaEnabled";
    }

    public static final class RecommendationEngineConstants {
        private RecommendationEngineConstants() {

        }

        public static final class DurationBasedEngine {
            private DurationBasedEngine() {

            }

            public static final class DurationAmount {
                public static final int SHORT_TERM_DURATION_DAYS = 1;
                public static final double SHORT_TERM_DURATION_DAYS_THRESHOLD = ((double) 30 / (24 * 60));
                public static final int MEDIUM_TERM_DURATION_DAYS = 7;
                public static final int MEDIUM_TERM_DURATION_DAYS_THRESHOLD = 2;
                public static final int LONG_TERM_DURATION_DAYS = 15;
                public static final int LONG_TERM_DURATION_DAYS_THRESHOLD = 8;
                // Represents the minimum number of data points required for different term thresholds.
                // Minimum data points are calculated based on the above threshold in days and a 15-minute measurement duration.
                // If the short-term threshold is 30 minutes and the measurement duration is 15 minutes
                // then the minimum data points = 30 / 15 = 2
                public static final int SHORT_TERM_MIN_DATAPOINTS = 2;
                public static final int MEDIUM_TERM_MIN_DATAPOINTS = 192;
                public static final int LONG_TERM_MIN_DATAPOINTS = 768;


                private DurationAmount() {

                }
            }

            public static final class RecommendationDurationRanges {
                public static final double MEASUREMENT_DURATION_BUFFER_IN_MINS = ((double) TimeConv.MEASUREMENT_DURATION_THRESHOLD_SECONDS / TimeConv.NO_OF_SECONDS_PER_MINUTE);
                public static final double SHORT_TERM_MIN_DATA_THRESHOLD_MINS = 30;
                public static final double MEDIUM_TERM_MIN_DATA_THRESHOLD_MINS = 2 * TimeConv.NO_OF_HOURS_PER_DAY * TimeConv.NO_OF_MINUTES_PER_HOUR;
                public static final double LONG_TERM_MIN_DATA_THRESHOLD_MINS = 8 * TimeConv.NO_OF_HOURS_PER_DAY * TimeConv.NO_OF_MINUTES_PER_HOUR;
                /* LONG TERM */
                public static final double LONG_TERM_TOTAL_DURATION_UPPER_BOUND_MINS = LONG_TERM_MIN_DATA_THRESHOLD_MINS + MEASUREMENT_DURATION_BUFFER_IN_MINS;
                public static final double LONG_TERM_TOTAL_DURATION_LOWER_BOUND_MINS =
                        LONG_TERM_MIN_DATA_THRESHOLD_MINS - MEASUREMENT_DURATION_BUFFER_IN_MINS;
                /* MEDIUM TERM */
                public static final double MEDIUM_TERM_TOTAL_DURATION_UPPER_BOUND_MINS = MEDIUM_TERM_MIN_DATA_THRESHOLD_MINS + MEASUREMENT_DURATION_BUFFER_IN_MINS;
                public static final double MEDIUM_TERM_TOTAL_DURATION_LOWER_BOUND_MINS = MEDIUM_TERM_MIN_DATA_THRESHOLD_MINS - MEASUREMENT_DURATION_BUFFER_IN_MINS;
                public static final double SHORT_TERM_HOURS = DurationAmount.SHORT_TERM_DURATION_DAYS * KruizeConstants.TimeConv.NO_OF_HOURS_PER_DAY;
                public static final double MEDIUM_TERM_HOURS = DurationAmount.MEDIUM_TERM_DURATION_DAYS * KruizeConstants.TimeConv.NO_OF_HOURS_PER_DAY;
                public static final double LONG_TERM_HOURS = DurationAmount.LONG_TERM_DURATION_DAYS * KruizeConstants.TimeConv.NO_OF_HOURS_PER_DAY;
                public static final double SHORT_TERM_TOTAL_DURATION_UPPER_BOUND_MINS = SHORT_TERM_MIN_DATA_THRESHOLD_MINS + MEASUREMENT_DURATION_BUFFER_IN_MINS;
                public static final double SHORT_TERM_TOTAL_DURATION_LOWER_BOUND_MINS = SHORT_TERM_MIN_DATA_THRESHOLD_MINS - MEASUREMENT_DURATION_BUFFER_IN_MINS;

                private RecommendationDurationRanges() {

                }

            }
        }


    }

    public static final class KRUIZE_CONFIG_DEFAULT_VALUE {
        public static final int DELETE_PARTITION_THRESHOLD_IN_DAYS = 16;
    }

    public static final class KRUIZE_RECOMMENDATION_METRICS {
        public static final String METRIC_NAME = "KruizeRecommendationsNotification";
        public static final String TAG_NAME = "recommendations_notifications";
        public static final String notification_format_for_LOG = "%s|%s|%s|%s|%s|%s|%s|%s|%s"; //experiment_name,container_name,endtime,level,termname,modelname,code,type,message
        public static final String notification_format_for_METRICS = "%s|%s|%s"; //termname,modelname,type

    }

    public static final class AuthenticationConstants {
        public static final String AUTHENTICATION = "authentication";
        public static final String AUTHENTICATION_TYPE = "type";
        public static final String AUTHENTICATION_CREDENTIALS = "credentials";
        public static final String AUTHENTICATION_USERNAME = "username";
        public static final String AUTHENTICATION_PASSWORD = "password";
        public static final String AUTHENTICATION_TOKEN_FILE = "tokenFilePath";
        public static final String AUTHENTICATION_TOKEN = "token";
        public static final String AUTHENTICATION_API_KEY = "apiKey";
        public static final String AUTHENTICATION_HEADER_NAME = "header";
        public static final String AUTHENTICATION_TOKEN_ENDPOINT = "tokenEndpoint";
        public static final String AUTHENTICATION_CLIENT_ID = "clientId";
        public static final String AUTHENTICATION_CLIENT_SECRET = "clientSecret";
        public static final String AUTHENTICATION_GRANT_TYPE = "grantType";
        public static final String NONE = "none";
        public static final String BASIC = "basic";
        public static final String BEARER = "bearer";
        public static final String API_KEY = "apikey";
        public static final String OAUTH2 = "oauth2";
        public static final String UNKNOWN_AUTHENTICATION = "Unknown authentication type: ";
        public static final String AUTHORIZATION = "Authorization";

    }

    public static final class KRUIZE_BULK_API {
        public static final String JOB_ID = "job_id";
        public static final String ERROR = "error";
        public static final String JOB_NOT_FOUND_MSG = "Job not found";
        public static final String IN_PROGRESS = "IN_PROGRESS";
        public static final String COMPLETED = "COMPLETED";
        public static final String FAILED = "FAILED";
        public static final String LIMIT_MESSAGE = "The number of experiments exceeds the defined limit.";
        public static final String NOTHING = "Nothing to do.";
        public static final String START_TIME = "start_time";
        public static final String END_TIME = "end_time";
        public static final String STEPS = "steps";
        public static final String ADDITIONAL_LABEL = "ADDITIONAL_LABEL";
        public static final String SUMMARY = "summary";
        public static final String SUMMARY_FILTER = "summaryFilter";
        public static final String EXPERIMENTS = "experiments";
        public static final String EXPERIMENTS_FILTER = "experimentFilter";
        public static final String JOB_FILTER = "jobFilter";
        public static final String BULK_JOB_SAVE_ERROR = "Not able to save experiment due to {}";
        public static final String BULK_JOB_LOAD_ERROR = "Not able to load bulk JOB {} due to {}";


        // TODO : Bulk API Create Experiments defaults
        public static final CreateExperimentConfigBean CREATE_EXPERIMENT_CONFIG_BEAN;

        // Static block to initialize the Bean
        static {
            CREATE_EXPERIMENT_CONFIG_BEAN = new CreateExperimentConfigBean();
            CREATE_EXPERIMENT_CONFIG_BEAN.setMode(AnalyzerConstants.MONITOR);
            CREATE_EXPERIMENT_CONFIG_BEAN.setTarget(AnalyzerConstants.LOCAL);
            CREATE_EXPERIMENT_CONFIG_BEAN.setVersion(AnalyzerConstants.VersionConstants.CURRENT_KRUIZE_OBJECT_VERSION);
            CREATE_EXPERIMENT_CONFIG_BEAN.setDatasourceName("prometheus-1");
            CREATE_EXPERIMENT_CONFIG_BEAN.setPerformanceProfile(AnalyzerConstants.PerformanceProfileConstants.RESOURCE_OPT_LOCAL_MON_PROFILE);
            CREATE_EXPERIMENT_CONFIG_BEAN.setMetadataProfile(AnalyzerConstants.MetadataProfileConstants.CLUSTER_METADATA_LOCAL_MON_PROFILE);
            CREATE_EXPERIMENT_CONFIG_BEAN.setThreshold(0.1);
            CREATE_EXPERIMENT_CONFIG_BEAN.setMeasurementDurationStr("15min");
            CREATE_EXPERIMENT_CONFIG_BEAN.setMeasurementDuration(15);
            CREATE_EXPERIMENT_CONFIG_BEAN.setMetadataProfile(AnalyzerConstants.MetadataProfileConstants.CLUSTER_METADATA_LOCAL_MON_PROFILE);
        }

        public static class NotificationConstants {

            public static final BulkJobStatus.Notification JOB_NOT_FOUND_INFO = new BulkJobStatus.Notification(
                    BulkJobStatus.NotificationType.WARNING,
                    JOB_NOT_FOUND_MSG,
                    404
            );
            public static final BulkJobStatus.Notification LIMIT_INFO = new BulkJobStatus.Notification(
                    BulkJobStatus.NotificationType.INFO,
                    LIMIT_MESSAGE,
                    400
            );
            public static final BulkJobStatus.Notification NOTHING_INFO = new BulkJobStatus.Notification(
                    BulkJobStatus.NotificationType.INFO,
                    NOTHING,
                    400
            );
            public static final BulkJobStatus.Notification FETCH_METRIC_FAILURE = new BulkJobStatus.Notification(
                    BulkJobStatus.NotificationType.ERROR,
                    "Not able to fetch metrics",
                    400
            );
            public static final BulkJobStatus.Notification DATASOURCE_NOT_REG_INFO = new BulkJobStatus.Notification(
                    BulkJobStatus.NotificationType.ERROR,
                    "Datasource not registered with Kruize. (%s)",
                    400
            );
            public static final BulkJobStatus.Notification DATASOURCE_DOWN_INFO = new BulkJobStatus.Notification(
                    BulkJobStatus.NotificationType.ERROR,
                    "HttpHostConnectException: Unable to connect to the data source. Please try again later. (%s)",
                    503
            );
            public static final BulkJobStatus.Notification DATASOURCE_GATEWAY_TIMEOUT_INFO = new BulkJobStatus.Notification(
                    BulkJobStatus.NotificationType.ERROR,
                    "SocketTimeoutException: request timed out waiting for a data source response. (%s)",
                    504
            );
            public static final BulkJobStatus.Notification DATASOURCE_CONNECT_TIMEOUT_INFO = new BulkJobStatus.Notification(
                    BulkJobStatus.NotificationType.ERROR,
                    "ConnectTimeoutException: cannot establish a data source connection in a given time frame due to connectivity issues. (%s)",
                    503
            );
            public static final BulkJobStatus.Notification EXPERIMENT_FAILED = new BulkJobStatus.Notification(
                    BulkJobStatus.NotificationType.ERROR,
                    "Not able to proceed due to. (%s)",
                    503
            );
            public static final BulkJobStatus.Notification METADATA_PROFILE_NOT_FOUND = new BulkJobStatus.Notification(
                    BulkJobStatus.NotificationType.ERROR,
                    "Metadata profile not found. (%s)",
                    400
            );


            // More notification constants can be added here as needed

            public enum Status {
                PROCESSED("PROCESSED"),
                UNPROCESSED("UNPROCESSED"),
                PROCESSING("PROCESSING"),
                FAILED("FAILED"),
                PUBLISHED("PUBLISHED"),
                PUBLISH_FAILED("PUBLISH_FAILED");

                private final String status;

                Status(String status) {
                    this.status = status;
                }

                public String getStatus() {
                    return status;
                }
            }

            public enum WebHookStatus {
                INITIATED,       // The  Webhook has initiated a request
                IN_PROGRESS,     // The request to the Webhook is actively being processed
                QUEUED,          // The request to the  Webhook has been queued, waiting for resources
                SENT,            // The request has been sent to the Webhook, but no response yet
                RECEIVED,        // The Webhook has received a response, but further processing continues
                SUCCESS,         // The request to the Webhook was successful
                FAILED,          // The call to the Webhook failed due to an error
                RETRYING,        // The  Webhook is retrying the call due to a transient error
                TIMED_OUT,       // The request to the Webhook exceeded the allowed response time
                ERROR_LOGGED,    // The error has been logged for debugging or monitoring
                COMPLETED,       // The entire process, including subsequent processing, is finished
                CANCELLED        // The request was cancelled, potentially by user action or system condition
            }


        }
    }

    public static final class MetadataProfileConstants {
        public static final String METADATA_PROFILE_VALIDATION_FAILURE = "Validation failed: ";
        public static final String METADATA_PROFILE_VALIDATION_AND_ADD_FAILURE = "Validate and add metadata profile failed: {}";
        public static final String ADD_METADATA_PROFILE = "Added MetadataProfile: {}";
        public static final String CHECKING_AVAILABLE_METADATA_PROFILE_FROM_DB = "Checking available metadata profiles from database: ";
        public static final String NO_METADATA_PROFILE_FOUND_IN_DB = "No metadata profile found in database.";
        public static final String METADATA_PROFILE_FOUND = "MetadataProfile found: ";
        public static final String ADDING_METADATA_PROFILE = "Trying to add the metadata profile to collection: ";
        public static final String METADATA_PROFILE_ALREADY_EXISTS = "MetadataProfile already exists: ";
        public static final String METADATA_PROFILE_ADDED = "MetadataProfile added to the collection successfully: {}";
        public static final String CONVERT_INPUT_JSON_TO_METADATA_PROFILE_FAILURE = "Failed to convert input JSON to MetadataProfile object due to: {}";
        public static final String METADATA_PROFILE_FILE_PATH = "MetadataProfile file path: {}";

        public static class MetadataProfileErrorMsgs {

            public static final String ADD_METADATA_PROFILE_TO_DB_ERROR = "Failed to add Metadata Profile due to {}";
            public static final String LOAD_METADATA_PROFILES_FROM_DB_FAILURE = "Failed to load Metadata Profiles from DB.";
            public static final String CONVERTING_METADATA_PROFILE_DB_OBJECT_ERROR = "Error occurred while reading from MetadataProfile DB object due to : {}";
            public static final String PROCESS_METADATA_PROFILE_OBJECT_ERROR = "Failed to process metadata of metadataProfile object due to : {}";
            public static final String PROCESS_QUERY_VARIABLES_ERROR = "Error occurred while processing query_variables data due to : {}";
            public static final String CONVERT_METADATA_PROFILE_TO_DB_OBJECT_FAILURE = "Failed to convert MetadataProfile Object to MetadataProfile DB object due to {}";
            public static final String INVALID_METADATA_PROFILE = "Metadata profile name either does not exist or is not valid: ";
            public static final String ADD_DEFAULT_METADATA_PROFILE_EXCEPTION = "Exception occurred while adding default Metadata profile: {}";
            public static final String SET_UP_DEFAULT_METADATA_PROFILE_ERROR = "Failed to set up default MetadataProfile due to: {}";
            public static final String FILE_NOT_FOUND_ERROR = "File not found: {}";
            public static final String FILE_READ_ERROR_ERROR_MESSAGE = "Failed to read the JSON file from the specified path: {}";

            private MetadataProfileErrorMsgs() {
            }
        }

    }

    public static final class MetadataProfileAPIMessages {
        public static final String CREATE_METADATA_PROFILE_SUCCESS_MSG = "Metadata Profile : %s created successfully.";
        public static final String VIEW_METADATA_PROFILES_MSG = " View Metadata Profiles at /listMetadataProfiles";
        public static final String ADD_METADATA_PROFILE_TO_DB_WITH_VERSION = "Added Metadata Profile : {} into the DB with version: {}";
        public static final String DELETE_METADATA_PROFILE_SUCCESS_MSG = "Metadata profile: %s deleted successfully.";
        public static final String DELETE_METADATA_PROFILE_FROM_DB_SUCCESS_MSG = "Metadata profile deleted successfully from the DB.";

        private MetadataProfileAPIMessages() {
        }
    }

    public static final class KAFKA_CONSTANTS {
        public static final String BULK_INPUT_TOPIC = "bulk-input-topic";
        public static final String RECOMMENDATIONS_TOPIC = "recommendations-topic";
        public static final String ERROR_TOPIC = "error-topic";
        public static final String SUMMARY_TOPIC = "summary-topic";
        public static final String UNKNOWN_TOPIC = "Unknown topic: %s";

        public static final String SUMMARY = "summary";
        public static final String EXPERIMENTS = "experiments";
        public static final String RECOMMENDATIONS = "recommendations";
        public static final String ALL = "all";

        public static final String BOOTSTRAP_SERVER_MISSING = "Kafka is enabled, but no bootstrap server URL is provided!";
        public static final String KAFKA_CONNECTION_SUCCESS = "✅ Kafka connection successful: {}";
        public static final String KAFKA_CONNECTION_FAILURE = "❌ Failed to connect to Kafka at %s : ";


        public static final String MESSAGE_SENT_SUCCESSFULLY = "Message sent successfully to topic {} at partition {} and offset {}";
        public static final String KAFKA_MESSAGE_TIMEOUT_ERROR = "Kafka timeout while sending message to topic {}: {}";
        public static final String KAFKA_MESSAGE_FAILED = "Error sending message to Kafka topic {}: {}";
        public static final String KAFKA_PRODUCER_CLOSED = "Kafka producer closed.";
        public static final String MISSING_KAFKA_TOPIC = "Kafka topic '%s' does not exist! Skipping message publishing.";
        public static final String KAFKA_PUBLISH_FAILED = "Failed to publish to Kafka: {}";

        public static final String MESSAGE_RECEIVED_SUCCESSFULLY = "Received Input: Request_Id={}, Value={}, Partition={}, Offset={}";

    }
}
