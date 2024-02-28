package com.autotune.common.metadata;

import java.util.Map;

public class Cluster {
    private String clusterName;
    private Namespace defaultNamespace;
    private Map<String, Namespace> namespaces;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public Namespace getDefaultNamespace() {
        return defaultNamespace;
    }

    public void setDefaultNamespace(Namespace defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    public Map<String, Namespace> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(Map<String, Namespace> namespaces) {
        this.namespaces = namespaces;
    }
}
