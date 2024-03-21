package com.autotune.analyzer.recommendations.objects;

import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.HashMap;

public class TermRecommendations implements MappedRecommendationForTerm {

    public TermRecommendations() {
    }

    @SerializedName(KruizeConstants.JSONKeys.DURATION_IN_HOURS)
    private double durationInHrs;

    @SerializedName(KruizeConstants.JSONKeys.NOTIFICATIONS)
    private HashMap<Integer, RecommendationNotification> termLevelNotificationMap;
    @SerializedName(KruizeConstants.JSONKeys.MONITORING_START_TIME)
    private Timestamp monitoringStartTime;

    @SerializedName(KruizeConstants.JSONKeys.RECOMMENDATION_ENGINES)
    private HashMap<String, MappedRecommendationForModel> recommendationForModelHashMap;

    public TermRecommendations(RecommendationConstants.RecommendationTerms recommendationTerm) {
        this.durationInHrs = recommendationTerm.getDuration();
    }

    @Override
    public HashMap<Integer, RecommendationNotification> getNotifications() {
        return this.termLevelNotificationMap;
    }

    @Override
    public Timestamp getMonitoringStartTime() {
        return this.monitoringStartTime;
    }

    @Override
    public double getDurationInHrs() {
        return this.durationInHrs;
    }

    public void setDurationInHrs(double durationInHrs) {
        this.durationInHrs = durationInHrs;
    }

    @Override
    public MappedRecommendationForModel getRecommendationByEngine(String EngineName) {
        return null;
    }

    public void setTermLevelNotificationMap(HashMap<Integer, RecommendationNotification> termLevelNotificationMap) {
        this.termLevelNotificationMap = termLevelNotificationMap;
    }

    public void setMonitoringStartTime(Timestamp monitoringStartTime) {
        this.monitoringStartTime = monitoringStartTime;
    }

    public HashMap<String, MappedRecommendationForModel> getRecommendationForModelHashMap() {
        return recommendationForModelHashMap;
    }

    public void setRecommendationForModelHashMap(HashMap<String, MappedRecommendationForModel> recommendationForModelHashMap) {
        this.recommendationForModelHashMap = recommendationForModelHashMap;
    }

    public void setRecommendationForEngineHashMap(String engineName, MappedRecommendationForModel mappedRecommendationForModel) {
        if (null != engineName && null != mappedRecommendationForModel) {
            if (null == this.recommendationForModelHashMap)
                this.recommendationForModelHashMap = new HashMap<>();
            this.recommendationForModelHashMap.put(engineName, mappedRecommendationForModel);
        }
    }

    public MappedRecommendationForModel getCostRecommendations() {
        if (null != this.recommendationForModelHashMap && this.recommendationForModelHashMap.containsKey(KruizeConstants.JSONKeys.COST))
            return this.recommendationForModelHashMap.get(KruizeConstants.JSONKeys.COST);
        return null;
    }

    public MappedRecommendationForModel getPerformanceRecommendations() {
        if (null != this.recommendationForModelHashMap && this.recommendationForModelHashMap.containsKey(KruizeConstants.JSONKeys.PERFORMANCE))
            return this.recommendationForModelHashMap.get(KruizeConstants.JSONKeys.PERFORMANCE);
        return null;
    }

    public void addNotification(RecommendationNotification recommendationNotification) {
        if (null == this.termLevelNotificationMap)
            this.termLevelNotificationMap = new HashMap<>();

        if (null != recommendationNotification)
            this.termLevelNotificationMap.put(recommendationNotification.getCode(), recommendationNotification);
    }
}
