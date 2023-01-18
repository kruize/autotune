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
package com.autotune.common.k8sObjects;

import com.autotune.common.performanceProfiles.AggregationFunctions;
import com.autotune.utils.AnalyzerConstants;
import com.autotune.utils.AnalyzerErrorConstants;
import com.autotune.utils.AutotuneSupportedTypes;

import java.util.HashMap;
import java.util.List;

/**
 * Check if the Performance Profile object in the kubernetes cluster is valid
 */
public class ValidatePerformanceProfileObject
{
	private ValidatePerformanceProfileObject() { }

	/**
	 * Check if the PerformanceProfileObject is valid
	 * @param map - contains the parameters and their values to be validated
	 * @return - returns the concatenated error msg, if any.
	 */
	public static StringBuilder validate(HashMap<String, Object> map) {
		StringBuilder errorString = new StringBuilder();

		// Check if k8s type is supported
		String k8sType = (String) map.get(AnalyzerConstants.K8S_TYPE);
		if (!AutotuneSupportedTypes.K8S_TYPES_SUPPORTED.contains(k8sType))
			errorString.append("k8s type ").append(k8sType).append(" is not supported");

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
		if (sloInfo.getSloClass().equalsIgnoreCase("response_time") && !sloInfo.getDirection().equalsIgnoreCase("minimize")) {
			errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.INVALID_DIRECTION_FOR_SLO_CLASS);
		}

		//check if slo_class is 'throughput' and direction is maximize
		if (sloInfo.getSloClass().equalsIgnoreCase("throughput") && !sloInfo.getDirection().equalsIgnoreCase("maximize")) {
			errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.INVALID_DIRECTION_FOR_SLO_CLASS);
		}

		// Check if function_variables is empty
		if (sloInfo.getFunctionVariables().isEmpty()) {
			errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.FUNCTION_VARIABLES_EMPTY);
		}

		for (Metric functionVariable : sloInfo.getFunctionVariables()) {
			// Check if datasource is supported
			if (!AutotuneSupportedTypes.MONITORING_AGENTS_SUPPORTED.contains(functionVariable.getDatasource().toLowerCase()))
				errorString.append("function_variable: ").append(functionVariable.getName()).append(" datasource not supported\n");

			// Check if value_type is supported
			if (!AutotuneSupportedTypes.VALUE_TYPES_SUPPORTED.contains(functionVariable.getValueType().toLowerCase()))
				errorString.append("function_variable: ").append(functionVariable.getName()).append(" value_type not supported\n");

			// Check if kubernetes_object type is supported
			String kubernetes_object = functionVariable.getKubernetesObject().toLowerCase();
			if (!AutotuneSupportedTypes.KUBERNETES_OBJECTS_SUPPORTED.contains(functionVariable.getKubernetesObject().toLowerCase()))
				errorString.append("kubernetes_object ").append(kubernetes_object).append(" is not supported");
			// Check if one of query or aggregation_functions is present
			String query = (String) map.get(AnalyzerConstants.AutotuneObjectConstants.QUERY);
			List<AggregationFunctions>  aggregationFunctionsList = functionVariable.getAggregationFunctions();

			if (query == null && aggregationFunctionsList.isEmpty()) {
				errorString.append("One of query or aggregation_functions is mandatory. Both cannot be null!");
			}
		}
		return errorString;
	}
}
