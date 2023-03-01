package com.autotune.common.k8sObjects;

import java.util.List;

public class K8sObject {
    private String type; // TODO: Change to ENUM
    private String name;
    private String namespace;
    private List<ContainerObject> containers;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public List<ContainerObject> getContainers() {
        return containers;
    }

    public void setContainers(List<ContainerObject> containers) {
        this.containers = containers;
    }
}
