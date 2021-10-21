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
package com.autotune.analyzer.k8sObjects;

import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.utils.AutotuneSupportedTypes;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Check if the AutotuneConfig object in the kubernetes cluster is valid
 */
public class ValidateAutotuneConfig
{
	/**
	 * Check if the AutotuneConfig is valid
	 * @param map
	 * @return
	 */
	public static StringBuilder validate(HashMap<String, Object> map) {
		StringBuilder errorString = new StringBuilder();

		// Check if name is valid
		if (map.get(AnalyzerConstants.AutotuneConfigConstants.NAME) == null || ((String)map.get(AnalyzerConstants.AutotuneConfigConstants.NAME)).isEmpty()) {
			errorString.append(AnalyzerErrorConstants.AutotuneConfigErrors.AUTOTUNE_CONFIG_NAME_NULL);
		}

		// Check if either presence, layerPresenceQuery or layerPresenceLabel are set. presence field has highest priority.
		if (map.get(AnalyzerConstants.AutotuneConfigConstants.PRESENCE) == null ||
				!map.get(AnalyzerConstants.AutotuneConfigConstants.PRESENCE).equals(AnalyzerConstants.PRESENCE_ALWAYS)) {
			if ((map.get(AnalyzerConstants.AutotuneConfigConstants.LAYER_PRESENCE_LABEL) == null || map.get(AnalyzerConstants.AutotuneConfigConstants.LAYER_PRESENCE_LABEL_VALUE) == null) &&
					(map.get(AnalyzerConstants.AutotuneConfigConstants.LAYER_PRESENCE_QUERY) == null || map.get(AnalyzerConstants.AutotuneConfigConstants.LAYER_PRESENCE_KEY) == null)) {
				errorString.append(AnalyzerErrorConstants.AutotuneConfigErrors.LAYER_PRESENCE_MISSING);
			}
		}

		// Check if both layerPresenceQuery and layerPresenceLabel are set
		if (map.get(AnalyzerConstants.AutotuneConfigConstants.LAYER_PRESENCE_QUERY) != null && map.get(AnalyzerConstants.AutotuneConfigConstants.LAYER_PRESENCE_LABEL) != null) {
			errorString.append(AnalyzerErrorConstants.AutotuneConfigErrors.BOTH_LAYER_QUERY_AND_LABEL_SET);
		}

		// Check if level is valid
		if ((Integer)map.get(AnalyzerConstants.AutotuneConfigConstants.LAYER_LEVEL) < 0) {
			errorString.append(AnalyzerErrorConstants.AutotuneConfigErrors.LAYER_LEVEL_INVALID);
		}

		// Check if tunables is empty
		if ((map.get(AnalyzerConstants.AutotuneConfigConstants.TUNABLES)) == null) {
			errorString.append(AnalyzerErrorConstants.AutotuneConfigErrors.NO_TUNABLES);
		}

		// Validate sla_class in tunables
		ArrayList<Tunable> tunableArrayList = (ArrayList<Tunable>) map.get(AnalyzerConstants.AutotuneConfigConstants.TUNABLES);
		if (tunableArrayList != null) {
			for (Tunable tunable : tunableArrayList) {
				if (tunable.getName().trim().isEmpty()) {
					errorString.append(AnalyzerErrorConstants.AutotuneConfigErrors.TUNABLE_NAME_EMPTY);
				}
				for (String sla_class : tunable.getSloClassList()) {
					if (!AutotuneSupportedTypes.SLO_CLASSES_SUPPORTED.contains(sla_class)) {
						errorString.append(AnalyzerErrorConstants.AutotuneConfigErrors.INVALID_SLO_CLASS).append(tunable.getName()).append("\n");
					}

					if (tunable.getStep() == 0) {
						errorString.append(AnalyzerErrorConstants.AutotuneConfigErrors.ZERO_STEP);
					}
				}
			}
		}
		return errorString;
	}

}
