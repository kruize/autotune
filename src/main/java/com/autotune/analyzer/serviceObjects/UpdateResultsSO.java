package com.autotune.analyzer.serviceObjects;

import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.utils.AutotuneConstants;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.List;

public class UpdateResultsSO extends BaseSO{
    @SerializedName(AutotuneConstants.JSONKeys.START_TIMESTAMP)
    public Timestamp startTimestamp;
    @SerializedName(AutotuneConstants.JSONKeys.END_TIMESTAMP)
    public Timestamp endTimestamp;
    @SerializedName(AutotuneConstants.JSONKeys.KUBERNETES_OBJECTS)
    private List<K8sObject> kubernetesObjects;

    public Timestamp getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(Timestamp startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public Timestamp getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(Timestamp endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public List<K8sObject> getKubernetesObjects() {
        return kubernetesObjects;
    }

    public void setKubernetesObjects(List<K8sObject> kubernetesObjects) {
        this.kubernetesObjects = kubernetesObjects;
    }
}
