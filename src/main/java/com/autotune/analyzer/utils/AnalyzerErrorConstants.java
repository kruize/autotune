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

import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.KruizeSupportedTypes;

import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

/**
 * Contains strings describing the errors encountered
 */
public class AnalyzerErrorConstants {
    private AnalyzerErrorConstants() {
    }

    public static final class AutotuneConfigErrors {
        public static final String AUTOTUNE_CONFIG_NAME_NULL = "KruizeLayer object name cannot be null or empty\n";
        public static final String LAYER_PRESENCE_MISSING = "Layer presence missing! Must be indicated through a presence field, layerPresenceQuery or layerPresenceLabel\n";
        public static final String BOTH_LAYER_QUERY_AND_LABEL_SET = "Both layerPresenceQuery and layerPresenceLabel cannot be set\n";
        public static final String LAYER_LEVEL_INVALID = "Layer level must be a non-negative integer\n";
        public static final String NO_TUNABLES = "KruizeLayer has no tunables\n";
        public static final String COULD_NOT_GET_LIST_OF_APPLICATIONS = "Could not get the applications for the layer ";
        public static final String INVALID_SLO_CLASS = "Invalid slo_class for tunable ";
        public static final String TUNABLE_NAME_EMPTY = "Tunable name cannot be empty";
        public static final String VALUE_TYPE_NULL = "value_type cannot be null";
        public static final String ZERO_STEP = "Tunable step cannot be 0 or null";
        public static final String INVALID_TUNABLE_CHOICE = "Invalid categorical choice for tunable ";

        private AutotuneConfigErrors() {
        }
    }

