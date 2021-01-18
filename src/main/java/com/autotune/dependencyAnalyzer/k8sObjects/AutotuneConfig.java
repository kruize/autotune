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
import com.autotune.dependencyAnalyzer.exceptions.InvalidValueException;
import com.autotune.dependencyAnalyzer.util.DAConstants;
import com.autotune.dependencyAnalyzer.util.DAErrorConstants;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Container class for the AutotuneConfig kubernetes kind, which is used to tune
 * a layer (container, runtime, framework or application)
 *
 * Refer to examples dir for a reference AutotuneConfig yaml.
 */
public final class AutotuneConfig implements Validate
{
	private final int level;
	private final String name;
	private final String details;
	//If true, apply to all autotuneobjects
	private final String presence;

	/*
	Used to detect the presence of the layer in an application. Autotune runs the query, looks for
	the key, and all applications in the query output are matched to the AutotuneConfig object.
	*/
	private final String layerPresenceKey;
	private final String layerPresenceQuery;

	private final String layerPresenceLabel;
	private final String layerPresenceLabelValue;

	private final ArrayList<Tunable> tunables;

	public AutotuneConfig(String name,
			int level,
			String details,
			String presence,
			String layerPresenceQuery,
			String layerPresenceKey,
			String layerPresenceLabel,
			String layerPresenceLabelValue,
			ArrayList<Tunable> tunables) throws InvalidValueException {
		HashMap<Object, Object> map = new HashMap<>();
		map.put(DAConstants.AutotuneConfigConstants.NAME, name);
		map.put(DAConstants.AutotuneConfigConstants.LAYER_LEVEL, level);
		map.put(DAConstants.AutotuneConfigConstants.PRESENCE, presence);
		map.put(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_QUERY, layerPresenceQuery);
		map.put(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_KEY, layerPresenceKey);
		map.put(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_LABEL, layerPresenceLabel);
		map.put(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_LABEL_VALUE, layerPresenceLabelValue);
		map.put(DAConstants.AutotuneConfigConstants.TUNABLES, tunables);

		String error = validate(map);
		if (error.isEmpty()) {
			this.name = name;
			this.presence = presence;
			this.level = level;
			this.details = details;
			this.layerPresenceKey = layerPresenceKey;
			this.layerPresenceQuery = layerPresenceQuery;
			this.layerPresenceLabel = layerPresenceLabel;
			this.layerPresenceLabelValue = layerPresenceLabelValue;
			this.tunables = new ArrayList<>(tunables);

		} else {
			throw new InvalidValueException(error);
		}
	}

	public AutotuneConfig(AutotuneConfig copy) {
		this.name = copy.getName();
		this.level = copy.getLevel();
		this.details = copy.getDetails();
		this.layerPresenceQuery = copy.getLayerPresenceQuery();
		this.layerPresenceKey = copy.getLayerPresenceKey();
		this.layerPresenceLabel = copy.getLayerPresenceLabel();
		this.layerPresenceLabelValue = copy.getLayerPresenceLabelValue();
		this.presence = copy.presence;

		this.tunables = new ArrayList<>(copy.getTunables());
	}

	/**
	 * Check if the AutotuneConfig is valid
	 * @param map
	 * @return
	 */
	public String validate(HashMap<Object, Object> map) {
		StringBuilder errorString = new StringBuilder();

		// Check if name is valid
		if (map.get(DAConstants.AutotuneConfigConstants.NAME) == null || ((String)map.get(DAConstants.AutotuneConfigConstants.NAME)).isEmpty()) {
			errorString.append(DAErrorConstants.AutotuneConfigErrors.AUTOTUNE_CONFIG_NAME_NULL);
		}

		// Check if either presence, layerPresenceQuery or layerPresenceLabel are set. presence field has highest priority.
		if (((String)map.get(DAConstants.AutotuneConfigConstants.PRESENCE)) == null ||
				!((String)map.get(DAConstants.AutotuneConfigConstants.PRESENCE)).equals(DAConstants.PRESENCE_ALWAYS)) {
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
		return errorString.toString();
	}

	public int getLevel() {
		return level;
	}

	public String getDetails() {
		return details;
	}

	public String getName() {
		return name;
	}

	public String getPresence() {
		return presence;
	}

	public ArrayList<Tunable> getTunables() {
		return new ArrayList<>(tunables);
	}

	public String getLayerPresenceKey() {
		return layerPresenceKey;
	}

	public String getLayerPresenceQuery() {
		return layerPresenceQuery;
	}

	public String getLayerPresenceLabel() {
		return layerPresenceLabel;
	}

	public String getLayerPresenceLabelValue() {
		return layerPresenceLabelValue;
	}

	@Override
	public String toString() {
		return "AutotuneConfig{" +
				"level=" + level +
				", name='" + name + '\'' +
				", details='" + details + '\'' +
				", levelPresenceQuery='" + layerPresenceQuery + '\'' +
				", levelPresenceKey='" + layerPresenceKey + '\'' +
				", tunables=" + tunables +
				'}';
	}
}
