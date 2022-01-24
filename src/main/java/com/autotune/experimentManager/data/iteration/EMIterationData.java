package com.autotune.experimentManager.data.iteration;

public class EMIterationData {
    private final int iterationIndex;
    private final int warmCycles;
    private final int measurementCycles;
    private final int totalCycles;
    private int currentCycle;
    private EMIterationResult emIterationResult;

    public EMIterationData(int iterationIndex, int warmCycles, int measurementCycles) {
        this.iterationIndex = iterationIndex;
        this.warmCycles = warmCycles;
        this.measurementCycles = measurementCycles;
        this.currentCycle = 1;
        this.totalCycles = this.warmCycles + this.measurementCycles;
    }

    public void incrementCycle() {
        this.currentCycle++;
    }

    public int getIterationIndex() {
        return iterationIndex;
    }

    public int getWarmCycles() {
        return warmCycles;
    }

    public int getMeasurementCycles() {
        return measurementCycles;
    }

    public int getTotalCycles() {
        return totalCycles;
    }

    public EMIterationResult getEmIterationResult() {
        return emIterationResult;
    }

    public void addResults(EMMetricData emMetricData) {
        if (this.currentCycle <= this.warmCycles) {
            getEmIterationResult().addToWarmUpList(emMetricData);
        } else {
            getEmIterationResult().addToMeasurementList(emMetricData);
        }
    }
}
