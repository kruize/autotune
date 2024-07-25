package com.autotune.analyzer.serviceObjects;

import com.autotune.analyzer.recommendations.NamespaceRecommendations;
import com.autotune.common.data.metrics.Metric;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Simulating the Namespace class for the create experiment API
 */
public class NamespaceAPIObject {
    private String namespace_name;
    @SerializedName(KruizeConstants.JSONKeys.RECOMMENDATIONS)
    private NamespaceRecommendations namespaceRecommendations;
    private List<Metric> metrics;

    public NamespaceAPIObject(String namespace_name, NamespaceRecommendations namespaceRecommendations, List<Metric> metrics) {
        this.namespace_name = namespace_name;
        this.namespaceRecommendations = namespaceRecommendations;
        this.metrics = metrics;
    }

    public NamespaceAPIObject() {

    }

    public String getnamespace_name() {
        return namespace_name;
    }

    public NamespaceRecommendations getnamespaceRecommendations() {
        return namespaceRecommendations;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        return "NamespaceAPIObject{" +
                "namespace_name='" + namespace_name + '\'' +
                ", namespaceRecommendations=" + namespaceRecommendations +
                ", metrics=" + metrics +
                '}';
    }
}
