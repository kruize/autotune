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
import com.autotune.common.parallelengine.executor.AutotuneExecutor;
import com.autotune.common.parallelengine.worker.AutotuneWorker;
import com.autotune.common.parallelengine.worker.CallableFactory;
import com.autotune.experimentManager.data.result.*;
import com.autotune.experimentManager.handler.LoadValidationHandler;
import com.autotune.experimentManager.handler.eminterface.EMHandlerFactory;
import com.autotune.experimentManager.handler.eminterface.EMHandlerInterface;
import com.autotune.experimentManager.handler.util.EMStatusUpdateHandler;
import com.autotune.experimentManager.utils.EMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/**
 * This is worker to execute experiments  in several steps sequentially.
 */
public class IterationManager implements AutotuneWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(IterationManager.class);

    public IterationManager() {
    }

    @Override
    public void execute(Object o, AutotuneExecutor autotuneExecutor, ServletContext context) {
        ExperimentTrial experimentTrial = (ExperimentTrial) o;
        if (experimentTrial.getStatus().equals(EMUtil.EMExpStatus.QUEUED)) {
            LOGGER.debug("Experiment name {} started processing", experimentTrial.getExperimentName());
            initWorkflow(experimentTrial);
            autotuneExecutor.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            AutotuneWorker theWorker = new CallableFactory().create(autotuneExecutor.getWorker());
                            theWorker.execute(experimentTrial, autotuneExecutor, context);
                        }
                    }
            );
        } else {
            LOGGER.debug("Experiment name {} status is {}", experimentTrial.getExperimentName(),experimentTrial.getStatus());
            findAndSubmitTask(experimentTrial,autotuneExecutor,context);
        }
    }

    /**
     * THis function finds step to execute automatically based on
     * Experiment,Trial etc status
     * @param experimentTrial
     * @param autotuneExecutor
     * @param context
     */
    private void findAndSubmitTask(ExperimentTrial experimentTrial, AutotuneExecutor autotuneExecutor, ServletContext context) {
        AtomicBoolean taskSubmitted = new AtomicBoolean(false);
        if (experimentTrial.getStatus().equals(EMUtil.EMExpStatus.IN_PROGRESS)) {
            experimentTrial.getTrialDetails().forEach((trialNum, trialDetails) -> {
                if (taskSubmitted.get()) return;
                LOGGER.debug("Experiment name's {} trial no {} status is {}", experimentTrial.getExperimentName(), trialNum, trialDetails.getTrialMetaData().getStatus());
                EMStatusUpdateHandler.updateTrialMetaDataStatus(experimentTrial,trialDetails);
                if (trialDetails.getTrialMetaData().getStatus().equals(EMUtil.EMExpStatus.QUEUED) ||
                        trialDetails.getTrialMetaData().getStatus().equals(EMUtil.EMExpStatus.IN_PROGRESS)) {
                    trialDetails.getTrialMetaData().getCycles().forEach((cycleName, cycleMetaData) -> {
                        if (taskSubmitted.get()) return;
                        LOGGER.debug("Experiment name's {} trial no {} Cycle {} status is {}", experimentTrial.getExperimentName(), trialNum, cycleName, cycleMetaData.getStatus());
                        if (cycleMetaData.getStatus().equals(EMUtil.EMExpStatus.QUEUED) ||
                                cycleMetaData.getStatus().equals(EMUtil.EMExpStatus.IN_PROGRESS)) {
                            cycleMetaData.getIterationWorkflow().forEach((iterationNum, workflowDetail) -> {
                                if (taskSubmitted.get()) return;
                                LOGGER.debug("Experiment name's {} trial no {} Cycle {} Iteration num is {}", experimentTrial.getExperimentName(), trialNum, cycleName, iterationNum);
                                workflowDetail.forEach((stepName, stepsMetaData) -> {
                                    if (taskSubmitted.get()) return;
                                    LOGGER.debug("Experiment name's {} trial no {} Cycle {} Iteration num is {} workflow name {} status is {}", experimentTrial.getExperimentName(), trialNum, cycleName, iterationNum, stepName, stepsMetaData.getStatus());
                                    if (stepsMetaData.getStatus().equals(EMUtil.EMExpStatus.QUEUED)) {
                                        String stepClassName = experimentTrial.getExperimentMetaData().getAutoTuneWorkFlow().getIterationWorkflowMap().get(stepName);
                                        EMHandlerInterface theWorker = null;
                                        theWorker = new EMHandlerFactory().create(stepClassName);
                                        if (null != theWorker) {
                                            EMStatusUpdateHandler.updateCycleMetaDataStatus(cycleMetaData);
                                            theWorker.execute(experimentTrial,
                                                    trialDetails,
                                                    cycleMetaData,
                                                    stepsMetaData,
                                                    autotuneExecutor,
                                                    context);
                                        } else {
                                            LOGGER.error("Class : {} implementation not found ", stepClassName);
                                            stepsMetaData.setStatus(EMUtil.EMExpStatus.FAILED);
                                        }
                                        taskSubmitted.set(true);
                                        return;
                                    }
                                });
                            });
                        }
                    });
                    // Call Trial Workflow
                    trialDetails.getTrialMetaData().getTrialWorkflow().forEach((stepName,stepsMetaData)->{
                        if (taskSubmitted.get()) return;
                        LOGGER.debug("Experiment name's {} trial no {} Trial workflow name {} status is {}", experimentTrial.getExperimentName(), trialNum, stepName, stepsMetaData.getStatus());
                        if (stepsMetaData.getStatus().equals(EMUtil.EMExpStatus.QUEUED)) {
                            String stepClassName = experimentTrial.getExperimentMetaData().getAutoTuneWorkFlow().getTrialWorkflowMap().get(stepName);
                            EMHandlerInterface theWorker = null;
                            theWorker = new EMHandlerFactory().create(stepClassName);
                            if (null != theWorker) {
                                theWorker.execute(experimentTrial,
                                        trialDetails,
                                        null,
                                        stepsMetaData,
                                        autotuneExecutor,
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


    private void initWorkflow(ExperimentTrial experimentTrial) {
        try {
            //Update experiment level metadata
            ExperimentMetaData experimentMetaData = experimentTrial.getExperimentMetaData();
            AutoTuneWorkFlow autoTuneWorkFlow = experimentMetaData.getAutoTuneWorkFlow();
            if (null == autoTuneWorkFlow) {
                autoTuneWorkFlow = new AutoTuneWorkFlow(experimentTrial.getExperimentSettings().isDo_experiments(),
                        experimentTrial.getExperimentSettings().isDo_monitoring(),
                        experimentTrial.getExperimentSettings().isWait_for_load(),
                        experimentTrial.getTrialResultURL());     //Check Workflow is Experiment or Monitoring Workflow
                experimentMetaData.setAutoTuneWorkFlow(autoTuneWorkFlow);
                experimentTrial.setExperimentMetaData(experimentMetaData);
            }

            //update Trial level metadata
            experimentTrial.getTrialDetails().forEach((trailNum, trialDetail) -> {
                TrialMetaData trialMetaData = trialDetail.getTrialMetaData();
                if (trialMetaData.getStatus().equals(EMUtil.EMExpStatus.QUEUED)) {
                    LinkedHashMap<String, CycleMetaData> cycleMetaDataLinkedHashMap = new LinkedHashMap<>();
                    String warmupCycles = experimentTrial.getExperimentSettings().getTrialSettings().getTrialWarmupCycles();
                    String measurementCycles = experimentTrial.getExperimentSettings().getTrialSettings().getTrialMeasurementCycles();
                    HashMap<String, Integer> cycles = new HashMap<>();
                    if (null != warmupCycles) {
                        if (Integer.parseInt(warmupCycles) > 0) {
                            cycles.put("WarmupCycles", Integer.parseInt(warmupCycles));
                        }
                    }
                    if (null != measurementCycles) {
                        if (Integer.parseInt(measurementCycles) > 0) {
                            cycles.put("MeasurementCycles", Integer.parseInt(measurementCycles));
                        }
                    }
                    cycles.forEach((cycleName, count) -> {
                        CycleMetaData cycleMetaData = new CycleMetaData();
                        cycleMetaData.setStatus(EMUtil.EMExpStatus.QUEUED);
                        LinkedHashMap<Integer, LinkedHashMap<String, StepsMetaData>> cycleIterationMap = new LinkedHashMap<>();
                        IntStream.rangeClosed(1, count).forEach(
                                i -> {
                                    LinkedHashMap<String, StepsMetaData> stepsMetaDataLinkedHashMap = new LinkedHashMap<>();
                                    experimentTrial.getExperimentMetaData().getAutoTuneWorkFlow().getIterationWorkflowMap().forEach(
                                            (workerName, workerNameClass) -> {
                                                StepsMetaData stepsMetaData = new StepsMetaData();
                                                stepsMetaData.setStatus(EMUtil.EMExpStatus.QUEUED);
                                                stepsMetaDataLinkedHashMap.put(
                                                        workerName, stepsMetaData
                                                );
                                            }
                                    );
                                    cycleIterationMap.put(i, stepsMetaDataLinkedHashMap);
                                }
                        );
                        cycleMetaData.setIterationWorkflow(cycleIterationMap);
                        cycleMetaDataLinkedHashMap.put(cycleName, cycleMetaData);
                    });
                    trialMetaData.setCycles(cycleMetaDataLinkedHashMap);
                    LinkedHashMap<String, StepsMetaData> trialWorkflowSteps = new LinkedHashMap<>();
                    experimentTrial.getExperimentMetaData().getAutoTuneWorkFlow().getTrialWorkflowMap().forEach(
                            (workerName, workerNameClass) -> {
                                StepsMetaData stepsMetaData = new StepsMetaData();
                                stepsMetaData.setStatus(EMUtil.EMExpStatus.QUEUED);
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
            experimentTrial.getExperimentMetaData().setBeginTimestamp(new Timestamp(System.currentTimeMillis()));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            experimentTrial.setStatus(EMUtil.EMExpStatus.FAILED);
            experimentTrial.getExperimentMetaData().setEndTimestamp(new Timestamp(System.currentTimeMillis()));
        }
    }


}
