package com.autotune.analyzer.loop;

import com.autotune.analyzer.AutotuneExperiment;
import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.k8sObjects.Metric;
import com.autotune.utils.HttpUtils;
import com.autotune.utils.TrialHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.sql.Timestamp;
import java.time.Instant;

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
            String runId = HttpUtils.postRequest(createExperimentTrialURL, experimentTrialJSON.toString());
            autotuneExperiment.setExperimentStatus(STATUS_TRIAL_NUMBER + trialNumber + STATUS_RUNNING_TRIAL + runId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param autotuneExperiment
     * @param experimentTrial
     */
    public static void ProcessTrialResultFromEM(AutotuneExperiment autotuneExperiment,
                                                ExperimentTrial experimentTrial) {
        int trialNumber = experimentTrial.getTrialInfo().getTrialNum();

        LOGGER.info("Processing trial result for " + trialNumber);

        Metric reqSum = experimentTrial.getPodMetricsHashMap().get(REQUEST_SUM);
        Metric reqCount = experimentTrial.getPodMetricsHashMap().get(REQUEST_COUNT);
        experimentTrial.getTrialDetails().get(String.valueOf(trialNumber)).setEndTime(Timestamp.from(Instant.now()));

        // TODO: Need to parse all the obj function results and calculate the result
        try {
            double reqSumMean = reqSum.getEmMetricResult().getEmMetricGenericResults().getMean();
            double reqCountMean = reqCount.getEmMetricResult().getEmMetricGenericResults().getMean();
            double rspTime = reqSumMean / reqCountMean;
            LOGGER.info("Calculated rspTime (" + rspTime + ") = reqSumMean (" + reqSumMean + ") / reqCountMean (" + reqCountMean + ");");
            experimentTrial.getTrialDetails().get(String.valueOf(trialNumber)).setResult(String.valueOf(rspTime));
        } catch (Exception e) {
            LOGGER.error("Error calculating response time due to {}", e.getMessage());
            experimentTrial.getTrialDetails().get(String.valueOf(trialNumber)).setResult(null);
        }
        experimentTrial.getTrialDetails().get(String.valueOf(trialNumber)).setResultError("None");
    }
}
