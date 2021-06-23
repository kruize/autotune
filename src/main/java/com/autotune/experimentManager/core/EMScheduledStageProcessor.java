package com.autotune.experimentManager.core;

import com.autotune.experimentManager.data.*;
import com.autotune.experimentManager.exceptions.EMInvalidTransitionException;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class EMScheduledStageProcessor implements Callable {
    public EMExecutorService emExecutorService;

    public EMScheduledStageProcessor() {
        emExecutorService = EMExecutorService.getService();
    }

    @Override
    public Object call() throws Exception {
        while(true) {
            if(EMStageProcessQueue.getStageProcessQueueInstance().getScheduledQueue().size() == 0) {
                synchronized (this) {
                    wait();
                }
            } else {
                EMStageScheduledTransition transitionToBeLaunched = (EMStageScheduledTransition) EMStageProcessQueue.getStageProcessQueueInstance().getScheduledQueue().poll();
                if (transitionToBeLaunched.getEmStageTransition().getTargetStage() != ((ExperimentTrialData) EMMapper.getInstance().getMap().get(transitionToBeLaunched.getEmStageTransition().getRunId())).getTargetStage()) {
                    throw new EMInvalidTransitionException();
                }
                RunExperiment experiment = new RunExperiment(transitionToBeLaunched.getEmStageTransition().getRunId());
                emExecutorService.scheduledExecute(experiment, transitionToBeLaunched.getDelayInSecs(), TimeUnit.SECONDS);
            }
        }
    }

    public void notifyProcessor() {
        synchronized (this) {
            notify();
        }
    }
}
