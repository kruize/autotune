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
package com.autotune.common.data.metrics;

import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;
import org.json.JSONObject;

public class MetricResults {
    private String name;
    @SerializedName("aggregation_info")
    private MetricAggregationInfoResults metricAggregationInfoResults;
    @SerializedName("percentile_info")
    private MetricPercentileResults metricPercentileResults;
    private Double value;
    private String format;
    private boolean isPercentileResultsAvailable;

    public MetricResults() {
        metricAggregationInfoResults = new MetricAggregationInfoResults();
        metricPercentileResults = new MetricPercentileResults();
    }

    public MetricResults(JSONObject jsonObject) throws IncompatibleInputJSONException {
        if (!jsonObject.has(KruizeConstants.JSONKeys.AGGREGATION_INFO) &&
                !jsonObject.has(KruizeConstants.JSONKeys.PERCENTILE_INFO)) {
            throw new IncompatibleInputJSONException();
        }
        metricAggregationInfoResults = new MetricAggregationInfoResults(jsonObject.getJSONObject(KruizeConstants.JSONKeys.AGGREGATION_INFO));
        if (jsonObject.has(KruizeConstants.JSONKeys.PERCENTILE_INFO)) {
            isPercentileResultsAvailable = true;
            metricPercentileResults = new MetricPercentileResults(jsonObject.getJSONObject(KruizeConstants.JSONKeys.PERCENTILE_INFO));
        }
    }

    public boolean isPercentileResultsAvailable() {
        return isPercentileResultsAvailable;
    }

    public void setPercentileResultsAvailable(boolean percentileResultsAvailable) {
        isPercentileResultsAvailable = percentileResultsAvailable;
    }
    public MetricAggregationInfoResults getAggregationInfoResult() {
        return metricAggregationInfoResults;
    }

    public MetricPercentileResults getEmMetricPercentileResults() {
        return metricPercentileResults;
    }

    public void setAggregationInfoResult(MetricAggregationInfoResults metricAggregationInfoResults) {
        this.metricAggregationInfoResults = metricAggregationInfoResults;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    @Override
    public String toString() {
        return "MetricResults{" +
                "name='" + name + '\'' +
                ", metricAggregationInfoResults=" + metricAggregationInfoResults +
                ", metricPercentileResults=" + metricPercentileResults +
                ", value=" + value +
                ", format='" + format + '\'' +
                ", isPercentileResultsAvailable=" + isPercentileResultsAvailable +
                '}';
    }
}
