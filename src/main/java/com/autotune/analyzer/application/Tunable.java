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
    import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Contains the tunable to optimize, along with its upper & lower bounds or categorical choices, and value type.
 * <p>
 * Example:
 * - name: <Tunable>
 *   value_type: double
 *   upper_bound: 4.0
 *   lower_bound: 2.0
 *   step: 0.01
 * <p>
 * Or for categorical:
 * - name: <Tunable>
 *   value_type: categorical
 *   choices:
 *     - option1
 *     - option2
 */
public class Tunable {
    private String name;
    private String description;

    @SerializedName("value_type")
    private String valueType;

    @SerializedName("upper_bound")
    private Double upperBoundValue;

    @SerializedName("lower_bound")
    private Double lowerBoundValue;

    private double step;

    public List<String> choices;

    // No-arg constructor for YAML deserialization
    public Tunable() {
    }

    // Constructor for bounded tunables
    public Tunable(String name, String valueType, double step, Double upperBoundValue, Double lowerBoundValue)
            throws InvalidBoundsException {
        this.name = name;
        this.valueType = valueType;
        this.step = step;
        this.upperBoundValue = upperBoundValue;
        this.lowerBoundValue = lowerBoundValue;
        validateBounds();
    }

    // Constructor for categorical tunables
    public Tunable(String name, String valueType, List<String> choices) {
        this.name = name;
        this.valueType = valueType;
        this.choices = choices;
    }

    private void validateBounds() throws InvalidBoundsException {
        if (upperBoundValue == null || lowerBoundValue == null) {
            return; // No bounds to validate
        }

        /*
         * Validation rules:
         * - step must be strictly positive (> 0)
         * - Bounds cannot be negative
         * - upperBound has to be greater than lowerBound
         * - step has to be lesser than or equal to the difference between the two bounds
         */
        if (step <= 0) {
            throw new InvalidBoundsException("ERROR: Tunable: " + name +
                    " has invalid step; step must be > 0, got: " + step);
        }

        if (upperBoundValue < 0 ||
            lowerBoundValue < 0 ||
            lowerBoundValue >= upperBoundValue ||
            step > (upperBoundValue - lowerBoundValue)) {
            throw new InvalidBoundsException("ERROR: Tunable: " + name +
                    " has invalid bounds; upperBound: " + upperBoundValue +
                    " lowerBound: " + lowerBoundValue +
                    " step: " + step);
        }
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getValueType() {
        return valueType;
    }

    public Double getUpperBoundValue() {
        return upperBoundValue;
    }

    public Double getLowerBoundValue() {
        return lowerBoundValue;
    }

    public double getStep() {
        return step;
    }

    public List<String> getChoices() {
        return choices;
    }

    // Setters for YAML deserialization
    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public void setUpperBoundValue(Double upperBoundValue) {
        this.upperBoundValue = upperBoundValue;
    }

    public void setLowerBoundValue(Double lowerBoundValue) {
        this.lowerBoundValue = lowerBoundValue;
    }

    public void setStep(double step) {
        this.step = step;
    }

    public void setChoices(List<String> choices) {
        this.choices = choices;
    }

    @Override
    public String toString() {
        return "Tunable{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", valueType='" + valueType + '\'' +
                ", upperBound=" + upperBoundValue +
                ", lowerBound=" + lowerBoundValue +
                ", step=" + step +
                ", choices=" + choices +
                '}';
    }
}
