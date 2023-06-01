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
        public static final String MISSING_PERF_PROFILE = "Performance Profile doesn't exist : ";
        public static final String UNSUPPORTED_EXPERIMENT = String.format("At present, the system does not support bulk entries!");
        public static final String UNSUPPORTED_EXPERIMENT_RESULTS = String.format("At present, the system does not support bulk entries exceeding %s in quantity!", KruizeDeploymentInfo.bulk_update_results_limit);
        public static final String UNSUPPORTED_BULK_KUBERNETES = "Bulk Kubernetes objects are currently unsupported!";
        public static final String DUPLICATE_EXPERIMENT = "Experiment name already exists: ";
        public static final String WRONG_TIMESTAMP = "The Start time should precede the End time!";

        public static final String MEASUREMENT_DURATION_ERROR = "Interval duration cannot be less than or greater than measurement_duration by more than " + KruizeConstants.TimeConv.MEASUREMENT_DURATION_THRESHOLD_SECONDS + " seconds";

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

        public static final class UpdateRecommendationsAPI {
            public static final String EXPERIMENT_NAME_MANDATORY = KruizeConstants.JSONKeys.EXPERIMENT_NAME + " is mandatory";
            public static final String INTERVAL_END_TIME_MANDATORY = KruizeConstants.JSONKeys.INTERVAL_END_TIME + " is mandatory";
            public static final String DATA_NOT_FOUND = "Data not found!";
            public static final String TIME_COMPARE = "The Start time should precede the End time!";
            public static final String TIME_GAP_LIMIT = String.format("The gap between the interval_start_time and interval_end_time must be within a maximum of %s days!", KruizeDeploymentInfo.generate_recommendations_date_range_limit_in_days);

            private UpdateRecommendationsAPI() {
            }
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
}
