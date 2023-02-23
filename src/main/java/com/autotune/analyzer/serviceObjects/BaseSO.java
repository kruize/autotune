package com.autotune.analyzer.serviceObjects;

import com.google.gson.annotations.SerializedName;

public abstract class BaseSO {
    @SerializedName("version")
    private String apiVersion;
    @SerializedName("experiment_name")
    private String experimentName;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }
}
