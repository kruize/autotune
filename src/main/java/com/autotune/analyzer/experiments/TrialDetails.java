package com.autotune.analyzer.experiments;

import com.autotune.analyzer.k8sObjects.Metric;

import java.util.ArrayList;

public class TrialDetails {
    private String deploymentType;
    private final String deploymentName;
    private final String deploymentNameSpace;
    private String state;
    private String result;
    private String resultInfo;
    private String resultError;
    private ArrayList<Metric> metrics;
    private Resources requests;
    private Resources limits;
    private String runtimeOptions;

    public TrialDetails(String deploymentType,
						String deploymentName,
						String deploymentNameSpace,
						String state,
						String result,
						String resultInfo,
						String resultError,
						ArrayList<Metric> metrics,
						Resources requests,
						Resources limits,
						String runtimeOptions) {
        this.deploymentType = deploymentType;
        this.deploymentName = deploymentName;
        this.deploymentNameSpace = deploymentNameSpace;
        this.state = state;
        this.result = result;
        this.resultInfo = resultInfo;
        this.resultError = resultError;
        this.metrics = metrics;
        this.requests = requests;
        this.limits = limits;
        this.runtimeOptions = runtimeOptions;
    }

    public String getDeploymentType() {
        return deploymentType;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getDeploymentNameSpace() {
        return deploymentNameSpace;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResultInfo() {
        return resultInfo;
    }

    public void setResultInfo(String resultInfo) {
        this.resultInfo = resultInfo;
    }

    public String getResultError() {
        return resultError;
    }

    public void setResultError(String resultError) {
        this.resultError = resultError;
    }

    public ArrayList<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(ArrayList<Metric> metrics) {
        this.metrics = metrics;
    }

    public Resources getRequests() {
        return requests;
    }

    public void setRequests(Resources requests) {
        this.requests = requests;
    }

    public Resources getLimits() {
        return limits;
    }

    public void setLimits(Resources limits) {
        this.limits = limits;
    }

    public String getRuntimeOptions() {
        return runtimeOptions;
    }

    public void setRuntimeOptions(String runtimeOptions) {
        this.runtimeOptions = runtimeOptions;
    }
}
