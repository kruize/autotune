package com.autotune.analyzer.experiments;

import com.autotune.analyzer.k8sObjects.Metric;

import java.util.ArrayList;

public class Deployments {
    String deploymentType;
    String deploymentName;
    String deploymentNameSpace;
    String state;
    String result;
    String resultInfo;
    String resultError;
    ArrayList<Metric> metrics;
    Resources requests;
    Resources limits;
    String jvmOptions;

    public Deployments(String deploymentType,
                       String deploymentName,
                       String deploymentNameSpace,
                       String state,
                       String result,
                       String resultInfo,
                       String resultError,
                       ArrayList<Metric> metrics,
                       Resources requests,
                       Resources limits,
                       String jvmOptions) {
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
        this.jvmOptions = jvmOptions;
    }

    public String getDeploymentType() {
        return deploymentType;
    }

    public void setDeploymentType(String deploymentType) {
        this.deploymentType = deploymentType;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public String getDeploymentNameSpace() {
        return deploymentNameSpace;
    }

    public void setDeploymentNameSpace(String deploymentNameSpace) {
        this.deploymentNameSpace = deploymentNameSpace;
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

    public String getJvmOptions() {
        return jvmOptions;
    }

    public void setJvmOptions(String jvmOptions) {
        this.jvmOptions = jvmOptions;
    }
}