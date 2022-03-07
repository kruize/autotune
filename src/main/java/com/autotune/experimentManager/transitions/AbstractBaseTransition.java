package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.core.EMScheduledStageProcessor;
import com.autotune.experimentManager.core.EMTransitionRegistry;
import com.autotune.experimentManager.core.ExperimentManager;
import com.autotune.experimentManager.data.*;
import com.autotune.experimentManager.utils.EMUtil;

public abstract class AbstractBaseTransition implements BaseTransition {

    @Override
    public void processNextTransition(String runId) {
        ExperimentTrialData trialData = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        EMUtil.EMExpStages nextStage = EMTransitionRegistry.getNextStage(trialData.getTargetStage(), runId);
        trialData.setCurrentStage(trialData.getTargetStage());
        trialData.setTargetStage(nextStage);
        // TODO: need to check if the stage is isScheduled and take a decision to push in appropriate queue
        // Just pushing to regular queue for demo

        if (nextStage.isScheduled()) {
            System.out.println("Next Stage : " + nextStage.toString());
            EMStageTransition transition = new EMStageTransition(runId, nextStage);
            EMStageScheduledTransition scheduledTransistion = new EMStageScheduledTransition(transition, EMUtil.getDelayTimerForStage(nextStage, trialData));
            EMStageProcessQueue.getStageProcessQueueInstance().getScheduledQueue().add(scheduledTransistion);
            ExperimentManager.notifyScheduledQueueProcessor();
        } else {
            System.out.println("Next Stage : " + nextStage.toString());
            EMStageTransition transition = new EMStageTransition(runId, nextStage);
            EMStageProcessQueue.getStageProcessQueueInstance().getQueue().add(transition);
            ExperimentManager.notifyQueueProcessor();
        }
    }
}
