package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.core.EMTrialManager;
import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.utils.EMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitionToCleanDeployment implements BaseTransition{
    private static final Logger LOGGER = LoggerFactory.getLogger(TransitionToCleanDeployment.class);
    @Override
    public void transit(String runId) {
        LOGGER.info("Executing transition - TransitionToCleanDeployment on thread - {} For RunId - ", Thread.currentThread().getId(), runId);
        ExperimentTrialData etd = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        etd.setStatus(EMUtil.EMExpStatus.COMPLETED);
    }

    @Override
    public void processNextTransition(String runId) {

    }
}
