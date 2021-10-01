package com.autotune.experimentManager.core;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.exceptions.EMInvalidTransitionException;
import com.autotune.experimentManager.transitions.BaseTransition;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class RunExperiment implements Callable<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunExperiment.class);
    private String runId;

    public RunExperiment(String runId) {
        this.runId = runId;
    }

    @Override
    public String call() throws Exception {
        LOGGER.info(EMConstants.Logs.RunExperiment.RUNNING_TRANSITION_ON_THREAD_ID, Thread.currentThread().getId());

        ExperimentTrialData trailData = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);

        if (! EMTransitionRegistry.validateStages(trailData.getCurrentStage(), trailData.getTargetStage())) {
            throw new EMInvalidTransitionException();
        }

        BaseTransition transition = EMTransitionBeanFactory.getTransitionHandler(trailData.getTargetStage());

        if (null != transition) {
            LOGGER.info(EMConstants.Logs.RunExperiment.START_TRANSITION_FOR_RUNID, trailData.getTargetStage().toString(), runId);
            transition.transit(runId);
            LOGGER.info(EMConstants.Logs.RunExperiment.END_TRANSITION_FOR_RUNID, trailData.getTargetStage().toString(),  runId);
        }

        if (trailData.getStatus().toString().equalsIgnoreCase(EMUtil.EMExpStatus.COMPLETED.toString())) {
            if (trailData.isNotifyTrialCompletion()) {
                EMTrialManager.launchWaitingExperiment(runId);
            }
        }

        return runId;
    }
}
