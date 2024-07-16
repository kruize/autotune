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
package com.autotune.analyzer.kruizeObject;

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.common.data.metrics.Metric;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Holds information about the slo key in the autotune object yaml
 * <p>
 * Example:
 * slo:
 * objective_function:
 *   function_type: expression
 *   expression: "transaction_response_time"
 * slo_class: "response_time"
 * direction: "minimize"
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
    @SerializedName("function_variables")
    private final ArrayList<Metric> metrics;

    public SloInfo(String sloClass,
                   ObjectiveFunction objectiveFunction,
                   String direction,
                   ArrayList<Metric> metrics) throws InvalidValueException {
        this.sloClass = sloClass;
        this.objectiveFunction = objectiveFunction;
        this.direction = direction;
        this.metrics = new ArrayList<>(metrics);
    }

    public SloInfo(SloInfo copy) {
        this.sloClass = copy.getSloClass();
        this.objectiveFunction = copy.getObjectiveFunction();
        this.direction = copy.getDirection();
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

    public ArrayList<Metric> getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        return "SloInfo{" +
                "sloClass='" + sloClass + '\'' +
                ", objectiveFunction='" + objectiveFunction + '\'' +
                ", direction='" + direction + '\'' +
                ", functionVariables=" + metrics +
                '}';
    }
}
