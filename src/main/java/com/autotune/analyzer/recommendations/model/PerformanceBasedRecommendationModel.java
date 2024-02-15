package com.autotune.analyzer.recommendations.model;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.common.data.result.IntervalResults;

import java.sql.Timestamp;
import java.util.Map;

public class PerformanceBasedRecommendationModel implements RecommendationModel {

    private int percentile;

    public PerformanceBasedRecommendationModel(int percentile) {
        this.percentile = percentile;
    }

    @Override
    public RecommendationConfigItem getCPURequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap) {
        return null;
    }

    @Override
    public RecommendationConfigItem getMemoryRequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap) {
        return null;
    }

    @Override
    public void validate() {

    }
}
