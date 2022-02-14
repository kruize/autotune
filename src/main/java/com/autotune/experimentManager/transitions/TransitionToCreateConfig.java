package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.transitions.util.TransistionHelper;
import com.autotune.experimentManager.utils.EMConstants;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.RollingUpdateDeployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TransitionToCreateConfig extends AbstractBaseTransition {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransitionToCreateConfig.class);

    @Override
    public void transit(String runId) {
        ExperimentTrialData trialData = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        JSONArray containerConfigs = trialData.getConfig().getTrainingContainers();
        KubernetesClient client = new DefaultKubernetesClient();
        RollingUpdateDeployment rud = new RollingUpdateDeployment();
        IntOrString maxSurge = new IntOrString(1);
        IntOrString maxUnavailable = new IntOrString(0);
        rud.setMaxSurge(maxSurge);
        rud.setMaxUnavailable(maxUnavailable);
        client.apps().deployments().inNamespace(EMConstants.DeploymentConstants.NAMESPACE).withName(trialData.getConfig().getDeploymentName()).edit().editSpec().editOrNewStrategy().withRollingUpdate(rud).endStrategy().endSpec().done();
        Deployment currentDeployment = client.apps().deployments().inNamespace(EMConstants.DeploymentConstants.NAMESPACE).withName(trialData.getConfig().getDeploymentName()).get();
        trialData.setCurrentDeployment(currentDeployment);
        List<Container> deployedContainers = currentDeployment.getSpec().getTemplate().getSpec().getContainers();
        for (Container deployedAppContainer : deployedContainers) {
            JSONArray configs = TransistionHelper.ConfigHelper.getContainerConfig(deployedAppContainer.getName(), containerConfigs);
            for (Object config : configs) {
                JSONObject obj = (JSONObject) config;
                JSONObject containerObj = obj.getJSONObject("spec").getJSONObject("template").getJSONObject("spec")
                        .getJSONObject("container");
                ResourceRequirements resourcesRequirement = new ResourceRequirements();
                if (containerObj.has("resources")) {
                    resourcesRequirement = deployedAppContainer.getResources();
                    JSONObject resourcesRec = containerObj.getJSONObject("resources");
                    Map<String, Quantity> propertiesMap = new HashMap<String, Quantity>();
                    JSONObject requests = resourcesRec.getJSONObject("requests");
                    if (requests != null) {
                        Iterator<String> keysItr = requests.keys();
                        while (keysItr.hasNext()) {
                            String key = keysItr.next();
                            String value = requests.get(key).toString();
                            propertiesMap.put(key, new Quantity(value));
                        }

                        resourcesRequirement.setRequests(propertiesMap);

                    }
                    propertiesMap.clear();
                    JSONObject limits = resourcesRec.getJSONObject("limits");
                    if (limits != null) {
                        Iterator<String> keysItr = limits.keys();
                        while (keysItr.hasNext()) {
                            String key = keysItr.next();
                            String value = requests.get(key).toString();
                            propertiesMap.put(key, new Quantity(value));
                        }

                        resourcesRequirement.setLimits(propertiesMap);
                    }
                    deployedAppContainer.setResources(resourcesRequirement);
                }
                if (containerObj.has("env")) {
                    JSONObject recommendedEnv = containerObj.getJSONObject("env");
                    List<EnvVar> envList = new ArrayList<EnvVar>();
                    Iterator<String> recIter = recommendedEnv.keys();
                    while (recIter.hasNext()) {
                        String key = recIter.next();
                        String value = recommendedEnv.getString(key);

                        // setting env. variables
                        EnvVar arg = new EnvVar();
                        arg.setName(key);
                        arg.setValue(value);
                        envList.add(arg);
                    }
                    deployedAppContainer.setEnv(envList);
                }
            }
        }

        trialData.setTrailDeployment(currentDeployment);
        processNextTransition(runId);
    }
}
