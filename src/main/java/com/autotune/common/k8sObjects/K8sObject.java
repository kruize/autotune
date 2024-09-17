package com.autotune.common.k8sObjects;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.NamespaceData;
import com.autotune.utils.KruizeConstants;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class K8sObject {
    private String type; // TODO: Change to ENUM
    private String name;
    private String namespace;
    private String experiment_type;
    @SerializedName(KruizeConstants.JSONKeys.CONTAINERS)
    private HashMap<String, ContainerData> containerDataMap;
    @SerializedName(KruizeConstants.JSONKeys.NAMESPACES)
    private NamespaceData namespaceData;

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

    public String getExperimentType() {
        return experiment_type;
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

    public void setExperimentType(String experiment_type) {
        this.experiment_type = experiment_type;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @JsonProperty(KruizeConstants.JSONKeys.CONTAINERS)
    public HashMap<String, ContainerData> getContainerDataMap() {
        return containerDataMap;
    }

    public void setContainerDataMap(HashMap<String, ContainerData> containerDataMap) {
        this.containerDataMap = containerDataMap;
    }

    @JsonProperty(KruizeConstants.JSONKeys.NAMESPACES)
    public NamespaceData getNamespaceData() {
        return namespaceData;
    }

    public void setNamespaceData(NamespaceData namespaceData) {
        this.namespaceData = namespaceData;
    }

    @Override
    public String toString() {
        return "K8sObject{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", containerDataMap=" + containerDataMap +
                ", namespaceData=" + namespaceData +
                '}';
    }
}
