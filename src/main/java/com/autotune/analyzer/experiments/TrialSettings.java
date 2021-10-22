package com.autotune.analyzer.experiments;

public class TrialSettings {
    private final String trialRun;
    private final String trialMeasurementTime;

    public TrialSettings(String trialRun, String trialMeasurementTime) {
        this.trialRun = trialRun;
        this.trialMeasurementTime = trialMeasurementTime;
    }

    public String getTrialRun() {
        return trialRun;
    }

    public String getTrialMeasurementTime() {
        return trialMeasurementTime;
    }
}