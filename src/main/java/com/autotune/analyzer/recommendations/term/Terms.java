package com.autotune.analyzer.recommendations.term;

import com.autotune.analyzer.performanceProfiles.PerformanceProfile;

public class Terms {
    int days;
    double threshold_in_days;

    PerformanceProfile performanceProfile;

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public double getThreshold_in_days() {
        return threshold_in_days;
    }

    public void setThreshold_in_days(double threshold_in_days) {
        this.threshold_in_days = threshold_in_days;
    }

    public PerformanceProfile getPerformanceProfile() {
        return performanceProfile;
    }

    public void setPerformanceProfile(PerformanceProfile performanceProfile) {
        this.performanceProfile = performanceProfile;
    }
}
