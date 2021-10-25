package com.autotune.experimentManager.data.input.deployments;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.data.input.metrics.EMCollectiveCyclesMetrics;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;

public class EMConfigDeploymentMeasurementResults implements ConvertToJSON {
    private EMCollectiveCyclesMetrics collectiveCyclesMetrics;

    public EMConfigDeploymentMeasurementResults() {
        this.collectiveCyclesMetrics = new EMCollectiveCyclesMetrics();
    }

    public EMCollectiveCyclesMetrics getCollectiveCyclesMetrics() {
        return collectiveCyclesMetrics;
    }

    public void setCollectiveCyclesMetrics(EMCollectiveCyclesMetrics collectiveCyclesMetrics) {
        this.collectiveCyclesMetrics = collectiveCyclesMetrics;
    }

    @Override
    public JSONObject toJSON() throws EMDataObjectIsInEditingException, EMDataObjectIsNotFilledException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(EMConstants.EMJSONKeys.MEASUREMENT_RESULTS, collectiveCyclesMetrics.toJSON());
        return jsonObject;
    }
}
