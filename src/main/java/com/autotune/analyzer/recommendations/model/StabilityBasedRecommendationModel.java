package com.autotune.analyzer.recommendations.model;

import com.autotune.analyzer.kruizeObject.ResourceScopeSettings;
import com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationScope;
import com.autotune.utils.KruizeConstants;

import java.util.List;

/**
 * Stability model: sizes like the performance model (same percentiles), but by default surfaces only
 * workloads at risk, i.e. those where both CPU and memory are under-provisioned.
 */
public class StabilityBasedRecommendationModel extends GenericRecommendationModel {

    public static final String defaultname = KruizeConstants.JSONKeys.STABILITY;

    public StabilityBasedRecommendationModel(RecommendationTunables recommendationTunables) {
        super(defaultname, recommendationTunables);
    }

    @Override
    public ResourceScopeSettings getDefaultResourceScopeSettings() {
        String underProvisioned = RecommendationScope.UNDER_PROVISIONED.getValue();
        ResourceScopeSettings settings = new ResourceScopeSettings(List.of(underProvisioned), List.of(underProvisioned));
        settings.parse();
        return settings;
    }
}
