package com.autotune.analyzer.Ruleset;

import com.google.gson.annotations.SerializedName;

public class TunableRule {

    @SerializedName("name")
    private String name;
    @SerializedName("phase")
    private String phase;
    @SerializedName("expr")
    private String expr;
    @SerializedName("on_violation")
    private OnVoilation onViolation;
    @SerializedName("message")
    private String message;

    public TunableRule() {}
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPhase() {
        return phase;
    }
    public void setPhase(String phase) {
        this.phase = phase;
    }
    public String getExpr() {
        return expr;
    }
    public void setExpr(String expr) {
        this.expr = expr;
    }
    public String getMessage(String message) {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
