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
import com.autotune.common.k8sObjects.ExperimentResultData;
import com.autotune.common.k8sObjects.Metric;
import com.autotune.common.parallelengine.executor.AutotuneExecutor;
import com.autotune.experimentManager.data.result.StepsMetaData;
import com.autotune.experimentManager.data.result.TrialIterationMetaData;
import com.autotune.experimentManager.handler.eminterface.EMHandlerInterface;
import com.autotune.experimentManager.handler.util.EMStatusUpdateHandler;
import com.autotune.experimentManager.utils.EMUtil;
import com.autotune.utils.HttpUtils;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Post results back to Analyser or specified trialResult URL.
 */
public class PostResultsHandler implements EMHandlerInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostResultsHandler.class);

    @Override
    public void execute(ExperimentTrial experimentTrial, TrialDetails trialDetails, TrialIterationMetaData iterationMetaData, StepsMetaData stepsMeatData, AutotuneExecutor autotuneExecutor, ServletContext context) {
        try {
            LOGGER.debug("ExperimentName: \"{}\" - TrialNo: {} - Iteration: {} - StepName: {}",
                    experimentTrial.getExperimentName(),
                    trialDetails.getTrialNumber(),
                    (null != iterationMetaData) ? iterationMetaData.getIterationNumber() : null,
                    stepsMeatData.getStepName()
            );
            stepsMeatData.setStatus(EMUtil.EMExpStatus.IN_PROGRESS);
            stepsMeatData.setBeginTimestamp(new Timestamp(System.currentTimeMillis()));
            /**
             * Implement PostResultsHandler Logic
             */
            HashMap<String, Metric> podMetricsMap = experimentTrial.getPodMetricsHashMap();
            for (Map.Entry<String, Metric> podMetricEntry : podMetricsMap.entrySet()) {
                Metric podMetric = podMetricEntry.getValue();
                if (null != podMetric.getEmMetricResult() && Float.MIN_VALUE != podMetric.getEmMetricResult().getEmMetricGenericResults().getMean()) {
                    LOGGER.info("Mean result for {} is {} ", podMetric.getName(), podMetric.getEmMetricResult().getEmMetricGenericResults().getMean());
                }
            }
            HashMap<String, HashMap<String, Metric>> containersMap = experimentTrial.getContainerMetricsHashMap();
            for (Map.Entry<String, HashMap<String, Metric>> containerMapEntry : containersMap.entrySet()) {
                for (Map.Entry<String, Metric> containerMetricEntry : containerMapEntry.getValue().entrySet()) {
                    Metric containerMetric = containerMetricEntry.getValue();
                    if (null != containerMetric.getEmMetricResult() && Float.MIN_VALUE != containerMetric.getEmMetricResult().getEmMetricGenericResults().getMean()) {
                        LOGGER.info("Mean result for {} is {} ", containerMetric.getName(), containerMetric.getEmMetricResult().getEmMetricGenericResults().getMean());
                    }
                }
            }
            ExperimentResultData experimentResultData = EMUtil.getRealMetricsJSON(experimentTrial, false, trialDetails.getTrialNumber());
            String retJson = new Gson().toJson(experimentResultData);
            LOGGER.info("JSON Getting posted to analyser : \n {} ", retJson);
            URL trial_result_url = null;
            if (null != experimentTrial.getExperimentSettings() && null != experimentTrial.getTrialResultURL()) {
                try {
                    trial_result_url = new URL(experimentTrial.getTrialResultURL());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                LOGGER.debug("POST to URL. {}", trial_result_url);
                LOGGER.debug(retJson.toString());
                HttpUtils.postRequest(trial_result_url, "[" + retJson.toString() + "]");
            }
            stepsMeatData.setEndTimestamp(new Timestamp(System.currentTimeMillis()));
            stepsMeatData.setStatus(EMUtil.EMExpStatus.COMPLETED);
            if (null != iterationMetaData)
                EMStatusUpdateHandler.updateTrialIterationDataStatus(experimentTrial, trialDetails, iterationMetaData);
            EMStatusUpdateHandler.updateTrialMetaDataStatus(experimentTrial, trialDetails);
            EMStatusUpdateHandler.updateExperimentTrialMetaDataStatus(experimentTrial);
        } catch (Exception e) {
            trialDetails.getTrialMetaData().setStatus(EMUtil.EMExpStatus.FAILED);
            e.printStackTrace();
            LOGGER.error("Failed to execute PostResultsHandler ExperimentName: \"{}\" - TrialNo: {} - Iteration: {} - StepName: {} -- due to {}",
                    experimentTrial.getExperimentName(),
                    trialDetails.getTrialNumber(),
                    (null != iterationMetaData) ? iterationMetaData.getIterationNumber() : null,
                    stepsMeatData.getStepName(),
                    e.getMessage()
            );
        }
    }
}
