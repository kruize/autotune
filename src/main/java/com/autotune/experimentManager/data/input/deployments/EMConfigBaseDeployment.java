package com.autotune.experimentManager.data.input.deployments;

import com.autotune.experimentManager.data.input.EMMetricInput;

import java.util.ArrayList;

public abstract class EMConfigBaseDeployment {
    abstract public String getType();
    abstract public String getDeploymentName();
    abstract public String getNamespace();
    abstract public ArrayList<EMMetricInput> getMetrics();
    abstract public ArrayList<EMConfigDeploymentContainerConfig> getContainers();
}
