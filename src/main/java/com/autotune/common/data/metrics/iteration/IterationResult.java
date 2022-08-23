package com.autotune.common.data.metrics.iteration;

import com.autotune.common.data.metrics.EMMetricResult;

import java.util.ArrayList;

public class IterationResult {
    private int iterationIndex;
    private ArrayList<EMMetricResult> warmUpResults;
    private ArrayList<EMMetricResult> measurementResults;
    private ArrayList<EMMetricResult> summarisedResults;
    private boolean isLatest;

    public IterationResult() {
        this.warmUpResults = new ArrayList<EMMetricResult>();
        this.measurementResults = new ArrayList<EMMetricResult>();
        this.summarisedResults = new ArrayList<EMMetricResult>();
        this.isLatest = true;
        this.iterationIndex = -1;
    }

    public int getIterationIndex() {
        return iterationIndex;
    }

    public void setIterationIndex(int iterationIndex) {
        this.iterationIndex = iterationIndex;
    }

    public ArrayList<EMMetricResult> getWarmUpResults() {
        return warmUpResults;
    }

    public ArrayList<EMMetricResult> getMeasurementResults() {
        return measurementResults;
    }

    public ArrayList<EMMetricResult> getSummarisedResults() {
        if(!isLatest) {
//            this.summarisedResults = EMDataOperator.calculateSummaryResults(getWarmUpResults(), getMeasurementResults());
        }
        return summarisedResults;
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
