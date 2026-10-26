package com.autotune.analyzer.recommendations.model;

import com.autotune.analyzer.recommendations.RecommendationConstants;

public class StabilityBasedRecommendationModel extends GenericRecommendationModel {

    public static final String defaultname = RecommendationConstants.RecommendationEngine.ModelNames.STABILITY;

    public StabilityBasedRecommendationModel(RecommendationTunables recommendationTunables) {
        super(defaultname, recommendationTunables);
    }
}
