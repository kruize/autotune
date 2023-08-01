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
package com.autotune.analyzer.recommendations.summary;

import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Storage object for recommendation
 */

public class RecommendationSummary {

    @SerializedName(KruizeConstants.JSONKeys.CURRENT)
    private HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentConfig;
    @SerializedName(KruizeConstants.JSONKeys.CONFIG)
    private HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config;
    @SerializedName(KruizeConstants.JSONKeys.CHANGE)
    private HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> change;
    private NotificationsSummary notificationsSummary;

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


    // Check if the Recommendation object is empty
    public boolean isEmpty() {
        return this.currentConfig == null &&
                this.config == null &&
                this.change == null &&
                this.notificationsSummary == null;
    }

    // Merge existing values with new ones
    public void mergeValues(RecommendationSummary newRecommendationSum) {
        if (newRecommendationSum.getCurrentConfig() != null) {
            this.currentConfig = newRecommendationSum.getCurrentConfig();
        }
        if (newRecommendationSum.getConfig() != null) {
            this.config = newRecommendationSum.getConfig();
        }
        if (newRecommendationSum.getChange() != null) {
            this.change = newRecommendationSum.change;
        }
        if (newRecommendationSum.notificationsSummary != null) {
            this.notificationsSummary = newRecommendationSum.notificationsSummary;
        }
    }

    @Override
    public String toString() {
        return "RecommendationSummary{" +
                "currentConfig=" + currentConfig +
                ", config=" + config +
                ", change=" + change +
                ", notificationsSummary=" + notificationsSummary +
                '}';
    }
}
