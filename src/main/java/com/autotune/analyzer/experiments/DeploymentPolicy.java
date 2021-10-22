package com.autotune.analyzer.experiments;

public class DeploymentPolicy {
    private final String deploymentType;
    private final String targetEnv;
    private final String agent;

    public DeploymentPolicy(String deploymentType, String targetEnv, String agent) {
        this.deploymentType = deploymentType;
        this.targetEnv = targetEnv;
        this.agent = agent;
    }

    public String getDeploymentType() {
        return deploymentType;
    }

    public String getTargetEnv() {
        return targetEnv;
    }

    public String getAgent() {
        return agent;
    }

    /*
    public void setAgent(String agent) {
        this.agent = agent;
    }
     */
}