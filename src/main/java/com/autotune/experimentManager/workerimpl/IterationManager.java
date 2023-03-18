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
package com.autotune.experimentManager.workerimpl;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.TrialDetails;
import com.autotune.common.k8sObjects.KruizeObject;
import com.autotune.common.parallelengine.executor.KruizeExecutor;
import com.autotune.common.parallelengine.worker.KruizeWorker;
import com.autotune.common.parallelengine.worker.CallableFactory;
import com.autotune.experimentManager.data.result.*;
import com.autotune.experimentManager.handler.eminterface.EMHandlerFactory;
import com.autotune.experimentManager.handler.eminterface.EMHandlerInterface;
import com.autotune.experimentManager.handler.util.EMStatusUpdateHandler;
import com.autotune.experimentManager.utils.EMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/**
 * This is worker to execute experiments  in several steps sequentially.
 */
public class IterationManager implements KruizeWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(IterationManager.class);

    public IterationManager() {
    }

    @Override
    public void execute(KruizeObject kruizeObject, Object o, KruizeExecutor kruizeExecutor, ServletContext context) {
        ExperimentTrial experimentTrial = (ExperimentTrial) o;
        if (experimentTrial.getStatus().equals(EMUtil.EMExpStatus.QUEUED)) {
            LOGGER.debug("Experiment name {} started processing", experimentTrial.getExperimentName());
            initWorkflow(experimentTrial);
            kruizeExecutor.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            KruizeWorker theWorker = new CallableFactory().create(kruizeExecutor.getWorker());
                            theWorker.execute(null, experimentTrial, kruizeExecutor, context);
                        }
                    }
            );
        } else {
            findAndSubmitTask(experimentTrial, kruizeExecutor, context);
        }
    }

    /**
     * This function helps to execute workflow steps automatically based on
     * Experiment,Trial,Iteration status
     *
     * @param experimentTrial
     * @param kruizeExecutor
     * @param context
     */
    private void findAndSubmitTask(ExperimentTrial experimentTrial, KruizeExecutor kruizeExecutor, ServletContext context) {
        AtomicBoolean taskSubmitted = new AtomicBoolean(false);
        if (experimentTrial.getStatus().equals(EMUtil.EMExpStatus.IN_PROGRESS)) {
            experimentTrial.getTrialDetails().forEach((trialNum, trialDetails) -> {
                if (taskSubmitted.get()) return;
                if (proceed(trialDetails)) {
                    EMStatusUpdateHandler.updateTrialMetaDataStatus(experimentTrial, trialDetails);
                    trialDetails.getTrialMetaData().getIterations().forEach((iteration, iterationTrialMetaDetails) -> {
                        if (taskSubmitted.get()) return;
                        if (proceed(iterationTrialMetaDetails)) {
                            iterationTrialMetaDetails.getWorkFlow().forEach((stepName, stepMetadata) -> {
                                if (taskSubmitted.get()) return;
                                if (stepMetadata.getStatus().equals(EMUtil.EMExpStatus.QUEUED)) {
                                    String stepClassName = experimentTrial.getExperimentMetaData().getAutoTuneWorkFlow().getIterationWorkflowMap().get(stepName);
                                    EMHandlerInterface theWorker = null;
                                    theWorker = new EMHandlerFactory().create(stepClassName);
                                    if (null != theWorker) {
                                        EMStatusUpdateHandler.updateTrialIterationDataStatus(experimentTrial, trialDetails, iterationTrialMetaDetails);
                                        theWorker.execute(experimentTrial,
                                                trialDetails,
                                                iterationTrialMetaDetails,
                                                stepMetadata,
												kruizeExecutor,
                                                context);
                                    } else {
                                        LOGGER.error("Class : {} implementation not found ", stepClassName);
                                        stepMetadata.setStatus(EMUtil.EMExpStatus.FAILED);
                                    }
                                    taskSubmitted.set(true);
                                    return;
                                }
                            });
                        }
                    });
                    // Call Trial Workflow
                    trialDetails.getTrialMetaData().getTrialWorkflow().forEach((stepName, stepsMetaData) -> {
                        if (taskSubmitted.get()) return;
                        if (stepsMetaData.getStatus().equals(EMUtil.EMExpStatus.QUEUED)) {
                            String stepClassName = experimentTrial.getExperimentMetaData().getAutoTuneWorkFlow().getTrialWorkflowMap().get(stepName);
                            EMHandlerInterface theWorker = null;
                            theWorker = new EMHandlerFactory().create(stepClassName);
                            if (null != theWorker) {
                                theWorker.execute(experimentTrial,
                                        trialDetails,
                                        null,
                                        stepsMetaData,
										kruizeExecutor,
                                        context);
                            } else {
                                LOGGER.error("Class : {} implementation not found ", stepClassName);
                                stepsMetaData.setStatus(EMUtil.EMExpStatus.FAILED);
                            }
                            taskSubmitted.set(true);
                            return;
                        }
                    });
                }
            });
        }
    }

    /*
        initiate Metadata to track Experiment progress and workflow progress.
     */
    private void initWorkflow(ExperimentTrial experimentTrial) {
        try {
            //Update experiment level metadata
            ExperimentMetaData experimentMetaData = experimentTrial.getExperimentMetaData();
            KruizeWorkFlow kruizeWorkFlow = experimentMetaData.getAutoTuneWorkFlow();
            if (null == kruizeWorkFlow) {
                kruizeWorkFlow = new KruizeWorkFlow(experimentTrial.getExperimentSettings().isDo_experiment(),
                        experimentTrial.getExperimentSettings().isDo_monitoring(),
                        experimentTrial.getExperimentSettings().isWait_for_load(),
                        experimentTrial.getTrialResultURL());     //Check Workflow is Experiment or Monitoring Workflow
                experimentMetaData.setAutoTuneWorkFlow(kruizeWorkFlow);
                experimentTrial.setExperimentMetaData(experimentMetaData);
            }

            //update Trial level metadata
            experimentTrial.getTrialDetails().forEach((trailNum, trialDetail) -> {
                TrialMetaData trialMetaData = trialDetail.getTrialMetaData();
                if (trialMetaData.getStatus().equals(EMUtil.EMExpStatus.QUEUED)) {
                    int iterationCount = Integer.parseInt(experimentTrial.getExperimentSettings().getTrialSettings().getTrialIterations());
                    LinkedHashMap<Integer, TrialIterationMetaData> trialIterationMap = new LinkedHashMap<>();
                    LinkedHashMap<Integer, LinkedHashMap<String, StepsMetaData>> iterationWorkflow = new LinkedHashMap<>();
                    IntStream.rangeClosed(1, iterationCount).forEach((iteration) -> {
                        TrialIterationMetaData trialIterationMetaData = new TrialIterationMetaData();
                        LinkedHashMap<String, StepsMetaData> stepsMetaDataLinkedHashMap = new LinkedHashMap<>();
                        experimentTrial.getExperimentMetaData().getAutoTuneWorkFlow().getIterationWorkflowMap().forEach(
                                (workerName, workerNameClass) -> {
                                    StepsMetaData stepsMetaData = new StepsMetaData();
                                    stepsMetaData.setStatus(EMUtil.EMExpStatus.QUEUED);
                                    stepsMetaData.setStepName(workerName);
                                    stepsMetaDataLinkedHashMap.put(
                                            workerName, stepsMetaData
                                    );
                                }
                        );
                        trialIterationMetaData.setWorkFlow(stepsMetaDataLinkedHashMap);
                        trialIterationMetaData.setStatus(EMUtil.EMExpStatus.QUEUED);
                        trialIterationMetaData.setIterationNumber(iteration);
                        trialIterationMap.put(iteration, trialIterationMetaData);
                    });
                    trialMetaData.setIterations(trialIterationMap);
                    LinkedHashMap<String, StepsMetaData> trialWorkflowSteps = new LinkedHashMap<>();
                    experimentTrial.getExperimentMetaData().getAutoTuneWorkFlow().getTrialWorkflowMap().forEach(
                            (workerName, workerNameClass) -> {
                                StepsMetaData stepsMetaData = new StepsMetaData();
                                stepsMetaData.setStatus(EMUtil.EMExpStatus.QUEUED);
                                stepsMetaData.setStepName(workerName);
                                trialWorkflowSteps.put(
                                        workerName, stepsMetaData
                                );
                            }
                    );
                    trialMetaData.setTrialWorkflow(trialWorkflowSteps);
                    trialMetaData.setStatus(EMUtil.EMExpStatus.QUEUED);
                    trialDetail.setTrialMetaData(trialMetaData);
                }
            });
            experimentTrial.setStatus(EMUtil.EMExpStatus.IN_PROGRESS);
            LOGGER.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~Name:{}-Status:{}~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~", experimentTrial.getExperimentName(), EMUtil.EMExpStatus.IN_PROGRESS);
            if (null == experimentTrial.getExperimentMetaData().getBeginTimestamp())
                experimentTrial.getExperimentMetaData().setBeginTimestamp(new Timestamp(System.currentTimeMillis()));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            experimentTrial.setStatus(EMUtil.EMExpStatus.FAILED);
            experimentTrial.getExperimentMetaData().setEndTimestamp(new Timestamp(System.currentTimeMillis()));
        }
    }

    public boolean proceed(TrialDetails trialDetails) {
        return trialDetails.getTrialMetaData().getStatus().equals(EMUtil.EMExpStatus.QUEUED) ||
                trialDetails.getTrialMetaData().getStatus().equals(EMUtil.EMExpStatus.IN_PROGRESS);
    }

    public boolean proceed(TrialIterationMetaData trialIterationMetaData) {
        return trialIterationMetaData.getStatus().equals(EMUtil.EMExpStatus.QUEUED) ||
                trialIterationMetaData.getStatus().equals(EMUtil.EMExpStatus.IN_PROGRESS);
    }

}
