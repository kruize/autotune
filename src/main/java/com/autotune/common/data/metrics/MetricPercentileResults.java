package com.autotune.common.data.metrics;

import com.autotune.utils.KruizeConstants;
import org.json.JSONObject;

public class MetricPercentileResults {
    private Float percentile50;
    private Float percentile97;
    private Float percentile95;
    private Float percentile99;
    private Float percentile99Point9;
    private Float percentile99Point99;
    private Float percentile99Point999;
    private Float percentile99Point9999;
    private Float percentile100;

    public MetricPercentileResults() {
    }

    public MetricPercentileResults(JSONObject jsonObject) {
        this.percentile50 = (jsonObject.has(KruizeConstants.JSONKeys.P_50_0)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_50_0) : null;
        this.percentile95 = (jsonObject.has(KruizeConstants.JSONKeys.P_95_0)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_95_0) : null;
        this.percentile97 = (jsonObject.has(KruizeConstants.JSONKeys.P_97_0)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_97_0) : null;
        this.percentile99 = (jsonObject.has(KruizeConstants.JSONKeys.P_99_0)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_99_0) : null;
        this.percentile99Point9 = (jsonObject.has(KruizeConstants.JSONKeys.P_99_9)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_99_9) : null;
        this.percentile99Point99 = (jsonObject.has(KruizeConstants.JSONKeys.P_99_99)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_99_99) : null;
        this.percentile99Point999 = (jsonObject.has(KruizeConstants.JSONKeys.P_99_999)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_99_999) : null;
        this.percentile99Point9999 = (jsonObject.has(KruizeConstants.JSONKeys.P_99_9999)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_99_9999) : null;
        this.percentile100 = (jsonObject.has(KruizeConstants.JSONKeys.P_100_0)) ? jsonObject.getFloat(KruizeConstants.JSONKeys.P_100_0) : null;
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
