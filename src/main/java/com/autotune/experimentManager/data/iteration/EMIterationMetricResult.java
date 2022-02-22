package com.autotune.experimentManager.data.iteration;

import com.autotune.experimentManager.data.input.metrics.EMMetricResult;
import com.autotune.experimentManager.data.util.EMDataOperator;

import java.util.ArrayList;

public class EMIterationMetricResult {
    private ArrayList<EMMetricResult> warmUpResults;
    private ArrayList<EMMetricResult> measurementResults;
    private EMMetricResult summarisedResults;
    private boolean isLatest;

    public EMIterationMetricResult(int warmupCycles, int measurementCycles) {
        this.warmUpResults = new ArrayList<EMMetricResult>(warmupCycles);
        this.measurementResults = new ArrayList<EMMetricResult>(measurementCycles);
        this.summarisedResults = new EMMetricResult();
        this.isLatest = true;
    }

    public EMMetricResult getSummarisedResults() {
        if(!isLatest) {
            this.summarisedResults = EMDataOperator.calculateSummaryResults(getWarmUpResults(), getMeasurementResults());
        }
        return summarisedResults;
    }

    public ArrayList<EMMetricResult> getWarmUpResults() {
        return warmUpResults;
    }

    public ArrayList<EMMetricResult> getMeasurementResults() {
        return measurementResults;
    }

    public boolean isLatest() {
        return isLatest;
    }

    public void addToWarmUpList(EMMetricResult emMetricData) {
        getWarmUpResults().add(emMetricData);
        isLatest = false;
    }

    public void addToMeasurementList(EMMetricResult emMetricData) {
        getMeasurementResults().add(emMetricData);
        isLatest = false;
    }
}
