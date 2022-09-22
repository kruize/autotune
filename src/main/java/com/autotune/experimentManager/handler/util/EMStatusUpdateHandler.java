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

package com.autotune.experimentManager.handler.util;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.TrialDetails;
import com.autotune.experimentManager.data.result.TrialIterationMetaData;
import com.autotune.experimentManager.handler.PreValidationHandler;
import com.autotune.experimentManager.utils.EMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Util class helps in marking Experiment,Trial,Cycle,Workflow step status
 * to either InProgress or Complete.
 */
public class EMStatusUpdateHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreValidationHandler.class);

    public static void updateTrialIterationDataStatus(ExperimentTrial experimentTrial, TrialDetails trialDetails, TrialIterationMetaData iterationMetaData) {
        // Check if all Trial's iteration's  steps is Queued if yes set beginTimestamp and update status to Inprogress
        // Check if all Trial's iteration's  steps is Completed or Failed if yes set EndTimestamp and update status to Complete or Failed
        AtomicInteger queuedCount = new AtomicInteger(0);
        AtomicInteger stepsCount = new AtomicInteger(0);
        AtomicInteger completedOrFailedCount = new AtomicInteger(0);
        iterationMetaData.getWorkFlow().forEach((stepName, stepMetadata) -> {
            if (stepMetadata.getStatus().equals(EMUtil.EMExpStatus.QUEUED)) {
                queuedCount.set(queuedCount.get() + 1);
            } else if (stepMetadata.getStatus().equals(EMUtil.EMExpStatus.COMPLETED)) {
                completedOrFailedCount.set(completedOrFailedCount.get() + 1);
            } else if (stepMetadata.getStatus().equals(EMUtil.EMExpStatus.FAILED)) {
                completedOrFailedCount.set(completedOrFailedCount.get() + 1);
            }
        });

        int total = iterationMetaData.getWorkFlow().size();
        if (queuedCount.intValue() == total) {
            iterationMetaData.setBeginTimestamp(new Timestamp(System.currentTimeMillis()));
            iterationMetaData.setStatus(EMUtil.EMExpStatus.IN_PROGRESS);
            LOGGER.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~{}-{}-{}-{}~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~", experimentTrial.getExperimentName(), trialDetails.getTrailID(), iterationMetaData.getIterationNumber(), EMUtil.EMExpStatus.IN_PROGRESS);
        } else if (completedOrFailedCount.intValue() == total) {
            iterationMetaData.setEndTimestamp(new Timestamp(System.currentTimeMillis()));
            iterationMetaData.setStatus(EMUtil.EMExpStatus.COMPLETED);
            LOGGER.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~{}-{}-{}-{}~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~", experimentTrial.getExperimentName(), trialDetails.getTrailID(), iterationMetaData.getIterationNumber(), EMUtil.EMExpStatus.COMPLETED);
        }
    }

    public static void updateTrialMetaDataStatus(ExperimentTrial experimentTrial, TrialDetails trialDetails) {
        AtomicInteger queuedCount = new AtomicInteger(0);
        AtomicInteger completedOrFailedCount = new AtomicInteger(0);
        trialDetails.getTrialMetaData().getIterations().forEach((iterationNum, iterationMetaData) -> {
            if (iterationMetaData.getStatus().equals(EMUtil.EMExpStatus.QUEUED)) {
                queuedCount.set(queuedCount.get() + 1);
            } else if (iterationMetaData.getStatus().equals(EMUtil.EMExpStatus.COMPLETED)) {
                completedOrFailedCount.set(completedOrFailedCount.get() + 1);
            } else if (iterationMetaData.getStatus().equals(EMUtil.EMExpStatus.FAILED)) {
                completedOrFailedCount.set(completedOrFailedCount.get() + 1);
            }
        });

        trialDetails.getTrialMetaData().getTrialWorkflow().forEach((stepName, stepsMetaData) -> {
            if (stepsMetaData.getStatus().equals(EMUtil.EMExpStatus.QUEUED)) queuedCount.set(queuedCount.get() + 1);
            else if (stepsMetaData.getStatus().equals(EMUtil.EMExpStatus.COMPLETED))
                completedOrFailedCount.set(completedOrFailedCount.get() + 1);
            else if (stepsMetaData.getStatus().equals(EMUtil.EMExpStatus.FAILED))
                completedOrFailedCount.set(completedOrFailedCount.get() + 1);
        });
        int totalSteps = trialDetails.getTrialMetaData().getIterations().size() + trialDetails.getTrialMetaData().getTrialWorkflow().size();
        if (queuedCount.intValue() == totalSteps) {
            trialDetails.getTrialMetaData().setBeginTimestamp(new Timestamp(System.currentTimeMillis()));
            trialDetails.getTrialMetaData().setStatus(EMUtil.EMExpStatus.IN_PROGRESS);
            LOGGER.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~{}-{}-{}~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~", experimentTrial.getExperimentName(), trialDetails.getTrailID(), EMUtil.EMExpStatus.IN_PROGRESS);
        } else if (completedOrFailedCount.intValue() == totalSteps) {
            trialDetails.getTrialMetaData().setEndTimestamp(new Timestamp(System.currentTimeMillis()));
            trialDetails.getTrialMetaData().setStatus(EMUtil.EMExpStatus.COMPLETED);
            LOGGER.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~{}-{}-{}~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~", experimentTrial.getExperimentName(), trialDetails.getTrailID(), EMUtil.EMExpStatus.COMPLETED);
        }
    }

    public static void updateExperimentTrialMetaDataStatus(ExperimentTrial experimentTrial) {
        AtomicInteger completedOrFailedCount = new AtomicInteger(0);
        experimentTrial.getTrialDetails().forEach((trialNum, trialDetails) -> {
            if (trialDetails.getTrialMetaData().getStatus().equals(EMUtil.EMExpStatus.COMPLETED) ||
                    trialDetails.getTrialMetaData().getStatus().equals(EMUtil.EMExpStatus.FAILED)) {
                completedOrFailedCount.set(completedOrFailedCount.get() + 1);
            }
        });
        if (completedOrFailedCount.intValue() == experimentTrial.getTrialDetails().size()) {
            experimentTrial.getExperimentMetaData().setEndTimestamp(new Timestamp(System.currentTimeMillis()));
            experimentTrial.setStatus(EMUtil.EMExpStatus.COMPLETED);
            LOGGER.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~{}-{}~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~", experimentTrial.getExperimentName(), EMUtil.EMExpStatus.COMPLETED);
        }
    }
}
