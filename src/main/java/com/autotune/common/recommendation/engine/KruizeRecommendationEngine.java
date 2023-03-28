package com.autotune.common.recommendation.engine;

import com.autotune.analyzer.recommendations.Recommendation;

import java.util.HashMap;

public interface KruizeRecommendationEngine {
    public String getEngineName();
    public HashMap<String, Recommendation> getRecommendations();
}
