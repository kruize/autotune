package com.autotune.experimentManager.data.input.metrics;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.utils.AutotuneConstants;
import org.json.JSONObject;

public class EMMetricPercentileResults implements ConvertToJSON {
    private float percentile50;
    private float percentile97;
    private float percentile95;
    private float percentile99;
    private float percentile99Point9;
    private float percentile99Point99;
    private float percentile99Point999;
    private float percentile99Point9999;
    private float percentile100;

    public EMMetricPercentileResults() {

    }

    public EMMetricPercentileResults(JSONObject jsonObject) {
        this.percentile50 = jsonObject.getFloat(AutotuneConstants.JSONKeys.P_50_0);
        this.percentile95 = jsonObject.getFloat(AutotuneConstants.JSONKeys.P_95_0);
        this.percentile97 = jsonObject.getFloat(AutotuneConstants.JSONKeys.P_97_0);
        this.percentile99 = jsonObject.getFloat(AutotuneConstants.JSONKeys.P_99_0);
        this.percentile99Point9 = jsonObject.getFloat(AutotuneConstants.JSONKeys.P_99_9);
        this.percentile99Point99 = jsonObject.getFloat(AutotuneConstants.JSONKeys.P_99_99);
        this.percentile99Point999 = jsonObject.getFloat(AutotuneConstants.JSONKeys.P_99_999);
        this.percentile99Point9999 = jsonObject.getFloat(AutotuneConstants.JSONKeys.P_99_9999);
        this.percentile100 = jsonObject.getFloat(AutotuneConstants.JSONKeys.P_100_0);
    }

    public float getPercentile50() {
        return percentile50;
    }

    public void setPercentile50(float percentile50) {
        this.percentile50 = percentile50;
    }

    public float getPercentile97() {
        return percentile97;
    }

    public void setPercentile97(float percentile97) {
        this.percentile97 = percentile97;
    }

    public float getPercentile95() {
        return percentile95;
    }

    public void setPercentile95(float percentile95) {
        this.percentile95 = percentile95;
    }

    public float getPercentile99() {
        return percentile99;
    }

    public void setPercentile99(float percentile99) {
        this.percentile99 = percentile99;
    }

    public float getPercentile99Point9() {
        return percentile99Point9;
    }

    public void setPercentile99Point9(float percentile99Point9) {
        this.percentile99Point9 = percentile99Point9;
    }

    public float getPercentile99Point99() {
        return percentile99Point99;
    }

    public void setPercentile99Point99(float percentile99Point99) {
        this.percentile99Point99 = percentile99Point99;
    }

    public float getPercentile99Point999() {
        return percentile99Point999;
    }

    public void setPercentile99Point999(float percentile99Point999) {
        this.percentile99Point999 = percentile99Point999;
    }

    public float getPercentile99Point9999() {
        return percentile99Point9999;
    }

    public void setPercentile99Point9999(float percentile99Point9999) {
        this.percentile99Point9999 = percentile99Point9999;
    }

    public float getPercentile100() {
        return percentile100;
    }

    public void setPercentile100(float percentile100) {
        this.percentile100 = percentile100;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(AutotuneConstants.JSONKeys.P_50_0, percentile50);
        jsonObject.put(AutotuneConstants.JSONKeys.P_95_0, percentile95);
        jsonObject.put(AutotuneConstants.JSONKeys.P_97_0, percentile97);
        jsonObject.put(AutotuneConstants.JSONKeys.P_99_0, percentile99);
        jsonObject.put(AutotuneConstants.JSONKeys.P_99_9, percentile99Point9);
        jsonObject.put(AutotuneConstants.JSONKeys.P_99_99, percentile99Point99);
        jsonObject.put(AutotuneConstants.JSONKeys.P_99_999, percentile99Point999);
        jsonObject.put(AutotuneConstants.JSONKeys.P_99_9999, percentile99Point9999);
        jsonObject.put(AutotuneConstants.JSONKeys.P_100_0, percentile100);
        return jsonObject;
    }
}
