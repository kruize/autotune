/*******************************************************************************
 * Copyright (c) 2024 Red Hat, IBM Corporation and others.
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

import com.autotune.analyzer.recommendations.NamespaceRecommendations;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.Metric;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.HashMap;


/**
 * This NamespaceData object is used to store information about namespace-related metrics,
 * data collected from the data source, and recommendations generated for a namespace.
 */
public class NamespaceData {
    @SerializedName(KruizeConstants.JSONKeys.NAMESPACE_NAME)
    private String namespaceName;
    // key for the hashmap is intervalEndTime
    private HashMap<Timestamp, IntervalResults> results;
    @SerializedName(KruizeConstants.JSONKeys.RECOMMENDATIONS)
    private NamespaceRecommendations namespaceRecommendations;
    private HashMap<AnalyzerConstants.MetricName, Metric> metrics;

    public NamespaceData(String namespaceName, NamespaceRecommendations namespaceRecommendations, HashMap<AnalyzerConstants.MetricName, Metric> metrics) {
        this.namespaceName = namespaceName;
        if (null == namespaceRecommendations) {
            namespaceRecommendations = new NamespaceRecommendations();
        }
        this.namespaceRecommendations = namespaceRecommendations;
        this.metrics = metrics;
    }

    public NamespaceData() {
    }

    /**
     * Returns the name of the namespace
     * @return String containing the name of the namespace
     */
    public String getNamespace_name() {
        return namespaceName;
    }

    /**
     * Sets the name of the namespace
     * @param namespace_name String containing the name of the namespace
     */
    public void setNamespace_name(String namespace_name) {
        this.namespaceName = namespace_name;
    }

    /**
     * Returns the hashmap containing the data collected from datasource
     * @return hashmap containing the data collected from data source and intervalEndTime as a key
     */
    public HashMap<Timestamp, IntervalResults> getResults() {
        return results;
    }

    /**
     * Stores the data collected from datasource
     * @param results hashmap containing the data collected from data source and intervalEndTime as a key
     */
    public void setResults(HashMap<Timestamp, IntervalResults> results) {
        this.results = results;
    }

    /**
     * Returns the recommendations object for the namespace
     * @return namespace recommendations object containing the recommendations for a namespace
     */
    public NamespaceRecommendations getNamespaceRecommendations() {
        return namespaceRecommendations;
    }

    /**
     * Stores the recommendations generated for a namespace
     * @param namespaceRecommendations generated recommendations for a namespace
     */
    public void setNamespaceRecommendations(NamespaceRecommendations namespaceRecommendations) {
        this.namespaceRecommendations = namespaceRecommendations;
    }

    /**
     * Returns the namespace related metrics
     * @return hashmap containing the namespace related metrics and metric name as a key
     */
    public HashMap<AnalyzerConstants.MetricName, Metric> getMetrics() {
        return metrics;
    }

    /**
     * Stores the metrics related to namespace
     * @param metrics hashmap conatining metrics related to namespace and metric name as a key
     */
    public void setMetrics(HashMap<AnalyzerConstants.MetricName, Metric> metrics) {
        this.metrics = metrics;
    }

    @Override
    public String toString() {
        return "NamespaceData{" +
                "namespaceName='" + namespaceName + '\'' +
                ", results=" + results +
                ", namespaceRecommendations=" + namespaceRecommendations +
                ", metrics=" + metrics +
                '}';
    }
}
