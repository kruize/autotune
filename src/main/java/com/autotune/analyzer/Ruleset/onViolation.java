package com.autotune.analyzer.Ruleset;

import com.google.gson.annotations.SerializedName;

public class onViolation {

    @SerializedName("fix")
    private String fix;

    public onViolation(String fix) {}
    public String getFix() {
        return fix;
    }
    public void setFix(String fix) {
        this.fix = fix;
    }

}
