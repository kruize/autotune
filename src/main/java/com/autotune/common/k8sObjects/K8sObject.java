package com.autotune.common.k8sObjects;

import com.autotune.common.data.result.ContainerData;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.List;

public class K8sObject {
    private String type; // TODO: Change to ENUM
    private String name;
    private String namespace;
    @SerializedName(KruizeConstants.JSONKeys.CONTAINERS)
    private List<ContainerData> containerDataList;

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

    public List<ContainerData> getContainerDataList() {
        return containerDataList;
    }

    public void setContainerDataList(List<ContainerData> containerDataList) {
        this.containerDataList = containerDataList;
    }

    @Override
    public String toString() {
        return "K8sObject{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", containerDataList=" + containerDataList +
                '}';
    }
}
