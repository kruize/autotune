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
import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.utils.AnalyzerConstants;

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
	private final LayerPresenceInfo layerPresenceInfo;
	private final ArrayList<Tunable> tunables;

	public AutotuneConfig(String id, String name,
			String layerName,
			int level,
			String details,
			LayerPresenceInfo layerPresenceInfo,
			ArrayList<Tunable> tunables) throws InvalidValueException {
		this.id = id;
		HashMap<String, Object> map = new HashMap<>();
		map.put(AnalyzerConstants.AutotuneConfigConstants.NAME, name);
		map.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_NAME, layerName);
		map.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_LEVEL, level);
		map.put(AnalyzerConstants.AutotuneConfigConstants.PRESENCE, layerPresenceInfo.getPresence());
		map.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_PRESENCE_QUERY, layerPresenceInfo.getLayerPresenceQuery());
		map.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_PRESENCE_KEY, layerPresenceInfo.getLayerPresenceKey());
		map.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_PRESENCE_LABEL, layerPresenceInfo.getLayerPresenceLabel());
		map.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_PRESENCE_LABEL_VALUE, layerPresenceInfo.getLayerPresenceLabelValue());
		map.put(AnalyzerConstants.AutotuneConfigConstants.TUNABLES, tunables);

		StringBuilder error = ValidateAutotuneConfig.validate(map);
		if (error.toString().isEmpty()) {
			this.name = name;
			this.layerName = layerName;
			this.level = level;
			this.details = details;
			this.layerPresenceInfo = layerPresenceInfo;
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
		this.layerPresenceInfo = copy.getLayerPresenceInfo();
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
		return layerPresenceInfo.getPresence();
	}

	public ArrayList<Tunable> getTunables() {
		return new ArrayList<>(tunables);
	}

	public String getLayerPresenceKey() {
		return layerPresenceInfo.getLayerPresenceKey();
	}

	public String getLayerPresenceQuery() {
		return layerPresenceInfo.getLayerPresenceQuery();
	}

	public String getLayerPresenceLabel() {
		return layerPresenceInfo.getLayerPresenceLabel();
	}

	public String getLayerPresenceLabelValue() {
		return layerPresenceInfo.layerPresenceLabelValue;
	}

	public LayerPresenceInfo getLayerPresenceInfo() {
		return layerPresenceInfo;
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
				", presence='" + layerPresenceInfo.getPresence() + '\'' +
				", layerPresenceKey='" + layerPresenceInfo.getLayerPresenceKey() + '\'' +
				", layerPresenceQuery='" + layerPresenceInfo.getLayerPresenceQuery() + '\'' +
				", layerPresenceLabel='" + layerPresenceInfo.getLayerPresenceLabel() + '\'' +
				", layerPresenceLabelValue='" + layerPresenceInfo.getLayerPresenceLabelValue() + '\'' +
				", tunables=" + tunables +
				'}';
	}
}
