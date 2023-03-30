package com.autotune.analyzer.recommendations.engine;

import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.result.ContainerData;

import java.sql.Timestamp;
import java.util.HashMap;

public class ProfileBasedRecommendationEngine implements KruizeRecommendationEngine{
    private String name;
    private String key;
    private AnalyzerConstants.RecommendationCategory category;

    public ProfileBasedRecommendationEngine() {
        this.name = AnalyzerConstants.RecommendationEngine.EngineNames.PROFILE_BASED;
        this.key = AnalyzerConstants.RecommendationEngine.EngineKeys.PROFILE_BASED_KEY;
    }

    public ProfileBasedRecommendationEngine(String name) {
        this.name = name;
    }

    @Override
    public String getEngineName() {
        return this.name;
    }

    @Override
    public String getEngineKey() {
        return this.key;
    }

    @Override
    public AnalyzerConstants.RecommendationCategory getEngineCategory() {
        return this.category;
    }

    @Override
    public HashMap<String, Recommendation> getRecommendations(ContainerData containerData, Timestamp monitoringEndTime) {
        // TODO: Needs to be implemented
        return null;
    }
}
