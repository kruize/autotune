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
import com.autotune.common.utils.ExponentialBackOff;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

import java.util.List;
import java.util.Map;


/**
 * List of methods that gets implemented which are used to communicate
 * with Kubernetes cluster.
 */
public interface KubernetesServices {
    //get all namespaces. Used by GUI
    List<Namespace> getNamespaces();

    //get all service list. Used by GUI and Other modules to fetch endpoints for services like prometheus etc.
    List<Service> getServicelist(String namespace);

    //get all pods.
    List<Pod> getPodsBy(String namespace);

    //get all pod by name
    Pod getPodsBy(String namespace, String name);

    //get all pods using String namespace, String labelKey, String labelValue
    List<Pod> getPodsBy(String namespace, String labelKey, String labelValue);

    //get replicas set. Get map of pods matching the autotune object using the labels.
    List<ReplicaSet> getReplicasBy(String namespace, String labelKey, String labelValue);

    //get Deployment object
    Deployment getDeploymentBy(String namespace, String deploymentName);

    //get list of Deployments object based on  labelKey, labelValue
    List<Deployment> getDeploymentsBy(String namespace, String deploymentName, String labelKey, String labelValue);

    //Restart deployment. Used by EM to restart deployment during warmup/measurements cycle.
    boolean restartDeployment(String namespace, String deploymentName);

    //Deploy deployment using config
    boolean startDeploying(String namespace, String deploymentName, ContainerConfigData containerConfigData);

    //get events.
    Event getEvent(String namespace, String eventName);

    //get environment variable from CRD's. Parse AutotuneConfig JSON and create matching AutotuneConfig object
    Map<String, Object> getCRDEnvMap(CustomResourceDefinitionContext crd, String namespace, String kubernetesType);

    //Create Event. Event logging class that allows creating or replacing events with custom messages and reasons
    boolean createEvent(String namespace, String eventName, Event newEvent);

    //Replace event. Event logging class that allows creating or replacing events with custom messages and reasons
    boolean replaceEvent(String namespace, String eventName, Event newEvent);

    //Close connection with kubernetes
    boolean shutdownClient();

    //Add watcher. Used to register watch endpoints.
    void addWatcher(CustomResourceDefinitionContext crd, Watcher watcher);

    //Watch endpoints. Used to trigger events based on some action to kubernetes resources.
    void watchEndpoints(CustomResourceDefinitionContext crd);

    //Check if deployment is ready
    public boolean isDeploymentReady(String namespace, String deploymentName);

    //Check if deployment is ready using Exponential backoff
    public boolean isDeploymentReady(String namespace, String deploymentName, ExponentialBackOff exponentialBackOff);

    //Check if pods are running
    public boolean arePodsRunning(String namespace, String deploymentName);

    //Check if pods are running using Exponential backoff
    public boolean arePodsRunning(String namespace, String deploymentName, ExponentialBackOff exponentialBackOff);
}