    public static final class AutotuneObjectErrors {
        public static final String UNSUPPORTED = " is not supported\n";
        public static final String AUTOTUNE_OBJECT_NAME_MISSING = "Autotune object name cannot be null or empty\n";
        public static final String INVALID_MATCHLABEL = "Invalid MatchLabel in selector\n";
        public static final String INVALID_MATCHLABEL_VALUE = "Invalid or blank MatchLabelValue in selector\n";
        public static final String SLO_CLASS_NOT_SUPPORTED = "slo_class " + UNSUPPORTED;
        public static final String DIRECTION_NOT_SUPPORTED = "direction " + UNSUPPORTED;
        public static final String FUNCTION_VARIABLES_EMPTY = "function_variables is empty\n";
        public static final String QUERY_VARIABLES_EMPTY = "query_variables is empty\n";
        public static final String OBJECTIVE_FUNCTION_MISSING = "objective_function missing\n";
        public static final String MODE_NOT_SUPPORTED = "Autotune object mode " + UNSUPPORTED;
        public static final String TARGET_CLUSTER_NOT_SUPPORTED = "Autotune object targetCluster " + UNSUPPORTED;
        public static final String HPO_ALGO_NOT_SUPPORTED = "HPO algorithm " + UNSUPPORTED;
        public static final String INVALID_OBJECTIVE_FUNCTION = "objective_function improperly formatted\n";
        public static final String OBJECTIVE_FUNCTION_MAP_MISSING = "objective_function_map is missing or empty\n";
        public static final String NO_DEPLOYMENTS_FOUND = "No deployments related to the Autotune object found\n";
        public static final String INVALID_DIRECTION_FOR_SLO_CLASS = "Invalid direction for slo_class\n";
        public static final String DATASOURCE_NOT_SUPPORTED = " datasource " + UNSUPPORTED;
        public static final String VALUE_TYPE_NOT_SUPPORTED = " value_type " + UNSUPPORTED;
        public static final String QUERY_FUNCTION_MISSING = "One of query or aggregation_functions is mandatory. Both cannot be null!";
        public static final String MISSING_AGG_FUNCTION = "At least one aggregation function value needs to be present ";
        public static final String AGG_FUNCTION_MISMATCH = "Missing aggregation functions in resultDataObjects: ";
        public static final String FUNCTION_VARIABLE_ERROR = "missing in objective_function\n";
        public static final String MISSING_EXPRESSION = "Expression value is missing or null!\n";
        public static final String MISPLACED_EXPRESSION = "Expression is not allowed when the type is source\n";
        public static final String INVALID_TYPE = "Objective function type can only be either expression or source\n";
        public static final String NO_PERF_PROFILE = "No performance profiles present!";
        public static final String MISSING_VALUE = "Missing 'value' in the results for the metric : ";
        public static final String MISSING_SLO_DATA = "No Performance Profile or SLO data is Present!";
        public static final String SLO_REDUNDANCY_ERROR = "SLO Data and Performance Profile cannot exist simultaneously!";
        public static final String DUPLICATE_PERF_PROFILE = "Performance Profile already exists: ";
        public static final String MISSING_PERF_PROFILE = "Not Found: performance_profile does not exist: ";
        public static final String MISSING_METRIC_PROFILE_METADATA= "metadata missing\n";
        public static final String DUPLICATE_METRIC_PROFILE = "Metric Profile already exists: ";
        public static final String MISSING_METADATA_PROFILE = "Not Found: metadata_profile does not exist: ";
        public static final String MISSING_METADATA_PROFILE_FIELD = "Missing `metadata_profile` field";
        public static final String METADATA_PROFILE_NOT_SUPPORTED = "Metadata profile is not supported in remote monitoring: ";
        public static final String MISSING_METADATA_PROFILE_METADATA= "metadata missing\n";
        public static final String DUPLICATE_METADATA_PROFILE = "Metadata Profile already exists: ";
        public static final String MISSING_EXPERIMENT_NAME = "Not Found: experiment_name does not exist: ";
        public static final String NO_METRICS_AVAILABLE = "No metrics available from %s to %s";
        public static final String UNSUPPORTED_EXPERIMENT = String.format("At present, the system does not support bulk entries!");
        public static final String UNSUPPORTED_EXPERIMENT_RESULTS = String.format("At present, the system does not support bulk entries exceeding %s in quantity!", KruizeDeploymentInfo.bulk_update_results_limit);
        public static final String UNSUPPORTED_BULK_KUBERNETES = "Bulk Kubernetes objects are currently unsupported!";
        public static final String DUPLICATE_EXPERIMENT = "Experiment name already exists: ";
        public static final String WRONG_TIMESTAMP = "The Start time should precede the End time!";
        public static final String MEASUREMENT_DURATION_ERROR = "Interval duration cannot be less than or greater than measurement_duration by more than " + KruizeConstants.TimeConv.MEASUREMENT_DURATION_THRESHOLD_SECONDS + " seconds";
        public static final String MISSING_METRICS = "Metric data is not present for container : %s for experiment: %s. ";
        public static final String BLANK_AGGREGATION_INFO_VALUE = " cannot be negative or blank for the metric variable: ";
        public static final String UNSUPPORTED_FORMAT = " Format value should be among these values: ".concat(KruizeSupportedTypes.SUPPORTED_FORMATS.toString());
        public static final String UNSUPPORTED_METRIC = "Metric variable name should be among these values: ".concat(Arrays.toString(AnalyzerConstants.MetricName.values()));
        public static final String CONTAINER_AND_EXPERIMENT = " for container : %s for experiment: %s.";
        public static final String NAMESPACE_AND_EXPERIMENT = " for namespace : %s for experiment: %s.";
        public static final String JSON_PARSING_ERROR = "Failed to parse the JSON. Please check the input payload ";
        public static final String AGGREGATION_INFO_INVALID_VALUE = "Invalid value type for aggregation_info objects. Expected a numeric value (Double).";
        public static final String VERSION_MISMATCH = "Version number mismatch found. Expected: %s , Found: %s";
        public static final String NULL_OR_BLANK_CONTAINER_IMAGE_NAME = "container_image_name cannot be null or blank";
        public static final String NULL_OR_BLANK_CONTAINER_NAME = "container_name cannot be null or blank";
        public static final String EXPERIMENT_AND_INTERVAL_END_TIME = " for experiment : %s interval_end_time: %s";
        public static final String LOCAL_MONITORING_DATASOURCE_MANDATORY = "Experiment %s: datasource mandatory for Local Monitoring type";
        public static final String LOAD_METADATA_PROFILE_FAILURE = "Loading saved Metadata Profile %s failed: %s";
        public static final String METADATA_PROFILE_VALIDATION_FAILED = "Validation of metadata profile failed!";
        public static final String PARSE_ERROR_MESSAGE = "Exception occurred while parsing the data: %s";
        public static final String DELETED_METADATA_PROFILE = "Deleted metadata profile object: %s";
        public static final String INVALID_METADATA_PROFILE_NAME = "MetadataProfile 'name' field is either null or empty!";
        public static final String INVALID_METRICS_FOUND = "Invalid metrics found for experiment - %s: %s";
        public static final String MISSING_MANDATORY_PARAMETERS = "Missing one of the following mandatory parameters for experiment - %s : %s";


