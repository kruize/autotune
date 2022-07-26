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

package com.autotune.common.target.kubernetes.service.impl;

import com.autotune.common.target.common.exception.TargetHandlerConnectException;
import com.autotune.common.target.common.exception.TargetHandlerException;
import com.autotune.common.target.kubernetes.model.ContainerConfigData;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.autotune.common.target.kubernetes.params.KubeConstants.*;

/**
 * KubernetesServicesImpl implements functions which are used to
 * communicate with Kubernetes cluster.
 */
public class KubernetesServicesImpl implements KubernetesServices {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesServicesImpl.class);
    private KubernetesClient kubernetesClient;

    public KubernetesServicesImpl() {
        try {
            this.kubernetesClient = new DefaultKubernetesClient();
        } catch (Exception e) {
            new TargetHandlerConnectException(e, "Default connection to kubernetes failed!");
        }
    }

    /**
     * getNamespaceList query and return list of Namespace object
     * later it will be processed further into JSON with only required attributes.
     *
     * @return List<Namespace>
     */
    @Override
    public List<Namespace> getNamespaces() {
        List<Namespace> namespaceList = null;
        try {
            namespaceList = kubernetesClient.namespaces().list().getItems();
        } catch (Exception e) {
            new TargetHandlerException(e, "getNamespaces failed!");
        }
        return namespaceList;
    }

    /**
     * Return list of pods in specified namespace.
     *
     * @param namespace
     * @return
     */
    public List<Pod> getPodsBy(String namespace) {
        List<Pod> podList = null;
        try {
            podList = kubernetesClient
                    .pods()
                    .inNamespace(namespace)
                    .list()
                    .getItems();
        } catch (Exception e) {
            new TargetHandlerException(e, "getPodsBy failed!");
        }
        return podList;
    }

    /**
     * Get deployment object
     *
     * @param namespace
     * @param deploymentName
     * @return
     */
    @Override
    public Deployment getDeploymentBy(String namespace, String deploymentName) {
        Deployment deployment = null;
        try {
            deployment = kubernetesClient
                    .apps()
                    .deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .get();
        } catch (Exception e) {
            new TargetHandlerException(e, "getDeploymentBy(namespace,deploymentName) failed!");
        }
        return deployment;
    }

    /**
     * Get deployment object using json input
     *
     * @param deploymentDetails
     * @return
     */
    @Override
    public Deployment getDeploymentBy(JSONObject deploymentDetails) {
        Deployment deployment = null;
        try {
            String namespace = deploymentDetails.getString(NAMESPACE);
            String deploymentName = deploymentDetails.getString(DEPLOYMENT_NAME);
            deployment = getDeploymentBy(namespace, deploymentName);
        } catch (Exception e) {
            new TargetHandlerException(e, "getDeploymentBy failed!");
        }
        return deployment;
    }

    /**
     * set Requests,Limit and environments for existing deployment
     *
     * @param deploymentDetails
     * @return
     */
    @Override
    public boolean deployDeployment(JSONObject deploymentDetails) {
        boolean deployed = false;
        try {
            Deployment existingDeployment = getDeploymentBy(deploymentDetails);
            ContainerConfigData containerConfigData = (ContainerConfigData) deploymentDetails.get(CONTAINER_CONFIG_DATA);
            if (existingDeployment != null) {
                existingDeployment
                        .getSpec()
                        .getTemplate()
                        .getSpec()
                        .getContainers()   //TODO : Check for which container config should get applied
                        .forEach(
                                (deployedAppContainer) -> {
                                    ResourceRequirements resourceRequirements = deployedAppContainer.getResources();
                                    resourceRequirements.setRequests(containerConfigData.getRequestPropertiesMap());
                                    resourceRequirements.setLimits(containerConfigData.getLimitPropertiesMap());
                                    deployedAppContainer.setResources(resourceRequirements);
                                    deployedAppContainer.setEnv(containerConfigData.getEnvList());
                                }
                        );
                deployed = true;
            } else {
                //TODO : create new deployment
            }
        } catch (Exception e) {
            new TargetHandlerException(e, "deployDeployment failed!");
        }
        return deployed;
    }

}
