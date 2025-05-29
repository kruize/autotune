package com.autotune.analyzer.serviceObjects;

import com.autotune.analyzer.recommendations.NamespaceRecommendations;
import com.autotune.common.data.metrics.Metric;
import com.autotune.utils.KruizeConstants;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * This NamespaceAPIObject class simulates the NamespaceData class for the create experiment and list experiment API
 */
public class NamespaceAPIObject {
    @SerializedName(KruizeConstants.JSONKeys.NAMESPACE)
    private String namespace;
    @SerializedName(KruizeConstants.JSONKeys.RECOMMENDATIONS)
    private NamespaceRecommendations namespaceRecommendations;
    private List<Metric> metrics;

    public NamespaceAPIObject(String namespace, NamespaceRecommendations namespaceRecommendations, List<Metric> metrics) {
        this.namespace = namespace;
        this.namespaceRecommendations = namespaceRecommendations;
        this.metrics = metrics;
    }

    public NamespaceAPIObject() {
    }

    /**
     * Returns the name of the namespace
     * @return String containing the name of the namespace
     */
    @JsonProperty(KruizeConstants.JSONKeys.NAMESPACE)
    public String getnamespace() {
        return namespace;
    }

    /**
     * Returns the recommendations object for the namespace
     * @return namespace recommendations object containing the recommendations for a namespace
     */
    @JsonProperty(KruizeConstants.JSONKeys.RECOMMENDATIONS)
    public NamespaceRecommendations getnamespaceRecommendations() {
        return namespaceRecommendations;
    }

    /**
     * Returns the namespace related metrics
     * @return hashmap containing the namespace related metrics and metric name as a key
     */
    public List<Metric> getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        return "NamespaceObject{" +
                "namespace='" + namespace + '\'' +
                ", namespaceRecommendations=" + namespaceRecommendations +
                ", metrics=" + metrics +
                '}';
    }
}
