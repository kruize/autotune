package com.autotune.experimentManager.core;

import com.autotune.experimentManager.data.input.EMConfigObject;
import com.autotune.experimentManager.data.iteration.EMIterationData;
import com.autotune.experimentManager.exceptions.EMInvalidIterationId;

import java.util.ArrayList;

public class EMIterationManager {
    private final int iterations;
    private int currentIteration;
    private ArrayList<EMIterationData> emIterationData;

    public ArrayList<EMIterationData> getIterationDataList() {
        return emIterationData;
    }

    public EMIterationManager(EMConfigObject emConfigObject) {
        this.iterations = emConfigObject.getSettings().getTrialSettings().getIterations();
        this.currentIteration = 1;
        emIterationData = new ArrayList<EMIterationData>(iterations);
        for (int i = 0; i < iterations; i++) {
            EMIterationData emIterationDataObj = new EMIterationData(i + 1,
                    emConfigObject.getSettings().getTrialSettings().getWarmupCycles(),
                    emConfigObject.getSettings().getTrialSettings().getMeasurementCycles(),
                    emConfigObject.getDeployments().getTrainingDeployment().getAllMetrics());
            emIterationData.add(emIterationDataObj);
        }
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
        getIterationDataList().add(emIterationData);
    }

    public EMIterationData getIterationData(int iteration) throws EMInvalidIterationId {
        if (iteration <= 0 || iteration > this.iterations) {
            throw new EMInvalidIterationId();
        }
        return getIterationDataList().get(iteration - 1);
    }

}
