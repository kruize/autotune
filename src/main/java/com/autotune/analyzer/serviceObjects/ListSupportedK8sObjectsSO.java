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
