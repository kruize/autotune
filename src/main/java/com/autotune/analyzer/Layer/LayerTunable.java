package com.autotune.analyzer.Layer;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LayerTunable {

    private LayerTunableMetadata metadata;
    @SerializedName("depends_on")
    private LayerDependsOn dependsOn;
    private List<Calculation> calculations;

    public LayerTunable(LayerTunableMetadata metadata, LayerDependsOn dependsOn, List<Calculation> calculations) {
        this.metadata = metadata;
        this.dependsOn = dependsOn;
        this.calculations = calculations;
    }

    public LayerTunableMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(LayerTunableMetadata metadata) {
        this.metadata = metadata;
    }

    public LayerDependsOn getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(LayerDependsOn dependsOn) {
        this.dependsOn = dependsOn;
    }

    public List<Calculation> getCalculations() {
        return calculations;
    }

    public void setCalculations(List<Calculation> calculations) {
        this.calculations = calculations;
    }
}
