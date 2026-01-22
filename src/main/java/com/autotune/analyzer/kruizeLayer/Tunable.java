/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.kruizeLayer;

import com.autotune.analyzer.exceptions.InvalidBoundsException;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a tunable parameter that can be optimized by Kruize.
 * Supports two types of tunables:
 * 1. Bounded (numeric) - has upper/lower bounds and a step value
 * 2. Categorical - has a list of discrete choices
 */
public class Tunable {
    private String name;
    private String description;

    @SerializedName("value_type")
    private String valueType;

    @SerializedName("upper_bound")
    private String upperBoundValue;

    @SerializedName("lower_bound")
    private String lowerBoundValue;

    private Double step;

    private List<String> choices;

    /**
     * Default constructor for YAML/JSON deserialization
     */
    public Tunable() {
    }

    /**
     * Constructor for bounded (numeric) tunables
     *
     * @param name Name of the tunable
     * @param valueType Type of value (e.g., "double", "integer")
     * @param step Step size for optimization
     * @param upperBoundValue Upper bound
     * @param lowerBoundValue Lower bound
     * @throws InvalidBoundsException if bounds are invalid
     */
    public Tunable(String name, String valueType, Double step, String upperBoundValue, String lowerBoundValue)
            throws InvalidBoundsException {
        this.name = name;
        this.valueType = valueType;
        this.step = step;
        this.upperBoundValue = upperBoundValue;
        this.lowerBoundValue = lowerBoundValue;
        validateBounds();
    }

    /**
     * Constructor for categorical tunables
     *
     * @param name Name of the tunable
     * @param valueType Type of value (should be "categorical")
     * @param choices List of valid choices
     * @throws IllegalArgumentException if choices is null or empty
     */
    public Tunable(String name, String valueType, List<String> choices) {
        this.name = name;
        this.valueType = valueType;
        this.choices = choices;
        validateCategorical();
    }

    /**
     * Validates bounded tunables after deserialization or setter usage.
     * Should be called explicitly after using setters.
     *
     * @throws InvalidBoundsException if validation fails
     */
    public void validate() throws InvalidBoundsException {
        if (choices != null && !choices.isEmpty()) {
            validateCategorical();
        } else if (upperBoundValue != null || lowerBoundValue != null) {
            validateBounds();
        }
    }

    /**
     * Validates bounds for numeric tunables
     *
     * Validation rules:
     * - step must be strictly positive (> 0)
     * - Bounds cannot be negative
     * - upperBound must be greater than lowerBound
     * - step must be less than or equal to (upperBound - lowerBound)
     */
    private void validateBounds() throws InvalidBoundsException {
        if (upperBoundValue == null || lowerBoundValue == null) {
            throw new InvalidBoundsException("ERROR: Tunable: " + name +
                    " has null bounds; both upperBound and lowerBound must be set");
        }

        // Parse strings to doubles for numeric validation
        double upper, lower;
        try {
            upper = Double.parseDouble(upperBoundValue);
            lower = Double.parseDouble(lowerBoundValue);
        } catch (NumberFormatException e) {
            throw new InvalidBoundsException("ERROR: Tunable: " + name +
                    " has non-numeric bounds: upper=" + upperBoundValue +
                    ", lower=" + lowerBoundValue);
        }

        if (step == null) {
            throw new InvalidBoundsException("ERROR: Tunable: " + name +
                    " has null step; step must be set for bounded tunables");
        }

        if (step <= 0) {
            throw new InvalidBoundsException("ERROR: Tunable: " + name +
                    " has invalid step; step must be > 0, got: " + step);
        }

        if (upper < 0 || lower < 0) {
            throw new InvalidBoundsException("ERROR: Tunable: " + name +
                    " has negative bounds; upperBound: " + upper +
                    " lowerBound: " + lower);
        }

        if (lower >= upper) {
            throw new InvalidBoundsException("ERROR: Tunable: " + name +
                    " has invalid bounds; lowerBound (" + lower +
                    ") must be less than upperBound (" + upper + ")");
        }

        if (step > (upper - lower)) {
            throw new InvalidBoundsException("ERROR: Tunable: " + name +
                    " has invalid step; step (" + step +
                    ") must be <= (upperBound - lowerBound) (" + (upper - lower) + ")");
        }
    }

    /**
     * Validates categorical tunables
     */
    private void validateCategorical() {
        if (choices == null || choices.isEmpty()) {
            throw new IllegalArgumentException("ERROR: Tunable: " + name +
                    " is categorical but has null or empty choices list");
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

    public String getUpperBoundValue() {
        return upperBoundValue;
    }

    public String getLowerBoundValue() {
        return lowerBoundValue;
    }

    public Double getStep() {
        return step;
    }

    public List<String> getChoices() {
        return choices;
    }

    // Setters for YAML/JSON deserialization
    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public void setUpperBoundValue(String upperBoundValue) {
        this.upperBoundValue = upperBoundValue;
    }

    public void setLowerBoundValue(String lowerBoundValue) {
        this.lowerBoundValue = lowerBoundValue;
    }

    public void setStep(Double step) {
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
