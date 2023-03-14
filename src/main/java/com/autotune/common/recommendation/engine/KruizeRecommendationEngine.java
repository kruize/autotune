package com.autotune.common.recommendation.engine;

import com.autotune.common.data.result.Recommendation;

import java.util.HashMap;

public interface KruizeRecommendationEngine {
    public HashMap<String, Recommendation> getRecommendations();
}
