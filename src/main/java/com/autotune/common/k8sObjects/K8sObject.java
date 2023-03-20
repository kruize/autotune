package com.autotune.common.k8sObjects;

import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class K8sObject {
    private String type; // TODO: Change to ENUM
    private String name;
    private String namespace;
    @SerializedName(KruizeConstants.JSONKeys.CONTAINERS)
    private List<ContainerObject> containerObjects;

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

    public List<ContainerObject> getContainerObjects() {
        return containerObjects;
    }

    public void setContainerObjects(List<ContainerObject> containerObjects) {
        this.containerObjects = containerObjects;
    }

    @Override
    public String toString() {
        return "K8sObject{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", containerObjects=" + containerObjects +
                '}';
    }
}
