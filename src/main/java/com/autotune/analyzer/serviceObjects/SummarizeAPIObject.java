package com.autotune.analyzer.serviceObjects;

import com.autotune.analyzer.recommendations.summary.RecommendationSummary;
import com.autotune.analyzer.recommendations.summary.Summary;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.HashMap;

public class SummarizeAPIObject {

    @SerializedName(KruizeConstants.JSONKeys.CLUSTER_NAME)
    private String clusterName;

    @SerializedName(KruizeConstants.JSONKeys.NAMESPACE)
    private String namespace;

    @SerializedName(KruizeConstants.JSONKeys.SUMMARY)
    private Summary summary;

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

    @Override
    public String toString() {
        return "SummarizeAPIObject{" +
                "clusterName='" + clusterName + '\'' +
                ", namespace='" + namespace + '\'' +
                ", summary=" + summary +
                '}';
    }
}
