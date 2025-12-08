package com.autotune.analyzer.kruizeLayer;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LayerPresence {
    private String presence;

    @SerializedName("label")
    private List<LayerDetector> labels;

    private List<LayerDetector> queries;

    public LayerPresence() {}

    public String getPresence() {
        return presence;
    }

    public void setPresence(String presence) {
        this.presence = presence;
    }

    public List<LayerDetector> getLabels() {
        return labels;
    }

    public void setLabels(List<LayerDetector> labels) {
        this.labels = labels;
    }

    public List<LayerDetector> getQueries() {
        return queries;
    }

    public void setQueries(List<LayerDetector> queries) {
        this.queries = queries;
    }

    /**
     * Returns the active list of detectors regardless of whether
     * they came from 'label' or 'queries'.
     */
    public List<LayerDetector> getActiveDetectors() {
        if (labels != null && !labels.isEmpty()) {
            return labels;
        }
        if (queries != null && !queries.isEmpty()) {
            return queries;
        }
        return new java.util.ArrayList<>(); // Return empty list to prevent NullPointerExceptions
    }

    @Override
    public String toString() {
        return "LayerPresence{" +
                "presence='" + presence + '\'' +
                ", label=" + labels +
                ", queries=" + queries +
                '}';
    }
}