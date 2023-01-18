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
package com.autotune.common.k8sObjects;

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.utils.AutotuneSupportedTypes;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Holds information about the slo key in the autotune object yaml
 * <p>
 * Example:
 * slo:
 * objective_function: "transaction_response_time"
 * slo_class: "response_time"
 * direction: "minimize"
 * hpo_algo_impl: "optuna_tpe"
 * function_variables:
 * - name: "transaction_response_time"
 * query: "application_org_acme_microprofile_metrics_PrimeNumberChecker_checksTimer_mean_seconds"
 * datasource: "prometheus"
 * value_type: "double"
 * mode: "show"
 * selector:
 * matchLabel: "app.kubernetes.io/name"
 * matchLabelValue: "petclinic-deployment"
 */
public final class SloInfo {
    private final String sloClass;
    @SerializedName("objective_function")
    private final ObjectiveFunction objectiveFunction;
    private final String direction;
    private final String hpoAlgoImpl;
    @SerializedName("function_variables")
    private final ArrayList<Metric> metrics;

    public SloInfo(String sloClass,
                   ObjectiveFunction objectiveFunction,
                   String direction,
                   String hpoAlgoImpl,
                   ArrayList<Metric> metrics) throws InvalidValueException {
        if (AutotuneSupportedTypes.SLO_CLASSES_SUPPORTED.contains(sloClass))
            this.sloClass = sloClass;
        else
            throw new InvalidValueException("slo_class: " + sloClass + " not supported");
        this.objectiveFunction = objectiveFunction;

        if (AutotuneSupportedTypes.DIRECTIONS_SUPPORTED.contains(direction))
            this.direction = direction;
        else
            throw new InvalidValueException("Invalid direction for autotune kind");

        if (AutotuneSupportedTypes.HPO_ALGOS_SUPPORTED.contains(hpoAlgoImpl))
            this.hpoAlgoImpl = hpoAlgoImpl;
        else
            throw new InvalidValueException("Hyperparameter Optimization Algorithm " + hpoAlgoImpl + " not supported");

        this.metrics = new ArrayList<>(metrics);
    }

    public SloInfo(SloInfo copy) {
        this.sloClass = copy.getSloClass();
        this.objectiveFunction = copy.getObjectiveFunction();
        this.direction = copy.getDirection();
        this.hpoAlgoImpl = copy.getHpoAlgoImpl();

        this.metrics = new ArrayList<>(copy.getFunctionVariables());

    }

    public String getSloClass() {
        return sloClass;
    }

    public String getDirection() {
        return direction;
    }

    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    public ArrayList<Metric> getFunctionVariables() {
        return new ArrayList<>(metrics);
    }

    public String getHpoAlgoImpl() {
        return hpoAlgoImpl;
    }

    @Override
    public String toString() {
        return "SloInfo{" +
                "sloClass='" + sloClass + '\'' +
                ", objectiveFunction='" + objectiveFunction + '\'' +
                ", direction='" + direction + '\'' +
                ", hpoAlgoImpl='" + this.hpoAlgoImpl + '\'' +
                ", functionVariables=" + metrics +
                '}';
    }
}
