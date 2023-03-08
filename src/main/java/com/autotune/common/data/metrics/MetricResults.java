package com.autotune.common.data.metrics;

import com.autotune.common.data.result.AggregationInfoResult;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.AutotuneConstants;
import com.google.gson.annotations.SerializedName;
import org.json.JSONObject;

public class MetricResults {
    @SerializedName("aggregation_info")
    private AggregationInfoResult aggregationInfoResult;
    @SerializedName("percentile_info")
    private MetricPercentileResults metricPercentileResults;
    private Double value;
    private String format;
    private boolean isPercentileResultsAvailable;

    public MetricResults() {
        aggregationInfoResult = new AggregationInfoResult();
        metricPercentileResults = new MetricPercentileResults();
    }

    public MetricResults(JSONObject jsonObject) throws IncompatibleInputJSONException {
        if (!jsonObject.has(AutotuneConstants.JSONKeys.AGGREGATION_INFO) &&
                !jsonObject.has(AutotuneConstants.JSONKeys.PERCENTILE_INFO)) {
            throw new IncompatibleInputJSONException();
        }
        aggregationInfoResult = new AggregationInfoResult(jsonObject.getJSONObject(AutotuneConstants.JSONKeys.AGGREGATION_INFO));
        if (jsonObject.has(AutotuneConstants.JSONKeys.PERCENTILE_INFO)) {
            isPercentileResultsAvailable = true;
            metricPercentileResults = new MetricPercentileResults(jsonObject.getJSONObject(AutotuneConstants.JSONKeys.PERCENTILE_INFO));
        }
    }

    public boolean isPercentileResultsAvailable() {
        return isPercentileResultsAvailable;
    }

    public void setPercentileResultsAvailable(boolean percentileResultsAvailable) {
        isPercentileResultsAvailable = percentileResultsAvailable;
    }
    public AggregationInfoResult getAggregationInfoResult() {
        return aggregationInfoResult;
    }

    public MetricPercentileResults getEmMetricPercentileResults() {
        return metricPercentileResults;
    }

    public void setAggregationInfoResult(AggregationInfoResult aggregationInfoResult) {
        this.aggregationInfoResult = aggregationInfoResult;
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
    @Override
    public String toString() {
        return "MetricResult{" +
                "aggregationInfoResult=" + aggregationInfoResult +
                ", metricPercentileResults=" + metricPercentileResults +
                ", value=" + value +
                ", format='" + format + '\'' +
                ", isPercentileResultsAvailable=" + isPercentileResultsAvailable +
                '}';
    }
}
