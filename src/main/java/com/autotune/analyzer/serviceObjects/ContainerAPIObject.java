package com.autotune.analyzer.serviceObjects;

import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.common.data.metrics.Metric;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Simulating the ContainerData class for the create experiment API
 */
public class ContainerAPIObject {
    private String container_image_name;
    private String container_name;
    @SerializedName(KruizeConstants.JSONKeys.RECOMMENDATIONS)
    private ContainerRecommendations containerRecommendations;
    private List<Metric> metrics;

    public ContainerAPIObject(String container_name, String container_image_name, ContainerRecommendations containerRecommendations, List<Metric> metrics) {
        this.container_name = container_name;
        this.containerRecommendations = containerRecommendations;
        this.container_image_name = container_image_name;
        this.metrics = metrics;
    }

    public String getContainer_image_name() {
        return container_image_name;
    }

    public String getContainer_name() {
        return container_name;
    }

    public ContainerRecommendations getContainerRecommendations() {
        return containerRecommendations;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        return "Container{" +
                "container_image_name='" + container_image_name + '\'' +
                ", container_name='" + container_name + '\'' +
                ", containerRecommendations=" + containerRecommendations +
                ", metrics=" + metrics +
                '}';
    }
}
