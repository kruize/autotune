package com.autotune.service;

import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.operator.KruizeDeploymentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileService.class);
    private static volatile ConcurrentHashMap<String, PerformanceProfile> performanceProfileMap;
    private static volatile ConcurrentHashMap<String, PerformanceProfile> metricProfileMap;

    private static void init() {
        if (null == performanceProfileMap || null == metricProfileMap) {
            synchronized (ProfileService.class) {
                if (null == performanceProfileMap || null == metricProfileMap) {
                    performanceProfileMap = new ConcurrentHashMap<>();
                    metricProfileMap = new ConcurrentHashMap<>();
                    try {
                        if (KruizeDeploymentInfo.is_ros_enabled) //ROS always deploy Kruize in REMOTE mode only.
                            new ExperimentDBService().loadAllPerformanceProfiles(performanceProfileMap);
                        else
                            new ExperimentDBService().loadAllMetricProfiles(metricProfileMap);
                        LOGGER.info("Profile cache is initialized successfully.");
                    } catch (Exception e) {
                        LOGGER.error("Failed to load performance profiles from database.", e);
                    }
                }
            }
        }
    }

    public static boolean isExists(String profileName) {
        init();
        LOGGER.info("Check for profile : {}", profileName);
        return performanceProfileMap.containsKey(profileName) || metricProfileMap.containsKey(profileName);
    }

    public static PerformanceProfile getPerformanceProfile(String profileName) {
        init();
        LOGGER.info("Retrieve performance profile : {}", profileName);
        return performanceProfileMap.get(profileName);
    }

    public static void removePerformanceProfile(String  performanceProfileName) {
        init();
        LOGGER.info("Delete performance profile : {}", performanceProfileName);
        performanceProfileMap.remove(performanceProfileName);
    }

    public static void addPerformanceProfile(PerformanceProfile performanceProfile) {
        init();
        LOGGER.info("Add performance profile : {}", performanceProfile.getName());
        performanceProfileMap.put(performanceProfile.getName(), performanceProfile);
    }

    public static ConcurrentHashMap<String, PerformanceProfile> getPerformanceProfileMap() {
        init();
        LOGGER.info("Retrieve performance profile cache. Current size : {}", performanceProfileMap.size());
        return performanceProfileMap;
    }

    public static PerformanceProfile getMetricProfile(String metricProfileName) {
        init();
        LOGGER.info("Retrieve metric profile : {}", metricProfileName);
        return metricProfileMap.get(metricProfileName);
    }

    public static boolean removeMetricProfile(String  metricProfileName) {
        init();
        LOGGER.info("Delete metric profile : {}", metricProfileName);
        return metricProfileMap.remove(metricProfileName) != null;
    }

    public static boolean addMetricProfile(PerformanceProfile metricProfile) {
        init();
        LOGGER.info("Add metric profile : {}", metricProfile.getName());
        return metricProfileMap.put(metricProfile.getName(), metricProfile) != null;
    }

    public static ConcurrentHashMap<String, PerformanceProfile> getMetricProfileMap() {
        init();
        LOGGER.info("Retrieve metric profile cache. Current size : {}", metricProfileMap.size());
        return metricProfileMap;
    }
}