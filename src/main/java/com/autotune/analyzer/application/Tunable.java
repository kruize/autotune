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

import com.autotune.analyzer.exceptions.InvalidBoundsException;
import com.autotune.analyzer.utils.AutotuneSupportedTypes;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import static com.autotune.analyzer.utils.AnalyzerConstants.AutotuneConfigConstants.*;
import static com.autotune.analyzer.utils.AnalyzerErrorConstants.AutotuneConfigErrors.*;

/**
 * Contains the tunable to optimize, along with its upper and lower bounds, value type
 * and the list of slo_class (throughput, response_time, right_size, etc.) for which it is applicable.
 *
 * Example:
 * - name: <Tunable>
 *   value_type: double
 *   upper_bound: 4.0
 *   lower_bound: 2.0
 *   step: 0.01
 *   queries:
 *     datasource:
 *     - name: 'prometheus'
 *       query: '(container_cpu_usage_seconds_total{$CONTAINER_LABEL$!="POD", $POD_LABEL$="$POD$"}[1m])'
 *   slo_class:
 *   - response_time
 *   - throughput
 */
public class Tunable
{
	private final String name;
	private final double step;
	private final String valueType;
	private final Double upperBoundValue;
	private final Double lowerBoundValue;
	private final String boundUnits;
	private String description;
	private Map<String, String> queries;
	private final String layerName;

    /*
    TODO Think about bounds for other valueTypes
    String bound; //[1.5-3.5], [true, false]
    */

	public ArrayList<String> sloClassList;

	private void validateBounds(Double upperBoundValue,
								Double lowerBoundValue,
								String upperBoundUnits,
								String lowerBoundUnits) throws InvalidBoundsException {
		if (upperBoundUnits != null &&
			!upperBoundUnits.trim().isEmpty() &&
			lowerBoundUnits != null &&
			!lowerBoundUnits.trim().isEmpty() &&
			!lowerBoundUnits.equalsIgnoreCase(upperBoundUnits)) {
			throw new InvalidBoundsException("Tunable: " + name +
					" has invalid bound units; ubv: " + upperBoundValue +
					" lbv: " + lowerBoundValue +
					" ubu: " + upperBoundUnits +
					" lbu: " + lowerBoundUnits);
		}

		/*
		 * Bounds cannot be negative.
		 * upperBound has to be greater than lowerBound.
		 * step has to be lesser than or equal to the difference between the two bounds.
		 */
		if (upperBoundValue < 0 ||
			lowerBoundValue < 0 ||
			lowerBoundValue >= upperBoundValue ||
			step > (upperBoundValue - lowerBoundValue)
		) {
			throw new InvalidBoundsException("ERROR: Tunable: " + name +
					" has invalid bounds; ubv: " + upperBoundValue +
					" lbv: " + lowerBoundValue +
					" ubu: " + upperBoundUnits +
					" lbu: " + lowerBoundUnits);
		}
	}

	public Tunable(String name,
				   double step,
				   String upperBound,
				   String lowerBound,
				   String valueType,
				   Map<String, String> queries,
				   ArrayList<String> sloClassList,
				   String layerName) throws InvalidBoundsException {
		this.queries = queries;
		this.name = Objects.requireNonNull(name, TUNABLE_NAME_EMPTY);
		this.valueType = Objects.requireNonNull(valueType, VALUE_TYPE_NULL);
		this.sloClassList = Objects.requireNonNull(sloClassList, INVALID_SLO_CLASS);
		this.step = Objects.requireNonNull(step, ZERO_STEP);
		if (AutotuneSupportedTypes.LAYERS_SUPPORTED.contains(layerName))
			this.layerName = layerName;
		else
			this.layerName = "generic";

		/* Parse the value for the bounds from the strings passed in */
		Double upperBoundValue = Double.parseDouble(BOUND_CHARS.matcher(upperBound).replaceAll(""));
		Double lowerBoundValue = Double.parseDouble(BOUND_CHARS.matcher(lowerBound).replaceAll(""));

		/* Parse the bound units from the strings passed in and make sure they are the same */
		String upperBoundUnits = BOUND_DIGITS.matcher(upperBound).replaceAll("");
		String lowerBoundUnits = BOUND_DIGITS.matcher(lowerBound).replaceAll("");

		validateBounds(upperBoundValue, lowerBoundValue, upperBoundUnits, lowerBoundUnits);

		this.lowerBoundValue = lowerBoundValue;
		this.upperBoundValue = upperBoundValue;
		this.boundUnits = upperBoundUnits;

		System.out.println("Adding Tunable: " + name +
				" has bounds; ubv: " + this.upperBoundValue +
				" lbv: " + this.lowerBoundValue +
				" ubu: " + upperBoundUnits +
				" lbu: " + lowerBoundUnits);
	}

	public Tunable(Tunable copy) {
		this.name = copy.name;
		this.step = copy.step;
		this.upperBoundValue = copy.upperBoundValue;
		this.lowerBoundValue = copy.lowerBoundValue;
		this.boundUnits = copy.boundUnits;
		this.valueType = copy.valueType;
		this.description = copy.description;
		this.queries = copy.queries;
		this.sloClassList = copy.sloClassList;
		this.layerName = copy.layerName;
	}

	public String getName() {
		return name;
	}

	private String getBound(Double boundVal, String boundUnits, String valueType) {
		if (valueType.equalsIgnoreCase(INTEGER)) {
			return boundVal.intValue() + boundUnits;
		}
		if (valueType.equalsIgnoreCase(LONG)) {
			return boundVal.longValue() + boundUnits;
		}
		return boundVal + boundUnits;
	}

	public Double getUpperBoundValue() {
		return upperBoundValue;
	}

	public String getUpperBound() {
		return getBound(upperBoundValue, boundUnits, valueType);
	}

	public Double getLowerBoundValue() {
		return lowerBoundValue;
	}

	public String getLowerBound() {
		return getBound(lowerBoundValue, boundUnits, valueType);
	}

	public String getBoundUnits() {	return boundUnits; }

	public String getValueType() {
		return valueType;
	}

	public Map<String, String> getQueries() {
		return queries;
	}

	public void setQueries(Map<String, String> queries) {
		this.queries = queries;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ArrayList<String> getSloClassList() {
		return sloClassList;
	}

	public double getStep() {
		return step;
	}

	public String getLayerName() { return layerName; }

	@Override
	public String toString() {
		return "Tunable{" +
				"name='" + name + '\'' +
				", step=" + step +
				", valueType='" + valueType + '\'' +
				", upperBound='" + upperBoundValue + '\'' +
				", lowerBound='" + lowerBoundValue + '\'' +
				", boundUnits='" + boundUnits + '\'' +
				", description='" + description + '\'' +
				", queries=" + queries +
				", sloClassList=" + sloClassList +
				", layer=" + layerName +
				'}';
	}
}
