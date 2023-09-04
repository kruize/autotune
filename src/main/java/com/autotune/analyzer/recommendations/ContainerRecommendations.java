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

package com.autotune.analyzer.recommendations;

import com.autotune.analyzer.recommendations.objects.MappedRecommendationForTimestamp;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.HashMap;

public class ContainerRecommendations {
    private String version;
    @SerializedName(KruizeConstants.JSONKeys.NOTIFICATIONS)
    private HashMap<Integer, RecommendationNotification> notificationMap;
    @SerializedName(KruizeConstants.JSONKeys.DATA)
    private HashMap<Timestamp, MappedRecommendationForTimestamp> data;

    public ContainerRecommendations() {
        this.notificationMap = new HashMap<Integer, RecommendationNotification>();
        this.data = new HashMap<Timestamp, MappedRecommendationForTimestamp>();
        this.version = KruizeConstants.KRUIZE_RECOMMENDATION_API_VERSION.LATEST.getVersionNumber();
        RecommendationNotification recommendationNotification = new RecommendationNotification(
                RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA
        );
        this.notificationMap.put(recommendationNotification.getCode(), recommendationNotification);
    }

    public HashMap<Integer, RecommendationNotification> getNotificationMap() {
        return notificationMap;
    }

    public HashMap<Timestamp, MappedRecommendationForTimestamp> getData() {
        return data;
    }

    public void setData(HashMap<Timestamp, MappedRecommendationForTimestamp> data) {
        if (!data.isEmpty())
            if (this.notificationMap.containsKey(RecommendationConstants.NotificationCodes.INFO_NOT_ENOUGH_DATA))
                this.notificationMap.remove(RecommendationConstants.NotificationCodes.INFO_NOT_ENOUGH_DATA);
        this.data = data;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "ContainerRecommendations{" +
                "notifications=" + notificationMap +
                ", data=" + data +
                '}';
    }
}
