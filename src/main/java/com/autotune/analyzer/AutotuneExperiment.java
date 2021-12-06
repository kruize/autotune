package com.autotune.analyzer;

import com.autotune.analyzer.application.ApplicationServiceStack;
import com.autotune.analyzer.experiments.ExperimentTrial;
import com.autotune.analyzer.k8sObjects.AutotuneObject;

import java.util.ArrayList;

public class AutotuneExperiment {
    private final String experimentId;
    private final String experimentName;
    private final AutotuneObject autotuneObject;
    private String experimentStatus;
    private final ApplicationServiceStack applicationServiceStack;
    ArrayList<ExperimentTrial> experimentTrials;
    private Thread experimentThread;

    public AutotuneExperiment(String experimentId,
                              String experimentName,
                              AutotuneObject autotuneObject,
                              String experimentStatus,
                              ApplicationServiceStack applicationServiceStack,
                              ArrayList<ExperimentTrial> experimentTrials) {
        this.experimentId = experimentId;
        this.experimentName = experimentName;
        this.autotuneObject = autotuneObject;
        this.experimentStatus = experimentStatus;
        this.applicationServiceStack = applicationServiceStack;
        this.experimentTrials = experimentTrials;
    }

    public String getExperimentId() {
        return experimentId;
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

    public ApplicationServiceStack getApplicationServiceStack() {
        return applicationServiceStack;
    }

    public ArrayList<ExperimentTrial> getExperimentTrials() {
        return experimentTrials;
    }

    public Thread getExperimentThread() {
        return experimentThread;
    }

    public void setExperimentThread(Thread experimentThread) {
        this.experimentThread = experimentThread;
    }
}
