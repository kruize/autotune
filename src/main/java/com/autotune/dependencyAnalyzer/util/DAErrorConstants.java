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
 * Contains strings describing the errors encountered
 */
public class DAErrorConstants
{
	public static final String INVALID_AUTOTUNE_YAML = "Invalid Autotune yaml";
	public static final String INVALID_AUTOTUNE_CONFIG_YAML = "Invalid AutotuneConfig yaml";
	public static final String LAYER_PRESENCE_MISSING = "Cannot detect layer presence in applications : Missing fields";
	public static final String COULD_NOT_GET_LIST_OF_APPLICATIONS = "Could not get the applications for the layer ";
	public static final String MULTIPLE_FUNCTION_VARIABLES_WITH_SAME_NAME = "Multiple function varibles with same name";
	public static final String INVALID_VALUE_TYPE = "Invalid value_type";

}
