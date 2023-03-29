package com.autotune.analyzer.recommendations.engine;

import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.k8sObjects.ContainerObject;

import java.sql.Timestamp;
import java.util.HashMap;

public interface KruizeRecommendationEngine {
    public String getEngineName();
    public String getEngineKey();
    public AnalyzerConstants.RecommendationCategory getEngineCategory();
    public HashMap<String, Recommendation> getRecommendations(ContainerObject containerObject, Timestamp monitoringEndTime);
}
