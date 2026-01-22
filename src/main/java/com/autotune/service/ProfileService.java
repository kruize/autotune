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
        if (remoteMode) {
            LOGGER.info("[ProfileService] Check for performance profile : {}", profileName);
            return performanceProfileMap.containsKey(profileName);
        } else {
            LOGGER.info("[ProfileService] Check for metric profile : {}", profileName);
            return metricProfileMap.containsKey(profileName);
        }
    }

    public static PerformanceProfile getProfile(String profileName) {
        init();
        if (remoteMode) {
            LOGGER.info("[ProfileService] Check for performance profile : {}", profileName);
            return performanceProfileMap.get(profileName);
        } else {
            LOGGER.info("[ProfileService] Check for metric profile : {}", profileName);
            return metricProfileMap.get(profileName);
        }
    }

    public static PerformanceProfile removeProfile(String profileName) {
        init();
        if (remoteMode) {
            LOGGER.info("[ProfileService] Removing performance profile : {}", profileName);
            return performanceProfileMap.remove(profileName);
        } else {
            LOGGER.info("[ProfileService] Removing metric profile : {}", profileName);
            return metricProfileMap.remove(profileName);
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