package com.autotune.recommendation_manager;

public class ApplicationTunable
{
	String name;
	String valueType;
	double lowerBound;
	double upperBound;
	double step;
	String queryURL;

	public ApplicationTunable(String name,
			String valueType,
			double lowerBound,
			double upperBound,
			double step, String queryURL) {
		this.name = name;
		this.valueType = valueType;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.step = step;
		this.queryURL = queryURL;
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

	public double getLowerBound() {
		return lowerBound;
	}

	public void setLowerBound(double lowerBound) {
		this.lowerBound = lowerBound;
	}

	public double getUpperBound() {
		return upperBound;
	}

	public void setUpperBound(double upperBound) {
		this.upperBound = upperBound;
	}

	public double getStep() {
		return step;
	}

	public void setStep(double step) {
		this.step = step;
	}

	public String getQueryURL() {
		return queryURL;
	}

	public void setQueryURL(String queryURL) {
		this.queryURL = queryURL;
	}
}
