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

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Container class for the AutotuneConfig kubernetes kind, which is used to tune
 * a layer (container, runtime, framework or application)
 *
 * Refer to examples dir for a reference AutotuneConfig yaml.
 */
public final class AutotuneConfig
{
	private final String id;
	private final int level;
	private final String name;
	private final String layerName;
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

	public AutotuneConfig(String id, String name,
			String layerName,
			int level,
			String details,
			String presence,
			String layerPresenceQuery,
			String layerPresenceKey,
			String layerPresenceLabel,
			String layerPresenceLabelValue,
			ArrayList<Tunable> tunables) throws InvalidValueException {
		this.id = id;
		HashMap<String, Object> map = new HashMap<>();
		map.put(DAConstants.AutotuneConfigConstants.NAME, name);
		map.put(DAConstants.AutotuneConfigConstants.LAYER_NAME, layerName);
		map.put(DAConstants.AutotuneConfigConstants.LAYER_LEVEL, level);
		map.put(DAConstants.AutotuneConfigConstants.PRESENCE, presence);
		map.put(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_QUERY, layerPresenceQuery);
		map.put(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_KEY, layerPresenceKey);
		map.put(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_LABEL, layerPresenceLabel);
		map.put(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_LABEL_VALUE, layerPresenceLabelValue);
		map.put(DAConstants.AutotuneConfigConstants.TUNABLES, tunables);

		StringBuilder error = ValidateAutotuneConfig.validate(map);
		if (error.toString().isEmpty()) {
			this.name = name;
			this.layerName = layerName;
			this.presence = presence;
			this.level = level;
			this.details = details;
			this.layerPresenceKey = layerPresenceKey;
			this.layerPresenceQuery = layerPresenceQuery;
			this.layerPresenceLabel = layerPresenceLabel;
			this.layerPresenceLabelValue = layerPresenceLabelValue;
			this.tunables = new ArrayList<>(tunables);

		} else {
			throw new InvalidValueException(error.toString());
		}
	}

	public AutotuneConfig(AutotuneConfig copy) {
		this.id = copy.getId();
		this.name = copy.getName();
		this.layerName = copy.getLayerName();
		this.level = copy.getLevel();
		this.details = copy.getDetails();
		this.layerPresenceQuery = copy.getLayerPresenceQuery();
		this.layerPresenceKey = copy.getLayerPresenceKey();
		this.layerPresenceLabel = copy.getLayerPresenceLabel();
		this.layerPresenceLabelValue = copy.getLayerPresenceLabelValue();
		this.presence = copy.presence;

		this.tunables = new ArrayList<>(copy.getTunables());
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

	public String getLayerName() {
		return layerName;
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

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return "AutotuneConfig{" +
				"level=" + level +
				", name='" + name + '\'' +
				", layerName='" + layerName + '\'' +
				", presence='" + presence + '\'' +
				", layerPresenceKey='" + layerPresenceKey + '\'' +
				", layerPresenceQuery='" + layerPresenceQuery + '\'' +
				", layerPresenceLabel='" + layerPresenceLabel + '\'' +
				", layerPresenceLabelValue='" + layerPresenceLabelValue + '\'' +
				", tunables=" + tunables +
				'}';
	}
}
