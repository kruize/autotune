package com.autotune.analyzer.Ruleset;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class LayerRules {

    @SerializedName("tunables")
    private Map<String, List<TunableRule>> tunables;

    public LayerRules() {}

    public Map<String, List<TunableRule>> getTunables() {
        return tunables;
    }

    public void setTunables(Map<String, List<TunableRule>> tunables) {
        this.tunables = tunables;
    }
}
