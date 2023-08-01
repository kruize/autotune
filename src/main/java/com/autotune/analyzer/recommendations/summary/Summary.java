package com.autotune.analyzer.recommendations.summary;

import java.sql.Timestamp;
import java.util.HashMap;

public class Summary {
    private HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> data;

    private NotificationsSummary notificationsSummary;

    private Namespaces namespaces;

    public HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> getData() {
        return data;
    }

    public void setData(HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> data) {
        this.data = data;
    }

    public NotificationsSummary getNotificationsSummary() {
        return notificationsSummary;
    }

    public void setNotificationsSummary(NotificationsSummary notificationsSummary) {
        this.notificationsSummary = notificationsSummary;
    }

    public Namespaces getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(Namespaces namespaces) {
        this.namespaces = namespaces;
    }
}
