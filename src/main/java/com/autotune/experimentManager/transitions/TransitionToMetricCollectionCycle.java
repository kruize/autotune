package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;

public class TransitionToMetricCollectionCycle extends AbstractBaseTransition{

    @Override
    public void transit(String runId) {
        System.out.println("Running metrics collection");
        ExperimentTrialData trialData = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        trialData.getEmIterationManager().getEmIterationData().get(trialData.getEmIterationManager().getCurrentIteration()).incrementCycle();
        processNextTransition(runId);
    }
}
