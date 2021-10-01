package com.autotune.analyzer.experiments;

public class DeploymentPolicy {
    String deploymentType;
    String targetEnv;
    String agent;

    public DeploymentPolicy(String deploymentType, String targetEnv, String agent) {
        this.deploymentType = deploymentType;
        this.targetEnv = targetEnv;
        this.agent = agent;
    }

    public String getDeploymentType() {
        return deploymentType;
    }

    public void setDeploymentType(String deploymentType) {
        this.deploymentType = deploymentType;
    }

    public String getTargetEnv() {
        return targetEnv;
    }

    public void setTargetEnv(String targetEnv) {
        this.targetEnv = targetEnv;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }
}
