package com.autotune.analyzer;

import com.autotune.analyzer.application.ApplicationServiceStack;
import com.autotune.analyzer.experiments.ExperimentTrial;

import java.util.ArrayList;

public class AutotuneExperiment {
    String experimentId;
    String experimentName;
    String experimentStatus;
    ApplicationServiceStack applicationServiceStack;
    ArrayList<ExperimentTrial> experimentTrials;
    Thread experimentThread;

    public AutotuneExperiment(String experimentId,
                              String experimentName,
                              String experimentStatus,
                              ApplicationServiceStack applicationServiceStack,
                              ArrayList<ExperimentTrial> experimentTrials) {
        this.experimentId = experimentId;
        this.experimentName = experimentName;
        this.experimentStatus = experimentStatus;
        this.applicationServiceStack = applicationServiceStack;
        this.experimentTrials = experimentTrials;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
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

    public void setApplicationServiceStack(ApplicationServiceStack applicationServiceStack) {
        this.applicationServiceStack = applicationServiceStack;
    }

    public ArrayList<ExperimentTrial> getExperimentTrials() {
        return experimentTrials;
    }

    public void setExperimentTrials(ArrayList<ExperimentTrial> experimentTrials) {
        this.experimentTrials = experimentTrials;
    }

    public Thread getExperimentThread() {
        return experimentThread;
    }

    public void setExperimentThread(Thread experimentThread) {
        this.experimentThread = experimentThread;
    }
}