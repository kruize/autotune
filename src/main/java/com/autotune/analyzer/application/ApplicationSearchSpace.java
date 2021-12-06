package com.autotune.analyzer.application;

import java.util.HashMap;
import java.util.Map;

public class ApplicationSearchSpace
{
	private final String experimentName;
	private final String experimentId;
	private final String objectiveFunction;
	private final String hpoAlgoImpl;
	private final String direction;
	private final String valueType;

	Map<String, Tunable> tunablesMap;

	public ApplicationSearchSpace(String experimentName,
								  String experimentId,
								  String objectiveFunction,
								  String hpoAlgoImpl,
								  String direction,
								  String valueType) {
		this.experimentName = experimentName;
		this.experimentId = experimentId;
		this.objectiveFunction = objectiveFunction;
		this.hpoAlgoImpl = hpoAlgoImpl;
		this.direction = direction;
		this.valueType = valueType;

		this.tunablesMap = new HashMap<>();
	}

	public String getExperimentName() {	return experimentName;	}

	public String getExperimentId() {
		return experimentId;
	}

	public String getObjectiveFunction() {
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
}
