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
    private float min;
    private float max;

    public EMMetricGenericResults() {
        this.score = Float.MIN_VALUE;
        this.error = Float.MIN_VALUE;
        this.mean = Float.MIN_VALUE;
        this.mode = Float.MIN_VALUE;
        this.spike = Float.MIN_VALUE;
        this.min = Float.MIN_VALUE;
        this.max = Float.MIN_VALUE;
    }

    public EMMetricGenericResults(JSONObject jsonObject) {
        this.score = jsonObject.getFloat(EMConstants.EMJSONKeys.SCORE);
        this.error = jsonObject.getFloat(EMConstants.EMJSONKeys.ERROR);
        this.mean = jsonObject.getFloat(EMConstants.EMJSONKeys.MEAN);
        this.mode = jsonObject.getFloat(EMConstants.EMJSONKeys.MODE);
        this.spike = jsonObject.getFloat(EMConstants.EMJSONKeys.SPIKE);
        this.max = jsonObject.getFloat(EMConstants.EMJSONKeys.MAX);
        this.min = jsonObject.getFloat(EMConstants.EMJSONKeys.MIN);
    }

    public float getMin() {
        return min;
    }

    public void setMin(float min) {
        this.min = min;
    }

    public float getMax() {
        return max;
    }

    public void setMax(float max) {
        this.max = max;
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
        if (this.spike != Float.MIN_VALUE)
            jsonObject.put(EMConstants.EMJSONKeys.SPIKE, spike);
        if (this.error != Float.MIN_VALUE)
            jsonObject.put(EMConstants.EMJSONKeys.ERROR, error);
        if (this.mode != Float.MIN_VALUE)
            jsonObject.put(EMConstants.EMJSONKeys.MODE, mode);
        if (this.mean != Float.MIN_VALUE)
            jsonObject.put(EMConstants.EMJSONKeys.MEAN, mean);
        if (this.score != Float.MIN_VALUE)
            jsonObject.put(EMConstants.EMJSONKeys.SCORE, score);
        if (this.min != Float.MIN_VALUE)
            jsonObject.put(EMConstants.EMJSONKeys.MIN, min);
        if (this.max != Float.MIN_VALUE)
            jsonObject.put(EMConstants.EMJSONKeys.MAX, max);
        return jsonObject;
    }
}
