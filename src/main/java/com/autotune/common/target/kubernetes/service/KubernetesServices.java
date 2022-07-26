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

package com.autotune.common.target.kubernetes.service;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.json.JSONObject;

import java.util.List;

/**
 * List of methode that gets implemented which are used to communicate
 * with Kubernetes cluster.
 */
public interface KubernetesServices {
    List<Namespace> getNamespaces();
    List<Pod> getPodsBy(String namespace);
    Deployment getDeploymentBy(String namespace,String deploymentName);
    Deployment getDeploymentBy(JSONObject deploymentDetails);
    boolean deployDeployment(JSONObject deploymentDetails);
}
