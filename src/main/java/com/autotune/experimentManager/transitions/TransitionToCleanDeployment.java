package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.core.EMTrialManager;
import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.utils.EMUtil;

public class TransitionToCleanDeployment implements BaseTransition{
    @Override
    public void transit(String runId) {
        System.out.println("Executing transition - TransitionToCleanDeployment on thread - {}" + Thread.currentThread().getId()  + "For RunId - " + runId);
        ExperimentTrialData etd = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        etd.setStatus(EMUtil.EMExpStatus.COMPLETED);
    }

    @Override
    public void processNextTransition(String runId) {

    }
}
