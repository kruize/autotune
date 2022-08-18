package com.autotune.common.experiments;

import com.google.gson.annotations.SerializedName;

public class ResourceDetails {
    private String namespace;
    @SerializedName("deployment_name")
    private String deploymentName;

    public ResourceDetails(String namespace, String deploymentName) {
        this.namespace = namespace;
        this.deploymentName = deploymentName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    @Override
    public String toString() {
        return "ResourceDetails{" +
                "namespace='" + namespace + '\'' +
                ", deploymentName='" + deploymentName + '\'' +
                '}';
    }
}
