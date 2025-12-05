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
