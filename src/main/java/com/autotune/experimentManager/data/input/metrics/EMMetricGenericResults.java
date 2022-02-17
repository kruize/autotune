package com.autotune.experimentManager.data.input.metrics;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;

public class EMMetricGenericResults implements ConvertToJSON {
    private float score;
    private float error;
    private float mean;
    private float mode;
    private float spike;

    public EMMetricGenericResults() {

    }

    public EMMetricGenericResults(JSONObject jsonObject) {
        this.score = jsonObject.getFloat(EMConstants.EMJSONKeys.SCORE);
        this.error = jsonObject.getFloat(EMConstants.EMJSONKeys.ERROR);
        this.mean = jsonObject.getFloat(EMConstants.EMJSONKeys.MEAN);
        this.mode = jsonObject.getFloat(EMConstants.EMJSONKeys.MODE);
        this.spike = jsonObject.getFloat(EMConstants.EMJSONKeys.SPIKE);
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public float getError() {
        return error;
    }

    public void setError(float error) {
        this.error = error;
    }

    public float getMean() {
        return mean;
    }

    public void setMean(float mean) {
        this.mean = mean;
    }

    public float getMode() {
        return mode;
    }

    public void setMode(float mode) {
        this.mode = mode;
    }

    public float getSpike() {
        return spike;
    }

    public void setSpike(float spike) {
        this.spike = spike;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(EMConstants.EMJSONKeys.SPIKE, spike);
        jsonObject.put(EMConstants.EMJSONKeys.ERROR, error);
        jsonObject.put(EMConstants.EMJSONKeys.MODE, mode);
        jsonObject.put(EMConstants.EMJSONKeys.MEAN, mean);
        jsonObject.put(EMConstants.EMJSONKeys.SCORE, score);
        return jsonObject;
    }
}
