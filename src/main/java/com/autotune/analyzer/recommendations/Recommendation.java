/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.analyzer.recommendations;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Storage object for recommendation
 */

public class Recommendation {
    @SerializedName(KruizeConstants.JSONKeys.MONITORING_START_TIME)
    private Timestamp monitoringStartTime;
    @SerializedName(KruizeConstants.JSONKeys.MONITORING_END_TIME)
    private Timestamp monitoringEndTime;
    private Double duration_in_hours;
    @SerializedName(KruizeConstants.JSONKeys.PODS_COUNT)
    private int podsCount;
    private double confidence_level;

    @SerializedName(KruizeConstants.JSONKeys.CONFIG)
    private HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config;
    @SerializedName(KruizeConstants.JSONKeys.VARIATION)
    private HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> variation;
    @SerializedName(KruizeConstants.JSONKeys.NOTIFICATIONS)
    private HashMap<Integer, RecommendationNotification> notifications;

    public Recommendation(Timestamp monitoringStartTime, Timestamp monitoringEndTime) {
        this.monitoringStartTime = monitoringStartTime;
        this.monitoringEndTime = monitoringEndTime;
        notifications = new HashMap<Integer, RecommendationNotification>();
    }

    public Recommendation(RecommendationNotification notification) {
        if (null == notifications)
            notifications = new HashMap<Integer, RecommendationNotification>();
        if (null != notification)
            notifications.put(notification.getCode(), notification);
    }

    public Recommendation() {

    }

    public Timestamp getMonitoringStartTime() {
        return monitoringStartTime;
    }

    public void setMonitoringStartTime(Timestamp monitoringStartTime) {
        this.monitoringStartTime = monitoringStartTime;
    }

    public Timestamp getMonitoringEndTime() {
        return monitoringEndTime;
    }

    public void setMonitoringEndTime(Timestamp monitoringEndTime) {
        this.monitoringEndTime = monitoringEndTime;
    }

    public Double getDuration_in_hours() {
        return duration_in_hours;
    }

    public void setDuration_in_hours(Double duration_in_hours) {
        this.duration_in_hours = duration_in_hours;
    }

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

    public void addNotification(RecommendationNotification notification) {
        if (null != notification)
            this.notifications.put(notification.getCode(), notification);
    }

    public HashMap<Integer, RecommendationNotification> getNotifications() {
        return notifications;
    }

    public void setNotifications(HashMap<Integer, RecommendationNotification> notifications) {
        this.notifications = notifications;
    }

    @Override
    public String toString() {
        return "Recommendation{" +
                "monitoringStartTime=" + monitoringStartTime +
                ", monitoringEndTime=" + monitoringEndTime +
                ", duration_in_hours=" + duration_in_hours +
                ", podsCount=" + podsCount +
                ", confidence_level=" + confidence_level +
                ", config=" + config +
                '}';
    }
}
