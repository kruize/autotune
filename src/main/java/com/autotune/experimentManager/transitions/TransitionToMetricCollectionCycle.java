package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.data.input.EMMetricInput;
import com.autotune.experimentManager.data.input.deployments.EMConfigDeploymentContainerConfig;
import com.autotune.experimentManager.data.input.metrics.EMMetricResult;
import com.autotune.experimentManager.data.iteration.EMMetricData;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMUtil;
import com.autotune.utils.GenericRestApiClient;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class TransitionToMetricCollectionCycle extends AbstractBaseTransition{

    @Override
    public void transit(String runId) {
        ExperimentTrialData trialData = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        EMMetricResult emMetricData = new EMMetricResult();
        String podLabel = "app=galaxies-deployment";
        String contName = null;
        try {
            System.out.println("Running metrics collection");
            GenericRestApiClient apiClient = new GenericRestApiClient(
                                                EMUtil.getBaseDataSourceUrl(
                                                    trialData.getConfig().getEmConfigObject().getInfo().getDataSourceInfo().getDatasources().get(0).getUrl(),
                                                    trialData.getConfig().getEmConfigObject().getInfo().getDataSourceInfo().getDatasources().get(0).getName()
                                                )
                                             );

            ArrayList<EMMetricInput> pod_metrics = trialData.getConfig().getEmConfigObject().getDeployments().getTrainingDeployment().getMetrics();
            ArrayList<EMMetricInput> container_metrics = new ArrayList<EMMetricInput>();
            for (EMConfigDeploymentContainerConfig config : trialData.getConfig().getEmConfigObject().getDeployments().getTrainingDeployment().getContainers()) {
                container_metrics.addAll(config.getContainerMetrics());
                contName = config.getContainerName();
            }
            for (EMMetricInput metricInput : pod_metrics) {
                JSONObject jsonObject = apiClient.fetchMetricsJson(
                        EMConstants.HttpConstants.MethodType.GET,
                        metricInput.getQuery());
                System.out.println(jsonObject.toString(2));
            }
            KubernetesClient client = new DefaultKubernetesClient();
            System.out.println(podLabel);
            String podName = client.pods().inNamespace(trialData.getConfig().getDeploymentNamespace()).withLabel(podLabel).list().getItems().get(0).getMetadata().getName();
            System.out.println("PodName - " + podName);
            for (EMMetricInput metricInput: container_metrics) {
                String reframedQuery = metricInput.getQuery();
                if (reframedQuery.contains("$CONTAINER_LABEL$=\"\"")) {
                    reframedQuery.replace("$CONTAINER_LABEL$=\"\"", "container="+podLabel);
                }
                if (reframedQuery.contains("$POD_LABEL$")) {
                    reframedQuery.replace("$POD_LABEL$", "pod");
                }
                if (reframedQuery.contains("$$POD$$")) {
                    reframedQuery.replace("$POD$", podName);
                }
                System.out.println(reframedQuery);
                if (reframedQuery.contains("CONTAINER_LABEL")) {
                    reframedQuery = reframedQuery.split("\\{")[0] + "{container=\""+contName+"\",pod=\""+podName+"\"}";
                }
                System.out.println("Reframed Query - " + reframedQuery);
                JSONObject jsonObject = apiClient.fetchMetricsJson(
                        EMConstants.HttpConstants.MethodType.GET,
                        reframedQuery);
                System.out.println(jsonObject.toString(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Current Cycle - " + trialData.getEmIterationManager().getEmIterationData().get(trialData.getEmIterationManager().getCurrentIteration()-1).getCurrentCycle());
        trialData.getEmIterationManager().getEmIterationData().get(trialData.getEmIterationManager().getCurrentIteration()-1).incrementCycle();
        System.out.println("Next Cycle - " + trialData.getEmIterationManager().getEmIterationData().get(trialData.getEmIterationManager().getCurrentIteration()-1).getCurrentCycle());
        processNextTransition(runId);
    }
}
