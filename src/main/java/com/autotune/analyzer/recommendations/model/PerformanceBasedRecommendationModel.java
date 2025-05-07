package com.autotune.analyzer.recommendations.model;

import com.autotune.analyzer.recommendations.RecommendationConstants;

public class PerformanceBasedRecommendationModel extends  GenericRecommendationModel{

    public static final String defaultName = RecommendationConstants.RecommendationEngine.ModelNames.PERFORMANCE;

    public PerformanceBasedRecommendationModel(RecommendationTunables recommendationTunables) {
        super(defaultName, recommendationTunables);
    }
}
