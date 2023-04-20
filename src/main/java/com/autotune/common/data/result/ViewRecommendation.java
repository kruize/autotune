/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.data.result;

import java.util.HashMap;

/**
 * ListRecommendation API use this object to show recommendation
 */
public class ViewRecommendation {
    private String experiment_name;
    private String namespace;
    private String deployment_name;
    private HashMap<String, ContainerData> containers;

    public ViewRecommendation(String experiment_name, String namespace, String deployment_name, HashMap<String, ContainerData> containers) {
        this.experiment_name = experiment_name;
        this.namespace = namespace;
        this.deployment_name = deployment_name;
        this.containers = containers;
    }

    public String getExperiment_name() {
        return experiment_name;
    }

    public void setExperiment_name(String experiment_name) {
        this.experiment_name = experiment_name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getDeployment_name() {
        return deployment_name;
    }

    public void setDeployment_name(String deployment_name) {
        this.deployment_name = deployment_name;
    }

    public HashMap<String, ContainerData> getContainers() {
        return containers;
    }

    public void setContainers(HashMap<String, ContainerData> containers) {
        this.containers = containers;
    }
}
