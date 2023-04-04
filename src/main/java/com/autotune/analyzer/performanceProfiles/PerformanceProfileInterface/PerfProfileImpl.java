package com.autotune.analyzer.performanceProfiles.PerformanceProfileInterface;

import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.recommendations.engine.KruizeRecommendationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class PerfProfileImpl implements PerfProfileInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerfProfileImpl.class);

    @Override
    public String getName(PerformanceProfile performanceProfile) {
        return AnalyzerConstants.PerformanceProfileConstants.PerfProfileNames.get(performanceProfile.getName());
    }

    public abstract AnalyzerConstants.RegisterRecommendationEngineStatus registerEngine(KruizeRecommendationEngine kruizeRecommendationEngine);
    public abstract List<KruizeRecommendationEngine> getEngines();
}
