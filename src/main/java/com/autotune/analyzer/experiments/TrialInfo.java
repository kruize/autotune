package com.autotune.analyzer.experiments;

public class TrialInfo {
    private final String trialId;
    private final int trialNum;

    public TrialInfo(String trialId, int trialNum) {
        this.trialId = trialId;
        this.trialNum = trialNum;
    }

    public String getTrialId() {
        return trialId;
    }

    public int getTrialNum() {
        return trialNum;
    }
}