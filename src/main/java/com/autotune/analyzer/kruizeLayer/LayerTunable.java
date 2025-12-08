/*******************************************************************************
 * Copyright (c) 2025 Red Hat, IBM Corporation and others.
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

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LayerTunable {

    private String name;
    private String description;
    @SerializedName("value_type")
    private String valueType;
    private List<String> choices;
    @SerializedName("upper_bound")
    private String upperBound;
    @SerializedName("lower_bound")
    private String lowerBound;
    private Double step;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public List<String> getChoices() {
        return choices;
    }

    public void setChoices(List<String> choices) {
        this.choices = choices;
    }

    public String getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(String upperBound) {
        this.upperBound = upperBound;
    }

    public String getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(String lowerBound) {
        this.lowerBound = lowerBound;
    }

    public Double getStep() {
        return step;
    }

    public void setStep(Double step) {
        this.step = step;
    }

    @Override
    public String toString() {
        return "LayerTunable{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", valueType='" + valueType + '\'' +
                ", choices=" + choices +
                ", upperBound='" + upperBound + '\'' +
                ", lowerBound='" + lowerBound + '\'' +
                ", step=" + step +
                '}';
    }
}
