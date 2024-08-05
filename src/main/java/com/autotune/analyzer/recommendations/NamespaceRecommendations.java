/*******************************************************************************
 * Copyright (c) 2024 Red Hat, IBM Corporation and others.
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

/**
 * This NamespaceRecommendations object is used to store recommendations generated for a namespace
 */
public class NamespaceRecommendations {
    private String version;
    @SerializedName(KruizeConstants.JSONKeys.NOTIFICATIONS)
    private HashMap<Integer, RecommendationNotification> notificationMap;
    @SerializedName(KruizeConstants.JSONKeys.DATA)
    private HashMap<Timestamp, MappedRecommendationForTimestamp> data;

    public NamespaceRecommendations() {
        this.notificationMap = new HashMap<Integer, RecommendationNotification>();
        this.data = new HashMap<Timestamp, MappedRecommendationForTimestamp>();
        this.version = KruizeConstants.KRUIZE_RECOMMENDATION_API_VERSION.LATEST.getVersionNumber();
        RecommendationNotification recommendationNotification = new RecommendationNotification(
                RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA
        );
        this.notificationMap.put(recommendationNotification.getCode(), recommendationNotification);
    }

    /**
     * Returns the namespace recommendation data
     * @return hashmap containing the namespace recommendation data and monitoring end time as a key
     */
    public HashMap<Timestamp, MappedRecommendationForTimestamp> getData() {
        return data;
    }

    /**
     * Stores the namespace recommendation data
     * @param data hashmap containing the namespace recommendation data and monitoring end time as a key
     */
    public void setData(HashMap<Timestamp, MappedRecommendationForTimestamp> data) {
        if (!data.isEmpty()) {
            if (this.notificationMap.containsKey(RecommendationConstants.NotificationCodes.INFO_NOT_ENOUGH_DATA)) {
                this.notificationMap.remove(RecommendationConstants.NotificationCodes.INFO_NOT_ENOUGH_DATA);
            }
        }
        this.data = data;
    }

    /**
     * Returns the version for recommendations
     * @return string containing the version for the recommendations
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version for recommendations
     * @param version string containing the version for the recommendations
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the recommendation notifications
     * @return hashmap containing the recommendation notifications
     */
    public HashMap<Integer, RecommendationNotification> getNotificationMap() {
        return notificationMap;
    }

    /**
     * Sets the recommendation notifications
     * @param notificationMap hashmap containing the recommendation notifications
     */
    public void setNotificationMap(HashMap<Integer, RecommendationNotification> notificationMap) {
        this.notificationMap = notificationMap;
    }

    @Override
    public String toString() {
        return "NamespaceRecommendations{" +
                "version='" + version + '\'' +
                ", notificationMap=" + notificationMap +
                ", data=" + data +
                '}';
    }
}
