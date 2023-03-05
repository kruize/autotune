package com.autotune.analyzer.serviceObjects;

import com.autotune.utils.AutotuneConstants;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ListSupportedK8sObjectsSO {
    @SerializedName(AutotuneConstants.JSONKeys.KUBERNETES_OBJECTS)
    private List<String> kubernetesObjects;

    public ListSupportedK8sObjectsSO() {
        kubernetesObjects = new ArrayList<String>();
    }

    public void addSupportedK8sObject(String k8sObject) {
        kubernetesObjects.add(k8sObject);
    }
}
