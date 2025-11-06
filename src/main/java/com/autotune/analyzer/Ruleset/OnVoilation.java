package com.autotune.analyzer.Ruleset;

import com.google.gson.annotations.SerializedName;

public class OnVoilation {

    @SerializedName("fix")
    private String fix;

    public OnVoilation(String fix) {}
    public String getFix() {
        return fix;
    }
    public void setFix(String fix) {
        this.fix = fix;
    }

}
