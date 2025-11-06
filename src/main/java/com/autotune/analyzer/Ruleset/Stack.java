package com.autotune.analyzer.Ruleset;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Stack {

    @SerializedName("layers")
    private List<StackLayer> layers;

    public Stack() {}

    public List<StackLayer> getLayers() {
        return layers;
    }

    public void setLayers(List<StackLayer> layers) {
        this.layers = layers;
    }
}
