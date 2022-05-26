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

import com.autotune.common.experiments.ExperimentTrial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Service class helper used to control and execute Lifecycle of Experiments using trial numbers.
 */
public class ExperimentTrialHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentTrialHandler.class);
    private ExperimentTrial experimentTrial;

    public ExperimentTrialHandler(ExperimentTrial experimentTrial) {
        this.experimentTrial = experimentTrial;
    }

    public ExperimentTrial getExperimentTrial() {
        return experimentTrial;
    }

    public void setExperimentTrial(ExperimentTrial experimentTrial) {
        this.experimentTrial = experimentTrial;
    }

    public ArrayList<String> getTrackers() {
        return this.experimentTrial.getExperimentSettings().getDeploymentSettings().getDeploymentTracking().getTrackers();
    }

    public void startExperimentTrials() {
        this.experimentTrial.getTrialDetails().forEach((tracker, trialDetails) -> {
            trialDetails.getPodContainers().forEach((imageName, podContainer) -> {
                podContainer.getTrialConfigs().forEach((trialNumber, containerConfigData) -> {
                    new DeploymentHandler(trialDetails.getDeploymentNameSpace(), trialDetails.getDeploymentName(), containerConfigData).startDeploying();
                });
            });
        });
    }

}
