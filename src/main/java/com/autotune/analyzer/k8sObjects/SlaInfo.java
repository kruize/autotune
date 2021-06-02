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
package com.autotune.analyzer.k8sObjects;

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.utils.AutotuneSupportedTypes;

import java.util.ArrayList;

/**
 * Holds information about the sla key in the autotune object yaml
 *
 * Example:
 * sla:
 *     objective_function: "transaction_response_time"
 *     sla_class: "response_time"
 *     direction: "minimize"
 *     hpo_algo_impl: "optuna_tpe"
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
public final class SlaInfo
{
	private final String slaClass;
	private final String objectiveFunction;
	private final String direction;
	private final String hpoAlgoImpl;
	private final ArrayList<FunctionVariable> functionVariables;

	public SlaInfo(String slaClass,
		String objectiveFunction,
		String direction,
		String hpoAlgoImpl,
		ArrayList<FunctionVariable> functionVariables) throws InvalidValueException {
		if (AutotuneSupportedTypes.SLA_CLASSES_SUPPORTED.contains(slaClass))
			this.slaClass = slaClass;
		else
			throw new InvalidValueException("sla_class: " + slaClass + " not supported");
		this.objectiveFunction = objectiveFunction;

		if (AutotuneSupportedTypes.DIRECTIONS_SUPPORTED.contains(direction))
			this.direction = direction;
		else
			throw new InvalidValueException("Invalid direction for autotune kind");

		if (AutotuneSupportedTypes.HPO_ALGOS_SUPPORTED.contains(hpoAlgoImpl))
			this.hpoAlgoImpl = hpoAlgoImpl;
		else
			throw new InvalidValueException("Hyperparameter Optimization Algorithm " + hpoAlgoImpl + " not supported");

		this.functionVariables = new ArrayList<>(functionVariables);
	}

	public SlaInfo(SlaInfo copy) {
		this.slaClass = copy.getSlaClass();
		this.objectiveFunction = copy.getObjectiveFunction();
		this.direction = copy.getDirection();
		this.hpoAlgoImpl = copy.getHpoAlgoImpl();

		this.functionVariables = new ArrayList<>(copy.getFunctionVariables());

	}

	public String getSlaClass() {
		return slaClass;
	}

	public String getDirection() {
		return direction;
	}

	public String getObjectiveFunction() {
		return objectiveFunction;
	}

	public ArrayList<FunctionVariable> getFunctionVariables() {
		return new ArrayList<>(functionVariables);
	}

	public String getHpoAlgoImpl() {
		return hpoAlgoImpl;
	}

	@Override
	public String toString() {
		return "SlaInfo{" +
				"slaClass='" + slaClass + '\'' +
				", objectiveFunction='" + objectiveFunction + '\'' +
				", direction='" + direction + '\'' +
				", hpoAlgoImpl='" + this.hpoAlgoImpl + '\'' +
				", functionVariables=" + functionVariables +
				'}';
	}
}
