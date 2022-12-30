/*******************************************************************************
 * Copyright (c)  2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.k8sObjects;

import java.util.List;

/**
 * Experiment result storage for Pod and Container metrics.
 */

public class DeploymentResultData {
    private String deployment_name;
    private String namespace;
    private List<ContainerResultData> containers;
    private List<PodResultData> pod_metrics;

    public String getDeployment_name() {
        return deployment_name;
    }

    public void setDeployment_name(String deployment_name) {
        this.deployment_name = deployment_name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<ContainerResultData> getContainers() {
        return containers;
    }

    public void setContainers(List<ContainerResultData> containers) {
        this.containers = containers;
    }

    public List<PodResultData> getPod_metrics() {
        return pod_metrics;
    }

    public void setPod_metrics(List<PodResultData> pod_metrics) {
        this.pod_metrics = pod_metrics;
    }

    @Override
    public String toString() {
        return "DeploymentResultData{" +
                "deployment_name='" + deployment_name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", containers=" + containers +
                ", pod_metrics=" + pod_metrics +
                '}';
    }
}
