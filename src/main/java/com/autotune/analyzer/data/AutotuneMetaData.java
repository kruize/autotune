package com.autotune.analyzer.model;

public class AutotuneMetaData {
    private String name;
    private String namespace;

    public AutotuneMetaData(String name, String namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
