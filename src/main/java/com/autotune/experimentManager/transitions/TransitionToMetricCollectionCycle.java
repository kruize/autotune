package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.data.input.metrics.EMMetricResult;
import com.autotune.experimentManager.data.iteration.EMMetricData;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMUtil;
import com.autotune.utils.GenericRestApiClient;

import java.io.IOException;

public class TransitionToMetricCollectionCycle extends AbstractBaseTransition{

    @Override
    public void transit(String runId) {
        System.out.println("Running metrics collection");
        ExperimentTrialData trialData = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        EMMetricResult emMetricData = new EMMetricResult();
        GenericRestApiClient apiClient = new GenericRestApiClient(
                                                EMUtil.getBaseDataSourceUrl(
                                                    trialData.getConfig().getEmConfigObject().getInfo().getDataSourceInfo().getDatasources().get(0).getUrl(),
                                                    trialData.getConfig().getEmConfigObject().getInfo().getDataSourceInfo().getDatasources().get(0).getName()
                                                )
                                             );
        try {
            if(false) {
                // Jus to stop this code to get executed
                apiClient.fetchMetricsJson(EMConstants.HttpConstants.MethodType.GET,
                        trialData.getConfig().getEmConfigObject().getDeployments().getTrainingDeployment().getContainers().get(0).getContainerMetrics().get(0).getQuery());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Current Cycle - " + trialData.getEmIterationManager().getEmIterationData().get(trialData.getEmIterationManager().getCurrentIteration()-1).getCurrentCycle());
        trialData.getEmIterationManager().getEmIterationData().get(trialData.getEmIterationManager().getCurrentIteration()-1).incrementCycle();
        System.out.println("Next Cycle - " + trialData.getEmIterationManager().getEmIterationData().get(trialData.getEmIterationManager().getCurrentIteration()-1).getCurrentCycle());
        processNextTransition(runId);
    }
}
