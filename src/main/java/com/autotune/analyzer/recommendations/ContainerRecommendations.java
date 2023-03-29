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

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.*;

public class ContainerRecommendations {
    @SerializedName(KruizeConstants.JSONKeys.NOTIFICATIONS)
    private List<RecommendationNotification> notifications;
    @SerializedName(KruizeConstants.JSONKeys.DATA)
    private HashMap<Timestamp, HashMap<String, HashMap<String, Recommendation>>> data;

    public ContainerRecommendations() {
        this.notifications = Collections.synchronizedList(new ArrayList<RecommendationNotification>());
        this.data = new HashMap<Timestamp, HashMap<String, HashMap<String, Recommendation>>>();

        RecommendationNotification recommendationNotification = new RecommendationNotification(
                AnalyzerConstants.RecommendationNotificationTypes.INFO.getName(),
                AnalyzerConstants.RecommendationNotificationMsgConstant.NOT_ENOUGH_DATA
        );
        this.notifications.add(recommendationNotification);
    }

    public List<RecommendationNotification> getNotifications() {
        return notifications;
    }

    public HashMap<Timestamp, HashMap<String, HashMap<String, Recommendation>>> getData() {
        return data;
    }

    public void setData(HashMap<Timestamp, HashMap<String, HashMap<String, Recommendation>>> data) {
        if (!data.isEmpty())
            if (this.notifications.get(0).getMessage().equalsIgnoreCase(AnalyzerConstants.RecommendationNotificationMsgConstant.NOT_ENOUGH_DATA))
                this.notifications.remove(0);
        this.data = data;
    }

    @Override
    public String toString() {
        return "ContainerRecommendations{" +
                "notifications=" + notifications +
                ", data=" + data +
                '}';
    }
}
