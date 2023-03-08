package com.autotune.experimentManager.data.iteration;

import com.autotune.common.data.metrics.MetricResult;
import com.autotune.experimentManager.data.input.EMMetricInput;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.AutotuneConstants;
import org.json.JSONObject;

public class EMMetricData implements ConvertToJSON {
    private EMMetricInput emMetricInput;
    private MetricResult metricResult;

    public EMMetricData(EMMetricInput emMetricInput, MetricResult metricResult) {
        this.emMetricInput = emMetricInput;
        this.metricResult = metricResult;
    }

    public EMMetricData(JSONObject jsonObject) throws EMInvalidInstanceCreation, IncompatibleInputJSONException {
        emMetricInput =  new EMMetricInput(jsonObject.getJSONObject(AutotuneConstants.JSONKeys.METRIC_INFO));
        metricResult = new MetricResult(jsonObject.getJSONObject(AutotuneConstants.JSONKeys.METRICS_RESULTS));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject =  new JSONObject();
        jsonObject.put(AutotuneConstants.JSONKeys.METRIC_INFO, emMetricInput.toJSON());
        jsonObject.put(AutotuneConstants.JSONKeys.METRICS_RESULTS, metricResult.toJSON());
        return jsonObject;
    }
}
