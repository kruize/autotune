package com.autotune.service;

import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.operator.KruizeDeploymentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class ProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileService.class);
    private static volatile ConcurrentHashMap<String, PerformanceProfile> performanceProfileMap;
    private static volatile ConcurrentHashMap<String, PerformanceProfile> metricProfileMap;
    private static boolean remoteMode = false;

    private static void init() {
        if (null == performanceProfileMap || null == metricProfileMap) {
            synchronized (ProfileService.class) {
                if (null == performanceProfileMap || null == metricProfileMap) {
                    performanceProfileMap = new ConcurrentHashMap<>();
                    metricProfileMap = new ConcurrentHashMap<>();
                    try {
                        if (KruizeDeploymentInfo.is_ros_enabled && !KruizeDeploymentInfo.local) { //ROS always deploy Kruize in REMOTE mode only.
                            new ExperimentDBService().loadAllPerformanceProfiles(performanceProfileMap);
                            remoteMode = true;
                            LOGGER.info("Profile cache is initialized successfully with {} profiles.", performanceProfileMap.size());
                        } else {
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

    public static boolean isExists(String profileName) {
        init();
        boolean found = false;
        if (remoteMode) {
            found  = performanceProfileMap.containsKey(profileName);
            LOGGER.info("[ProfileService] Check for performance profile: {}; found: {}", profileName, found);
        } else {
            found = metricProfileMap.containsKey(profileName);
            LOGGER.info("[ProfileService] Check for metric profile: {}; found: {}", profileName, found);
        }
        return found;
    }

    public static PerformanceProfile getProfile(String profileName) {
        init();
        PerformanceProfile performanceProfile = null;
        if (remoteMode) {
            performanceProfile = performanceProfileMap.get(profileName);
            LOGGER.info("[ProfileService] Retrieving performance profile: {}; found: {}", profileName, performanceProfile!=null);
        } else {
            performanceProfile = metricProfileMap.get(profileName);
            LOGGER.info("[ProfileService] Retrieving metric profile: {}; found: {}", profileName, performanceProfile!=null);
        }
        return performanceProfile;
    }

    public static void removeProfile(String profileName) {
        init();
        PerformanceProfile performanceProfile = null;
        if (remoteMode) {
            performanceProfile = performanceProfileMap.remove(profileName);
            LOGGER.info("[ProfileService] Removing performance profile: {}; deleted? {}", profileName, performanceProfile!=null);
        } else {
            performanceProfile = metricProfileMap.remove(profileName);
            LOGGER.info("[ProfileService] Removing metrics profile: {}; deleted? {}", profileName, performanceProfile!=null);
        }
    }

    public static void addProfile(PerformanceProfile profile) {
        init();
        if (remoteMode) {
            LOGGER.info("[ProfileService] Adding performance profile : {}", profile.getName());
            performanceProfileMap.put(profile.getName(), profile);
        } else {
            LOGGER.info("[ProfileService] Adding metric profile : {}", profile.getName());
            metricProfileMap.put(profile.getName(), profile);
        }
    }

    public static ConcurrentHashMap<String, PerformanceProfile> getProfileMap() {
        init();
        if (remoteMode) {
            LOGGER.info("[ProfileService] Retrieve performance profile cache. Current size : {}", performanceProfileMap.size());
            return performanceProfileMap;
        } else {
            LOGGER.info("[ProfileService] Retrieve metric profile cache. Current size : {}", metricProfileMap.size());
            return metricProfileMap;
        }
    }
}