        private AutotuneObjectErrors() {
        }

    }

    public static final class AutotuneServiceMessages {
        public static final String AUTOTUNE_OBJECTS_NOT_FOUND = "Error: No objects of kind Autotune found!";
        public static final String ERROR_EXPERIMENT_NAME = "Error: Experiment Name ";
        public static final String ERROR_DEPLOYMENT_NAME = "Error: Deployment Name ";
        public static final String NOT_FOUND = " not found!";
        public static final String LAYER_NOT_FOUND = "Error: No Layer (KruizeLayer) object found!";
        public static final String ERROR_LAYER = "Error: Layer (KruizeLayer) object ";
        public static final String ERROR_SLO_CLASS = "Error: Tunables matching slo_class ";
        public static final String ERROR_STACK_NAME = "Error: Experiment related to Stack (Container Image) ";

        private AutotuneServiceMessages() {
        }
    }

    public static final class RunExperimentMessages {
        public static final String AUTOTUNE_OBJECTS_NOT_FOUND = "Error: No objects of kind Autotune found!";

        private RunExperimentMessages() {
        }

    }

    public static final class APIErrors {
        private APIErrors() {

        }

        public static final class updateResultsAPI {
            public static final String RESULTS_ALREADY_EXISTS = "An entry for this record already exists!";

            public static final HashMap<String, Integer> ERROR_CODE_MAP = new HashMap<String, Integer>() {{
                put(RESULTS_ALREADY_EXISTS, HttpServletResponse.SC_CONFLICT);
            }};


        }

        public static final class generateRecommendationsAPI {
            public static final String ERROR_FETCHING_METRICS = "Error while fetching metrics.";
            public static final String NULL_OR_EMPTY_MODEL_NAME = "Model name cannot be null or empty";
            public static final String NULL_RECOMMENDATION_TUNABLES = "Recommendation Tunables cannot be null";
            public static final String DATA_IRREGULARITY_DETECTED = "Data irregularity detected, " +
                    "Notification needs to be added explaining we changed the memory usage to 100% as it's more than 100%";

            private generateRecommendationsAPI() {

            }
        }

        public static final class ListRecommendationsAPI {
            public static final String RECOMMENDATION_DOES_NOT_EXIST_EXCPTN = "Recommendation does not exist";
            public static final String RECOMMENDATION_DOES_NOT_EXIST_MSG = "Recommendation for timestamp - \" %s \" does not exist";
            public static final String INVALID_TIMESTAMP_EXCPTN = "Invalid Timestamp format";
            public static final String INVALID_TIMESTAMP_MSG = "Given timestamp - \" %s \" is not a valid timestamp format";
            public static final String INVALID_EXPERIMENT_NAME_EXCPTN = "Invalid Experiment Name";
            public static final String INVALID_EXPERIMENT_NAME_MSG = "Given experiment name - \" %s \" is not valid";
            public static final String INVALID_QUERY_PARAM = "The query param(s) - \" %s \" is/are invalid";
            public static final String INVALID_QUERY_PARAM_VALUE = "The query param value(s) is/are invalid";

            private ListRecommendationsAPI() {

            }
        }

        public static final class CreateExperimentAPI {
            public static final String NAMESPACE_AND_CONTAINER_NOT_NULL = "Only one of Namespace or Container information can be specified.";
            public static final String CONTAINER_DATA_NOT_NULL_FOR_NAMESPACE_EXP = "Can not specify container data for namespace experiment";
            public static final String NAMESPACE_DATA_NOT_NULL_FOR_CONTAINER_EXP = "Can not specify namespace data for container experiment";
            public static final String NAMESPACE_EXP_NOT_SUPPORTED_FOR_REMOTE = "Namespace experiment type is not supported for remote monitoring use case.";
            public static final String INVALID_MODE_FOR_NAMESPACE_EXP = "Auto or recreate mode is not supported for namespace experiment.";
            public static final String INVALID_OBJECT_TYPE_FOR_AUTO_EXP = "Kubernetes object type is not supported for auto or recreate mode.";
            public static final String AUTO_EXP_NOT_SUPPORTED_FOR_REMOTE = "Auto or recreate mode is not supported for remote monitoring use case.";
            public static final String INVALID_TERM_NAME = " term name is not supported. Use short, medium or long term.";
            public static final String TERM_SETTINGS_UNDEFINED= "Term settings are not defined in the recommendation settings.";
            public static final String MULTIPLE_TERMS_UNSUPPORTED = "Multiple terms are currently not supported for auto or recreate mode.";
            public static final String INVALID_MODEL_NAME = " model name is not supported. Use cost or performance.";
            public static final String MULTIPLE_MODELS_UNSUPPORTED = "Multiple models are currently not supported for auto or recreate mode.";
            public static final String WHITESPACE_NOT_ALLOWED = "Whitespace can not be entered as a term or model value.";
            public static final String MISSING_NAMESPACE_DATA = "Missing NamespaceData for experimentType: %s";
            public static final String MISSING_NAMESPACE = "Missing namespace for experimentType: %s";
            public static final String INVALID_EXPERIMENT_TYPE = "Invalid experiment_type : %s";

