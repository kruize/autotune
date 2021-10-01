package com.autotune.analyzer.experiments;

public class ExperimentSettings {
    TrialSettings trialSettings;
    DeploymentSettings deploymentSettings;

    public ExperimentSettings(TrialSettings trialSettings, DeploymentSettings deploymentSettings) {
        this.trialSettings = trialSettings;
        this.deploymentSettings = deploymentSettings;
    }

    public TrialSettings getTrialSettings() {
        return trialSettings;
    }

    public void setTrialSettings(TrialSettings trialSettings) {
        this.trialSettings = trialSettings;
    }

    public DeploymentSettings getDeploymentSettings() {
        return deploymentSettings;
    }

    public void setDeploymentSettings(DeploymentSettings deploymentSettings) {
        this.deploymentSettings = deploymentSettings;
    }
}
