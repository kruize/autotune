package com.autotune.analyzer.data;

import java.util.concurrent.TimeUnit;

public class DurationBasedRecommendationSubCategory implements RecommendationSubCategory{
    private String name;
    private int duration;
    private TimeUnit recommendationDurationUnits;

    public DurationBasedRecommendationSubCategory(String name, int duration, TimeUnit recommendationDurationUnits) {
        this.name = name;
        this.duration = duration;
        this.recommendationDurationUnits = recommendationDurationUnits;
    }

    // Adding private constructor to avoid object creation without passing any attributes
    private DurationBasedRecommendationSubCategory() {

    }

    public int getDuration() {
        return this.duration;
    }

    public TimeUnit getRecommendationDurationUnits() {
        return this.recommendationDurationUnits;
    }

    @Override
    public String getSubCategory() {
        return this.name;
    }
}