            private CreateExperimentAPI() {

            }
        }

        public static final class UpdateRecommendationsAPI {
            public static final String EXPERIMENT_NAME_MANDATORY = KruizeConstants.JSONKeys.EXPERIMENT_NAME + " is mandatory";
            public static final String INTERVAL_END_TIME_MANDATORY = KruizeConstants.JSONKeys.INTERVAL_END_TIME + " is mandatory";
            public static final String DATA_NOT_FOUND = "Data not found!";
            public static final String TIME_COMPARE = "The Start time should precede the End time!";
            public static final String TIME_GAP_LIMIT = String.format("The gap between the interval_start_time and interval_end_time must be within a maximum of %s days!", KruizeDeploymentInfo.generate_recommendations_date_range_limit_in_days);
            public static final String UPDATE_RECOMMENDATIONS_COUNT = "UpdateRecommendations API request count: %s ";

            public static final String UPDATE_RECOMMENDATIONS_FAILED_COUNT = UPDATE_RECOMMENDATIONS_COUNT + "failed";
            public static final String UPDATE_RECOMMENDATIONS_SUCCESS_COUNT = UPDATE_RECOMMENDATIONS_COUNT + "success";
            public static final String UPDATE_RECOMMENDATIONS_COMPLETED_COUNT = UPDATE_RECOMMENDATIONS_COUNT + "completed";
            public static final String RECOMMENDATION_ERROR = "Failed to create recommendation for experiment: %s and interval_start_time: %s and interval_end_time: %s";
            public static final String RECOMMENDATION_EXCEPTION = "Exception occurred while generating recommendations for experiment: {} and interval_end_time: {} : {} ";
            public static final String METRIC_EXCEPTION = "Exception occurred while fetching metrics from the datasource: ";
            public static final String FETCHING_RESULTS_FAILED = "Failed to fetch the results from the DB: %s";
            public static final String INTERNAL_MAP_EMPTY = "Internal map sent to populate method cannot be null or empty";
            public static final String NULL_NOTIFICATIONS = "Notifications cannot be null";
            public static final String NULL_RECOMMENDATIONS = "Recommendation cannot be null";
            public static final String INVALID_RECOMMENDATION_TERM = "Invalid Recommendation Term : %s";
            public static final String NULL_RECOMMENDATION_TERM = "Recommendation term cannot be null";
            public static final String INVALID_MEMORY_THRESHOLD = "Given Memory Threshold is invalid, setting Default Memory Threshold : %s";
            public static final String INVALID_CPU_THRESHOLD = "Given CPU Threshold is invalid, setting Default CPU Threshold :  %s";
            public static final String NULL_RECOMMENDATION_SETTINGS = "Recommendation Settings are null, setting Default CPU Threshold : %s and Memory Threshold : %s";
            public static final String INVALID_THRESHOLD = "Given Threshold is invalid, setting Default CPU Threshold : %s and Memory Threshold : %s";
            public static final String THRESHOLD_NOT_SET = "Threshold is not set, setting Default CPU Threshold : %s and Memory Threshold : %s";
            public static final String BOX_PLOTS_FAILURE = "Box plots Failed due to : %s";
            public static final String LOAD_EXPERIMENT_FAILURE = "Failed to load experiment from DB: %s";
            public static final String GENERATE_RECOMMENDATION_FAILURE = "Not able to generate recommendation for expName : {} due to {}";
            public static final String RESULTS_SAVE_FAILURE = "Failed to add results to local storage due to: {}";


