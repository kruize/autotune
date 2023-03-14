package com.autotune.common.utils;

import com.autotune.common.recommendation.engine.KruizeRecommendationEngine;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class KruizeLocalCache {
    private static volatile KruizeLocalCache kruizeLocalCache = null;
    private volatile ConcurrentHashMap<String, List<KruizeRecommendationEngine>> recommendationEngineMap;

    private KruizeLocalCache() {
        recommendationEngineMap = new ConcurrentHashMap<String, List<KruizeRecommendationEngine>>();
    }

    public static KruizeLocalCache getInstance() {
        if (null == kruizeLocalCache) {
            synchronized (KruizeLocalCache.class) {
                if (null == kruizeLocalCache) {
                    kruizeLocalCache = new KruizeLocalCache();
                }
            }
        }
        return kruizeLocalCache;
    }

    public List<KruizeRecommendationEngine> getRecommendationEngine(String performanceProfile) {
        if (!recommendationEngineMap.containsKey(performanceProfile))
            return null;
        return recommendationEngineMap.get(performanceProfile);
    }

    // If the caller makes a forced update check if Overwritten status is returned
    public CommonUtils.RecommendationEngineUtils.SetEngineOutputStatus setRecommendationEngine(String performanceProfile,
                                                                                               List<KruizeRecommendationEngine> kruizeRecommendationEngine,
                                                                                               boolean force) {
        if (null == performanceProfile)
            return CommonUtils.RecommendationEngineUtils.SetEngineOutputStatus.FAILURE;

        if (null == kruizeRecommendationEngine)
            return CommonUtils.RecommendationEngineUtils.SetEngineOutputStatus.FAILURE;

        if (!force) {
            if (recommendationEngineMap.containsKey(performanceProfile)) {
                return CommonUtils.RecommendationEngineUtils.SetEngineOutputStatus.DUPLICATE_FOUND;
            }
        }

        recommendationEngineMap.put(performanceProfile, kruizeRecommendationEngine);
        if (force)
            return CommonUtils.RecommendationEngineUtils.SetEngineOutputStatus.OVERWRITTEN;
        return CommonUtils.RecommendationEngineUtils.SetEngineOutputStatus.SUCCESS;
    }
}
