package com.autotune.analyzer.recommendations.engine;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.utils.AnalyzerConstants;

import java.util.HashMap;

public interface KruizeRecommendationSections {
    public HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> generateRecommendationConfig();
    public HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> generateCurrentConfig();
    public HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> generateVariation();
    public void populateNotifications();
    public HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> generateRequests(AnalyzerConstants.RecommendationSection recommendationSection);
    public HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> generateLimits(AnalyzerConstants.RecommendationSection recommendationSection);
}
