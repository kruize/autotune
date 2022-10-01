/*******************************************************************************
 * Copyright (c)  2022 Red Hat, IBM Corporation and others.
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

package com.autotune.experimentManager.handler;

import com.autotune.common.experiments.ContainerConfigData;
import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.TrialDetails;
import com.autotune.common.parallelengine.executor.AutotuneExecutor;
import com.autotune.common.parallelengine.worker.AutotuneWorker;
import com.autotune.common.parallelengine.worker.CallableFactory;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.utils.ExponentialBackOff;
import com.autotune.experimentManager.data.result.StepsMetaData;
import com.autotune.experimentManager.data.result.TrialIterationMetaData;
import com.autotune.experimentManager.handler.eminterface.EMHandlerInterface;
import com.autotune.experimentManager.handler.util.EMStatusUpdateHandler;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.sql.Timestamp;

/**
 * Deploy application using specified config
 */
public class DeploymentHandler implements EMHandlerInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentHandler.class);
    private boolean alreadyDeployedJustRestart = false;
    private String nameSpace;
    private String deploymentName;
    private ContainerConfigData containerConfigData;
    private KubernetesServices kubernetesServices;

    @Override
    public void execute(ExperimentTrial experimentTrial, TrialDetails trialDetails, TrialIterationMetaData iterationMetaData, StepsMetaData stepsMeatData, AutotuneExecutor autotuneExecutor, ServletContext context) {
        LOGGER.debug("ExperimentName: \"{}\" - TrialNo: {} - Iteration: {} - StepName: {}",
                experimentTrial.getExperimentName(),
                trialDetails.getTrialNumber(),
                iterationMetaData.getIterationNumber(),
                stepsMeatData.getStepName()
        );
        try {
            stepsMeatData.setStatus(EMUtil.EMExpStatus.IN_PROGRESS);
            stepsMeatData.setBeginTimestamp(new Timestamp(System.currentTimeMillis()));
            /**
             * Implement DeploymentHandler Logic
             */
            this.kubernetesServices = (KubernetesServices) context.getAttribute(EMConstants.EMKeys.EM_KUBERNETES_SERVICE);
            this.alreadyDeployedJustRestart = iterationMetaData.isAlreadyDeployedJustRestart();
            this.nameSpace = experimentTrial.getResourceDetails().getNamespace();
            this.deploymentName = experimentTrial.getResourceDetails().getDeploymentName();
            this.containerConfigData = trialDetails.getConfigData();
            initiateDeploy(iterationMetaData);
            ExponentialBackOff exponentialBackOffForDeployment = ExponentialBackOff.Builder.newInstance().build();
            boolean deploymentReady = kubernetesServices.isDeploymentReady(nameSpace,deploymentName,exponentialBackOffForDeployment);
            if (deploymentReady) {
                ExponentialBackOff exponentialBackOffForPods = ExponentialBackOff.Builder.newInstance()
                        .setMaxElapsedTimeMillis(2 * 60 * 1000)
                        .setInitialIntervalMillis(30 * 1000)                        // TODO : this value should be driven from input json OR Capture application time UP From Dry run.
                        .setRandomizationFactor(0.5)
                        .build();
                boolean podsRunning = kubernetesServices.arePodsRunning(nameSpace, deploymentName, exponentialBackOffForPods);
                stepsMeatData.setEndTimestamp(new Timestamp(System.currentTimeMillis()));
                if (podsRunning) {
                    stepsMeatData.setStatus(EMUtil.EMExpStatus.COMPLETED);
                } else {
                    stepsMeatData.setStatus(EMUtil.EMExpStatus.FAILED);
                }
            }else{
                stepsMeatData.setEndTimestamp(new Timestamp(System.currentTimeMillis()));
                stepsMeatData.setStatus(EMUtil.EMExpStatus.FAILED);
            }
            EMStatusUpdateHandler.updateTrialIterationDataStatus(experimentTrial, trialDetails, iterationMetaData);
            EMStatusUpdateHandler.updateTrialMetaDataStatus(experimentTrial, trialDetails);
            EMStatusUpdateHandler.updateExperimentTrialMetaDataStatus(experimentTrial);
            //Submit to next task
            autotuneExecutor.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            AutotuneWorker theWorker = new CallableFactory().create(autotuneExecutor.getWorker());
                            theWorker.execute(experimentTrial, autotuneExecutor, context);
                        }
                    }
            );
        } catch (Exception e) {
            trialDetails.getTrialMetaData().setStatus(EMUtil.EMExpStatus.FAILED);
            e.printStackTrace();
            LOGGER.error("Failed to execute DeploymentHandler ExperimentName: \"{}\" - TrialNo: {} - Iteration: {} - StepName: {} -- due to {}",
                    experimentTrial.getExperimentName(),
                    trialDetails.getTrialNumber(),
                    iterationMetaData.getIterationNumber(),
                    stepsMeatData.getStepName(),
                    e.getMessage()
            );
        }
    }

    public void initiateDeploy(TrialIterationMetaData iterationMetaData) {
        LOGGER.debug("START DEPLOYING");
        try {
            if (!this.alreadyDeployedJustRestart) {
                //Check here if deployment type is rolling-update   .withName(this.deploymentName)
                this.kubernetesServices.startDeploying(this.nameSpace, this.deploymentName, this.containerConfigData);
                iterationMetaData.setAlreadyDeployedJustRestart(true);
            } else {
                this.kubernetesServices.restartDeployment(this.nameSpace, this.deploymentName);
            }
        } catch (Exception e) {
            LOGGER.error(e.toString());
            LOGGER.error(e.getMessage(), e.getStackTrace().toString());
            e.printStackTrace();
        }
        LOGGER.debug("END DEPLOYING");
    }

    public EMUtil.DeploymentReadinessStatus isDeploymentReady(ExperimentTrial experimentTrial, TrialDetails trialDetails) {
        boolean running = false;
        try {
            for (int j = 0; j < EMConstants.StandardDefaults.BackOffThresholds.DEPLOYMENT_READINESS_THRESHOLD; j++) {
                running = this.kubernetesServices.isDeploymentReady(this.nameSpace, this.deploymentName);
                if (true == running) {
                    return EMUtil.DeploymentReadinessStatus.READY;
                }
                LOGGER.debug("Deployment for experiment - \"{}\" with trial number - \"{}\"  is not ready after {} checks, Will be checking after {} secs",
                        experimentTrial.getExperimentName(),
                        trialDetails.getTrialNumber(),
                        j + 1,
                        EMUtil.timeToSleep(j, EMUtil.ThresholdIntervalType.LINEAR));
                // Will be replaced by a exponential looper mechanism
                Thread.sleep(EMUtil.timeToSleep(j, EMUtil.ThresholdIntervalType.LINEAR) * 1000);
            }
        } catch (Exception e) {
            LOGGER.error(e.toString());
            LOGGER.error(e.getMessage(), e.getStackTrace().toString());
            e.printStackTrace();
        }
        if (true == running) {
            return EMUtil.DeploymentReadinessStatus.READY;
        }
        return EMUtil.DeploymentReadinessStatus.NOT_READY;
    }
}
