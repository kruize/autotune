package com.autotune.analyzer.recommendations.summary;

import java.sql.Timestamp;
import java.util.HashMap;

public class Summary {
    private HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> data;

    private NotificationsSummary notificationsSummary;

    private Namespaces namespaces;
    private Workloads workloads;
    private Clusters clusters;
    private Containers containers;

    public HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> getData() {
        return data;
    }

    public void setData(HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> data) {
        this.data = data;
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
    public Workloads getWorkloads() {
        return workloads;
    }

    public void setWorkloads(Workloads workloads) {
        this.workloads = workloads;
    }

    public Clusters getClusters() {
        return clusters;
    }

    public void setClusters(Clusters clusters) {
        this.clusters = clusters;
    }

    public Containers getContainers() {
        return containers;
    }

    public void setContainers(Containers containers) {
        this.containers = containers;
    }
    @Override
    public String toString() {
        return "Summary{" +
                "data=" + data +
                ", notificationsSummary=" + notificationsSummary +
                ", namespaces=" + namespaces +
                ", workloads=" + workloads +
                ", clusters=" + clusters +
                ", containers=" + containers +
                '}';
    }
}
