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

public class NamespaceData {
    private String namespace_name;
    //key is intervalEndTime
    private HashMap<Timestamp, IntervalResults> results;
    private NamespaceRecommendations namespaceRecommendations;
    private HashMap<AnalyzerConstants.MetricName, Metric> metrics;

    public NamespaceData(String namespace_name, NamespaceRecommendations namespaceRecommendations, HashMap<AnalyzerConstants.MetricName, Metric> metrics) {
        this.namespace_name = namespace_name;
        if (null == namespaceRecommendations)
            namespaceRecommendations = new NamespaceRecommendations();
        this.namespaceRecommendations = namespaceRecommendations;
        this.metrics = metrics;
    }

    public NamespaceData() {

    }

    public String getNamespace_name() {
        return namespace_name;
    }

    public void setNamespace_name(String namespace_name) {
        this.namespace_name = namespace_name;
    }

    public HashMap<Timestamp, IntervalResults> getResults() {
        return results;
    }

    public void setResults(HashMap<Timestamp, IntervalResults> results) {
        this.results = results;
    }

    public NamespaceRecommendations getNamespaceRecommendations() {
        return namespaceRecommendations;
    }

    public void setNamespaceRecommendations(NamespaceRecommendations namespaceRecommendations) {
        this.namespaceRecommendations = namespaceRecommendations;
    }

    public HashMap<AnalyzerConstants.MetricName, Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(HashMap<AnalyzerConstants.MetricName, Metric> metrics) {
        this.metrics = metrics;
    }

    @Override
    public String toString() {
        return "NamespaceData{" +
                "namespace_name='" + namespace_name + '\'' +
                ", results=" + results +
                ", namespaceRecommendations=" + namespaceRecommendations +
                ", metrics=" + metrics +
                '}';
    }
}
