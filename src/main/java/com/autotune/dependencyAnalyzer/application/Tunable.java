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
package com.autotune.dependencyAnalyzer.application;

import com.autotune.dependencyAnalyzer.exceptions.InvalidBoundsException;
import com.autotune.dependencyAnalyzer.exceptions.InvalidValueException;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

/**
 * Contains the tunable to optimize, along with its upper and lower bounds, value type
 * and the list of sla_class (throughput, response_time, right_size, etc.) for which it is applicable.
 *
 * Example:
 * - name: <Tunable>
 *   value_type: double
 *   upper_bound: '4.0'
 *   lower_bound: '2.0'
 *   queries:
 *     datasource:
 *     - name: 'prometheus'
 *       query: '(container_cpu_usage_seconds_total{$CONTAINER_LABEL$!="POD", $POD_LABEL$="$POD$"}[1m])'
 *   sla_class:
 *   - response_time
 *   - throughput
 */
public class Tunable
{
	private String name;
	private double upperBound;
	private double lowerBound;
	private String valueType;
	private String description;
	private Map<String, String> queries;

    /*
    TODO Think about bounds for other valueTypes
    String bound; //[1.5-3.5], [true, false]
    */

	public ArrayList<String> slaClassList;

	public Tunable(String name,
				   double upperBound,
				   double lowerBound,
				   String valueType,
				   Map<String, String> queries,
				   ArrayList<String> slaClassList) throws InvalidBoundsException {
		this.queries = queries;
		this.name = Objects.requireNonNull(name, "name cannot be null");
		this.valueType = Objects.requireNonNull(valueType, "Value type cannot be null");
		this.slaClassList = Objects.requireNonNull(slaClassList, "tunable should contain supported sla_classes");

		if (upperBound > lowerBound) {
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
		} else throw new InvalidBoundsException();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) throws InvalidValueException {
		if (name != null)
			this.name = name;
		else
			throw new InvalidValueException("Tunable name cannot be null");
	}

	public double getUpperBound() {
		return upperBound;
	}

	public void setUpperBound(double upperBound) {
		this.upperBound = upperBound;
	}

	public double getLowerBound() {
		return lowerBound;
	}

	public void setLowerBound(double lowerBound) throws InvalidBoundsException {
		if (lowerBound < this.upperBound)
			this.lowerBound = lowerBound;
		else
			throw new InvalidBoundsException();
	}

	public String getValueType() {
		return valueType;
	}

	public void setValueType(String valueType) throws InvalidValueException {
		if (valueType != null)
			this.valueType = valueType;
		else
			throw new InvalidValueException("Value type not set for tunable");
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

	public ArrayList<String> getSlaClassList() {
		return slaClassList;
	}

	public void setSlaClassList(ArrayList<String> slaClassList) {
		this.slaClassList = slaClassList;
	}

	@Override
	public String toString() {
		return "Tunable{" +
				"name='" + name + '\'' +
				", upperBound='" + upperBound + '\'' +
				", lowerBound='" + lowerBound + '\'' +
				", valueType='" + valueType + '\'' +
				", description='" + description + '\'' +
				", queries='" + queries + '\'' +
				", slaClassList=" + slaClassList +
				'}';
	}
}
