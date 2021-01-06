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

import com.autotune.dependencyAnalyzer.application.ApplicationServiceStack;
import com.autotune.dependencyAnalyzer.exceptions.InvalidValueException;
import com.autotune.dependencyAnalyzer.util.SupportedTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * Container class for the Autotune kubernetes kind objects.
 * Also contains details about applications matching the autotune object
 *
 * Refer to examples dir for a reference AutotuneObject yaml.
 */
public final class AutotuneObject
{
	private final String name;
	private final String namespace;
	private final String mode;
	private final SlaInfo slaInfo;
	private final SelectorInfo selectorInfo;
	/**
	 * Map of applications matching the label selector in the autotune object yaml
	 */
	private final Map<String, ApplicationServiceStack> applicationsStackMap;

	public AutotuneObject(String name,
			String namespace,
			String mode,
			SlaInfo slaInfo,
			SelectorInfo selectorInfo,
			Map<String, ApplicationServiceStack> applicationsStackMap) throws InvalidValueException {
		if (name != null)
			this.name = name;
		else throw new InvalidValueException("Name cannot be null");

		if (namespace != null)
			this.namespace = namespace;
		else throw new InvalidValueException("Namespace cannot be null");

		if (SupportedTypes.MODES_SUPPORTED.contains(mode))
			this.mode = mode;
		else throw new InvalidValueException("Invalid mode");

		this.slaInfo = new SlaInfo(slaInfo);
		this.selectorInfo = new SelectorInfo(selectorInfo);

		this.applicationsStackMap = new HashMap<>();

		for (String application : applicationsStackMap.keySet()) {
			this.applicationsStackMap.put(application, applicationsStackMap.get(application));
		}
	}

	public String getName() {
		return name;
	}

	public SlaInfo getSlaInfo() {
		return new SlaInfo(slaInfo);
	}

	public SelectorInfo getSelectorInfo() {
		return new SelectorInfo(selectorInfo);
	}

	public String getMode() {
		return mode;
	}

	public String getNamespace() {
		return namespace;
	}

	public Map<String, ApplicationServiceStack> getApplicationsStackMap() {
		HashMap<String, ApplicationServiceStack> map = new HashMap<>();

		for (String application : applicationsStackMap.keySet()) {
			map.put(application, applicationsStackMap.get(application));
		}
		return map;
	}

	@Override
	public String toString() {
		return "AutotuneObject{" +
				"name='" + name + '\'' +
				", namespace='" + namespace + '\'' +
				", mode='" + mode + '\'' +
				", slaInfo=" + slaInfo +
				", selectorInfo=" + selectorInfo +
				'}';
	}
}
