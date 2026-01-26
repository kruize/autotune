package com.autotune.utils;

import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.operator.KruizeDeploymentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class ProfileCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileCache.class);
    private static volatile ConcurrentHashMap<String, PerformanceProfile> performanceProfileMap;
    private static volatile ConcurrentHashMap<String, PerformanceProfile> metricProfileMap;

    public static void init() {
        if (null == performanceProfileMap || null == metricProfileMap) {
            synchronized (ProfileCache.class) {
                if (null == performanceProfileMap || null == metricProfileMap) {
                    performanceProfileMap = new ConcurrentHashMap<>();
                    metricProfileMap = new ConcurrentHashMap<>();
                    try {
                        if (KruizeDeploymentInfo.is_ros_enabled) { //ROS always deploy Kruize in REMOTE mode only.
                            new ExperimentDBService().loadAllPerformanceProfiles(performanceProfileMap);
                            LOGGER.info("Profile cache is initialized successfully with {} profiles.", performanceProfileMap.size());
                        }
                        if (KruizeDeploymentInfo.local) {
                            new ExperimentDBService().loadAllMetricProfiles(metricProfileMap);
                            LOGGER.info("Metric cache is initialized successfully with {} profiles.", metricProfileMap.size());
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to initialize profile cache.", e);
                    }
                }
            }
        }
    }

    public static boolean isExists(String profileName, ProfileType profileType) {
        init();
        boolean found = false;
        if (ProfileType.PERFORMANCE.equals(profileType)) {
            found  = performanceProfileMap.containsKey(profileName);
            LOGGER.info("Check for performance profile: {}; found: {}", profileName, found);
        } else if (ProfileType.METRIC.equals(profileType)) {
            found = metricProfileMap.containsKey(profileName);
            LOGGER.info("Check for metric profile: {}; found: {}", profileName, found);
        }
        return found;
    }

    public static PerformanceProfile getProfile(String profileName, ProfileType profileType) {
        init();
        PerformanceProfile performanceProfile = null;
        if (ProfileType.PERFORMANCE.equals(profileType)) {
            performanceProfile = performanceProfileMap.get(profileName);
            LOGGER.info("Retrieving performance profile: {}; found: {}", profileName, performanceProfile != null);
        } else if  (ProfileType.METRIC.equals(profileType)) {
            performanceProfile = metricProfileMap.get(profileName);
            LOGGER.info("Retrieving metric profile: {}; found: {}", profileName, performanceProfile != null);
        }
        return performanceProfile;
    }

    public static void removeProfile(String profileName, ProfileType profileType) {
        init();
        PerformanceProfile performanceProfile = null;
        if (ProfileType.PERFORMANCE.equals(profileType)) {
            performanceProfile = performanceProfileMap.remove(profileName);
            LOGGER.info("Removing performance profile: {}; deleted? {}", profileName, performanceProfile!=null);
        } else if (ProfileType.METRIC.equals(profileType)) {
            performanceProfile = metricProfileMap.remove(profileName);
            LOGGER.info("Removing metrics profile: {}; deleted? {}", profileName, performanceProfile!=null);
        }
    }

    public static void addProfile(PerformanceProfile profile, ProfileType profileType) {
        init();
        if (ProfileType.PERFORMANCE.equals(profileType)) {
            LOGGER.info("Adding performance profile : {}", profile.getName());
            performanceProfileMap.put(profile.getName(), profile);
        } else if (ProfileType.METRIC.equals(profileType)) {
            LOGGER.info("Adding metric profile : {}", profile.getName());
            metricProfileMap.put(profile.getName(), profile);
        }
    }

    public static ConcurrentHashMap<String, PerformanceProfile> getProfileMap(ProfileType profileType) {
        init();
        if (ProfileType.PERFORMANCE.equals(profileType)) {
            LOGGER.info("Retrieve performance profile cache. Current size : {}", performanceProfileMap.size());
            return performanceProfileMap;
        } else if (ProfileType.METRIC.equals(profileType)) {
            LOGGER.info("Retrieve metric profile cache. Current size : {}", metricProfileMap.size());
            return metricProfileMap;
        } else {
            return null;
        }
    }
}