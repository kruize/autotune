package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.core.EMTransitionRegistry;
import com.autotune.experimentManager.core.ExperimentManager;
import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.EMStageProcessQueue;
import com.autotune.experimentManager.data.EMStageTransition;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.data.input.deployments.EMConfigDeploymentMetrics;
import com.autotune.experimentManager.data.input.metrics.EMCycleMetrics;
import com.autotune.experimentManager.utils.EMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitionToInitiateMetricsCollectionPhase extends AbstractBaseTransition{
    private static final Logger LOGGER = LoggerFactory.getLogger(TransitionToInitiateMetricsCollectionPhase.class);

    @Override
    public void transit(String runId) {
        LOGGER.info("Executing transition - {} on thread - {} For RunId - {}", this.getClass().toString(), Thread.currentThread().getId(), runId);
        EMCycleMetrics metrics = new EMCycleMetrics();
        metrics.setMean("1");
        metrics.setMode("1");
        metrics.setError("none");
        metrics.setP95("1");
        metrics.setP99("1");
        metrics.setScore("1");
        metrics.setP99point9("1");
        metrics.setP99point99("1");
        metrics.setP99point999("1");
        metrics.setP99point9999("1");
        metrics.setP100("1");
        ExperimentTrialData etd = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        for (EMConfigDeploymentMetrics mt : etd.getConfig().getEmConfigObject().getDeployments().getTrainingDeployment().getMetrics()) {
            mt.getResults().getWarmupResults().getCollectiveCyclesMetrics().getCycleMetricsList().add(metrics);
        }
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.processNextTransition(runId);
    }

    @Override
    public void processNextTransition(String runId) {
        EMUtil.EMExpStages nextStage;
        ExperimentTrialData trialData = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        if (trialData.getCurrentMeasurementCycle() < trialData.getConfig().getTotalCycles()) {
            trialData.setCurrentMeasurementCycle(trialData.getCurrentMeasurementCycle() + 1);
            nextStage = EMUtil.EMExpStages.INITIAL_LOAD_CHECK;
        } else {
            nextStage = EMTransitionRegistry.getNextStage(trialData.getTargetStage());
        }
        trialData.setCurrentStage(trialData.getTargetStage());
        trialData.setTargetStage(nextStage);
        LOGGER.info("Next Stage : {} ", nextStage.toString());
        EMStageTransition transition = new EMStageTransition(runId, nextStage);
        EMStageProcessQueue.getStageProcessQueueInstance().getQueue().add(transition);
        ExperimentManager.notifyQueueProcessor();
    }
}
