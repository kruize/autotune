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

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.TrialDetails;
import com.autotune.common.parallelengine.executor.AutotuneExecutor;
import com.autotune.common.parallelengine.worker.AutotuneWorker;
import com.autotune.common.parallelengine.worker.CallableFactory;
import com.autotune.experimentManager.data.result.CycleMetaData;
import com.autotune.experimentManager.data.result.StepsMetaData;
import com.autotune.experimentManager.handler.eminterface.EMHandlerInterface;
import com.autotune.experimentManager.handler.util.EMStatusUpdateHandler;
import com.autotune.experimentManager.utils.EMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.sql.Timestamp;

/**
 * Validate input config before executing trials experiments.
 */
public class PreValidationHandler implements EMHandlerInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreValidationHandler.class);
    @Override
    public void execute(ExperimentTrial experimentTrial,TrialDetails trialDetails,
                        CycleMetaData cycleMetaData,
                        StepsMetaData stepsMeatData,
                        AutotuneExecutor autotuneExecutor,ServletContext context) {
        try {
            String cycleName = (cycleMetaData == null) ? "" : cycleMetaData.getCycleName();
            LOGGER.debug("ExperimentName: \"{}\" - TrialNo: {} - Cycle: {} - Iteration: {} - StepName: {}",
                    experimentTrial.getExperimentName(),
                    trialDetails.getTrailID(),
                    cycleName,
                    stepsMeatData.getIterationNumber(),
                    stepsMeatData.getStepName()
            );
            stepsMeatData.setStatus(EMUtil.EMExpStatus.IN_PROGRESS);
            stepsMeatData.setBeginTimestamp(new Timestamp(System.currentTimeMillis()));
            /**
             * Implement Prevalidation Logic
             */
            stepsMeatData.setEndTimestamp(new Timestamp(System.currentTimeMillis()));
            stepsMeatData.setStatus(EMUtil.EMExpStatus.COMPLETED);
            EMStatusUpdateHandler.updateCycleMetaDataStatus(experimentTrial, trialDetails,cycleMetaData);
            EMStatusUpdateHandler.updateTrialMetaDataStatus(experimentTrial,trialDetails);
            EMStatusUpdateHandler.updateExperimentTrialMetaDataStatus(experimentTrial);
            autotuneExecutor.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            AutotuneWorker theWorker = new CallableFactory().create(autotuneExecutor.getWorker());
                            theWorker.execute(experimentTrial, autotuneExecutor, context);
                        }
                    }
            );
        }catch (Exception e){
            trialDetails.getTrialMetaData().setStatus(EMUtil.EMExpStatus.FAILED);
            e.printStackTrace();
            LOGGER.error("Failed to execute PreValidate step for Experiment name :{} due to: {}"
                    ,experimentTrial.getExperimentName(),e.getMessage());
        }
    }
}
