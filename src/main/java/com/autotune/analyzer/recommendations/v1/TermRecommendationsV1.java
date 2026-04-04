/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

package com.autotune.analyzer.recommendations.v1;

import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.objects.TermRecommendations;
import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

/**
 * TermRecommendationsV1 extends TermRecommendations to add v1-specific fields.
 * Adds metrics_info field for pod_count metrics (avg, max, min).
 */
public class TermRecommendationsV1 extends TermRecommendations {

    @SerializedName(KruizeConstants.JSONKeys.METRICS_INFO)
    private HashMap<String, MetricAggregationInfoResults> metricsInfo;

    public TermRecommendationsV1() {
        super();
    }

    public TermRecommendationsV1(RecommendationConstants.RecommendationTerms recommendationTerm) {
        super(recommendationTerm);
    }

    public HashMap<String, MetricAggregationInfoResults> getMetricsInfo() {
        return metricsInfo;
    }

    public void setMetricsInfo(HashMap<String, MetricAggregationInfoResults> metricsInfo) {
        this.metricsInfo = metricsInfo;
    }

    /**
     * Add a metric to metrics_info
     */
    public void addMetricInfo(String metricName, MetricAggregationInfoResults metricInfo) {
        if (null == this.metricsInfo) {
            this.metricsInfo = new HashMap<>();
        }
        if (null != metricName && null != metricInfo) {
            this.metricsInfo.put(metricName, metricInfo);
        }
    }
}
