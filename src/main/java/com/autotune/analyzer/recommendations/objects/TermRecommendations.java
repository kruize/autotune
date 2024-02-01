package com.autotune.analyzer.recommendations.objects;

import com.autotune.analyzer.plots.PlotData;
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

    public TermRecommendations(RecommendationConstants.RecommendationTerms recommendationTerm) {
        this.durationInHrs = recommendationTerm.getDuration();
    }
    @SerializedName(KruizeConstants.JSONKeys.NOTIFICATIONS)
    private HashMap<Integer, RecommendationNotification> termLevelNotificationMap;

    @SerializedName(KruizeConstants.JSONKeys.MONITORING_START_TIME)
    private Timestamp monitoringStartTime;

    @SerializedName(KruizeConstants.JSONKeys.RECOMMENDATION_ENGINES)
    private HashMap<String, MappedRecommendationForEngine> recommendationForEngineHashMap;

    //@SerializedName(KruizeConstants.JSONKeys.PLOTS)
    private PlotData.PlotsData plots;

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
    public MappedRecommendationForEngine getRecommendationByEngine(String EngineName) {
        return null;
    }

    public void setTermLevelNotificationMap(HashMap<Integer, RecommendationNotification> termLevelNotificationMap) {
        this.termLevelNotificationMap = termLevelNotificationMap;
    }

    public void setMonitoringStartTime(Timestamp monitoringStartTime) {
        this.monitoringStartTime = monitoringStartTime;
    }

    public HashMap<String, MappedRecommendationForEngine> getRecommendationForEngineHashMap() {
        return recommendationForEngineHashMap;
    }

    public void setRecommendationForEngineHashMap(HashMap<String, MappedRecommendationForEngine> recommendationForEngineHashMap) {
        this.recommendationForEngineHashMap = recommendationForEngineHashMap;
    }

    public void setRecommendationForEngineHashMap(String engineName, MappedRecommendationForEngine mappedRecommendationForEngine) {
        if (null != engineName && null != mappedRecommendationForEngine) {
            if (null == this.recommendationForEngineHashMap)
                this.recommendationForEngineHashMap = new HashMap<>();
            this.recommendationForEngineHashMap.put(engineName, mappedRecommendationForEngine);
        }
    }

    public MappedRecommendationForEngine getCostRecommendations() {
        if (null != this.recommendationForEngineHashMap && this.recommendationForEngineHashMap.containsKey(KruizeConstants.JSONKeys.COST))
            return this.recommendationForEngineHashMap.get(KruizeConstants.JSONKeys.COST);
        return null;
    }

    public MappedRecommendationForEngine getPerformanceRecommendations() {
        if (null != this.recommendationForEngineHashMap && this.recommendationForEngineHashMap.containsKey(KruizeConstants.JSONKeys.PERFORMANCE))
            return this.recommendationForEngineHashMap.get(KruizeConstants.JSONKeys.PERFORMANCE);
        return null;
    }

    public void addNotification(RecommendationNotification recommendationNotification) {
        if (null == this.termLevelNotificationMap)
            this.termLevelNotificationMap = new HashMap<>();

        if (null != recommendationNotification)
            this.termLevelNotificationMap.put(recommendationNotification.getCode(), recommendationNotification);
    }

    public PlotData.PlotsData getPlots() {
        return plots;
    }

    public void setPlots(PlotData.PlotsData plots) {
        this.plots = plots;
    }
}
