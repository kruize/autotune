package com.autotune.common.recommendation.engine;

import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.analyzer.utils.AnalyzerConstants;

import java.util.HashMap;

public class ProfileBasedRecommendationEngine implements KruizeRecommendationEngine{
    private String name;

    public ProfileBasedRecommendationEngine() {
        this.name = AnalyzerConstants.RecommendationEngine.EngineNames.PROFILE_BASED;
    }

    public ProfileBasedRecommendationEngine(String name) {
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
