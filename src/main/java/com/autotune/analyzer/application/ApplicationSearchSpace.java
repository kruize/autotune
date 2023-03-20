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
package com.autotune.analyzer.application;

import com.autotune.analyzer.kruizeObject.ObjectiveFunction;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ApplicationSearchSpace
{
	private final String experimentName;
	private final String experimentId;
	private final ObjectiveFunction objectiveFunction;
	private final String hpoAlgoImpl;
	private final String direction;
	private final String valueType;
	private final Integer totalTrials;
	private final Integer parallelTrials;

	Map<String, Tunable> tunablesMap;

	public ApplicationSearchSpace(String experimentName,
								  String experimentId,
								  ObjectiveFunction objectiveFunction,
								  String hpoAlgoImpl,
								  String direction,
								  String valueType,
								  Integer totalTrials,
								  Integer parallelTrials) {
		this.experimentName = experimentName;
		this.experimentId = experimentId;
		this.objectiveFunction = objectiveFunction;
		this.hpoAlgoImpl = hpoAlgoImpl;
		this.direction = direction;
		this.valueType = valueType;
		this.totalTrials = totalTrials;
		this.parallelTrials = parallelTrials;

		this.tunablesMap = new HashMap<>();
	}

	public String getExperimentName() {	return experimentName;	}

	public String getExperimentId() {
		return experimentId;
	}

	public ObjectiveFunction getObjectiveFunction() {
		return objectiveFunction;
	}

	public String getHpoAlgoImpl() {
		return hpoAlgoImpl;
	}

	public String getDirection() {
		return direction;
	}

	public Map<String, Tunable> getTunablesMap() {
		return tunablesMap;
	}

	public String getValueType() {
		return valueType;
	}

	public Integer getTotalTrials() { return totalTrials; }

	public Integer getParallelTrials() { return parallelTrials; }
}
