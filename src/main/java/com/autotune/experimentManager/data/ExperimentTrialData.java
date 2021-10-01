package com.autotune.experimentManager.data;

import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.utils.EMUtil.EMExpStatus;
import com.autotune.experimentManager.utils.EMUtil.EMExpStages;
import io.fabric8.kubernetes.api.model.apps.Deployment;

public class ExperimentTrialData {

    private String trialResult;
    private String trialResultInfo;
    private String trialResultError;
    private final EMTrialConfig config;
    private Deployment currentDeployment;
    private Deployment trailDeployment;
    private EMExpStages currentStage;
    private EMExpStages targetStage;
    private EMExpStatus status;
    private boolean notifyTrialCompletion;

    public ExperimentTrialData(EMTrialConfig config) throws EMInvalidInstanceCreation {
        if (config == null) {
            throw new EMInvalidInstanceCreation();
        }
        this.config = config;
        currentStage = EMExpStages.INIT;
        targetStage = EMExpStages.CREATE_CONFIG;
        this.status = EMExpStatus.CREATED;
        this.notifyTrialCompletion = false;
    }

    public String getTrialResult() {
        return trialResult;
    }

    public void setTrialResult(String trialResult) {
        this.trialResult = trialResult;
    }

    public String getTrialResultInfo() {
        return trialResultInfo;
    }

    public void setTrialResultInfo(String trialResultInfo) {
        this.trialResultInfo = trialResultInfo;
    }

    public String getTrialResultError() {
        return trialResultError;
    }

    public void setTrialResultError(String trialResultError) {
        this.trialResultError = trialResultError;
    }

    public EMExpStages getCurrentStage() {
        return currentStage;
    }

    public EMTrialConfig getConfig() {
        return config;
    }

    public void setCurrentStage(EMExpStages currentStage) {
        this.currentStage = currentStage;
    }

    public EMExpStages getTargetStage() {
        return targetStage;
    }

    public void setTargetStage(EMExpStages targetStage) {
        this.targetStage = targetStage;
    }

    public Deployment getCurrentDeployment() {
        return currentDeployment;
    }

    public void setCurrentDeployment(Deployment currentDeployment) {
        this.currentDeployment = currentDeployment;
    }

    public Deployment getTrailDeployment() {
        return trailDeployment;
    }

    public void setTrailDeployment(Deployment trailDeployment) {
        this.trailDeployment = trailDeployment;
    }

    public EMExpStatus getStatus() {
        return status;
    }

    public void setStatus(EMExpStatus status) {
        this.status = status;
    }

    public boolean isNotifyTrialCompletion() {
        return notifyTrialCompletion;
    }

    public void setNotifyTrialCompletion(boolean notifyTrialCompletion) {
        this.notifyTrialCompletion = notifyTrialCompletion;
    }
}
