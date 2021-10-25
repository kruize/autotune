package com.autotune.experimentManager.data.input.deployments;

import org.json.JSONArray;

import java.util.ArrayList;

public abstract class EMConfigBaseDeployment {
    abstract public String getType();
    abstract public String getDeploymentName();
    abstract public String getNamespace();
    abstract public ArrayList<EMConfigDeploymentMetrics> getMetrics();
    abstract public ArrayList<EMConfigDeploymentConfig> getConfigs();
}
