package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.transitions.util.TransistionHelper;
import com.autotune.experimentManager.utils.EMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitionToInitiateTrialRunPhase extends AbstractBaseTransition{
    private static final Logger LOGGER = LoggerFactory.getLogger(TransitionToInitiateTrialRunPhase.class);
    @Override
    public void transit(String runId) {
        LOGGER.info("Executing transition - TransitionToInitiateTrailRunPhase on thread - {} For RunId - ", Thread.currentThread().getId(), runId);
        ExperimentTrialData currentETD = ((ExperimentTrialData) EMMapper.getInstance().getMap().get(runId));
        if (TransistionHelper.LoadAnalyser.isReadyToLoad()) {
            currentETD.setStatus(EMUtil.EMExpStatus.WAITING_FOR_LOAD);
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        processNextTransition(runId);
    }
}
