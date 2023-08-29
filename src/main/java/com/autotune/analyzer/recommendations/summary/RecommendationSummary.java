/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.recommendations.summary;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.services.Summarize;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Storage object for recommendation summary
 */

public class RecommendationSummary {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationSummary.class);

    @SerializedName(KruizeConstants.JSONKeys.CURRENT)
    private HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentConfig;
    @SerializedName(KruizeConstants.JSONKeys.CONFIG)
    private HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config;
    @SerializedName(KruizeConstants.JSONKeys.CHANGE)
    private HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> change;
    @SerializedName("notifications_summary")
    private NotificationsSummary notificationsSummary;
    @SerializedName("action_summary")
    private ActionSummary actionSummary;

    public ActionSummary getActionSummary() {
        return actionSummary;
    }

    public void setActionSummary(ActionSummary actionSummary) {
        this.actionSummary = actionSummary;
    }

    public HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> getCurrentConfig() {
        return currentConfig;
    }

    public void setCurrentConfig(HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentConfig) {
        this.currentConfig = currentConfig;
    }

    public HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> getConfig() {
        return config;
    }

    public void setConfig(HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config) {
        this.config = config;
    }

    public HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> getChange() {
        return change;
    }

    public void setChange(HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> change) {
        this.change = change;
    }

    public NotificationsSummary getNotificationsSummary() {
        return notificationsSummary;
    }

    public void setNotificationsSummary(NotificationsSummary notificationsSummary) {
        this.notificationsSummary = notificationsSummary;
    }

    // Merge existing values with new ones
    public RecommendationSummary mergeSummaries(RecommendationSummary existingSummary, RecommendationSummary currentSummary) {
        Summarize summarize = new Summarize();
        RecommendationSummary mergedSummary = new RecommendationSummary();
        try {
            mergedSummary.setCurrentConfig(summarize.mergeConfigItems(existingSummary.getCurrentConfig(), currentSummary.getCurrentConfig(), mergedSummary.getCurrentConfig()));
            mergedSummary.setConfig(summarize.mergeConfigItems(existingSummary.getConfig(), currentSummary.getConfig(), mergedSummary.getConfig()));
            mergedSummary.setChange(summarize.mergeChangeObjects(existingSummary, currentSummary));
            mergedSummary.setNotificationsSummary(existingSummary.getNotificationsSummary().mergeNotificationsSummary(existingSummary.getNotificationsSummary(), currentSummary.getNotificationsSummary()));
            mergedSummary.setActionSummary(existingSummary.getActionSummary().merge(currentSummary.getActionSummary()));
        } catch (Exception e){
            LOGGER.error("Exception occurred while merging recommendations: {}", e.getMessage());
        }
        return mergedSummary;
    }


    @Override
    public String toString() {
        return "RecommendationSummary{" +
                "currentConfig=" + currentConfig +
                ", config=" + config +
                ", change=" + change +
                ", notificationsSummary=" + notificationsSummary +
                ", actionSummary=" + actionSummary +
                '}';
    }
}
