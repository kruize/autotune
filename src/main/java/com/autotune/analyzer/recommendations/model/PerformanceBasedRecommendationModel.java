package com.autotune.analyzer.recommendations.model;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.common.data.result.IntervalResults;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;

public class PerformanceBasedRecommendationModel implements RecommendationModel {

    private int percentile;
    private String name;

    public PerformanceBasedRecommendationModel(int percentile) {
        this.percentile = percentile;
    }

    public PerformanceBasedRecommendationModel() {

    }

    @Override
    public RecommendationConfigItem getCPURequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications) {
        // TODO: Implement CPU recommendation logic based on performance
        // TODO: Use filteredResultsMap and percentile as needed
        return new RecommendationConfigItem();
    }

    @Override
    public RecommendationConfigItem getMemoryRequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications) {
        // TODO: Implement memory recommendation logic based on performance
        // TODO: Use filteredResultsMap and percentile as needed
        return new RecommendationConfigItem();
    }

    @Override
    public String getModelName() {
        return this.name;
    }

    @Override
    public void validate() {

    }
}
