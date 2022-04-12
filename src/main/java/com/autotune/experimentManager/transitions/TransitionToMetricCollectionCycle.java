package com.autotune.experimentManager.transitions;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.data.input.EMMetricInput;
import com.autotune.experimentManager.data.input.deployments.EMConfigDeploymentContainerConfig;
import com.autotune.experimentManager.data.input.metrics.EMMetricResult;
import com.autotune.experimentManager.data.iteration.EMIterationMetricResult;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMUtil;
import com.autotune.utils.GenericRestApiClient;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class TransitionToMetricCollectionCycle extends AbstractBaseTransition{

    @Override
    public void transit(String runId) {
        ExperimentTrialData trialData = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
        EMMetricResult emMetricData = new EMMetricResult();
        String podLabel = "app=tfb-qrh-deployment";
        String contName = null;
        try {
            System.out.println("Running metrics collection");
            GenericRestApiClient apiClient = new GenericRestApiClient(
                                                EMUtil.getBaseDataSourceUrl(
                                                    trialData.getConfig().getEmConfigObject().getInfo().getDataSourceInfo().getDatasources().get(0).getUrl(),
                                                    trialData.getConfig().getEmConfigObject().getInfo().getDataSourceInfo().getDatasources().get(0).getName()
                                                )
                                             );

            ArrayList<EMMetricInput> pod_metrics = trialData.getConfig().getEmConfigObject().getDeployments().getTrainingDeployment().getPodMetrics();
            ArrayList<EMMetricInput> container_metrics = new ArrayList<EMMetricInput>();
            for (EMConfigDeploymentContainerConfig config : trialData.getConfig().getEmConfigObject().getDeployments().getTrainingDeployment().getContainers()) {
                container_metrics.addAll(config.getContainerMetrics());
                contName = config.getContainerName();
            }
            for (EMMetricInput metricInput : pod_metrics) {
                Float mean_value = Float.MAX_VALUE;
                Float min_value = Float.MAX_VALUE;
                Float max_value = Float.MAX_VALUE;
                for (EMUtil.MetricResultType type : EMUtil.MetricResultType.values()) {
                    String query = EMUtil.buildQueryForType(metricInput.getQuery(), type);
                    System.out.println("Query before calling prometheus - " + query);
                    JSONObject jsonObject = apiClient.fetchMetricsJson(
                            EMConstants.HttpConstants.MethodType.GET,
                            query);
                    if (jsonObject.has("status")
                            && jsonObject.getString("status").equalsIgnoreCase("success")) {
                        if (jsonObject.has("data")
                                && jsonObject.getJSONObject("data").has("result")
                                && !jsonObject.getJSONObject("data").getJSONArray("result").isEmpty()) {
                            JSONArray result = jsonObject.getJSONObject("data").getJSONArray("result");
                            for (Object result_obj: result) {
                                JSONObject result_json = (JSONObject) result_obj;
                                if (result_json.has("value")
                                        && !result_json.getJSONArray("value").isEmpty()) {
                                    if (type == EMUtil.MetricResultType.MEAN)
                                        mean_value = Float.parseFloat(result_json.getJSONArray("value").getString(1));
                                    else if (type == EMUtil.MetricResultType.MAX)
                                        max_value = Float.parseFloat(result_json.getJSONArray("value").getString(1));
                                    else if (type == EMUtil.MetricResultType.MIN)
                                        min_value = Float.parseFloat(result_json.getJSONArray("value").getString(1));
                                }
                            }
                        }
                    }
                }
                EMIterationMetricResult emIterationMetricResult = trialData.getEmIterationManager()
                        .getIterationDataList()
                        .get(trialData.getEmIterationManager().getCurrentIteration()-1)
                        .getEmIterationResult()
                        .getIterationMetricResult(metricInput.getName());
                EMMetricResult emMetricResult = new EMMetricResult(false);
                if (mean_value != Float.MAX_VALUE)
                    emMetricResult.getEmMetricGenericResults().setMean(mean_value);
                if (min_value != Float.MAX_VALUE)
                    emMetricResult.getEmMetricGenericResults().setMin(min_value);
                if (max_value != Float.MAX_VALUE)
                    emMetricResult.getEmMetricGenericResults().setMax(max_value);
                if (trialData.getEmIterationManager()
                        .getIterationDataList()
                        .get(trialData.getEmIterationManager().getCurrentIteration()-1).getCurrentCycle()
                        >
                        trialData.getEmIterationManager()
                                .getIterationDataList()
                                .get(trialData.getEmIterationManager().getCurrentIteration()-1).getWarmCycles()
                ) {

                    emIterationMetricResult.addToMeasurementList(emMetricResult);
                }
                else {
                    emIterationMetricResult.addToWarmUpList(emMetricResult);
                }
            }
            KubernetesClient client = new DefaultKubernetesClient();
            System.out.println(podLabel);
            for (Pod pod : client.pods().inNamespace(trialData.getConfig().getDeploymentNamespace()).list().getItems()) {
                System.out.println(pod.getMetadata().toString());
            }
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
                    if (reframedQuery.split("\\}").length > 1 && null != reframedQuery.split("\\}")[1]){
                        System.out.println("Splitting");
                        reframedQuery = reframedQuery.split("\\{")[0] + "{container=\""+contName+"\",pod=\""+podName+"\"}" + reframedQuery.split("\\}")[1];
                    } else {
                        reframedQuery = reframedQuery.split("\\{")[0] + "{container=\""+contName+"\",pod=\""+podName+"\"}";
                    }
                }
                System.out.println("Reframed Query - " + reframedQuery);
                Float mean_value = Float.MAX_VALUE;
                Float min_value = Float.MAX_VALUE;
                Float max_value = Float.MAX_VALUE;
                for (EMUtil.MetricResultType type : EMUtil.MetricResultType.values()) {
                    String query = EMUtil.buildQueryForType(reframedQuery, type);
                    System.out.println("Query before calling prometheus - " + query);
                    JSONObject jsonObject = apiClient.fetchMetricsJson(
                            EMConstants.HttpConstants.MethodType.GET,
                            query);
                    if (jsonObject.has("status")
                            && jsonObject.getString("status").equalsIgnoreCase("success")) {
                        if (jsonObject.has("data")
                                && jsonObject.getJSONObject("data").has("result")
                                && !jsonObject.getJSONObject("data").getJSONArray("result").isEmpty()) {
                            JSONArray result = jsonObject.getJSONObject("data").getJSONArray("result");
                            if (EMUtil.QueryType.RUNTIME == EMUtil.detectQueryType(reframedQuery)) {
                                if (EMUtil.needsAggregatedResult(reframedQuery)) {
                                    System.out.println("Aggregating values");
                                    Float aggregatedValue = 0.0f;
                                    for (Object result_obj: result) {
                                        JSONObject result_json = (JSONObject) result_obj;
                                        if (result_json.has("value")
                                                && !result_json.getJSONArray("value").isEmpty()) {
                                            aggregatedValue = aggregatedValue + Float.parseFloat(result_json.getJSONArray("value").getString(1));
                                        }
                                    }
                                    if (type == EMUtil.MetricResultType.MEAN)
                                        mean_value = aggregatedValue;
                                    else if (type == EMUtil.MetricResultType.MAX)
                                        max_value = aggregatedValue;
                                    else if (type == EMUtil.MetricResultType.MIN)
                                        min_value = aggregatedValue;
                                }
                            } else {
                                for (Object result_obj: result) {
                                    JSONObject result_json = (JSONObject) result_obj;
                                    if (result_json.has("value")
                                            && !result_json.getJSONArray("value").isEmpty()) {
                                        if (type == EMUtil.MetricResultType.MEAN)
                                            mean_value = Float.parseFloat(result_json.getJSONArray("value").getString(1));
                                        else if (type == EMUtil.MetricResultType.MAX)
                                            max_value = Float.parseFloat(result_json.getJSONArray("value").getString(1));
                                        else if (type == EMUtil.MetricResultType.MIN)
                                            min_value = Float.parseFloat(result_json.getJSONArray("value").getString(1));
                                    }
                                }
                            }
                        }
                    }
                }
                EMIterationMetricResult emIterationMetricResult = trialData.getEmIterationManager()
                        .getIterationDataList()
                        .get(trialData.getEmIterationManager().getCurrentIteration()-1)
                        .getEmIterationResult()
                        .getIterationMetricResult(metricInput.getName());
                EMMetricResult emMetricResult = new EMMetricResult(false);
                if (mean_value != Float.MAX_VALUE)
                    emMetricResult.getEmMetricGenericResults().setMean(mean_value);
                if (min_value != Float.MAX_VALUE)
                    emMetricResult.getEmMetricGenericResults().setMin(min_value);
                if (max_value != Float.MAX_VALUE)
                    emMetricResult.getEmMetricGenericResults().setMax(max_value);
                if (trialData.getEmIterationManager()
                        .getIterationDataList()
                        .get(trialData.getEmIterationManager().getCurrentIteration()-1).getCurrentCycle()
                        >
                        trialData.getEmIterationManager()
                                .getIterationDataList()
                                .get(trialData.getEmIterationManager().getCurrentIteration()-1).getWarmCycles()
                ) {

                    emIterationMetricResult.addToMeasurementList(emMetricResult);
                }
                else {
                    emIterationMetricResult.addToWarmUpList(emMetricResult);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Live Metric Map :");
        EMUtil.printMetricMap(trialData);
        System.out.println("Current Cycle - " + trialData.getEmIterationManager().getIterationDataList().get(trialData.getEmIterationManager().getCurrentIteration()-1).getCurrentCycle());
        trialData.getEmIterationManager().getIterationDataList().get(trialData.getEmIterationManager().getCurrentIteration()-1).incrementCycle();
        System.out.println("Next Cycle - " + trialData.getEmIterationManager().getIterationDataList().get(trialData.getEmIterationManager().getCurrentIteration()-1).getCurrentCycle());
        processNextTransition(runId);
    }
}
