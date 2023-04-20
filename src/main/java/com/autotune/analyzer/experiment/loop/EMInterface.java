package com.autotune.analyzer.experiment.loop;

import com.autotune.analyzer.experiment.KruizeExperiment;
import com.autotune.common.trials.ExperimentTrial;
import com.autotune.common.data.metrics.Metric;
import com.autotune.utils.HttpUtils;
import com.autotune.utils.TrialHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.sql.Timestamp;
import java.time.Instant;

import static com.autotune.utils.KruizeConstants.JSONKeys.REQUEST_COUNT;
import static com.autotune.utils.KruizeConstants.JSONKeys.REQUEST_SUM;
import static com.autotune.utils.ExperimentMessages.RunExperiment.*;
import static com.autotune.utils.ServerContext.EXPERIMENT_MANAGER_CREATE_TRIAL_END_POINT;

/**
 *
 */
public class EMInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMInterface.class);

    /**
     * @param kruizeExperiment
     * @param experimentTrial
     */
    public static void SendTrialToEM(KruizeExperiment kruizeExperiment,
									 ExperimentTrial experimentTrial) {
        try {
            int trialNumber = experimentTrial.getTrialInfo().getTrialNum();
            // Prepare to send the trial config to EM
            String experimentTrialJSON = TrialHelpers.experimentTrialToJSON(experimentTrial);

            /* STEP 4: Send trial to EM */
            kruizeExperiment.setExperimentStatus(STATUS_TRIAL_NUMBER + trialNumber + STATUS_SENDING_TRIAL_CONFIG_INFO);
            LOGGER.info(experimentTrialJSON);
            URL createExperimentTrialURL = new URL(EXPERIMENT_MANAGER_CREATE_TRIAL_END_POINT);
            String runId = HttpUtils.postRequest(createExperimentTrialURL, experimentTrialJSON.toString());
            kruizeExperiment.setExperimentStatus(STATUS_TRIAL_NUMBER + trialNumber + STATUS_RUNNING_TRIAL + runId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param kruizeExperiment
     * @param experimentTrial
     */
    public static void ProcessTrialResultFromEM(KruizeExperiment kruizeExperiment,
												ExperimentTrial experimentTrial) {
        int trialNumber = experimentTrial.getTrialInfo().getTrialNum();

        LOGGER.info("Processing trial result for " + trialNumber);

        Metric reqSum = experimentTrial.getPodMetricsHashMap().get(REQUEST_SUM);
        Metric reqCount = experimentTrial.getPodMetricsHashMap().get(REQUEST_COUNT);
        experimentTrial.getTrialDetails().get(String.valueOf(trialNumber)).setEndTime(Timestamp.from(Instant.now()));

        // TODO: Need to parse all the obj function results and calculate the result
        try {
            double reqSumMean = reqSum.getMetricResult().getAggregationInfoResult().getAvg();
            double reqCountMean = reqCount.getMetricResult().getAggregationInfoResult().getAvg();
            double rspTime = reqSumMean / reqCountMean;
            LOGGER.info("Calculated rspTime (" + rspTime + ") = reqSumMean (" + reqSumMean + ") / reqCountMean (" + reqCountMean + ");");
            experimentTrial.getTrialDetails().get(String.valueOf(trialNumber)).setResult(String.valueOf(rspTime));
        } catch (Exception e) {
            LOGGER.error("Error calculating response time: {}", e.getMessage());
            experimentTrial.getTrialDetails().get(String.valueOf(trialNumber)).setResult(null);   // ToDO handle Null values at receiver end
        }


        experimentTrial.getTrialDetails().get(String.valueOf(trialNumber)).setResultError("None");
    }
}
