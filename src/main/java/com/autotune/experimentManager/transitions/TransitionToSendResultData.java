package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.core.EMIterationManager;
import com.autotune.experimentManager.core.EMTransitionRegistry;
import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.transitions.util.TransistionHelper;
import com.autotune.experimentManager.utils.EMUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitionToSendResultData extends AbstractBaseTransition{
    private static final Logger LOGGER = LoggerFactory.getLogger(TransitionToSendResultData.class);
    @Override
    public void transit(String runId) {
        LOGGER.info("Executing transition - TransitionToSendResultData on thread - {} For RunId - {}",
                Thread.currentThread().getId(),
                runId);

        ExperimentTrialData trialData = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        String trial_result_url = trialData.getConfig().getEmConfigObject().getInfo().getTrialInfo().getTrialResultUrl();
        JSONObject retJson = TransistionHelper.MetricsFormatter.getMetricsJson(runId);
        System.out.println(retJson.toString(4));
        TransistionHelper.DataPoster.sendData(trial_result_url, retJson);
        processNextTransition(runId);
    }
}
