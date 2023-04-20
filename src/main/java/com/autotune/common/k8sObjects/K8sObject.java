package com.autotune.common.k8sObjects;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.result.ContainerData;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class K8sObject {
    private String type; // TODO: Change to ENUM
    private String name;
    private String namespace;
    @SerializedName(KruizeConstants.JSONKeys.CONTAINERS)
    private HashMap<String, ContainerData> containerDataMap;
    public K8sObject(String name, String type, String namespace) {
        this.name = name;
        this.type = type;
        this.namespace = namespace;
    }
    public K8sObject() {
    }

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

    public HashMap<String, ContainerData> getContainerDataMap() {
        return containerDataMap;
    }

    public void setContainerDataMap(HashMap<String, ContainerData> containerDataMap) {
        this.containerDataMap = containerDataMap;
    }

    @Override
    public String toString() {
        return "K8sObject{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", containerDataMap=" + containerDataMap +
                '}';
    }
}
