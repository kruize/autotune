package com.autotune.experimentManager.data.input.metrics;

import com.autotune.experimentManager.data.input.abscls.DataEditor;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;

public class EMCycleMetrics extends DataEditor<EMCycleMetrics> implements ConvertToJSON {

    private String score;
    private String error;
    private String mean;
    private String mode;
    private String max;
    private String min;
    private String spike;
    private boolean isPercentileInfoAvailable = false;
    private EMMetricsPercentileInfo percentileInfo;

    public boolean isPercentileInfoAvailable() {
        return isPercentileInfoAvailable;
    }

    public void setPercentileInfoAvailable(boolean percentileInfoAvailable) {
        isPercentileInfoAvailable = percentileInfoAvailable;
    }

    public EMMetricsPercentileInfo getPercentileInfo() {
        return percentileInfo;
    }

    public void setPercentileInfo(EMMetricsPercentileInfo percentileInfo) {
        this.percentileInfo = percentileInfo;
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

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
        if (isPercentileInfoAvailable()) {
            if (null != percentileInfo) {
                try {
                    jsonObject.put(EMConstants.EMJSONKeys.PERCENTILE_INFO, getPercentileInfo().toJSON());
                } catch (EMDataObjectIsInEditingException e) {
                    e.printStackTrace();
                } catch (EMDataObjectIsNotFilledException e) {
                    e.printStackTrace();
                }
            }
        }

        return jsonObject;
    }

    @Override
    public EMCycleMetrics edit() {
        return null;
    }

    @Override
    public EMCycleMetrics done() {
        return null;
    }
}
