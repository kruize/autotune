package com.autotune.analyzer.recommendations.updater;

import com.autotune.analyzer.exceptions.InvalidRecommendationUpdaterType;

public interface RecommendationUpdater {
    RecommendationUpdaterImpl getUpdater(String updaterType) throws InvalidRecommendationUpdaterType;
    boolean isUpdaterInstalled();
}
