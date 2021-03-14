package com.autotune.recommendation_manager;

import java.util.ArrayList;

public class ApplicationSearchSpace
{
	String id;
	String applicationName;
	String objectiveFunction;
	String hpoAlgoImpl;
	String direction;
	String valueType;

	ArrayList<ApplicationTunable> applicationTunables;

	public ApplicationSearchSpace(String id, String applicationName, String objectiveFunction, String hpoAlgoImpl, String direction, String valueType, ArrayList<ApplicationTunable> applicationTunables) {
		this.id = id;
		this.applicationName = applicationName;
		this.objectiveFunction = objectiveFunction;
		this.hpoAlgoImpl = hpoAlgoImpl;
		this.direction = direction;
		this.valueType = valueType;
		this.applicationTunables = applicationTunables;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public String getObjectiveFunction() {
		return objectiveFunction;
	}

	public void setObjectiveFunction(String objectiveFunction) {
		this.objectiveFunction = objectiveFunction;
	}

	public String getHpoAlgoImpl() {
		return hpoAlgoImpl;
	}

	public void setHpoAlgoImpl(String hpoAlgoImpl) {
		this.hpoAlgoImpl = hpoAlgoImpl;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public ArrayList<ApplicationTunable> getApplicationTunables() {
		return applicationTunables;
	}

	public void setApplicationTunables(ArrayList<ApplicationTunable> applicationTunables) {
		this.applicationTunables = applicationTunables;
	}

	public String getValueType() {
		return valueType;
	}

	public void setValueType(String valueType) {
		this.valueType = valueType;
	}
}