            private UpdateRecommendationsAPI() {
            }
        }
        public static final class ListDataSourcesAPI {
            private ListDataSourcesAPI() {

            }
            public static final String INVALID_DATASOURCE_NAME_EXCPTN = "Invalid Datasource Name";
            public static final String INVALID_DATASOURCE_NAME_MSG = "Given datasource name - %s either does not exist or is not valid";
        }

        public static final class DSMetadataAPI {
            private DSMetadataAPI(){
            }
            public static final String DATASOURCE_NAME_MANDATORY = KruizeConstants.JSONKeys.DATASOURCE + " is mandatory";
            public static final String INVALID_DATASOURCE_NAME_METADATA_EXCPTN = "Invalid DataSource Name";
            public static final String INVALID_DATASOURCE_NAME_METADATA_MSG = "Metadata for a given datasource name - %s either does not exist or is not valid";
            public static final String MISSING_DATASOURCE_METADATA_EXCPTN = "Missing DataSource metadata";
            public static final String MISSING_DATASOURCE_METADATA_MSG = "Metadata for a given datasource - %s, cluster name - %s, namespace - %s " +
                    "either does not exist or is not valid";
            public static final String DATASOURCE_METADATA_IMPORT_ERROR_MSG = "Metadata cannot be imported for datasource - %s, either does not exist or is not valid";
            public static final String INVALID_QUERY_PARAM_EXCPTN  = "Invalid input query parameter(s)";
            public static final String INVALID_QUERY_PARAM = "The query param(s) - %s is/are invalid";
            public static final String INVALID_QUERY_PARAM_VALUE = "The query param value(s) - %s is/are invalid";
            public static final String INVALID_DATASOURCE_NAME_CLUSTER_NAME_METADATA_EXCPTN = "Invalid Datasource name and Cluster name";
            public static final String INVALID_DATASOURCE_NAME_CLUSTER_NAME_METADATA_MSG = "Metadata for a given datasource name - %s, cluster_name - %s either does not exist or is not valid";
            public static final String MISSING_QUERY_PARAM_EXCPTN = "Missing mandatory input param value(s)";
            public static final String DATASOURCE_METADATA_DELETE_EXCPTN = "Datasource metadata not found";
            public static final String DATASOURCE_METADATA_DELETE_ERROR_MSG = "Metadata cannot be deleted for datasource - %s, either does not exist or is not valid";
            public static final String DATASOURCE_METADATA_VALIDATION_FAILURE_EXCPTN = "Invalid DataSourceMetadata object";
            public static final String DATASOURCE_METADATA_MISSING_REQUEST_INPUT_EXCPTN = "Request input data cannot be null or empty";
            public static final String DATASOURCE_METADATA_CONNECTION_FAILED = "Metadata cannot be imported, datasource connection refused or timed out";
            public static final String INVALID_METADATA_PROFILE_NAME_EXCPTN = "Invalid MetadataProfile Name";
            public static final String INVALID_METADATA_PROFILE_NAME_MSG = "MetadataProfile - %s either does not exist or is not valid";
        }

        public static final class ListMetricProfileAPI {
            public ListMetricProfileAPI() {
            }
            public static final String INVALID_QUERY_PARAM = "The query param(s) - %s is/are invalid";
            public static final String INVALID_QUERY_PARAM_VALUE = "The query param value(s) is/are invalid";
            public static final String INVALID_METRIC_PROFILE_NAME_EXCPTN = "Invalid Metric Profile Name";
            public static final String INVALID_METRIC_PROFILE_NAME_MSG = "Given metric profile name - %s either does not exist or is not valid";
            public static final String NO_METRIC_PROFILES_EXCPTN = "No metric profile";
            public static final String NO_METRIC_PROFILES = "No metric profiles found!";
        }

        public static final class DeleteMetricProfileAPI {
            public DeleteMetricProfileAPI() {
            }
            public static final String INVALID_METRIC_PROFILE_NAME_EXCPTN = "Invalid Metric Profile Name";
            public static final String INVALID_METRIC_PROFILE_NAME_MSG = "Given metric profile name - %s either does not exist or is not valid";
            public static final String MISSING_METRIC_PROFILE_NAME_EXCPTN = "Missing Metric Profile Name";
            public static final String MISSING_METRIC_PROFILE_NAME_MSG = "Missing metric profile 'name' parameter";
            public static final String DELETE_METRIC_PROFILE_FROM_DB_FAILURE_MSG = "Failed to delete metric profile from DB: %s";
            public static final String DELETE_METRIC_PROFILE_FAILURE_MSG = "Failed to delete the specified metric profile data: %s";
            public static final String DELETE_METRIC_PROFILE_ENTRY_NOT_FOUND_WITH_NAME = "KruizeMetricProfileEntry not found with metric profile name: ";
            public static final String DELETE_METRIC_PROFILE_ENTRY_ERROR_MSG = "Not able to delete metric profile for metric profile {} due to {}";
        }


