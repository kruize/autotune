package com.autotune.analyzer.recommendations.engine;

import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.recommendations.term.Terms;

import java.sql.Timestamp;

public class RecommendationEngine {
    PerformanceProfile performanceProfile;
    String experimentName;
    Timestamp intervalEndTime;
    Terms terms;

    public RecommendationEngine(PerformanceProfile performanceProfile, String experimentName, Timestamp intervalEndTime, Terms terms) {
        this.performanceProfile = performanceProfile;
        this.experimentName = experimentName;
        this.intervalEndTime = intervalEndTime;
        this.terms = terms;
    }

    public RecommendationEngine() {

    }

    public boolean validate(Timestamp intervalEndTime, String experimentName) {
        return true;
    }
}
