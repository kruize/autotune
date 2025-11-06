package com.autotune.analyzer.Ruleset;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class Rules {
    @SerializedName("layers")
    private Map<String, LayerRules> layers;

    public Rules() {}

    public Map<String, LayerRules> getLayers() {
        return layers;
    }
    public void setLayers(Map<String, LayerRules> layers) {
        this.layers = layers;
    }

}

