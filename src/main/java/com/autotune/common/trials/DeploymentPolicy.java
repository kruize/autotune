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
package com.autotune.common.trials;

import com.google.gson.annotations.SerializedName;

/**
 * Experiment Manager deploys application based on Kubernetes Deployment Strategy like
 * . Rolling deployment
 * . Recreate
 * . Ramped slow rollout
 * . Best-effort controlled rollout
 * . Canary deployment   etc.
 * deploymentType is used to mention Kubernetes Deployment Strategy. Example
 *        "deployment_policy": {
 *                  "type": "rollingUpdate"
 *         }
 */
public class DeploymentPolicy {
    @SerializedName("type")
    private final String deploymentType;

    public DeploymentPolicy(String deploymentType) {
        this.deploymentType = deploymentType;
    }

    public String getDeploymentType() {
        return deploymentType;
    }

    @Override
    public String toString() {
        return "DeploymentPolicy{" +
                "deploymentType='" + deploymentType + '\'' +
                '}';
    }
}
