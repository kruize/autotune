package com.autotune.experimentManager.core;

import com.autotune.experimentManager.data.iteration.EMIterationData;

import java.util.ArrayList;

public class EMIterationManager {
    private final int iterations;
    private int currentIteration;
    ArrayList<EMIterationData> emIterationData;

    public ArrayList<EMIterationData> getEmIterationData() {
        return emIterationData;
    }

    public EMIterationManager(int iterations) {
        this.iterations = iterations;
        this.currentIteration = 1;
        emIterationData = new ArrayList<EMIterationData>(iterations);
    }

    public void incrementIteration() {
        this.currentIteration++;
    }

    public int getIterations() {
        return iterations;
    }

    public int getCurrentIteration() {
        return currentIteration;
    }

    public void addIterationData(EMIterationData emIterationData) {
        getEmIterationData().add(emIterationData);
    }
}
