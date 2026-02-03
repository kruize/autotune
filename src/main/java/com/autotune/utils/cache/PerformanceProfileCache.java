package com.autotune.utils.cache;

import com.autotune.analyzer.performanceProfiles.PerformanceProfile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PerformanceProfileCache {
    private static final Map<String, PerformanceProfile> performanceProfileMap = new ConcurrentHashMap<>();

    private PerformanceProfileCache() {

    }

    public static boolean exists(String performanceProfileName) {
        return performanceProfileMap.containsKey(performanceProfileName);
    }

    public static PerformanceProfile get(String performanceProfileName) {
        return performanceProfileMap.get(performanceProfileName);
    }

    public static void put(String performanceProfileName, PerformanceProfile performanceProfile) {
        performanceProfileMap.put(performanceProfileName, performanceProfile);
    }

    public static void remove(String performanceProfileName) {
        performanceProfileMap.remove(performanceProfileName);
    }
}