package com.autotune.analyzer;

import com.autotune.analyzer.application.ApplicationDeployment;
import com.autotune.analyzer.application.ApplicationSearchSpace;
import com.autotune.analyzer.k8sObjects.AutotuneObject;
import com.autotune.common.data.experiments.ExperimentSummary;
import com.autotune.common.data.experiments.ExperimentTrial;
import com.autotune.common.data.experiments.TrialDetails;

import java.util.TreeMap;

/**
 *
 */
public class AutotuneExperiment {
    private final String deploymentName;
    private final String experimentName;
    private final AutotuneObject autotuneObject;
    private String experimentStatus;
    private final ApplicationDeployment applicationDeployment;
    private ApplicationSearchSpace applicationSearchSpace;
    private ExperimentSummary experimentSummary;
    // Treemap of trials for this experiment
    // Uses trial_number as key
    TreeMap<Integer, ExperimentTrial> experimentTrials;
    private RunExperiment experimentThread;

    public AutotuneExperiment(String deploymentName,
                              String experimentName,
                              AutotuneObject autotuneObject,
                              String experimentStatus,
                              ExperimentSummary experimentSummary,
                              ApplicationDeployment applicationDeployment,
                              TreeMap<Integer, ExperimentTrial> experimentTrials) {
        this.deploymentName = deploymentName;
        this.experimentName = experimentName;
        this.autotuneObject = autotuneObject;
        this.experimentStatus = experimentStatus;
        this.experimentSummary = experimentSummary;
        this.applicationDeployment = applicationDeployment;
        this.experimentTrials = experimentTrials;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public AutotuneObject getAutotuneObject() {
        return autotuneObject;
    }

    public String getExperimentStatus() {
        return experimentStatus;
    }

    public ExperimentSummary getExperimentSummary() { return experimentSummary; }

    public void setExperimentStatus(String experimentStatus) {
        this.experimentStatus = experimentStatus;
    }

    public ApplicationDeployment getApplicationDeployment() {
        return applicationDeployment;
    }

    public TreeMap<Integer, ExperimentTrial> getExperimentTrials() {
        return experimentTrials;
    }

    public ApplicationSearchSpace getApplicationSearchSpace() {
        return applicationSearchSpace;
    }

    public void setApplicationSearchSpace(ApplicationSearchSpace applicationSearchSpace) {
        this.applicationSearchSpace = applicationSearchSpace;
    }

    public RunExperiment getExperimentThread() {
        return experimentThread;
    }

    public void setExperimentThread(RunExperiment experimentThread) {
        this.experimentThread = experimentThread;
    }

    /**
     *
     */
    public void initializeTrial(int trialNumber) {
        experimentSummary.setCurrentTrial(trialNumber);
        int ongoingTrials = getExperimentSummary().getTrialsOngoing();
        getExperimentSummary().setTrialsOngoing(++ongoingTrials);
    }

    /**
     * Update the counters and check the best trial
     */
    public void summarizeTrial(TrialDetails trialDetails) {
        int currentTrial = experimentSummary.getCurrentTrial();
        int goodTrials = experimentSummary.getTrialsPassed();
        int badTrials = experimentSummary.getTrialsFailed();
        int bestTrial = experimentSummary.getBestTrial();
        int ongoingTrials = experimentSummary.getTrialsOngoing();

        // currentTrial is zero based
        if (!trialDetails.getResultError().contains("ERROR")) {
            experimentSummary.setTrialsPassed(++goodTrials);
        } else {
            experimentSummary.setTrialsFailed(++badTrials);
        }
        int trialsCompleted = goodTrials + badTrials;
        experimentSummary.setTrialsCompleted(trialsCompleted);

        // Update bestTrial based on results of the current trial
        if (bestTrial == -1) {
            experimentSummary.setBestTrial(currentTrial);
        } else {
            double currentResult = Double.parseDouble(trialDetails.getResult());
            ExperimentTrial bestExperimentTrial = getExperimentTrials().get(bestTrial);
            TrialDetails bestTrialDetails = bestExperimentTrial.getTrialDetails().get("training");
            double bestResult = Double.parseDouble(bestTrialDetails.getResult());
            String direction = autotuneObject.getSloInfo().getDirection();
            if ((direction.equals("minimize") && currentResult < bestResult) ||
                    (direction.equals("maximize") && currentResult > bestResult)) {
                experimentSummary.setBestTrial(currentTrial);
            }
        }
        experimentSummary.setTrialsOngoing(--ongoingTrials);
    }
}
