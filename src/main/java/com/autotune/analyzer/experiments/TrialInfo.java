package com.autotune.analyzer.experiments;

public class TrialInfo {
    String trialId;
    int trialNum;

    public TrialInfo(String trialId, int trialNum) {
        this.trialId = trialId;
        this.trialNum = trialNum;
    }

    public String getTrialId() {
        return trialId;
    }

    public void setTrialId(String trialId) {
        this.trialId = trialId;
    }

    public int getTrialNum() {
        return trialNum;
    }

    public void setTrialNum(int trialNum) {
        this.trialNum = trialNum;
    }
}
