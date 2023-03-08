package com.autotune.common.data.metrics;

import com.autotune.common.data.result.AggregationInfoResult;
import com.autotune.common.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.AutotuneConstants;
import com.google.gson.annotations.SerializedName;
import org.json.JSONObject;

public class MetricResult implements ConvertToJSON {
    @SerializedName("aggregation_info")
    private AggregationInfoResult aggregationInfoResult;
    @SerializedName("percentile_info")
    private MetricPercentileResults metricPercentileResults;
    private boolean isPercentileResultsAvailable;

    public MetricResult() {
        aggregationInfoResult = new AggregationInfoResult();
        metricPercentileResults = new MetricPercentileResults();
    }

    public MetricResult(JSONObject jsonObject) throws IncompatibleInputJSONException {
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

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(AutotuneConstants.JSONKeys.AGGREGATION_INFO, aggregationInfoResult.toJSON());
        if (isPercentileResultsAvailable) {
            jsonObject.put(AutotuneConstants.JSONKeys.PERCENTILE_INFO, metricPercentileResults.toJSON());
        }
        return jsonObject;
    }

    public AggregationInfoResult getAggregationInfoResult() {
        return aggregationInfoResult;
    }

    public MetricPercentileResults getEmMetricPercentileResults() {
        return metricPercentileResults;
    }
}
