package com.autotune.analyzer;

import com.autotune.analyzer.application.ApplicationDeployment;
import com.autotune.analyzer.application.ApplicationSearchSpace;
import com.autotune.analyzer.k8sObjects.AutotuneObject;
import com.autotune.common.data.experiments.ExperimentTrial;

import java.util.HashMap;
import java.util.Map;

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
    HashMap<Integer, ExperimentTrial> experimentTrials;
    private RunExperiment experimentThread;

    public AutotuneExperiment(String deploymentName,
                              String experimentName,
                              AutotuneObject autotuneObject,
                              String experimentStatus,
                              ApplicationDeployment applicationDeployment,
                              HashMap<Integer, ExperimentTrial> experimentTrials) {
        this.deploymentName = deploymentName;
        this.experimentName = experimentName;
        this.autotuneObject = autotuneObject;
        this.experimentStatus = experimentStatus;
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

    public void setExperimentStatus(String experimentStatus) {
        this.experimentStatus = experimentStatus;
    }

    public ApplicationDeployment getApplicationDeployment() {
        return applicationDeployment;
    }

    public Map<Integer, ExperimentTrial> getExperimentTrials() {
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
}
