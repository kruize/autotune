package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.core.EMScheduledStageProcessor;
import com.autotune.experimentManager.core.EMTransitionRegistry;
import com.autotune.experimentManager.core.ExperimentManager;
import com.autotune.experimentManager.data.*;
import com.autotune.experimentManager.utils.EMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBaseTransition implements BaseTransition {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBaseTransition.class);

    @Override
    public void processNextTransition(String runId) {
        ExperimentTrialData trialData = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        EMUtil.EMExpStages nextStage = EMTransitionRegistry.getNextStage(trialData.getTargetStage());
        trialData.setCurrentStage(trialData.getTargetStage());
        trialData.setTargetStage(nextStage);
        LOGGER.info("Next Stage : " + nextStage.toString());
        EMStageTransition transition = new EMStageTransition(runId, nextStage);
        if (nextStage.isScheduled()) {
            EMStageScheduledTransition scheduledTransition = new EMStageScheduledTransition(transition, 20);
            EMStageProcessQueue.getStageProcessQueueInstance().getScheduledQueue().add(scheduledTransition);
            ExperimentManager.notifyScheduledQueueProcessor();
        } else {
            EMStageProcessQueue.getStageProcessQueueInstance().getQueue().add(transition);
            ExperimentManager.notifyQueueProcessor();
        }
    }
}
