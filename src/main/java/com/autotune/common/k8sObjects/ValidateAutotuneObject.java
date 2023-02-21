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
