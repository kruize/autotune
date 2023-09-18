package com.autotune.analyzer.recommendations.utils;

public class RecommendationUtils {
    public static int getThreshold(int value, int failoverPercentage, boolean direction) {
        if (direction) {
            return Math.round(value + value * (failoverPercentage / 100.0f));
        } else {
            return Math.round(value - value * (failoverPercentage / 100.0f));
        }
    }
}