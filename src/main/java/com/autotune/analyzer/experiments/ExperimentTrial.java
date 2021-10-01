package com.autotune.analyzer.experiments;

import java.util.ArrayList;

public class ExperimentTrial {
    String experimentId;
    String namespace;
    String applicationName;
    String appVersion;
    TrialInfo trialInfo;
    ExperimentSettings experimentSettings;
    ArrayList<Deployments> deployments;

    public ExperimentTrial(String experimentId,
                           String namespace,
                           String applicationName,
                           String appVersion,
                           TrialInfo trialInfo,
                           ExperimentSettings experimentSettings,
                           ArrayList<Deployments> deployments) {
        this.experimentId = experimentId;
        this.namespace = namespace;
        this.applicationName = applicationName;
        this.appVersion = appVersion;
        this.trialInfo = trialInfo;
        this.experimentSettings = experimentSettings;
        this.deployments = deployments;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public TrialInfo getTrialInfo() {
        return trialInfo;
    }

    public void setTrialInfo(TrialInfo trialInfo) {
        this.trialInfo = trialInfo;
    }

    public ExperimentSettings getExperimentSettings() {
        return experimentSettings;
    }

    public void setExperimentSettings(ExperimentSettings experimentSettings) {
        this.experimentSettings = experimentSettings;
    }

    public ArrayList<Deployments> getDeployments() {
        return deployments;
    }

    public void setDeployments(ArrayList<Deployments> deployments) {
        this.deployments = deployments;
    }
}
