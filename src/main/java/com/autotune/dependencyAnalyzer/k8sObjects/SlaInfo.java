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

import com.autotune.dependencyAnalyzer.util.DAConstants;
import com.autotune.dependencyAnalyzer.exceptions.InvalidValueException;

import java.util.ArrayList;

/**
 * Holds information about the sla key in the autotune object yaml
 *
 * Example:
 * sla:
 *     objective_function: "transaction_response_time"
 *     sla_class: "response_time"
 *     direction: "minimize"
 *     function_variables:
 *     - name: "transaction_response_time"
 *       query: "application_org_acme_microprofile_metrics_PrimeNumberChecker_checksTimer_mean_seconds"
 *       datasource: "prometheus"
 *       value_type: "double"
 *   mode: "show"
 *   selector:
 *     matchLabel: "app.kubernetes.io/name"
 *     matchLabelValue: "petclinic-deployment"
 */
public class SlaInfo
{
	private String slaClass;
	private String objectiveFunction;
	private String direction;
	private ArrayList<FunctionVariable> functionVariables;

	public String getSlaClass() {
		return slaClass;
	}

	public void setSlaClass(String slaClass) {
		this.slaClass = slaClass;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) throws InvalidValueException
	{
		if (DAConstants.DIRECTIONS_SUPPORTED.contains(direction))
			this.direction = direction;
		else throw new InvalidValueException("Invalid direction for autotune kind");
	}

	public String getObjectiveFunction()
	{
		return objectiveFunction;
	}

	public void setObjectiveFunction(String objectiveFunction)
	{
		this.objectiveFunction = objectiveFunction;
	}

	public ArrayList<FunctionVariable> getFunctionVariables()
	{
		return functionVariables;
	}

	@Override
	public String toString()
	{
		return "SlaInfo{" +
				"slaClass='" + slaClass + '\'' +
				", objectiveFunction='" + objectiveFunction + '\'' +
				", direction='" + direction + '\'' +
				", functionVariables=" + functionVariables +
				'}';
	}
}
