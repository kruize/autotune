package com.autotune.common.data.metrics;

import com.autotune.common.interfaces.ConvertToJSON;
import com.autotune.utils.AutotuneConstants;
import org.json.JSONObject;

public class EMMetricGenericResults implements ConvertToJSON {
    private float score;
    private float error;
    private Double mean;
    private float mode;
    private float spike;
    private float min;
    private float max;
    private String units;

    public EMMetricGenericResults() {
        score = Float.MIN_VALUE;
        error = Float.MIN_VALUE;
        mean = Double.MIN_VALUE;
        mode = Float.MIN_VALUE;
        spike = Float.MIN_VALUE;
        min = Float.MIN_VALUE;
        max = Float.MIN_VALUE;
        units = "";
    }

    public EMMetricGenericResults(JSONObject jsonObject) {
        this.score = (jsonObject.has(AutotuneConstants.JSONKeys.SCORE)) ? jsonObject.getFloat(AutotuneConstants.JSONKeys.SCORE) : Float.MIN_VALUE;
        this.error = (jsonObject.has(AutotuneConstants.JSONKeys.ERROR)) ? jsonObject.getFloat(AutotuneConstants.JSONKeys.ERROR) : Float.MIN_VALUE;
        this.mean = (jsonObject.has(AutotuneConstants.JSONKeys.MEAN)) ? jsonObject.getFloat(AutotuneConstants.JSONKeys.MEAN) : Double.MIN_VALUE;
        this.mode = (jsonObject.has(AutotuneConstants.JSONKeys.MODE)) ? jsonObject.getFloat(AutotuneConstants.JSONKeys.MODE) : Float.MIN_VALUE;
        this.spike = (jsonObject.has(AutotuneConstants.JSONKeys.SPIKE)) ? jsonObject.getFloat(AutotuneConstants.JSONKeys.SPIKE) : Float.MIN_VALUE;
        this.max = (jsonObject.has(AutotuneConstants.JSONKeys.MAX)) ? jsonObject.getFloat(AutotuneConstants.JSONKeys.MAX) : Float.MIN_VALUE;
        this.min = (jsonObject.has(AutotuneConstants.JSONKeys.MIN)) ? jsonObject.getFloat(AutotuneConstants.JSONKeys.MIN) : Float.MIN_VALUE;
        this.units = (jsonObject.has(AutotuneConstants.JSONKeys.UNITS)) ? jsonObject.getString(AutotuneConstants.JSONKeys.UNITS) : "";
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

    public Double getMean() {
        return mean;
    }

    public void setMean(Double mean) {
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

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        if (this.spike != Float.MIN_VALUE)
            jsonObject.put(AutotuneConstants.JSONKeys.SPIKE, spike);
        if (this.error != Float.MIN_VALUE)
            jsonObject.put(AutotuneConstants.JSONKeys.ERROR, error);
        if (this.mode != Float.MIN_VALUE)
            jsonObject.put(AutotuneConstants.JSONKeys.MODE, mode);
        if (this.mean != Float.MIN_VALUE)
            jsonObject.put(AutotuneConstants.JSONKeys.MEAN, mean);
        if (this.score != Float.MIN_VALUE)
            jsonObject.put(AutotuneConstants.JSONKeys.SCORE, score);
        if (this.max != Float.MIN_VALUE)
            jsonObject.put(AutotuneConstants.JSONKeys.MAX, max);
        if (this.min != Float.MIN_VALUE)
            jsonObject.put(AutotuneConstants.JSONKeys.MIN, min);
        if (null != this.units && !this.units.isEmpty() && !this.units.isBlank())
            jsonObject.put(AutotuneConstants.JSONKeys.UNITS, this.units);
        return jsonObject;
    }
}
