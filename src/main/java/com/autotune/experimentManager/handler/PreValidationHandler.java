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

import com.autotune.common.trials.ExperimentTrial;
import com.autotune.common.trials.TrialDetails;
import com.autotune.common.parallelengine.executor.KruizeExecutor;
import com.autotune.common.parallelengine.worker.KruizeWorker;
import com.autotune.common.parallelengine.worker.CallableFactory;
import com.autotune.common.utils.CommonUtils;
import com.autotune.common.validators.MetricsValidator;
import com.autotune.common.validators.Validator;
import com.autotune.experimentManager.data.result.StepsMetaData;
import com.autotune.experimentManager.data.result.TrialIterationMetaData;
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
    public void execute(ExperimentTrial experimentTrial, TrialDetails trialDetails,
						TrialIterationMetaData iterationMetaData,
						StepsMetaData stepsMeatData,
						KruizeExecutor kruizeExecutor, ServletContext context) {
        try {
            LOGGER.debug("ExperimentName: \"{}\" - TrialNo: {} - Iteration: {} - StepName: {}",
                    experimentTrial.getExperimentName(),
                    trialDetails.getTrialNumber(),
                    iterationMetaData.getIterationNumber(),
                    stepsMeatData.getStepName()
            );
            stepsMeatData.setStatus(EMUtil.EMExpStatus.IN_PROGRESS);
            stepsMeatData.setBeginTimestamp(new Timestamp(System.currentTimeMillis()));
            /**
             * Implement Prevalidation Logic
             *
             * Adding the input JSON check for query validity
             */
            MetricsValidator metricsValidator = Validator.getMetricsValidator();
            CommonUtils.QueryValidity validMetrics = metricsValidator.validateTrialMetrics(experimentTrial);
            if (CommonUtils.QueryValidity.VALID != validMetrics) {
                /**
                 * Need to stop the experiment.
                 */
                LOGGER.error("Metrics are invalid, exiting the experiment gracefully");
            }
            stepsMeatData.setEndTimestamp(new Timestamp(System.currentTimeMillis()));
            stepsMeatData.setStatus(EMUtil.EMExpStatus.COMPLETED);
            EMStatusUpdateHandler.updateTrialIterationDataStatus(experimentTrial, trialDetails, iterationMetaData);
            EMStatusUpdateHandler.updateTrialMetaDataStatus(experimentTrial, trialDetails);
            EMStatusUpdateHandler.updateExperimentTrialMetaDataStatus(experimentTrial);
            kruizeExecutor.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            KruizeWorker theWorker = new CallableFactory().create(kruizeExecutor.getWorker());
                            theWorker.execute(null, experimentTrial, kruizeExecutor, context);
                        }
                    }
            );
        } catch (Exception e) {
            trialDetails.getTrialMetaData().setStatus(EMUtil.EMExpStatus.FAILED);
            e.printStackTrace();
            LOGGER.error("Failed to execute PreValidationHandler ExperimentName: \"{}\" - TrialNo: {} - Iteration: {} - StepName: {} -- {}",
                    experimentTrial.getExperimentName(),
                    trialDetails.getTrialNumber(),
                    iterationMetaData.getIterationNumber(),
                    stepsMeatData.getStepName(),
                    e.getMessage()
            );
        }
    }
}
