package com.autotune.analyzer.utils;

import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;

/**
 * CPUUtilizationAnalyzer is a class to analyze the CPU utilization based on current and recommended values.
 */
public class CPUMEMORYUtilizationAnalyzer {
    /**
     * Analyzes the state of the CPU based on current and recommended CPU values.
     *
     * @param currentRequest     The current request CPU value.
     * @param currentLimit       The current limit CPU value.
     * @param recommendedRequest The recommended request CPU value.
     * @param recommendedLimit   The recommended limit CPU value.
     * @param threshold          The threshold value for determining overutilized and underutilized states.
     * @return The state of the CPU: "Overutilized", "Underutilized", or "Idle".
     */
    // Method to determine CPU state based on utilization ratio
    public static RecommendationNotification analyzeCPUState(double currentRequest, double currentLimit,
                                                             double recommendedRequest, double recommendedLimit,
                                                             double threshold) {
        // Calculate utilization ratios
        double currentUtilizationRatio = (currentRequest / currentLimit) * 100;
        double recommendedUtilizationRatio = (recommendedRequest / recommendedLimit) * 100;

        // Calculate thresholds for overutilized and underutilized states
        double overutilizedThreshold = recommendedUtilizationRatio + threshold;
        double underutilizedThreshold = recommendedUtilizationRatio - threshold;

        // Determine CPU state
        if (currentUtilizationRatio > overutilizedThreshold) {
            return new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_CPU_OVER_UTILIZED);
        } else if (currentUtilizationRatio < underutilizedThreshold) {
            return new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_CPU_UNDER_UTILIZED);
        } else {
            return null;
        }
    }

    // Method to determine Memory state based on utilization ratio
    public static RecommendationNotification analyzeMEMORYState(double currentRequest, double currentLimit,
                                                                double recommendedRequest, double recommendedLimit,
                                                                double threshold) {
        // Calculate utilization ratios
        double currentUtilizationRatio = (currentRequest / currentLimit) * 100;
        double recommendedUtilizationRatio = (recommendedRequest / recommendedLimit) * 100;

        // Calculate thresholds for overutilized and underutilized states
        double overutilizedThreshold = recommendedUtilizationRatio + threshold;
        double underutilizedThreshold = recommendedUtilizationRatio - threshold;

        // Determine CPU state
        if (currentUtilizationRatio > overutilizedThreshold) {
            return new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_MEMORY_OVER_UTILIZED);
        } else if (currentUtilizationRatio < underutilizedThreshold) {
            return new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_MEMORY_UNDER_UTILIZED);
        } else {
            return null;
        }
    }

}
