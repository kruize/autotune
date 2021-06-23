package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.utils.EMConstants;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class TransitionToDeployConfig extends AbstractBaseTransition{

    @Override
    public void transit(String runId) {
        ExperimentTrialData trialData = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        System.out.println("in stage two - Deploy Config");
        KubernetesClient client = new DefaultKubernetesClient();
        Deployment createdDeployment = client.apps().deployments().inNamespace(EMConstants.DeploymentConstants.NAMESPACE).createOrReplace(trialData.getTrailDeployment());
        trialData.setTrailDeployment(createdDeployment);
    }
}
