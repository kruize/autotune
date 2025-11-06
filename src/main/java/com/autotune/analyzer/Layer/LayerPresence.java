package com.autotune.analyzer.Layer;


import java.util.List;

public class LayerPresence {
    private String presence;

    private List<LayerDetector> detectors;

    public LayerPresence(String presence, List<LayerDetector> detectors) {
        this.presence = presence;
        this.detectors = detectors;
    }

    public String getPresence() {
        return presence;
    }

    public void setPresence(String presence) {
        this.presence = presence;
    }

    public List<LayerDetector> getDetectors() {
        return detectors;
    }

    public void setDetectors(List<LayerDetector> detectors) {
        this.detectors = detectors;
    }
}
