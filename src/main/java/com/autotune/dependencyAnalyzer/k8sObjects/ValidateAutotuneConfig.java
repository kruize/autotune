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

import com.autotune.dependencyAnalyzer.application.Tunable;
import com.autotune.dependencyAnalyzer.util.AutotuneSupportedTypes;
import com.autotune.dependencyAnalyzer.util.DAConstants;
import com.autotune.dependencyAnalyzer.util.DAErrorConstants;

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
		if (map.get(DAConstants.AutotuneConfigConstants.NAME) == null || ((String)map.get(DAConstants.AutotuneConfigConstants.NAME)).isEmpty()) {
			errorString.append(DAErrorConstants.AutotuneConfigErrors.AUTOTUNE_CONFIG_NAME_NULL);
		}

		// Check if either presence, layerPresenceQuery or layerPresenceLabel are set. presence field has highest priority.
		if (map.get(DAConstants.AutotuneConfigConstants.PRESENCE) == null ||
				!map.get(DAConstants.AutotuneConfigConstants.PRESENCE).equals(DAConstants.PRESENCE_ALWAYS)) {
			if ((map.get(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_LABEL) == null || map.get(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_LABEL_VALUE) == null) &&
					(map.get(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_QUERY) == null || map.get(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_KEY) == null)) {
				errorString.append(DAErrorConstants.AutotuneConfigErrors.LAYER_PRESENCE_MISSING);
			}
		}

		// Check if both layerPresenceQuery and layerPresenceLabel are set
		if (map.get(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_QUERY) != null && map.get(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_LABEL) != null) {
			errorString.append(DAErrorConstants.AutotuneConfigErrors.BOTH_LAYER_QUERY_AND_LABEL_SET);
		}

		// Check if level is valid
		if ((Integer)map.get(DAConstants.AutotuneConfigConstants.LAYER_LEVEL) < 0) {
			errorString.append(DAErrorConstants.AutotuneConfigErrors.LAYER_LEVEL_INVALID);
		}

		// Check if tunables is empty
		if ((map.get(DAConstants.AutotuneConfigConstants.TUNABLES)) == null) {
			errorString.append(DAErrorConstants.AutotuneConfigErrors.NO_TUNABLES);
		}

		// Validate sla_class in tunables
		ArrayList<Tunable> tunableArrayList = (ArrayList<Tunable>) map.get(DAConstants.AutotuneConfigConstants.TUNABLES);
		if (tunableArrayList != null) {
			for (Tunable tunable : tunableArrayList) {
				if (tunable.getName().isEmpty()) {
					errorString.append(DAErrorConstants.AutotuneConfigErrors.TUNABLE_NAME_EMPTY);
				}
				for (String sla_class : tunable.getSlaClassList()) {
					if (!AutotuneSupportedTypes.SLA_CLASSES_SUPPORTED.contains(sla_class)) {
						errorString.append(DAErrorConstants.AutotuneConfigErrors.INVALID_SLA_CLASS).append(tunable.getName()).append("\n");
					}
				}
			}
		}
		return errorString;
	}

}
