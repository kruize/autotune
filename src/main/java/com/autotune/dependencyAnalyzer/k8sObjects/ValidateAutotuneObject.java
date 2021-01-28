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
package com.autotune.dependencyAnalyzer.k8sObjects;

import com.autotune.dependencyAnalyzer.util.AutotuneSupportedTypes;
import com.autotune.dependencyAnalyzer.util.DAConstants;
import com.autotune.dependencyAnalyzer.util.DAErrorConstants;

import java.util.HashMap;

/**
 * Check if the Autotune object in the kubernetes cluster is valid
 */
public class ValidateAutotuneObject
{
	/**
	 * Check if the AutotuneObject is valid
	 * @param map
	 * @return
	 */
	public static StringBuilder validate(HashMap<String, Object> map) {
		StringBuilder errorString = new StringBuilder();

		// Check if name is valid
		if (map.get(DAConstants.AutotuneObjectConstants.NAME) == null || ((String) map.get(DAConstants.AutotuneObjectConstants.NAME)).isEmpty()) {
			errorString.append(DAErrorConstants.AutotuneObjectErrors.AUTOTUNE_OBJECT_NAME_MISSING);
		}

		// Check if mode is supported
		if (!AutotuneSupportedTypes.MODES_SUPPORTED.contains((String) map.get(DAConstants.AutotuneObjectConstants.MODE))) {
			errorString.append(DAErrorConstants.AutotuneObjectErrors.MODE_NOT_SUPPORTED);
		}

		// Check if matching label is set
		SelectorInfo selectorInfo = (SelectorInfo) map.get(DAConstants.AutotuneObjectConstants.SELECTOR);
		if (selectorInfo.getMatchLabel() == null || selectorInfo.getMatchLabel().isEmpty()) {
			errorString.append(DAErrorConstants.AutotuneObjectErrors.INVALID_MATCHLABEL);
		}

		if (selectorInfo.getMatchLabelValue() == null || selectorInfo.getMatchLabelValue().isEmpty()) {
			errorString.append(DAErrorConstants.AutotuneObjectErrors.INVALID_MATCHLABEL_VALUE);
		}

		// Check if sla_class is supported
		SlaInfo slaInfo = (SlaInfo) map.get(DAConstants.AutotuneObjectConstants.SLA);
		if (!AutotuneSupportedTypes.SLA_CLASSES_SUPPORTED.contains(slaInfo.getSlaClass())) {
			errorString.append(DAErrorConstants.AutotuneObjectErrors.SLA_CLASS_NOT_SUPPORTED);
		}

		// Check if direction is supported
		if (!AutotuneSupportedTypes.DIRECTIONS_SUPPORTED.contains(slaInfo.getDirection())) {
			errorString.append(DAErrorConstants.AutotuneObjectErrors.DIRECTION_NOT_SUPPORTED);
		}

		// Check if function_variables is empty
		if (slaInfo.getFunctionVariables().isEmpty()) {
			errorString.append(DAErrorConstants.AutotuneObjectErrors.FUNCTION_VARIABLES_EMPTY);
		}

		// Check if objective_function exists
		if (slaInfo.getObjectiveFunction() == null || slaInfo.getObjectiveFunction().isEmpty()) {
			errorString.append(DAErrorConstants.AutotuneObjectErrors.OBJECTIVE_FUNCTION_MISSING);
		}

		for (FunctionVariable functionVariable : slaInfo.getFunctionVariables()) {
			// Check if datasource is supported
			if (!AutotuneSupportedTypes.MONITORING_AGENTS_SUPPORTED.contains(functionVariable.getDatasource().toLowerCase()))
				errorString.append("function_variable: ").append(functionVariable.getName()).append(" datasource not supported\n");

			// Check if value_type is supported
			if (!AutotuneSupportedTypes.VALUE_TYPES_SUPPORTED.contains(functionVariable.getValueType().toLowerCase()))
				errorString.append("function_variable: ").append(functionVariable.getName()).append(" value_type not supported\n");

			// Check if function_variable is part of objective_function
			String objectiveFunction = slaInfo.getObjectiveFunction();
			if (objectiveFunction != null && !objectiveFunction.contains(functionVariable.getName()))
				errorString.append("function_variable ").append(functionVariable.getName()).append(" missing in objective_function\n");
		}
		return errorString;
	}
}
