package com.autotune.common.data.metrics;

import com.autotune.common.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.AutotuneConstants;
import com.google.gson.annotations.SerializedName;
import org.json.JSONObject;

public class EMMetricResult implements ConvertToJSON {
    @SerializedName("aggregation_info")
    private EMMetricGenericResults emMetricGenericResults;
    @SerializedName("percentile_info")
    private EMMetricPercentileResults emMetricPercentileResults;
    private boolean isPercentileResultsAvailable;

    public EMMetricResult() {
        emMetricGenericResults = new EMMetricGenericResults();
        emMetricPercentileResults = new EMMetricPercentileResults();
    }

    public EMMetricResult(JSONObject jsonObject) throws IncompatibleInputJSONException {
        if (!jsonObject.has(AutotuneConstants.JSONKeys.AGGREGATION_INFO) &&
                !jsonObject.has(AutotuneConstants.JSONKeys.PERCENTILE_INFO)) {
            throw new IncompatibleInputJSONException();
        }
        emMetricGenericResults = new EMMetricGenericResults(jsonObject.getJSONObject(AutotuneConstants.JSONKeys.AGGREGATION_INFO));
        if (jsonObject.has(AutotuneConstants.JSONKeys.PERCENTILE_INFO)) {
            isPercentileResultsAvailable = true;
            emMetricPercentileResults = new EMMetricPercentileResults(jsonObject.getJSONObject(AutotuneConstants.JSONKeys.PERCENTILE_INFO));
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
        jsonObject.put(AutotuneConstants.JSONKeys.AGGREGATION_INFO, emMetricGenericResults.toJSON());
        if (isPercentileResultsAvailable) {
            jsonObject.put(AutotuneConstants.JSONKeys.PERCENTILE_INFO, emMetricPercentileResults.toJSON());
        }
        return jsonObject;
    }

    public EMMetricGenericResults getEmMetricGenericResults() {
        return emMetricGenericResults;
    }

    public EMMetricPercentileResults getEmMetricPercentileResults() {
        return emMetricPercentileResults;
    }
}
