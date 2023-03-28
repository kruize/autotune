/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.data.result;

import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.Metric;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.HashMap;

public class ContainerData {
    private String container_image_name;
    private String container_name;
    //key is IntervalEnd
    private HashMap<Timestamp, IntervalResults> results;
    @SerializedName(KruizeConstants.JSONKeys.RECOMMENDATIONS)
    private ContainerRecommendations containerRecommendations;
    private HashMap<AnalyzerConstants.MetricName, Metric> metrics;

    public ContainerData(String container_name, String container_image_name, HashMap<AnalyzerConstants.MetricName, Metric> metrics) {
        this.container_name = container_name;
        containerRecommendations = new ContainerRecommendations();
        this.container_image_name = container_image_name;
        this.metrics = metrics;
    }

    public ContainerData() {

    }

    public String getContainer_image_name() {
        return container_image_name;
    }

    public void setContainer_image_name(String container_image_name) {
        this.container_image_name = container_image_name;
    }

    public String getContainer_name() {
        return container_name;
    }

    public void setContainer_name(String container_name) {
        this.container_name = container_name;
    }

    public HashMap<Timestamp, IntervalResults> getResults() {
        return results;
    }

    public void setResults(HashMap<Timestamp, IntervalResults> results) {
        this.results = results;
    }

    public ContainerRecommendations getContainerRecommendations() {
        return containerRecommendations;
    }

    public void setContainerRecommendations(ContainerRecommendations containerRecommendations) {
        this.containerRecommendations = containerRecommendations;
    }

    public HashMap<AnalyzerConstants.MetricName, Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(HashMap<AnalyzerConstants.MetricName, Metric> metrics) {
        this.metrics = metrics;
    }
    @Override
    public String toString() {
        return "ContainerData{" +
                "container_image_name='" + container_image_name + '\'' +
                ", container_name='" + container_name + '\'' +
                ", results=" + results +
                ", recommendations=" + containerRecommendations +
                ", metrics=" + metrics +
                '}';
    }
}
