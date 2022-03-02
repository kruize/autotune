package com.autotune.analyzer.k8sObjects;

import com.autotune.common.data.experiments.DeploymentPolicy;
import com.autotune.common.data.experiments.DeploymentSettings;
import com.autotune.common.data.experiments.TrialSettings;

public class AutotuneSettings {

    private TrialSettings trialSettings;
    private DeploymentSettings deploymentSettings;
    private DeploymentPolicy deploymentPolicy;

    public AutotuneSettings(TrialSettings trialSettings,
                            DeploymentSettings deploymentSettings,
                            DeploymentPolicy deploymentPolicy) {
        this.trialSettings = trialSettings;
        this.deploymentSettings = deploymentSettings;
        this.deploymentPolicy = deploymentPolicy;
    }

    public TrialSettings getTrialSettings() {
        return trialSettings;
    }

    public DeploymentSettings getDeploymentSettings() {
        return deploymentSettings;
    }

    public DeploymentPolicy getDeploymentPolicy() {
        return deploymentPolicy;
    }
}
