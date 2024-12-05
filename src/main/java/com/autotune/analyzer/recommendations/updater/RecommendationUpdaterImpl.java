package com.autotune.analyzer.recommendations.updater;

import com.autotune.analyzer.exceptions.InvalidRecommendationUpdaterType;
import com.autotune.analyzer.recommendations.updater.vpa.VpaUpdaterImpl;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;

import java.util.Objects;

public class RecommendationUpdaterImpl implements RecommendationUpdater{

    @Override
    public RecommendationUpdaterImpl getUpdater(String updaterType) throws InvalidRecommendationUpdaterType {
        if (AnalyzerConstants.RecommendationUpdaterConstants.VPA_UPDATER.equalsIgnoreCase(updaterType)) {
            return VpaUpdaterImpl.getInstance();
        } else {
            throw new InvalidRecommendationUpdaterType(String.format(AnalyzerErrorConstants.RecommendationUpdaterErrorConstant.INVALID_UPDATER_TYPE, updaterType));
        }
    }

    @Override
    public boolean isUpdaterInstalled() {
        return false;
    }
}
