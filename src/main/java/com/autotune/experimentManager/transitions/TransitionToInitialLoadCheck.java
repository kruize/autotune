package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.transitions.util.TransistionHelper;
import com.autotune.experimentManager.utils.EMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitionToInitialLoadCheck extends AbstractBaseTransition{
    private static final Logger LOGGER = LoggerFactory.getLogger(TransitionToInitialLoadCheck.class);
    @Override
    public void transit(String runId) {
        LOGGER.info("Executing transition - TransitionToInitialLoadCheck on thread - {} For RunId - {}", Thread.currentThread().getId(), runId);
        ExperimentTrialData currentETD = ((ExperimentTrialData) EMMapper.getInstance().getMap().get(runId));
        if (TransistionHelper.LoadAnalyser.isLoadApplied()) {
            currentETD.setStatus(EMUtil.EMExpStatus.APPLYING_LOAD);
        }
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        processNextTransition(runId);
    }
}
