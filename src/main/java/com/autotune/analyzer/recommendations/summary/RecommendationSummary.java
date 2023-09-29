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
    @SerializedName("recommendation_engines")
    private HashMap<String, RecommendationEngineSummary> recommendationEngineSummaryHashMap;
    @SerializedName("notifications_summary")
    private NotificationsSummary notificationsSummary;
    @SerializedName("action_summary")
    private ActionSummary actionSummary;

    public HashMap<String, RecommendationEngineSummary> getRecommendationEngineSummaryHashMap() {
        return recommendationEngineSummaryHashMap;
    }

    public void setRecommendationEngineSummaryHashMap(HashMap<String, RecommendationEngineSummary> recommendationEngineSummaryHashMap) {
        this.recommendationEngineSummaryHashMap = recommendationEngineSummaryHashMap;
    }

    public ActionSummary getActionSummary() {
        return actionSummary;
    }

    public void setActionSummary(ActionSummary actionSummary) {
        this.actionSummary = actionSummary;
    }

    public NotificationsSummary getNotificationsSummary() {
        return notificationsSummary;
    }

    public void setNotificationsSummary(NotificationsSummary notificationsSummary) {
        this.notificationsSummary = notificationsSummary;
    }

    @Override
    public String toString() {
        return "RecommendationSummary{" +
                "recommendationEngineSummaryHashMap=" + recommendationEngineSummaryHashMap +
                ", notificationsSummary=" + notificationsSummary +
                ", actionSummary=" + actionSummary +
                '}';
    }
}
