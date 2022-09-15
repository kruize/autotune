package com.autotune.experimentManager.data.iteration;

import com.autotune.experimentManager.data.input.EMMetricInput;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.data.input.metrics.EMMetricResult;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.utils.AutotuneConstants;
import org.json.JSONObject;

public class EMMetricData implements ConvertToJSON {
    private EMMetricInput emMetricInput;
    private EMMetricResult emMetricResult;

    public EMMetricData(EMMetricInput emMetricInput, EMMetricResult emMetricResult) {
        this.emMetricInput = emMetricInput;
        this.emMetricResult = emMetricResult;
    }

    public EMMetricData(JSONObject jsonObject) throws EMInvalidInstanceCreation, IncompatibleInputJSONException {
        emMetricInput =  new EMMetricInput(jsonObject.getJSONObject(AutotuneConstants.JSONKeys.METRIC_INFO));
        emMetricResult = new EMMetricResult(jsonObject.getJSONObject(AutotuneConstants.JSONKeys.METRICS_RESULTS));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject =  new JSONObject();
        jsonObject.put(AutotuneConstants.JSONKeys.METRIC_INFO, emMetricInput.toJSON());
        jsonObject.put(AutotuneConstants.JSONKeys.METRICS_RESULTS, emMetricResult.toJSON());
        return jsonObject;
    }
}
