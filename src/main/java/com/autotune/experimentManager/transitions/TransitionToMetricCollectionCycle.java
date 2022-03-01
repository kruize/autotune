package com.autotune.experimentManager.transitions;

public class TransitionToMetricCollectionCycle extends AbstractBaseTransition{

    @Override
    public void transit(String runId) {
        processNextTransition(runId);
    }
}
