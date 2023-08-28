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

package com.autotune.analyzer.serviceObjects;

import com.autotune.analyzer.recommendations.summary.NotificationsSummary;
import com.autotune.analyzer.recommendations.summary.ResourceInfo;
import com.autotune.analyzer.recommendations.summary.Summary;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class SummarizeAPIObject {

    @SerializedName(KruizeConstants.JSONKeys.CLUSTER_NAME)
    private String clusterName;

    @SerializedName(KruizeConstants.JSONKeys.NAMESPACE)
    private String namespace;

    @SerializedName(KruizeConstants.JSONKeys.SUMMARY)
    private Summary summary;

    @SerializedName("notifications_summary")
    private NotificationsSummary notificationsSummary;
    @SerializedName("action_summary")
    HashMap<String, HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo>> actionSummaryTopLevel;

    private HashMap<String, Object> namespaces;
    private HashMap<String, Object> workloads;
    private HashMap<String, Object> clusters;
    private HashMap<String, Object> containers;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public NotificationsSummary getNotificationsSummary() {
        return notificationsSummary;
    }

    public void setNotificationsSummary(NotificationsSummary notificationsSummary) {
        this.notificationsSummary = notificationsSummary;
    }

    public HashMap<String, HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo>> getActionSummaryTopLevel() {
        return actionSummaryTopLevel;
    }

    public void setActionSummaryTopLevel(HashMap<String, HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo>> actionSummaryTopLevel) {
        this.actionSummaryTopLevel = actionSummaryTopLevel;
    }

    public HashMap<String, Object> getWorkloads() {
        return workloads;
    }

    public void setWorkloads(HashMap<String, Object> workloads) {
        this.workloads = workloads;
    }

    public HashMap<String, Object> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(HashMap<String, Object> namespaces) {
        this.namespaces = namespaces;
    }

    public HashMap<String, Object> getClusters() {
        return clusters;
    }

    public void setClusters(HashMap<String, Object> clusters) {
        this.clusters = clusters;
    }

    public HashMap<String, Object> getContainers() {
        return containers;
    }

    public void setContainers(HashMap<String, Object> containers) {
        this.containers = containers;
    }

    @Override
    public String toString() {
        return "SummarizeAPIObject{" +
                "clusterName='" + clusterName + '\'' +
                ", namespace='" + namespace + '\'' +
                ", summary=" + summary +
                '}';
    }
}
