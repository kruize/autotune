package com.autotune.experimentManager.data.iteration;

import com.autotune.experimentManager.data.input.EMMetricInput;

import java.util.ArrayList;
import java.util.HashMap;

public class EMIterationResult {
    private HashMap<String, EMIterationMetricResult> metricMap;


    public EMIterationResult(int warmupCycles, int measurementCycles, ArrayList<EMMetricInput> metricsList) {
        this.metricMap = new HashMap<String, EMIterationMetricResult>();
        for(EMMetricInput emMetricInput: metricsList) {
            this.metricMap.put(emMetricInput.getName(), new EMIterationMetricResult(warmupCycles, measurementCycles));
        }
    }

    public EMIterationMetricResult getIterationMetricResult(String metricName) {
        return this.metricMap.get(metricName);
    }
}