        public static final class ListMetadataProfileAPI {
            public ListMetadataProfileAPI() {
            }
            public static final String INVALID_QUERY_PARAM = "The query param(s) - %s is/are invalid";
            public static final String INVALID_QUERY_PARAM_VALUE = "The query param value(s) is/are invalid";
            public static final String INVALID_METADATA_PROFILE_NAME_EXCPTN = "Invalid Metadata Profile Name";
            public static final String INVALID_METADATA_PROFILE_NAME_MSG = "Given metadata profile name - %s either does not exist or is not valid";
            public static final String NO_METADATA_PROFILES_EXCPTN = "No metadata profile";
            public static final String NO_METADATA_PROFILES = "No metadata profiles found!";
            public static final String LOAD_METADATA_PROFILE_ERROR = "Failed to load saved metadata profile data due to: {} ";
            public static final String LOAD_ALL_METADATA_PROFILES_ERROR = "Failed to load all the metadata profiles data due to: {} ";
        }

        public static final class DeleteMetadataProfileAPI {
            public DeleteMetadataProfileAPI() {
            }
            public static final String INVALID_METADATA_PROFILE_NAME_EXCPTN = "Invalid Metadata Profile Name";
            public static final String INVALID_METADATA_PROFILE_NAME_MSG = "Given metadata profile name - %s either does not exist or is not valid";
            public static final String MISSING_METADATA_PROFILE_NAME_EXCPTN = "Missing Metadata Profile Name";
            public static final String MISSING_METADATA_PROFILE_NAME_MSG = "Missing metadata profile 'name' parameter";
            public static final String DELETE_METADATA_PROFILE_FROM_DB_FAILURE_MSG = "Failed to delete metadata profile from DB: %s";
            public static final String DELETE_METADATA_PROFILE_FAILURE_MSG = "Failed to delete the specified metadata profile data: %s";
            public static final String DELETE_METADATA_PROFILE_ENTRY_NOT_FOUND_WITH_NAME = "KruizeLMMetadataProfileEntry not found with metadata profile name: ";
            public static final String DELETE_METADATA_PROFILE_ENTRY_ERROR_MSG = "Failed to delete metadata profile for metric profile {} due to {}";
        }
    }

    public static final class ConversionErrors {
        private ConversionErrors() {

        }

        public static final class KruizeRecommendationError {
            public static final String NOT_NULL = "{} Cannot be null";
            public static final String NOT_EQUAL = "{} - {} of {} is not equal to {} - {} of {}";
            public static final String NOT_EMPTY = "{} Cannot be empty";
            public static final String INVALID_JSON_STRUCTURE_MAPPING_TO_CLASS = "The JSON Structure in the JSON NODE is invalid to be mapped with class - {}";

            private KruizeRecommendationError() {


            }
        }
    }

    public static final class AutoscalerErrors {
        private AutoscalerErrors() {

        }

        public static final String UPDATER_SERVICE_START_ERROR = "Error occurred while initializing RecommendationUpdaterService.";
        public static final String UNSUPPORTED_UPDATER_TYPE = "Updater type %s is not supported.";
        public static final String GENERATE_RECOMMENDATION_FAILED = "Failed to generate recommendations for experiment: {}";
        public static final String UPDATER_NOT_INSTALLED = "Updater is not installed.";
        public static final String RECOMMENDATION_DATA_NOT_PRESENT = "Recommendations are not present for the experiment: {}";
        public static final String INVALID_VPA_NAME = "VPA name cannot be null or empty.";
        public static final String MISSING_REQUIRED_VALUES = "Recommended resource values (CPU or Memory) are missing in resourceMap";

        public static final class AcceleratorAutoscaler {
            private AcceleratorAutoscaler() {

            }
            public static final String NAMESPACE_NULL = "Namespace cannot be null";
            public static final String CONTAINER_NULL = "Container cannot be null";

        }
    }
}
