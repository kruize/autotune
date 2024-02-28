package com.autotune.common.metadata;

import com.autotune.common.data.result.ContainerData;

import java.util.List;

public class Namespace {
    private String namespace;
    private String type;
    private String workloadName;
    private List<ContainerData> containers;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getWorkloadName() {
        return workloadName;
    }

    public void setWorkloadName(String workloadName) {
        this.workloadName = workloadName;
    }

    public List<ContainerData> getContainers() {
        return containers;
    }

    public void setContainers(List<ContainerData> containers) {
        this.containers = containers;
    }
}
