package com.autotune.analyzer.serviceObjects;

import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.utils.AutotuneConstants;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ListRecommendationsSO extends BaseSO{
    @SerializedName(AutotuneConstants.JSONKeys.CLUSTER_NAME)
    private String clusterName;

    @SerializedName(AutotuneConstants.JSONKeys.KUBERNETES_OBJECTS)
    private List<K8sObject> kubernetesObjects;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public List<K8sObject> getKubernetesObjects() {
        return kubernetesObjects;
    }

    public void setKubernetesObjects(List<K8sObject> kubernetesObjects) {
        this.kubernetesObjects = kubernetesObjects;
    }
}
