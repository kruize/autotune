package com.autotune.analyzer.recommendations.objects;

import com.autotune.analyzer.plots.PlotData;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class TermRecommendations implements MappedRecommendationForTerm {

    public TermRecommendations() {
    }

    @SerializedName(KruizeConstants.JSONKeys.DURATION_IN_HOURS)
    private double durationInHrs;

    @SerializedName(KruizeConstants.JSONKeys.NOTIFICATIONS)
    private HashMap<Integer, RecommendationNotification> termLevelNotificationMap;

    @SerializedName(KruizeConstants.JSONKeys.METRICS_INFO)
    private Map<String, MetricAggregationInfoResults> metricsInfo;

    @SerializedName(KruizeConstants.JSONKeys.MONITORING_START_TIME)
    private Timestamp monitoringStartTime;

    @SerializedName(KruizeConstants.JSONKeys.RECOMMENDATION_ENGINES)
    private Map<String, MappedRecommendationForModel> recommendationForModelMap;

    private PlotData.PlotsData plots;

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

    public Map<String, MappedRecommendationForModel> getRecommendationForModelMap() {
        return recommendationForModelMap;
    }

    public void setRecommendationForModelMap(HashMap<String, MappedRecommendationForModel> recommendationForModelMap) {
        this.recommendationForModelMap = recommendationForModelMap;
    }

    public void setRecommendationForEngineHashMap(String engineName, MappedRecommendationForModel mappedRecommendationForModel) {
        if (null != engineName && null != mappedRecommendationForModel) {
            if (null == this.recommendationForModelMap)
                this.recommendationForModelMap = new HashMap<>();
            this.recommendationForModelMap.put(engineName, mappedRecommendationForModel);
        }
    }

    public MappedRecommendationForModel getCostRecommendations() {
        if (null != this.recommendationForModelMap && this.recommendationForModelMap.containsKey(KruizeConstants.JSONKeys.COST))
            return this.recommendationForModelMap.get(KruizeConstants.JSONKeys.COST);
        return null;
    }

    public MappedRecommendationForModel getPerformanceRecommendations() {
        if (null != this.recommendationForModelMap && this.recommendationForModelMap.containsKey(KruizeConstants.JSONKeys.PERFORMANCE))
            return this.recommendationForModelMap.get(KruizeConstants.JSONKeys.PERFORMANCE);
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

    public void addMetricsInfo(String metricName, MetricAggregationInfoResults metricAggregationInfoResults) {
        if (null != metricName && null != metricAggregationInfoResults) {
            if (null == this.metricsInfo)
                this.metricsInfo = new HashMap<>();
            this.metricsInfo.put(metricName, metricAggregationInfoResults);
        }
    }
}
