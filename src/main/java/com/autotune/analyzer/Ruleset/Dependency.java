package com.autotune.analyzer.Ruleset;

import com.google.gson.annotations.SerializedName;
import java.util.List;


public class Dependency {

    @SerializedName("primary")
    private String primary;
    @SerializedName("dependants")
    private List<String> dependants;

    public Dependency() {}

    public  String getPrimary() {
        return primary;
    }
    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public List<String> getDependants() {
        return dependants;
    }

    public void setDependants(List<String> dependants) {
        this.dependants = dependants;
    }
}
