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

import com.autotune.common.experiments.ContainerConfigData;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * List of methode that gets implemented which are used to communicate
 * with Kubernetes cluster.
 */
public interface KubernetesServices {
    //get all namespaces
    List<Namespace> getNamespaces();

    //get all service list
    List<Service> getServicelist(String namespace);

    //get all pods
    List<Pod> getPodsBy(String namespace);

    //get all pods using String namespace, String labelKey, String labelValue
    List<Pod> getPodsBy(String namespace, String labelKey, String labelValue);

    //get replicas set
    List<ReplicaSet> getReplicasBy(String namespace, String labelKey, String labelValue);

    //get Deployment object
    Deployment getDeploymentBy(String namespace, String deploymentName);

    //get Deployment object using JSONObject
    Deployment getDeploymentBy(JSONObject deploymentDetails);

    //Restart deployment
    boolean restartDeployment(String namespace, String deploymentName);

    //Replace deployment with new deployment
    boolean replaceDeployment(String namespace, String deploymentName, Deployment newDeployment);

    //Deploy deployment using config
    boolean deployDeployment(String namespace, String deploymentName, ContainerConfigData containerConfigData);

    //Deploy deployment using config
    boolean deployDeployment(JSONObject deploymentDetails);

    //get amendment deployment
    Deployment getAmendedDeployment(String namespace, String deploymentName, Deployment existingDeployment, ContainerConfigData containerConfigData);

    //get events
    Event getEvent(String namespace, String eventName);

    //get environment variable from CRD's
    Map<String, Object> getCRDEnvMap(CustomResourceDefinitionContext crd, String namespace, String kubernetesType);

    //Get kubernetes client
    KubernetesClient getClient();

    //Create Event
    boolean createEvent(String namespace, String eventName, Event newEvent);

    //Replace event
    boolean replaceEvent(String namespace, String eventName, Event newEvent);

    //Close connection with kubernetes
    boolean shutdownClient();

    //Add watcher
    void addWatcher(CustomResourceDefinitionContext crd, Watcher watcher);

    //Watch endpoints
    void watchEndpoints(CustomResourceDefinitionContext crd);
}
