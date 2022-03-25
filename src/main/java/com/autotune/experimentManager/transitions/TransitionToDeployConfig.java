package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.core.EMIterationManager;
import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.data.iteration.EMIterationData;
import com.autotune.experimentManager.utils.EMConstants;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitionToDeployConfig extends AbstractBaseTransition{
    private static final Logger LOGGER = LoggerFactory.getLogger(TransitionToDeployConfig.class);

    @Override
    public void transit(String runId) {
        ExperimentTrialData trialData = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        EMIterationManager emIterationManager = trialData.getEmIterationManager();
        KubernetesClient client = new DefaultKubernetesClient();
        if(emIterationManager.getCurrentIteration() == 1) {
            LOGGER.info("Launching new config to the deployment");
            Deployment createdDeployment = client.apps().deployments().inNamespace(trialData.getConfig().getDeploymentNamespace()).createOrReplace(trialData.getTrailDeployment());
            trialData.setTrailDeployment(createdDeployment);
        } else {
            LOGGER.info("Restarting the pod ... ");
            client.apps().deployments().inNamespace(trialData.getConfig().getDeploymentNamespace())
                    .withName(trialData.getConfig().getDeploymentName())
                    .rolling()
                    .restart();
            LOGGER.info("Done.");
        }
        processNextTransition(runId);
    }
}
