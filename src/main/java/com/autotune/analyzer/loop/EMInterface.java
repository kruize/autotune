package com.autotune.analyzer.loop;

import com.autotune.analyzer.AutotuneExperiment;
import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.TrialDetails;
import com.autotune.common.k8sObjects.Metric;
import com.autotune.utils.HttpUtils;
import com.autotune.utils.TrialHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.sql.Timestamp;
import java.time.Instant;

import static com.autotune.utils.AnalyzerConstants.ServiceConstants.TRAINING;
import static com.autotune.utils.AutotuneConstants.JSONKeys.REQUEST_COUNT;
import static com.autotune.utils.AutotuneConstants.JSONKeys.REQUEST_SUM;
import static com.autotune.utils.ExperimentMessages.RunExperiment.*;
import static com.autotune.utils.ServerContext.EXPERIMENT_MANAGER_CREATE_TRIAL_END_POINT;

/**
 *
 */
public class EMInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(HPOInterface.class);

    /**
     * @param autotuneExperiment
     * @param experimentTrial
     */
    public static void SendTrialToEM(AutotuneExperiment autotuneExperiment,
                                     ExperimentTrial experimentTrial) {
        try {
            int trialNumber = experimentTrial.getTrialInfo().getTrialNum();
            // Prepare to send the trial config to EM
            String experimentTrialJSON = TrialHelpers.experimentTrialToJSON(experimentTrial);

            /* STEP 4: Send trial to EM */
            autotuneExperiment.setExperimentStatus(STATUS_TRIAL_NUMBER + trialNumber + STATUS_SENDING_TRIAL_CONFIG_INFO);
            LOGGER.info(experimentTrialJSON);
            URL createExperimentTrialURL = new URL(EXPERIMENT_MANAGER_CREATE_TRIAL_END_POINT);
            String runId = HttpUtils.postRequest(createExperimentTrialURL, experimentTrialJSON);
            autotuneExperiment.setExperimentStatus(STATUS_TRIAL_NUMBER + trialNumber + STATUS_RUNNING_TRIAL + runId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param experimentTrial
     */
    public static void ProcessTrialResultFromEM(ExperimentTrial experimentTrial) {
        int trialNumber = experimentTrial.getTrialInfo().getTrialNum();

        LOGGER.info("Processing trial result for " + trialNumber);

        TrialDetails trialDetails = experimentTrial.getTrialDetails().get(TRAINING);
        Metric reqSum = trialDetails.getPodMetrics().get(REQUEST_SUM);
        Metric reqCount = trialDetails.getPodMetrics().get(REQUEST_COUNT);
        trialDetails.setEndTime(Timestamp.from(Instant.now()));

        // TODO: Need to parse all the obj function results and calculate the result
        double reqSumMean = reqSum.getEmMetricResult().getEmMetricGenericResults().getMean();
        double reqCountMean = reqCount.getEmMetricResult().getEmMetricGenericResults().getMean();
        double rspTime = reqSumMean / reqCountMean;
        LOGGER.info("Calculated rspTime (" + rspTime + ") = reqSumMean (" + reqSumMean + ") / reqCountMean (" + reqCountMean + ");");

        trialDetails.setResult(String.valueOf(rspTime));
        trialDetails.setResultError("None");
    }
}
