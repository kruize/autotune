package com.autotune.experimentManager.data.input.metrics;

import com.autotune.experimentManager.data.input.abscls.DataEditor;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;

public class EMCycleMetrics extends DataEditor<EMCycleMetrics> implements ConvertToJSON {

    private String score;
    private String error;
    private String mean;
    private String mode;
    private String spike;
    private String p95;
    private String p99;
    private String p99point9;
    private String p99point99;
    private String p99point999;
    private String p99point9999;
    private String p100;

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMean() {
        return mean;
    }

    public void setMean(String mean) {
        this.mean = mean;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getSpike() {
        return spike;
    }

    public void setSpike(String spike) {
        this.spike = spike;
    }

    public String getP95() {
        return p95;
    }

    public void setP95(String p95) {
        this.p95 = p95;
    }

    public String getP99() {
        return p99;
    }

    public void setP99(String p99) {
        this.p99 = p99;
    }

    public String getP99point9() {
        return p99point9;
    }

    public void setP99point9(String p99point9) {
        this.p99point9 = p99point9;
    }

    public String getP99point99() {
        return p99point99;
    }

    public void setP99point99(String p99point99) {
        this.p99point99 = p99point99;
    }

    public String getP99point999() {
        return p99point999;
    }

    public void setP99point999(String p99point999) {
        this.p99point999 = p99point999;
    }

    public String getP99point9999() {
        return p99point9999;
    }

    public void setP99point9999(String p99point9999) {
        this.p99point9999 = p99point9999;
    }

    public String getP100() {
        return p100;
    }

    public void setP100(String p100) {
        this.p100 = p100;
    }

    @Override
    public EMCycleMetrics edit() {
        return null;
    }

    @Override
    public EMCycleMetrics done() {
        return null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject =  new JSONObject();
        if (null != this.score)
            jsonObject.put(EMConstants.EMJSONKeys.SCORE, getScore());
        if (null != this.error)
            jsonObject.put(EMConstants.EMJSONKeys.ERROR, getError());
        if (null != this.mean)
            jsonObject.put(EMConstants.EMJSONKeys.MEAN, getMean());
        if (null != this.mode)
            jsonObject.put(EMConstants.EMJSONKeys.MODE, getMode());
        if (null != this.spike)
            jsonObject.put(EMConstants.EMJSONKeys.SPIKE, getSpike());
        if (null != this.p95)
            jsonObject.put(EMConstants.EMJSONKeys.P_95_0, getP95());
        if (null != this.p99)
            jsonObject.put(EMConstants.EMJSONKeys.P_99_0, getP99());
        if (null != this.p99point9)
            jsonObject.put(EMConstants.EMJSONKeys.P_99_9, getP99point9());
        if (null != this.p99point99)
            jsonObject.put(EMConstants.EMJSONKeys.P_99_99, getP99point99());
        if (null != this.p99point999)
            jsonObject.put(EMConstants.EMJSONKeys.P_99_999, getP99point999());
        if (null != this.p99point9999)
            jsonObject.put(EMConstants.EMJSONKeys.P_99_9999, getP99point9999());
        if (null != this.p100)
            jsonObject.put(EMConstants.EMJSONKeys.P_100_0, getP100());

        return jsonObject;
    }
}
