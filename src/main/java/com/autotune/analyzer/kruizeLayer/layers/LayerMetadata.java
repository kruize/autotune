package com.autotune.analyzer.kruizeLayer.layers;


public class LayerMetadata {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "LayerMetadata{" +
                "name='" + name + '\'' +
                '}';
    }
}