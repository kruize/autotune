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
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * KubernetesTargetHandler implements TargetHandler and facilitates to communicate with Kubernetes java client api by implementing
 * DeployApplication
 * Collect metrics
 */
public class KubernetesTargetHandler implements TargetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesTargetHandler.class);

    private final KubernetesServices kubernetesServices;

    public KubernetesTargetHandler() {
        this.kubernetesServices = new KubernetesServicesImpl();
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



}
