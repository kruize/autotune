package com.autotune.analyzer.application;

import java.util.HashMap;
import java.util.Map;

public class ApplicationSearchSpace
{
	String applicationId;
	String applicationName;
	String objectiveFunction;
	String hpoAlgoImpl;
	String direction;
	String valueType;

	Map<String, Tunable> applicationTunablesMap;

	public ApplicationSearchSpace(String applicationId,
								  String applicationName,
								  String objectiveFunction,
								  String hpoAlgoImpl,
								  String direction,
								  String valueType) {
		this.applicationId = applicationId;
		this.applicationName = applicationName;
		this.objectiveFunction = objectiveFunction;
		this.hpoAlgoImpl = hpoAlgoImpl;
		this.direction = direction;
		this.valueType = valueType;

		this.applicationTunablesMap = new HashMap<>();
	}

	public String getApplicationId() {
		return applicationId;
	}

	public String getApplicationName() {
		return applicationName;
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

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public Map<String, Tunable> getApplicationTunablesMap() {
		return applicationTunablesMap;
	}

	public String getValueType() {
		return valueType;
	}
}