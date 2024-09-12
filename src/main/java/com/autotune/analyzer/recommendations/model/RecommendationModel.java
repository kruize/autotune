package com.autotune.analyzer.recommendations.model;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.result.IntervalResults;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;

public interface RecommendationModel {

    RecommendationConfigItem getCPURequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications);
    RecommendationConfigItem getMemoryRequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications);
    // get namespace recommendations for CPU Request
    RecommendationConfigItem getCPURequestRecommendationForNamespace(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications);
    // get namespace recommendations for Memory Request
    RecommendationConfigItem getMemoryRequestRecommendationForNamespace(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications);
    Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> getGpuRequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications);

    public String getModelName();
    void validate();

}
