package com.autotune.analyzer.Ruleset;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RuleSets {
    @SerializedName("stack")
    private Stack stack;
    @SerializedName("dependencies")
    private List<Dependency> dependencies;
    @SerializedName("rules")
    private Rules rules;

    public RuleSets() {}

    public Stack getStack() {
        return stack;
    }
    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public Rules getRules() {
        return rules;
    }

    public void setRules(Rules rules) {
        this.rules = rules;
    }
}
