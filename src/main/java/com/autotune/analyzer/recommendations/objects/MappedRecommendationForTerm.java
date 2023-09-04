package com.autotune.analyzer.recommendations.objects;

import com.autotune.analyzer.recommendations.RecommendationConstants;

import java.sql.Timestamp;
import java.util.HashMap;

public interface MappedRecommendationForTerm {
    public HashMap<Integer, RecommendationConstants.RecommendationNotification> getNotifications();
    public Timestamp getMonitoringStartTime();
    public int getDurationInHrs();
    public MappedRecommendationForEngine getRecommendationByEngine(String EngineName);
}
