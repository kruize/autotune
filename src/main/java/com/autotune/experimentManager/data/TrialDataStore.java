package com.autotune.experimentManager.data;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.experimentManager.core.EMIterationManager;
import com.autotune.experimentManager.data.iteration.EMIterationData;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;

import java.util.ArrayList;

public class TrialDataStore {
    private ArrayList<EMIterationData> emIterationData;
    private ExperimentTrial experimentTrial;

    public TrialDataStore(ExperimentTrial experimentTrial, int size) throws EMInvalidInstanceCreation {
        if (null == experimentTrial) {
            throw new EMInvalidInstanceCreation();
        }
        this.experimentTrial = experimentTrial;
        emIterationData = new ArrayList<EMIterationData>(size);
    }

    public ArrayList<EMIterationData> getEmIterationData() {
        return emIterationData;
    }

    public ExperimentTrial getExperimentTrial() {
        return experimentTrial;
    }
}
