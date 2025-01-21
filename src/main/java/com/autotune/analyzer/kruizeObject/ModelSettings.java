package com.autotune.analyzer.kruizeObject;

import java.util.List;

public class ModelSettings {

    private List<String> models;

    public ModelSettings() {}

    public List<String> getModels() {
        return models;
    }

    public void setModels(List<String> models) {
        this.models = models;
    }

    @Override
    public String toString() {
        return "ModelSettings{" +
                "models=" + models +
                '}';
    }

}
