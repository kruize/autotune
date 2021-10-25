package com.autotune.experimentManager.data.input.deployments;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;

public class EMConfigDeploymentMetricResults implements ConvertToJSON {
    private boolean isResultsAvailable;
    private EMConfigDeploymentWarmupResults warmupResults;
    private EMConfigDeploymentMeasurementResults measurementResults;

    public EMConfigDeploymentMetricResults() {
        this.warmupResults = new EMConfigDeploymentWarmupResults();
        this.measurementResults = new EMConfigDeploymentMeasurementResults();
        this.isResultsAvailable =  false;
    }

    public boolean isResultsAvailable() {
        return isResultsAvailable;
    }

    public void setResultsAvailable(boolean resultsAvailable) {
        isResultsAvailable = resultsAvailable;
    }

    public EMConfigDeploymentWarmupResults getWarmupResults() {
        return warmupResults;
    }

    public void setWarmupResults(EMConfigDeploymentWarmupResults warmupResults) {
        this.warmupResults = warmupResults;
    }

    public EMConfigDeploymentMeasurementResults getMeasurementResults() {
        return measurementResults;
    }

    public void setMeasurementResults(EMConfigDeploymentMeasurementResults measurementResults) {
        this.measurementResults = measurementResults;
    }

    @Override
    public JSONObject toJSON() throws EMDataObjectIsInEditingException, EMDataObjectIsNotFilledException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(EMConstants.EMJSONKeys.WARMUP_RESULTS, warmupResults.toJSON());
        jsonObject.put(EMConstants.EMJSONKeys.MEASUREMENT_RESULTS, measurementResults.toJSON());
        return jsonObject;
    }
}
