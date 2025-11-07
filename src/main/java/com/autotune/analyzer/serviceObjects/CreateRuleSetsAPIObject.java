package com.autotune.analyzer.serviceObjects;

import com.autotune.analyzer.Ruleset.Metadata;
import com.autotune.analyzer.Ruleset.RuleSets;
import com.google.gson.annotations.SerializedName;

public class CreateRuleSetsAPIObject {

    @SerializedName("apiVersion")
    private String apiVersion;
    @SerializedName("kind")
    private String kind;
    @SerializedName("metadata")
    private Metadata metadata;
    @SerializedName("rulesets")
    private RuleSets rulesets;

    public CreateRuleSetsAPIObject() {}

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public RuleSets getRulesets() {
        return rulesets;
    }

    public void setRulesets(RuleSets rulesets) {
        this.rulesets = rulesets;
    }
}
