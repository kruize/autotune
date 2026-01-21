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

    Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> getAcceleratorRequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications);

     // CPU and Memory Limit recommendation methods with ratio-based calculation
    RecommendationConfigItem getCPULimitRecommendation(
            RecommendationConfigItem recommendedRequest,
            RecommendationConfigItem currentRequest,
            RecommendationConfigItem currentLimit,
            ArrayList<RecommendationNotification> notifications);

    RecommendationConfigItem getMemoryLimitRecommendation(
            RecommendationConfigItem recommendedRequest,
            RecommendationConfigItem currentRequest,
            RecommendationConfigItem currentLimit,
            ArrayList<RecommendationNotification> notifications);

    // CPU and Memory Limit recommendation methods for Namespace with ratio-based calculation
    RecommendationConfigItem getCPULimitRecommendationForNamespace(
            RecommendationConfigItem recommendedRequest,
            RecommendationConfigItem currentRequest,
            RecommendationConfigItem currentLimit,
            ArrayList<RecommendationNotification> notifications);

    RecommendationConfigItem getMemoryLimitRecommendationForNamespace(
            RecommendationConfigItem recommendedRequest,
            RecommendationConfigItem currentRequest,
            RecommendationConfigItem currentLimit,
            ArrayList<RecommendationNotification> notifications);
            
    public String getModelName();
    void validate();

}
