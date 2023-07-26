package com.autotune.analyzer.serviceObjects;

import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.HashMap;

public class SummarizeAPIObject {

    @SerializedName(KruizeConstants.JSONKeys.CLUSTER_NAME)
    private String clusterName;

    @SerializedName(KruizeConstants.JSONKeys.DATA)
    private HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> data;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> getData() {
        return data;
    }

    public void setData(HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "SummarizeAPIObject{" +
                "clusterName='" + clusterName + '\'' +
                ", data=" + data +
                '}';
    }
}
