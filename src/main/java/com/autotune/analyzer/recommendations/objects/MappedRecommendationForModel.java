package com.autotune.analyzer.recommendations.objects;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class MappedRecommendationForModel {

    public MappedRecommendationForModel() {
        this.podsCount = 0;
        this.confidence_level = 0.0;
        this.config = new HashMap<>();
        this.variation = new HashMap<>();
        this.notificationHashMap = new HashMap<>();
    }

    @SerializedName(KruizeConstants.JSONKeys.PODS_COUNT)
    private int podsCount;
    @SerializedName(KruizeConstants.JSONKeys.CONFIDENCE_LEVEL)
    private double confidence_level;
    @SerializedName(KruizeConstants.JSONKeys.CONFIG)
    private HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config;
    @SerializedName(KruizeConstants.JSONKeys.VARIATION)
    private HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> variation;

    @SerializedName(KruizeConstants.JSONKeys.NOTIFICATIONS)
    private HashMap<Integer, RecommendationNotification> notificationHashMap;

    public int getPodsCount() {
        return podsCount;
    }

    public void setPodsCount(int podsCount) {
        this.podsCount = podsCount;
    }

    public double getConfidence_level() {
        return confidence_level;
    }

    public void setConfidence_level(double confidence_level) {
        this.confidence_level = confidence_level;
    }

    public HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> getConfig() {
        return config;
    }

    public void setConfig(HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config) {
        this.config = config;
    }

    public HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> getVariation() {
        return variation;
    }

    public void setVariation(HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> variation) {
        this.variation = variation;
    }

    public HashMap<Integer, RecommendationNotification> getNotificationHashMap() {
        return notificationHashMap;
    }

    public void setNotificationHashMap(HashMap<Integer, RecommendationNotification> notificationHashMap) {
        this.notificationHashMap = notificationHashMap;
    }

    public void addNotification(RecommendationNotification recommendationNotification) {
        if (null == this.notificationHashMap)
            this.notificationHashMap = new HashMap<>();
        if (null != recommendationNotification)
            if (!this.notificationHashMap.containsKey(recommendationNotification.getCode()))
                this.notificationHashMap.put(recommendationNotification.getCode(), recommendationNotification);
    }
}
