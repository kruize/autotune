package com.autotune.analyzer.recommendations.model;

import com.autotune.analyzer.kruizeLayer.impl.TunableSpec;
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
    /** CPU limit recommendation (used by runtime layers; override when model has distinct limit logic). */
    RecommendationConfigItem getCPULimitRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications);
    /** Memory limit recommendation (used by runtime layers; override when model has distinct limit logic). */
    RecommendationConfigItem getMemoryLimitRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications);
    // get namespace recommendations for CPU Request
    RecommendationConfigItem getCPURequestRecommendationForNamespace(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications);
    // get namespace recommendations for Memory Request
    RecommendationConfigItem getMemoryRequestRecommendationForNamespace(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications);

    Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> getAcceleratorRequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications);
    Object getRuntimeRecommendations(String metricName, String layerName, Map<Timestamp, IntervalResults> filteredResultsMap, Map<TunableSpec, Object> context, ArrayList<RecommendationNotification> notifications);

    public String getModelName();
    void validate();

}
