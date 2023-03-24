package com.autotune.analyzer.experiment;

import com.autotune.analyzer.application.ApplicationDeployment;
import com.autotune.analyzer.application.ApplicationSearchSpace;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.common.trials.ExperimentSummary;
import com.autotune.common.trials.ExperimentTrial;
import com.autotune.common.trials.TrialDetails;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.performanceProfiles.PerformanceProfilesDeployment;

import java.util.TreeMap;

import static com.autotune.analyzer.utils.AnalyzerConstants.AutotuneObjectConstants.MAXIMIZE;
import static com.autotune.analyzer.utils.AnalyzerConstants.AutotuneObjectConstants.MINIMIZE;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.TRAINING;

/**
 *
 */
public class KruizeExperiment {
    private final String deploymentName;
    private final String experimentName;
    private final KruizeObject kruizeObject;
    private String experimentStatus;
    private final ApplicationDeployment applicationDeployment;
    private ApplicationSearchSpace applicationSearchSpace;
    private ExperimentSummary experimentSummary;
    // Treemap of trials for this experiment
    // Uses trial_number as key
    TreeMap<Integer, ExperimentTrial> experimentTrials;
    private RunExperiment experimentThread;
    private String HPOoperation;

    public KruizeExperiment(String deploymentName,
                            String experimentName,
                            KruizeObject kruizeObject,
                            String experimentStatus,
                            ExperimentSummary experimentSummary,
                            ApplicationDeployment applicationDeployment,
                            TreeMap<Integer, ExperimentTrial> experimentTrials) {
        this.deploymentName = deploymentName;
        this.experimentName = experimentName;
        this.kruizeObject = kruizeObject;
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

    public KruizeObject getAutotuneObject() {
        return kruizeObject;
    }

    public String getExperimentStatus() {
        return experimentStatus;
    }

    public ExperimentSummary getExperimentSummary() {
        return experimentSummary;
    }

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

    public String getHPOoperation() {
        return HPOoperation;
    }

    public void setHPOoperation(String HPOoperation) {
        this.HPOoperation = HPOoperation;
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
            TrialDetails bestTrialDetails = bestExperimentTrial.getTrialDetails().get(TRAINING);
            double bestResult = Double.parseDouble(bestTrialDetails.getResult());
            PerformanceProfile performanceProfile = PerformanceProfilesDeployment.performanceProfilesMap
                    .get(kruizeObject.getPerformanceProfile());
            String direction = performanceProfile.getSloInfo().getDirection();
            if ((direction.equals(MINIMIZE) && currentResult < bestResult) ||
                    (direction.equals(MAXIMIZE) && currentResult > bestResult)) {
                experimentSummary.setBestTrial(currentTrial);
            }
        }
        experimentSummary.setTrialsOngoing(--ongoingTrials);
    }
}
