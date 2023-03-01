package com.autotune.experimentManager.data.input.metrics;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.utils.AutotuneConstants;
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
        this.score = jsonObject.getFloat(AutotuneConstants.JSONKeys.SCORE);
        this.error = jsonObject.getFloat(AutotuneConstants.JSONKeys.ERROR);
        this.mean = jsonObject.getFloat(AutotuneConstants.JSONKeys.MEAN);
        this.mode = jsonObject.getFloat(AutotuneConstants.JSONKeys.MODE);
        this.spike = jsonObject.getFloat(AutotuneConstants.JSONKeys.SPIKE);
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
        jsonObject.put(AutotuneConstants.JSONKeys.SPIKE, spike);
        jsonObject.put(AutotuneConstants.JSONKeys.ERROR, error);
        jsonObject.put(AutotuneConstants.JSONKeys.MODE, mode);
        jsonObject.put(AutotuneConstants.JSONKeys.MEAN, mean);
        jsonObject.put(AutotuneConstants.JSONKeys.SCORE, score);
        return jsonObject;
    }
}
