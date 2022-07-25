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

import com.autotune.common.target.kubernetes.model.ContainerConfigData;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.autotune.common.target.kubernetes.params.KubeConstants.*;

/**
 * KubernetesServicesImpl implements functions which are used to
 * communicate with Kubernetes cluster.
 */
public class KubernetesServicesImpl implements KubernetesServices {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesServicesImpl.class);

    public KubernetesServicesImpl() {
    }

    /**
     * getNamespaceList query and return list of Namespace object
     * later it will be processed further into JSON with only required attributes.
     *
     * @return List<V1Namespace>
     */
    @Override
    public List<V1Namespace> getNamespaceList() {
        CoreV1Api api = new CoreV1Api();
        List<V1Namespace> kubernetesNamespaceList = new ArrayList<>();
        V1NamespaceList namespaceList = null;
        try {
            namespaceList = api.listNamespace(null, null, null, null, null, null, null, null, null, null);
        } catch (ApiException e) {
            LOGGER.error(e.getMessage());
        }
        if (namespaceList != null) {
            for (V1Namespace namespace : namespaceList.getItems()) {
                kubernetesNamespaceList.add(namespace);
            }
        }
        return kubernetesNamespaceList;
    }

    /**
     * TODO
     *
     * @param namespace
     * @return
     */
    @Override
    public List<String> getDeploymentNameList(String namespace) {
        return null;
    }

    /**
     * TODO
     *
     * @param namespace
     * @return
     */
    @Override
    public List<String> getPodNameList(String namespace) {
        return null;
    }

    /**
     * gets deployment object using input as namespace and deploymentname
     *
     * @param deploymentDetails
     * @return V1Deployment
     */
    @Override
    public V1Deployment getDeployment(JSONObject deploymentDetails) {
        String namespace = deploymentDetails.getString(NAMESPACE);
        String deploymentName = deploymentDetails.getString(DEPLOYMENT_NAME);
        AppsV1Api api = new AppsV1Api();
        V1Deployment deployment = null;
        try {
            V1DeploymentList v1DeploymentList = api.listNamespacedDeployment(namespace, null, null, null, null, null, null, null, null, null, false);
            for (V1Deployment v1Deployment : v1DeploymentList.getItems()) {
                if (v1Deployment.getMetadata().getName().equalsIgnoreCase(deploymentName)) {
                    deployment = v1Deployment;
                    break;
                }
            }
        } catch (ApiException e) {
            LOGGER.error("{}", e.getCode());
            LOGGER.error(e.getResponseBody());
        }
        return deployment;
    }

    /**
     * sets Requests,Limit and some environments for existing deployment
     *
     * @param deploymentDetails
     * @return
     */
    @Override
    public boolean deployDeployment(JSONObject deploymentDetails) {
        V1Deployment existingDeployment = this.getDeployment(deploymentDetails);
        ContainerConfigData containerConfigData = (ContainerConfigData) deploymentDetails.get(CONTAINER_CONFIG_DATA);
        if (existingDeployment != null) {
            existingDeployment
                    .getSpec()
                    .getTemplate()
                    .getSpec()
                    .getContainers()
                    .forEach(
                            (deployedAppContainer) -> {
                                V1ResourceRequirements resourceRequirements = deployedAppContainer.getResources();
                                resourceRequirements.setRequests(containerConfigData.getRequestPropertiesMap());
                                resourceRequirements.setLimits(containerConfigData.getLimitPropertiesMap());
                                deployedAppContainer.setResources(resourceRequirements);
                                deployedAppContainer.setEnv(containerConfigData.getEnvList());
                            }
                    );
        } else {
            //Todo create new deployment
        }
        return false;
    }

}
