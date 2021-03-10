package com.autotune.recommendation_manager;

public class ApplicationTunable
{
	String name;
	String valueType;
	String lowerBound;
	String upperBound;
	double step;

	public ApplicationTunable(String name,
			String valueType,
			String lowerBound,
			String upperBound,
			double step) {
		this.name = name;
		this.valueType = valueType;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.step = step;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValueType() {
		return valueType;
	}

	public void setValueType(String valueType) {
		this.valueType = valueType;
	}

	public String getLowerBound() {
		return lowerBound;
	}

	public void setLowerBound(String lowerBound) {
		this.lowerBound = lowerBound;
	}

	public String getUpperBound() {
		return upperBound;
	}

	public void setUpperBound(String upperBound) {
		this.upperBound = upperBound;
	}

	public double getStep() {
		return step;
	}

	public void setStep(double step) {
		this.step = step;
	}
}
