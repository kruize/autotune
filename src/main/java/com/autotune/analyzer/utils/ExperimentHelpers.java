package com.autotune.analyzer.utils;

import com.autotune.analyzer.AutotuneExperiment;
import com.autotune.common.k8sObjects.KruizeObject;
import com.autotune.common.experiments.ExperimentSummary;
import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.TrialDetails;
import org.json.JSONArray;
import org.json.JSONObject;

import static com.autotune.analyzer.utils.ServiceHelpers.addDeploymentDetails;
import static com.autotune.analyzer.utils.ServiceHelpers.addExperimentDetails;
import static com.autotune.utils.AnalyzerConstants.ServiceConstants.*;

/**
 *
 */
public class ExperimentHelpers {

    public static JSONObject experimentToJSON(AutotuneExperiment autotuneExperiment) {

        JSONObject experimentJson = new JSONObject();
        KruizeObject kruizeObject = autotuneExperiment.getAutotuneObject();
        if (null == kruizeObject) {
            return experimentJson;
        }

        experimentJson.put(TRIAL_STATUS, autotuneExperiment.getExperimentStatus());
        ExperimentSummary experimentSummary = autotuneExperiment.getExperimentSummary();
        JSONObject trialSummary = new JSONObject();
        trialSummary.put(TOTAL_TRIALS, experimentSummary.getTotalTrials());
        trialSummary.put(TRIALS_COMPLETED, experimentSummary.getTrialsCompleted());
        trialSummary.put(TRIALS_ONGOING, experimentSummary.getTrialsOngoing());
        trialSummary.put(TRIALS_PASSED, experimentSummary.getTrialsPassed());
        trialSummary.put(TRIALS_FAILED, experimentSummary.getTrialsFailed());
        trialSummary.put(BEST_TRIAL, experimentSummary.getBestTrial());
        experimentJson.put(TRIALS_SUMMARY, trialSummary);

        addExperimentDetails(experimentJson, kruizeObject);
        addDeploymentDetails(experimentJson, kruizeObject);

        JSONArray trialsJSONArray = new JSONArray();
        for (int trialNum : autotuneExperiment.getExperimentTrials().keySet()) {
            ExperimentTrial experimentTrial = autotuneExperiment.getExperimentTrials().get(trialNum);
            JSONObject trialsJSON = new JSONObject();
            trialsJSON.put(TRIAL_NUMBER, experimentTrial.getTrialInfo().getTrialNum());
            TrialDetails trainingTrialDetails = experimentTrial.getTrialDetails().get(String.valueOf(trialNum));
            trialsJSON.put(TRIAL_RESULT, trainingTrialDetails.getResult());
            trialsJSON.put(TRIAL_ERRORS, trainingTrialDetails.getResultError());
            StringBuilder durationSeconds;
            if (null != trainingTrialDetails.getEndTime()) {
                long milliseconds = (trainingTrialDetails.getEndTime().getTime()
                        - trainingTrialDetails.getStartTime().getTime()) / 1000;
                durationSeconds = new StringBuilder(String.valueOf(milliseconds)).append(SECONDS);
            } else {
                durationSeconds = new StringBuilder(NA);
            }
            trialsJSON.put(TRIAL_DURATION, durationSeconds);
            trialsJSONArray.put(trialsJSON);
        }
        experimentJson.put(EXPERIMENT_TRIALS, trialsJSONArray);

        return experimentJson;
    }
}
