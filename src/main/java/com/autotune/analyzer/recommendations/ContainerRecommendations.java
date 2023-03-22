package com.autotune.analyzer.recommendations;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ContainerRecommendations {
    @SerializedName(KruizeConstants.JSONKeys.NOTIFICATIONS)
    private List<RecommendationNotification> notifications;
    @SerializedName(KruizeConstants.JSONKeys.DATA)
    private HashMap<Timestamp, HashMap<String, HashMap<String, Recommendation>>> data;

    public ContainerRecommendations() {
        this.notifications = new ArrayList<RecommendationNotification>();
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

    public void setNotifications(List<RecommendationNotification> notifications) {
        this.notifications = notifications;
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
}
