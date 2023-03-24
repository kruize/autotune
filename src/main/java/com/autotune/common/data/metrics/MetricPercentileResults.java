package com.autotune.common.data.metrics;

import com.autotune.utils.KruizeConstants;
import org.json.JSONObject;

public class MetricPercentileResults {
    private float percentile50;
    private float percentile97;
    private float percentile95;
    private float percentile99;
    private float percentile99Point9;
    private float percentile99Point99;
    private float percentile99Point999;
    private float percentile99Point9999;
    private float percentile100;

    public MetricPercentileResults() {
        this.percentile50 = Float.MIN_VALUE;
        this.percentile97 = Float.MIN_VALUE;
        this.percentile95 = Float.MIN_VALUE;
        this.percentile99 = Float.MIN_VALUE;
        this.percentile99Point9 = Float.MIN_VALUE;
        this.percentile99Point99 = Float.MIN_VALUE;
        this.percentile99Point999 = Float.MIN_VALUE;
        this.percentile99Point9999 = Float.MIN_VALUE;
        this.percentile100 = Float.MIN_VALUE;
    }

    public MetricPercentileResults(JSONObject jsonObject) {
        this.percentile50 = (jsonObject.has(KruizeConstants.JSONKeys.P_50_0)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_50_0) : Float.MIN_VALUE;
        this.percentile95 = (jsonObject.has(KruizeConstants.JSONKeys.P_95_0)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_95_0) : Float.MIN_VALUE;
        this.percentile97 = (jsonObject.has(KruizeConstants.JSONKeys.P_97_0)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_97_0) : Float.MIN_VALUE;
        this.percentile99 = (jsonObject.has(KruizeConstants.JSONKeys.P_99_0)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_99_0) : Float.MIN_VALUE;
        this.percentile99Point9 = (jsonObject.has(KruizeConstants.JSONKeys.P_99_9)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_99_9) : Float.MIN_VALUE;
        this.percentile99Point99 = (jsonObject.has(KruizeConstants.JSONKeys.P_99_99)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_99_99) : Float.MIN_VALUE;
        this.percentile99Point999 = (jsonObject.has(KruizeConstants.JSONKeys.P_99_999)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_99_999) : Float.MIN_VALUE;
        this.percentile99Point9999 = (jsonObject.has(KruizeConstants.JSONKeys.P_99_9999)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_99_9999) : Float.MIN_VALUE;
        this.percentile100 = (jsonObject.has(KruizeConstants.JSONKeys.P_100_0)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_100_0) : Float.MIN_VALUE;
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

}
