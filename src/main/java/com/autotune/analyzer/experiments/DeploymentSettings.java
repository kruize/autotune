package com.autotune.analyzer.experiments;

public class DeploymentSettings {
    private final DeploymentPolicy deploymentPolicy;
    private final DeploymentTracking deploymentTracking;

    public DeploymentSettings(DeploymentPolicy deploymentPolicy, DeploymentTracking deploymentTracking) {
        this.deploymentPolicy = deploymentPolicy;
        this.deploymentTracking = deploymentTracking;
    }

    public DeploymentPolicy getDeploymentPolicy() {
        return deploymentPolicy;
    }

    public DeploymentTracking getDeploymentTracking() {
        return deploymentTracking;
    }
}