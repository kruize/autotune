package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.utils.EMUtil;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitionToCleanDeployment implements BaseTransition{
    private static final Logger LOGGER = LoggerFactory.getLogger(TransitionToCleanDeployment.class);
    @Override
    public void transit(String runId) {
        LOGGER.info("Executing transition - TransitionToCleanDeployment on thread - {} For RunId - ", Thread.currentThread().getId(), runId);
        ExperimentTrialData trialData = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        System.out.println("Final Metric Map :");
        System.out.println("--------------- METRIC MAP START ---------------");
        EMUtil.printMetricMap(trialData);
        System.out.println("--------------- METRIC MAP END ---------------");
        trialData.setStatus(EMUtil.EMExpStatus.COMPLETED);
        KubernetesClient client = new DefaultKubernetesClient();
        LOGGER.info("Rolling back the pod to old config... ");
        client.apps().deployments().inNamespace(trialData.getConfig().getDeploymentNamespace()).createOrReplace(trialData.getDefaultDeployment());
        LOGGER.info("Done.");
    }

    @Override
    public void processNextTransition(String runId) {

    }
}
