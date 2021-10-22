package com.autotune.analyzer.experiments;

public class ExperimentSettings {
    private final TrialSettings trialSettings;
    private final DeploymentSettings deploymentSettings;

    public ExperimentSettings(TrialSettings trialSettings, DeploymentSettings deploymentSettings) {
        this.trialSettings = trialSettings;
        this.deploymentSettings = deploymentSettings;
    }

    public TrialSettings getTrialSettings() {
        return trialSettings;
    }

    public DeploymentSettings getDeploymentSettings() {
        return deploymentSettings;
    }
}