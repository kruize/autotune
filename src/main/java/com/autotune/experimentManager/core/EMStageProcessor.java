package com.autotune.experimentManager.core;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.EMStageProcessQueue;
import com.autotune.experimentManager.data.EMStageTransition;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.exceptions.EMInvalidTransitionException;

import java.util.concurrent.Callable;

public class EMStageProcessor implements Callable {
    public EMExecutorService emExecutorService;

    public EMStageProcessor() {
        emExecutorService = EMExecutorService.getService();
    }

    @Override
    public Object call() throws Exception {
        while(true) {
            if(EMStageProcessQueue.getStageProcessQueueInstance().getQueue().size() == 0) {
                synchronized (this) {
                    System.out.println("Waiting for experiments ... ");
                    wait();
                }
            } else {
                System.out.println("Proceeding to run experiments ... ");
                EMStageTransition transitionToBeLaunched = (EMStageTransition) EMStageProcessQueue.getStageProcessQueueInstance().getQueue().poll();
                if (transitionToBeLaunched.getTargetStage() != ((ExperimentTrialData) EMMapper.getInstance().getMap().get(transitionToBeLaunched.getRunId())).getTargetStage()) {
                    throw new EMInvalidTransitionException();
                }
                RunExperiment experiment = new RunExperiment(transitionToBeLaunched.getRunId());
                emExecutorService.execute(experiment);
            }
        }
    }

    public void notifyProcessor() {
        synchronized (this) {
            notify();
        }
    }
}
