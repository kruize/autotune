package com.autotune.analyzer.recommendations.objects;

import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;

import java.sql.Timestamp;
import java.util.HashMap;

public interface MappedRecommendationForTerm {
    public HashMap<Integer, RecommendationNotification> getNotifications();
    public Timestamp getMonitoringStartTime();
    public int getDurationInHrs();
    public MappedRecommendationForEngine getRecommendationByEngine(String EngineName);
}
