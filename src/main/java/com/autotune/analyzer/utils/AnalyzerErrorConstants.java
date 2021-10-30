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
package com.autotune.analyzer.utils;

/**
 * Contains strings describing the errors encountered
 */
public class AnalyzerErrorConstants
{

	public static class AutotuneConfigErrors {
		public final static String AUTOTUNE_CONFIG_NAME_NULL = "AutotuneConfig object name cannot be null or empty\n";
		public final static String LAYER_PRESENCE_MISSING = "Layer presence missing! Must be indicated through a presence field, layerPresenceQuery or layerPresenceLabel\n";
		public final static String BOTH_LAYER_QUERY_AND_LABEL_SET = "Both layerPresenceQuery and layerPresenceLabel cannot be set\n";
		public final static String LAYER_LEVEL_INVALID = "Layer level must be a non-negative integer\n";
		public final static String NO_TUNABLES = "AutotuneConfig has no tunables\n";
		public static final String COULD_NOT_GET_LIST_OF_APPLICATIONS = "Could not get the applications for the layer ";
		public static final String INVALID_SLO_CLASS = "Invalid slo_class for tunable ";
		public static final String TUNABLE_NAME_EMPTY = "Tunable name cannot be empty";
		public static final String ZERO_STEP = "Tunable step cannot be 0";
	}

	public static class AutotuneObjectErrors {
		public static final String AUTOTUNE_OBJECT_NAME_MISSING = "Autotune object name cannot be null or empty\n";
		public static final String INVALID_MATCHLABEL = "Invalid MatchLabel in selector\n";
		public static final String INVALID_MATCHLABEL_VALUE = "Invalid or blank MatchLabelValue in selector\n";
		public static final String SLO_CLASS_NOT_SUPPORTED = "slo_class not supported\n";
		public static final String DIRECTION_NOT_SUPPORTED = "direction not supported\n";
		public static final String FUNCTION_VARIABLES_EMPTY = "function_variables is empty\n";
		public static final String OBJECTIVE_FUNCTION_MISSING = "objective_function missing\n";
		public static final String MODE_NOT_SUPPORTED = "Autotune object mode not supported\n";
		public static final String HPO_ALGO_NOT_SUPPORTED = "HPO algorithm not supported\n";
	}
}
