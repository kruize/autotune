package com.autotune.analyzer.Layer;


import java.util.List;

public class LayerDependsOn {

    private List<String> tunables;

    private List<String> metrics; // Optional

    public LayerDependsOn(List<String> tunables, List<String> metrics) {
        this.tunables = tunables;
        this.metrics = metrics;
    }

    public List<String> getTunables() {
        return tunables;
    }

    public void setTunables(List<String> tunables) {
        this.tunables = tunables;
    }

    public List<String> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<String> metrics) {
        this.metrics = metrics;
    }

    @Override
    public String toString() {
        return "LayerDependsOn{" +
                "tunables=" + tunables +
                ", metrics=" + metrics +
                '}';
    }
}
