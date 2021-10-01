package com.autotune.analyzer.experiments;

public class TrialSettings {
    String trialRun;
    String trialMeasurementTime;

    public TrialSettings(String trialRun, String trialMeasurementTime) {
        this.trialRun = trialRun;
        this.trialMeasurementTime = trialMeasurementTime;
    }

    public String getTrialRun() {
        return trialRun;
    }

    public void setTrialRun(String trialRun) {
        this.trialRun = trialRun;
    }

    public String getTrialMeasurementTime() {
        return trialMeasurementTime;
    }

    public void setTrialMeasurementTime(String trialMeasurementTime) {
        this.trialMeasurementTime = trialMeasurementTime;
    }
}
