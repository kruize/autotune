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

import com.autotune.utils.KruizeConstants;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Simulating the K8sObject class for the create experiment API
 */
public class KubernetesAPIObject {
    private String type;
    private String name;
    private String namespace;
    // Optional field to determine if the experiment type is 'container' or 'namespace'.
    // TODO: Update to make this field mandatory in the future.
    private String experiment_type;
    @SerializedName(KruizeConstants.JSONKeys.CONTAINERS)
    private List<ContainerAPIObject> containerAPIObjects;
    @SerializedName(KruizeConstants.JSONKeys.NAMESPACES)
    private NamespaceAPIObject namespaceAPIObject;

    public KubernetesAPIObject(String name, String type, String namespace) {
        this.name = name;
        this.type = type;
        this.namespace = namespace;
    }

    public KubernetesAPIObject() {

    }

    // getters and setters

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

    @JsonProperty(KruizeConstants.JSONKeys.CONTAINERS)
    public List<ContainerAPIObject> getContainerAPIObjects() {
        return containerAPIObjects;
    }

    public void setContainerAPIObjects(List<ContainerAPIObject> containerAPIObjects) {
        this.containerAPIObjects = containerAPIObjects;
    }

    @JsonProperty(KruizeConstants.JSONKeys.NAMESPACES)
    public NamespaceAPIObject getNamespaceAPIObjects() {
        return namespaceAPIObject;
    }

    @Override
    public String toString() {
        return "KubernetesObject{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", namespace_info=" + namespaceAPIObject +
                ", containers=" + containerAPIObjects +
                '}';
    }

    public String getExperimentType() {
        return experiment_type;
    }

    public void setExperimentType(String experimentType) {
        this.experiment_type = experimentType;
    }

    public NamespaceAPIObject getNamespaceAPIObject() {
        return namespaceAPIObject;
    }

    public void setNamespaceAPIObject(NamespaceAPIObject namespaceAPIObject) {
        this.namespaceAPIObject = namespaceAPIObject;
    }


}
