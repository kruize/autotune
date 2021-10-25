package com.autotune.experimentManager.data.input.metrics;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class EMCollectiveCyclesMetrics implements ConvertToJSON {
    private int cycles;
    private String duration;
    private ArrayList<EMCycleMetrics> cycleMetricsList;

    public EMCollectiveCyclesMetrics() {
        this.cycleMetricsList =  new ArrayList<EMCycleMetrics>();
    }

    public int getCycles() {
        return cycles;
    }

    public void setCycles(int cycles) {
        this.cycles = cycles;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public ArrayList<EMCycleMetrics> getCycleMetricsList() {
        return cycleMetricsList;
    }

    public void setCycleMetricsList(ArrayList<EMCycleMetrics> cycleMetricsList) {
        this.cycleMetricsList = cycleMetricsList;
    }

    @Override
    public JSONObject toJSON() throws EMDataObjectIsInEditingException, EMDataObjectIsNotFilledException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(EMConstants.EMJSONKeys.CYCLES, getCycles());
        jsonObject.put(EMConstants.EMJSONKeys.DURATION, getDuration());
        JSONArray jsonArray =  new JSONArray();
        for (EMCycleMetrics cycleMetrics: this.cycleMetricsList) {
            jsonArray.put(cycleMetrics.toJSON());
        }
        jsonObject.put(EMConstants.EMJSONKeys.RESULTS, jsonArray);
        return jsonObject;
    }
}
