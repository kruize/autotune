package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;

public class TransitionToMetricCollectionCycle extends AbstractBaseTransition{

    @Override
    public void transit(String runId) {
        System.out.println("Running metrics collection");
        ExperimentTrialData trialData = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        System.out.println("Current Cycle - " + trialData.getEmIterationManager().getEmIterationData().get(trialData.getEmIterationManager().getCurrentIteration()-1).getCurrentCycle());
        trialData.getEmIterationManager().getEmIterationData().get(trialData.getEmIterationManager().getCurrentIteration()-1).incrementCycle();
        System.out.println("Next Cycle - " + trialData.getEmIterationManager().getEmIterationData().get(trialData.getEmIterationManager().getCurrentIteration()-1).getCurrentCycle());
        processNextTransition(runId);
    }
}
