/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.analyzer.serviceObjects;

import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.List;

public class UpdateResultsAPIObject extends BaseSO{
    @SerializedName(KruizeConstants.JSONKeys.START_TIMESTAMP)
    public Timestamp startTimestamp;
    @SerializedName(KruizeConstants.JSONKeys.END_TIMESTAMP)
    public Timestamp endTimestamp;
    @SerializedName(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS)
    private List<KubernetesObject> kubernetesObjects;

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

    public List<KubernetesObject> getKubernetesObjects() {
        return kubernetesObjects;
    }

    public void setKubernetesObjects(List<KubernetesObject> kubernetesObjects) {
        this.kubernetesObjects = kubernetesObjects;
    }

    @Override
    public String toString() {
        return "UpdateResultsAPIObject{" +
                "startTimestamp=" + startTimestamp +
                ", endTimestamp=" + endTimestamp +
                ", kubernetesObjects=" + kubernetesObjects +
                '}';
    }
}
