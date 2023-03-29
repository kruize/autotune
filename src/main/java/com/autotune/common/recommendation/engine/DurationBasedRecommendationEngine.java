package com.autotune.common.recommendation.engine;

import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.analyzer.utils.AnalyzerConstants;

import java.util.HashMap;

public class DurationBasedRecommendationEngine implements KruizeRecommendationEngine{
    private String name;

    public DurationBasedRecommendationEngine() {
        this.name = AnalyzerConstants.RecommendationEngine.EngineNames.DURATION_BASED;
    }

    public DurationBasedRecommendationEngine(String name) {
        this.name = name;
    }

    @Override
    public String getEngineName() {
        return this.name;
    }

    @Override
    public HashMap<String, Recommendation> getRecommendations() {
        // TODO: Needs to be implemented

        return null;
    }
}
