package com.autotune.experimentManager.core;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.exceptions.EMInvalidTransitionException;
import com.autotune.experimentManager.transitions.BaseTransition;

import java.util.concurrent.Callable;

public class RunExperiment implements Callable<String> {
    private String runId;

    public RunExperiment(String runId) {
        this.runId = runId;
    }

    @Override
    public String call() throws Exception {
        ExperimentTrialData trailData = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        if (! EMTransitionRegistry.validateStages(trailData.getCurrentStage(), trailData.getTargetStage())) {
            throw new EMInvalidTransitionException();
        }
        BaseTransition transition = EMTransitionBeanFactory.getTransitionHandler(trailData.getTargetStage());
        if (null != transition) {
            transition.transit(runId);
        }

        return runId;
    }
}
