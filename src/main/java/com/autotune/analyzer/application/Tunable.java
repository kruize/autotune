/*******************************************************************************
 * Copyright (c) 2020, 2022 Red Hat, IBM Corporation and others.
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
import com.autotune.utils.AutotuneSupportedTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.autotune.utils.AnalyzerConstants.AutotuneConfigConstants.*;
import static com.autotune.utils.AnalyzerErrorConstants.AutotuneConfigErrors.*;

/**
 * Contains the tunable to optimize, along with its upper & lower bounds or categorical choices, value type
 * and the list of slo_class (throughput, response_time, right_size, etc.) for which it is applicable.
 * <p>
 * Example:
 * - name: <Tunable>
 * value_type: double
 * upper_bound: 4.0
 * lower_bound: 2.0
 * step: 0.01
 * queries:
 * datasource:
 * - name: 'prometheus'
 * query: '(container_cpu_usage_seconds_total{$CONTAINER_LABEL$!="POD", $POD_LABEL$="$POD$"}[1m])'
 * slo_class:
 * - response_time
 * - throughput
 */
public class Tunable {
    private String name;
    private String fullName;
    private double step;
    private String valueType;
    private Double upperBoundValue;
    private Double lowerBoundValue;
    private String boundUnits;
    private String description;
    private Map<String, String> queries;
    private String layerName;
    private String stackName;

    /*
    TODO Think about bounds for other valueTypes
    String bound; //[1.5-3.5], [true, false]
    */

    public ArrayList<String> sloClassList;
    public List<String> choices;

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

    /**
     * invoked when tunables contain categorical choices
     *
     * @param name
     * @param valueType
     * @param queries
     * @param sloClassList
     * @param layerName
     * @param choices
     */
    public Tunable(String name,
                   String valueType,
                   Map<String, String> queries,
                   ArrayList<String> sloClassList,
                   String layerName,
                   List<String> choices) {
        setCommonTunableParameters(queries, name, valueType, sloClassList, layerName);
        this.choices = Objects.requireNonNull(choices, INVALID_TUNABLE_CHOICE);
    }

    /**
     * invoked when tunables contain upper & lower bounds values
     *
     * @param name
     * @param valueType
     * @param queries
     * @param sloClassList
     * @param layerName
     * @param step
     * @param upperBound
     * @param lowerBound
     * @throws InvalidBoundsException
     */
    public Tunable(String name,
                   String valueType,
                   Map<String, String> queries,
                   ArrayList<String> sloClassList,
                   String layerName,
                   double step,
                   String upperBound,
                   String lowerBound
    ) throws InvalidBoundsException {
        setCommonTunableParameters(queries, name, valueType, sloClassList, layerName);
        this.step = Objects.requireNonNull(step, ZERO_STEP);
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
        this.fullName = copy.fullName;
        this.step = copy.step;
        this.upperBoundValue = copy.upperBoundValue;
        this.lowerBoundValue = copy.lowerBoundValue;
        this.boundUnits = copy.boundUnits;
        this.valueType = copy.valueType;
        this.description = copy.description;
        this.queries = copy.queries;
        this.sloClassList = copy.sloClassList;
        this.layerName = copy.layerName;
        this.stackName = copy.stackName;
    }

    /**
     * @param queries
     * @param name
     * @param valueType
     * @param sloClassList
     * @param layerName
     */
    private void setCommonTunableParameters(Map<String, String> queries,
                                            String name,
                                            String valueType,
                                            ArrayList<String> sloClassList,
                                            String layerName) {
        this.queries = queries;
        this.name = Objects.requireNonNull(name, TUNABLE_NAME_EMPTY);
        this.valueType = Objects.requireNonNull(valueType, VALUE_TYPE_NULL);
        this.sloClassList = Objects.requireNonNull(sloClassList, INVALID_SLO_CLASS);
        if (AutotuneSupportedTypes.LAYERS_SUPPORTED.contains(layerName))
            this.layerName = layerName;
        else
            this.layerName = "generic";
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
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

    public String getBoundUnits() {
        return boundUnits;
    }

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

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public String getLayerName() {
        return layerName;
    }

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    public List<String> getChoices() {
        return choices;
    }

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
