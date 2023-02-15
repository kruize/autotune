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
package com.autotune.common.k8sObjects;

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.utils.EvalExParser;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.utils.AutotuneSupportedTypes;
import com.autotune.utils.AnalyzerConstants;
import com.autotune.utils.AnalyzerErrorConstants;

import java.util.HashMap;

/**
 * Check if the Autotune object in the kubernetes cluster is valid
 */
public class ValidateAutotuneObject
{
	private ValidateAutotuneObject() { }

	/**
	 * Check if the AutotuneObject is valid
	 * @param map
	 * @return
	 */
	public static StringBuilder validate(HashMap<String, Object> map) {
		StringBuilder errorString = new StringBuilder();

		// Check if name is valid
		if (map.get(AnalyzerConstants.AutotuneObjectConstants.NAME) == null || ((String)map.get(AnalyzerConstants.AutotuneObjectConstants.NAME)).isEmpty()) {
			errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.AUTOTUNE_OBJECT_NAME_MISSING);
		}

		// Check if mode is supported
		if (!AutotuneSupportedTypes.MODES_SUPPORTED.contains((String)map.get(AnalyzerConstants.AutotuneObjectConstants.MODE))) {
			errorString.append("Mode: " + map.get(AnalyzerConstants.AutotuneObjectConstants.MODE) + " ");
			errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.MODE_NOT_SUPPORTED);
		}

		// Check if targetCluster is supported
		if (!AutotuneSupportedTypes.TARGET_CLUSTERS_SUPPORTED.contains((String)map.get(AnalyzerConstants.AutotuneObjectConstants.TARGET_CLUSTER))) {
			errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.TARGET_CLUSTER_NOT_SUPPORTED);
		}

		// Check if matching label is set to a valid value (not null or only whitespace)
		SelectorInfo selectorInfo = (SelectorInfo) map.get(AnalyzerConstants.AutotuneObjectConstants.SELECTOR);
		if (selectorInfo.getMatchLabel() == null || selectorInfo.getMatchLabel().trim().isEmpty()) {
			errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.INVALID_MATCHLABEL);
		}

		// Check if labelValue is valid
		if (selectorInfo.getMatchLabelValue() == null || selectorInfo.getMatchLabelValue().trim().isEmpty()) {
			errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.INVALID_MATCHLABEL_VALUE);
		}

		// Check if slo_class is supported
		SloInfo sloInfo = (SloInfo) map.get(AnalyzerConstants.AutotuneObjectConstants.SLO);
		if (!AutotuneSupportedTypes.SLO_CLASSES_SUPPORTED.contains(sloInfo.getSloClass())) {
			errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.SLO_CLASS_NOT_SUPPORTED);
		}

		// Check if direction is supported
		if (!AutotuneSupportedTypes.DIRECTIONS_SUPPORTED.contains(sloInfo.getDirection())) {
			errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.DIRECTION_NOT_SUPPORTED);
		}

		//check if slo_class is 'response_time' and direction is minimize
		if (sloInfo.getSloClass().equalsIgnoreCase(EMConstants.StandardDefaults.RESPONSE_TIME) && !sloInfo.getDirection().equalsIgnoreCase(AnalyzerConstants.AutotuneObjectConstants.MINIMIZE)) {
			errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.INVALID_DIRECTION_FOR_SLO_CLASS);
		}

		//check if slo_class is 'throughput' and direction is maximize
		if (sloInfo.getSloClass().equalsIgnoreCase(EMConstants.StandardDefaults.THROUGHPUT) && !sloInfo.getDirection().equalsIgnoreCase(AnalyzerConstants.AutotuneObjectConstants.MAXIMIZE)) {
			errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.INVALID_DIRECTION_FOR_SLO_CLASS);
		}


		// Check if hpo_algo_impl is supported
		if (!AutotuneSupportedTypes.HPO_ALGOS_SUPPORTED.contains(sloInfo.getHpoAlgoImpl())) {
			errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.HPO_ALGO_NOT_SUPPORTED);
		}

		// Check if objective_function and it's type exists
		if (sloInfo.getObjectiveFunction() == null) {
			errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.OBJECTIVE_FUNCTION_MISSING);
		}

		// Check if function_variables is empty
		if (sloInfo.getFunctionVariables().isEmpty()) {
			errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.FUNCTION_VARIABLES_EMPTY);
		}
		// Get the objective_function type
		String objFunctionType = sloInfo.getObjectiveFunction().getFunction_type();
		String expression = null;
		for (Metric functionVariable : sloInfo.getFunctionVariables()) {
			// Check if datasource is supported
			if (!AutotuneSupportedTypes.MONITORING_AGENTS_SUPPORTED.contains(functionVariable.getDatasource().toLowerCase()))
				errorString.append(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLE).append(functionVariable.getName())
						.append(AnalyzerErrorConstants.AutotuneObjectErrors.DATASOURCE_NOT_SUPPORTED);

			// Check if value_type is supported
			if (!AutotuneSupportedTypes.VALUE_TYPES_SUPPORTED.contains(functionVariable.getValueType().toLowerCase()))
				errorString.append(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLE).append(functionVariable.getName())
						.append(AnalyzerErrorConstants.AutotuneObjectErrors.VALUE_TYPE_NOT_SUPPORTED);

			// Validate Objective Function
			try {
				if (objFunctionType.equals(AnalyzerConstants.AutotuneObjectConstants.EXPRESSION)) {

						expression = sloInfo.getObjectiveFunction().getExpression();
						System.out.println("****** Exprssion = "+expression);
						if (null == expression || expression.equals(AnalyzerConstants.NULL))
							throw new NullPointerException(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_EXPRESSION);

				} else if (objFunctionType.equals(AnalyzerConstants.PerformanceProfileConstants.SOURCE)) {
					if (null != sloInfo.getObjectiveFunction().getExpression()) {
						errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.MISPLACED_EXPRESSION);
						throw new InvalidValueException(errorString.toString());
					}
				} else {
					errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.INVALID_TYPE);
					throw new InvalidValueException(errorString.toString());
				}
			} catch (NullPointerException | InvalidValueException npe) {
				errorString.append(npe.getMessage());
				return errorString;
			}

			// Check if function_variable is part of objective_function
			if (objFunctionType.equals(AnalyzerConstants.AutotuneObjectConstants.EXPRESSION)) {
				if (!expression.contains(functionVariable.getName()))
					errorString.append(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLE)
							.append(functionVariable.getName()).append(" ")
							.append(AnalyzerErrorConstants.AutotuneObjectErrors.FUNCTION_VARIABLE_ERROR);
				return errorString;
			}
		}

		// Check if objective_function is correctly formatted
		if (objFunctionType.equals(AnalyzerConstants.AutotuneObjectConstants.EXPRESSION)) {
			if (expression.equals(AnalyzerConstants.NULL) || !new EvalExParser().validate(sloInfo.getObjectiveFunction().getExpression(), sloInfo.getFunctionVariables())) {
				errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.INVALID_OBJECTIVE_FUNCTION);
			}
		}

		// Placeholder for cluster name validation
		if (map.containsKey(AnalyzerConstants.AutotuneObjectConstants.CLUSTER_NAME)) {
			// if you don't want cluster name to be `null` update the code block below
			if (map.get(AnalyzerConstants.AutotuneObjectConstants.CLUSTER_NAME) == null || ((String)map.get(AnalyzerConstants.AutotuneObjectConstants.CLUSTER_NAME)).isEmpty()) {
				// Add your logic of appending the error string
			}
		}

		return errorString;
	}
}
