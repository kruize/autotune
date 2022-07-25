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
package com.autotune.common.target.kubernetes;

import com.autotune.common.target.common.exception.TargetHandlerConnectException;
import com.autotune.common.target.common.exception.TargetHandlerException;
import com.autotune.common.target.common.main.TargetHandler;
import com.autotune.common.target.kubernetes.params.KubernetesConfig;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * KubernetesTargetHandler implements TargetHandler and facilitates to communicate with Kubernetes java client api by implementing
 * Connect
 * DeployApplication
 * Collect metrics
 */
public class KubernetesTargetHandler implements TargetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesTargetHandler.class);
    private final KubernetesConfig config;
    private final KubernetesServices kubernetesServices;

    public KubernetesTargetHandler() {
        this.config = new KubernetesConfig();
        this.kubernetesServices = new KubernetesServicesImpl();
    }

    /**
     * Connect() function is used to connect to kubernetes Java client.
     * at present this scripts support only connection within cluster.
     *
     * @throws TargetHandlerConnectException
     */
    @Override
    public void connect() throws TargetHandlerConnectException {
        if (this.config.isFromCluster()) {
            connectFromCluster();
        }
    }

    /**
     * @param deploymentDetails : JSONObject having details like
     *                          namespace
     *                          DeploymentName
     *                          ContainerConfigData -> Request , Limit , Env
     * @throws TargetHandlerException
     */
    @Override
    public void deployApplication(JSONObject deploymentDetails) throws TargetHandlerException {
        this.kubernetesServices.deployDeployment(deploymentDetails);
    }

    /**
     * TODO
     *
     * @param results
     * @return
     * @throws TargetHandlerException
     */

    @Override
    public List collectMetrics(List results) throws TargetHandlerException {
        return null;
    }

    /**
     * connectFromCluster helps to connect to Kubernetes API when this program is running inside cluster.
     *
     * @throws TargetHandlerConnectException
     */
    private void connectFromCluster() throws TargetHandlerConnectException {
        ApiClient apiClient = null;
        try {
            apiClient = Config.fromCluster();
            System.out.println("Successfully connected to kubernetes.");
        } catch (Exception e) {
            throw new TargetHandlerConnectException(e, "Error with Kubernetes connection due to : " + e.getMessage());
        }
        Configuration.setDefaultApiClient(apiClient);
    }

}
