package com.autotune.analyzer.kruizeLayer.layers;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Main class representing the Kruize Layer structure.
 */
public final class Layer {

    private String apiVersion;
    private String kind;
    private LayerMetadata metadata;
    private String name;
    @SerializedName("layer_name")
    private String layerName;
    @SerializedName("layer_level")
    private Integer layerLevel;
    private String details;
    @SerializedName("layer_presence")
    private LayerPresence layerPresence;
    private Map<String, LayerTunable> tunables;


    public Layer(String apiVersion, String kind, LayerMetadata metadata, LayerPresence layerPresence, Map<String, LayerTunable> tunables) {
        this.apiVersion = apiVersion;
        this.kind = kind;
        this.metadata = metadata;
        this.layerPresence = layerPresence;
        this.tunables = tunables;
    }

    public Layer() {
    }

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

    public LayerMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(LayerMetadata metadata) {
        this.metadata = metadata;
    }

    public LayerPresence getLayerPresence() {
        return layerPresence;
    }

    public void setLayerPresence(LayerPresence layerPresence) {
        this.layerPresence = layerPresence;
    }

    public Map<String, LayerTunable> getTunables() {
        return tunables;
    }

    public void setTunables(Map<String, LayerTunable> tunables) {
        this.tunables = tunables;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public Integer getLayerLevel() {
        return layerLevel;
    }

    public void setLayerLevel(Integer layerLevel) {
        this.layerLevel = layerLevel;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "Layer{" +
                "apiVersion='" + apiVersion + '\'' +
                ", kind='" + kind + '\'' +
                ", metadata=" + metadata +
                ", name='" + name + '\'' +
                ", layerName='" + layerName + '\'' +
                ", layerLevel=" + layerLevel +
                ", details='" + details + '\'' +
                ", layerPresence=" + layerPresence +
                ", tunables=" + tunables +
                '}';
    }
}