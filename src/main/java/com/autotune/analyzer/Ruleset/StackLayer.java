package com.autotune.analyzer.Ruleset;

import com.google.gson.annotations.SerializedName;

public class StackLayer {

    @SerializedName("name")
    private String name;
    @SerializedName("required")
    private boolean required;

    public StackLayer() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}
