package com.autotune.experimentManager.data.iteration;

import com.autotune.common.data.metrics.MetricResults;
import com.autotune.experimentManager.data.input.EMMetricInput;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.KruizeConstants;
import org.json.JSONObject;

public class EMMetricData {
    private EMMetricInput emMetricInput;
    private MetricResults metricResults;

    public EMMetricData(EMMetricInput emMetricInput, MetricResults metricResults) {
        this.emMetricInput = emMetricInput;
        this.metricResults = metricResults;
    }

    public EMMetricData(JSONObject jsonObject) throws EMInvalidInstanceCreation, IncompatibleInputJSONException {
        emMetricInput =  new EMMetricInput(jsonObject.getJSONObject(KruizeConstants.JSONKeys.METRIC_INFO));
        metricResults = new MetricResults(jsonObject.getJSONObject(KruizeConstants.JSONKeys.METRICS_RESULTS));
    }
}
