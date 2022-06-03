/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.experimentManager.core;

import com.autotune.common.experiments.ContainerConfigData;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service class helper used to deploy application after considering Container config details
 */

public class DeploymentHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentHandler.class);
    private final String nameSpace;
    private final String deploymentName;
    private final ContainerConfigData containerConfigData;
    private KubernetesClient client;
    private boolean alreadyDeployedJustRestart = false;

    public DeploymentHandler(String nameSpace, String deploymentName, ContainerConfigData containerConfigData) {
        this.nameSpace = nameSpace;
        this.deploymentName = deploymentName;
        this.containerConfigData = containerConfigData;
    }

    public KubernetesClient getKubernetesClient() {
        if (null == client) {
            this.client = new DefaultKubernetesClient();
        }
        return this.client;
    }

    public Deployment getModifiedDeployment() {
        Deployment defaultDeployment = null;
        try {
            defaultDeployment = getKubernetesClient()
                    .apps()
                    .deployments()
                    .inNamespace(this.nameSpace)
                    .withName(this.deploymentName)
                    .get();
            defaultDeployment
                    .getSpec()
                    .getTemplate()
                    .getSpec()
                    .getContainers()
                    .forEach(
                            (deployedAppContainer) -> {
                                ResourceRequirements resourcesRequirement = deployedAppContainer.getResources();
                                resourcesRequirement.setRequests(this.containerConfigData.getRequestPropertiesMap());
                                resourcesRequirement.setLimits(this.containerConfigData.getLimitPropertiesMap());
                                deployedAppContainer.setResources(resourcesRequirement);
                                deployedAppContainer.setEnv(this.containerConfigData.getEnvList());
                            }
                    );
        } catch (Exception e) {
            LOGGER.error(e.toString());
            LOGGER.error(e.getMessage(), e.getStackTrace().toString());
            e.printStackTrace();
        }
        return defaultDeployment;
    }

    public void initiateDeploy() {
        LOGGER.debug("START DEPLOYING");
        try {
            if (!this.alreadyDeployedJustRestart) {
                //Check here if deployment type is rolling-update   .withName(this.deploymentName)
                getKubernetesClient()
                        .apps()
                        .deployments()
                        .inNamespace(this.nameSpace)
                        .withName(this.deploymentName)
                        .createOrReplace(this.getModifiedDeployment());
                this.alreadyDeployedJustRestart = true;
            } else {
                this.rolloutRestartDeployment();
            }
        } catch (Exception e) {
            LOGGER.error(e.toString());
            LOGGER.error(e.getMessage(), e.getStackTrace().toString());
            e.printStackTrace();
        }
        LOGGER.debug("END DEPLOYING");
    }

    public void rolloutRestartDeployment() {
        LOGGER.debug("rolloutRestartDeployment");
        try {
            getKubernetesClient()
                    .apps()
                    .deployments()
                    .inNamespace(this.nameSpace)
                    .withName(this.deploymentName)
                    .rolling()
                    .restart();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e.getStackTrace().toString());
        }
    }
}
