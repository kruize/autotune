package com.autotune.analyzer.experiments;

public class DeploymentSettings {
    DeploymentPolicy deploymentPolicy;
    DeploymentTracking deploymentTracking;

    public DeploymentSettings(DeploymentPolicy deploymentPolicy, DeploymentTracking deploymentTracking) {
        this.deploymentPolicy = deploymentPolicy;
        this.deploymentTracking = deploymentTracking;
    }

    public DeploymentPolicy getDeploymentPolicy() {
        return deploymentPolicy;
    }

    public void setDeploymentPolicy(DeploymentPolicy deploymentPolicy) {
        this.deploymentPolicy = deploymentPolicy;
    }

    public DeploymentTracking getDeploymentTracking() {
        return deploymentTracking;
    }

    public void setDeploymentTracking(DeploymentTracking deploymentTracking) {
        this.deploymentTracking = deploymentTracking;
    }
}
