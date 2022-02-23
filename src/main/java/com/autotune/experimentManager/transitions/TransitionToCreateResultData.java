package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.core.EMIterationManager;
import com.autotune.experimentManager.core.EMTransitionRegistry;
import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.transitions.util.TransistionHelper;
import com.autotune.experimentManager.utils.EMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitionToCreateResultData extends AbstractBaseTransition{
    private static final Logger LOGGER = LoggerFactory.getLogger(TransitionToCreateResultData.class);
    @Override
    public void transit(String runId) {
        LOGGER.info("Executing transition - TransitionToCreateResultData on thread - {} For RunId - {}", Thread.currentThread().getId(), runId);

        ExperimentTrialData trialData = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        TransistionHelper.CollectMetrics.startMetricCollection(trialData);
        EMIterationManager emIterationManager = trialData.getEmIterationManager();
        if (emIterationManager.getCurrentIteration() != emIterationManager.getIterations()) {
            EMUtil.EMExpStages nextStage = EMTransitionRegistry.getNextStage(EMUtil.EMExpStages.INIT);
            trialData.setCurrentStage(EMUtil.EMExpStages.INIT);
            trialData.setTargetStage(nextStage);
            emIterationManager.incrementIteration();
        }

        processNextTransition(runId);
    }
}
