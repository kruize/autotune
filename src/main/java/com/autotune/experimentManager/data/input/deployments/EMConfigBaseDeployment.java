package com.autotune.experimentManager.data.input.deployments;

import com.autotune.experimentManager.data.input.EMMetricInput;

import java.util.ArrayList;

public abstract class EMConfigBaseDeployment {
    abstract public String getType();
    abstract public String getDeploymentName();
    abstract public String getNamespace();
    abstract public void setType(String type);
    abstract public void setDeploymentName(String deploymentName);
    abstract public void setNamespace(String namespace);
    abstract public ArrayList<EMMetricInput> getPodMetrics();
    abstract public void setPodMetrics(ArrayList<EMMetricInput> podMetrics);
    abstract public ArrayList<EMConfigDeploymentContainerConfig> getContainers();
    abstract public void setContainers(ArrayList<EMConfigDeploymentContainerConfig> containerConfigs);
}
