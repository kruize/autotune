package com.autotune.service;

import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.operator.KruizeDeploymentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileService.class);
    private static volatile Map<String, PerformanceProfile> performanceProfileMap;
    private static volatile Map<String, PerformanceProfile> metricProfileMap;

    private static void init() {
        if (null == performanceProfileMap || null == metricProfileMap) {
            synchronized (ProfileService.class) {
                if (null == performanceProfileMap || null == metricProfileMap) {
                    performanceProfileMap = new HashMap<>();
                    metricProfileMap = new HashMap<>();
                    try {
                        if (KruizeDeploymentInfo.is_ros_enabled) //ROS always deploy Kruize in REMOTE mode only.
                            new ExperimentDBService().loadAllPerformanceProfiles(performanceProfileMap);
                        else
                            new ExperimentDBService().loadAllMetricProfiles(metricProfileMap);
                    } catch (Exception e) {
                        LOGGER.error("Failed to load performance profiles from database.", e);
                    }
                }
            }
        }
    }

    public static boolean isExists(String profileName) {
        init();
        return performanceProfileMap.containsKey(profileName) || metricProfileMap.containsKey(profileName);
    }

    public static PerformanceProfile getPerformanceProfile(String profileName) {
        init();
        return performanceProfileMap.get(profileName);
    }

    public static void removePerformanceProfile(String  performanceProfileName) {
        init();
        performanceProfileMap.remove(performanceProfileName);
    }

    public static void addPerformanceProfile(PerformanceProfile performanceProfile) {
        init();
        performanceProfileMap.put(performanceProfile.getName(), performanceProfile);
    }

    public static Map<String, PerformanceProfile> getPerformanceProfileMap() {
        init();
        return performanceProfileMap;
    }

    public static boolean removeMetricProfile(String  metricProfileName) {
        init();
        return metricProfileMap.remove(metricProfileName) != null;
    }

    public static boolean addMetricProfile(PerformanceProfile metricProfile) {
        init();
        return metricProfileMap.put(metricProfile.getName(), metricProfile) != null;
    }

    public static Map<String, PerformanceProfile> getMetricProfileMap() {
        init();
        return metricProfileMap;
    }
}