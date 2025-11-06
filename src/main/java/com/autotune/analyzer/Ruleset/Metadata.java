package com.autotune.analyzer.Ruleset;

import com.google.gson.annotations.SerializedName;

public class Metadata {
    @SerializedName("name")
    private String name;

    public  Metadata() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
