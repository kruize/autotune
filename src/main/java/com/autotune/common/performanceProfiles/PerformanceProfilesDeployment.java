package com.autotune.common.performanceProfiles;

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.common.k8sObjects.KubernetesContexts;
import com.autotune.common.k8sObjects.Metric;
import com.autotune.common.k8sObjects.SloInfo;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import com.autotune.utils.AnalyzerConstants;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains information about the Performance Profile objects deployed in the cluster
 */
public class PerformanceProfilesDeployment {
    public static Map<String, PerformanceProfile> performanceProfileMap = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceProfile.class);

    /**
     * Get Performance Profile objects from kubernetes, and watch for any additions, modifications or deletions.
     *
     */
    public static void getPerformanceProfiles() {
        /* Watch for events (additions, modifications or deletions) of performance profile objects */
        Watcher<String> performanceProfileObjectWatcher = new Watcher<>() {
            @Override
            public void eventReceived(Action action, String resource) {
                PerformanceProfile performanceProfileObject;

                switch (action.toString().toUpperCase()) {
                    case "ADDED":
                        performanceProfileObject = getPerformanceProfile(resource);
                        if (performanceProfileObject != null) {
                            addPerformanceProfileObject(performanceProfileObject);
                        }
                        break;
                    case "MODIFIED":
                        performanceProfileObject = getPerformanceProfile(resource);
                        if (performanceProfileObject != null) {
                            // Check if any of the values have changed from the existing object in the map
                            if (!performanceProfileMap.get(performanceProfileObject.getName())
                                    .equals(performanceProfileObject)) {
                                deleteExistingPerformanceProfileObject(resource);
                                addPerformanceProfileObject(performanceProfileObject);
                            }
                        }
                        break;
                    case "DELETED":
                        deleteExistingPerformanceProfileObject(resource);
                    default:
                        break;
                }
            }

            @Override
            public void onClose(KubernetesClientException e) { }
        };

        KubernetesServices kubernetesServices = new KubernetesServicesImpl();
        kubernetesServices.addWatcher(KubernetesContexts.getPerformanceProfileCrdContext(), performanceProfileObjectWatcher);
    }

    private static PerformanceProfile getPerformanceProfile(String performanceProfileObjectJsonStr) {
        try {
            JSONObject performanceProfileObjectJson = new JSONObject(performanceProfileObjectJsonStr);
            JSONObject metadataJson = performanceProfileObjectJson
                    .getJSONObject(AnalyzerConstants.AutotuneObjectConstants.METADATA);

            String name;
            String k8s_type;
            double profile_version;
            SloInfo sloInfo;

            JSONObject sloJson;
            String slo_class = null;
            String direction = null;
            String objectiveFunction = null;

            name = metadataJson.optString(AnalyzerConstants.PERF_PROFILE_NAME);
            profile_version = Double.parseDouble(performanceProfileObjectJson.optString(AnalyzerConstants.PROFILE_VERSION,
                    String.valueOf(AnalyzerConstants.DEFAULT_PROFILE_VERSION)));
            k8s_type = performanceProfileObjectJson.optString(AnalyzerConstants.K8S_TYPE,AnalyzerConstants.DEFAULT_K8S_TYPE);
            sloJson = performanceProfileObjectJson.optJSONObject(AnalyzerConstants.AutotuneObjectConstants.SLO);

            if (sloJson != null) {
                slo_class = sloJson.optString(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS);
                direction = sloJson.optString(AnalyzerConstants.AutotuneObjectConstants.DIRECTION);
                objectiveFunction = sloJson.optString(AnalyzerConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION);
            }

            JSONArray functionVariables = new JSONArray();
            JSONArray aggregationFunctionsArr;
            if (sloJson != null) {
                functionVariables = sloJson.getJSONArray(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLES);
            }
            ArrayList<Metric> metricArrayList = new ArrayList<>();

            for (Object functionVariableObj : functionVariables) {
                JSONObject functionVariableJson = (JSONObject) functionVariableObj;

                String variableName = functionVariableJson.optString(AnalyzerConstants.AutotuneObjectConstants.NAME);
                String query = functionVariableJson.optString(AnalyzerConstants.AutotuneObjectConstants.QUERY);
                String datasource = functionVariableJson.optString(AnalyzerConstants.AutotuneObjectConstants.DATASOURCE);
                String valueType = functionVariableJson.optString(AnalyzerConstants.AutotuneObjectConstants.VALUE_TYPE);
                String kubernetes_object = functionVariableJson.optString(AnalyzerConstants.KUBERNETES_OBJECTS);

                aggregationFunctionsArr = ((JSONObject) functionVariableObj).getJSONArray(AnalyzerConstants.AGGREGATION_FUNCTIONS);

                HashMap<String, List<AggregationFunctions>>  aggregationFunctionsMap = new HashMap<>();
                List<AggregationFunctions> aggregationFunctionsList = new ArrayList<>();
                for (Object aggregationFunctionsObj : aggregationFunctionsArr) {
                    JSONObject aggregationFunctionsJson = (JSONObject) aggregationFunctionsObj;
                    String function = aggregationFunctionsJson.optString(AnalyzerConstants.FUNCTION);
                    String aggregationFunctionSQuery = aggregationFunctionsJson.optString(
                            AnalyzerConstants.AutotuneObjectConstants.QUERY);
                    String versions = aggregationFunctionsJson.optString(AnalyzerConstants.VERSIONS);

                    AggregationFunctions aggregationFunctions = new AggregationFunctions(function,
                            aggregationFunctionSQuery, versions);
                    aggregationFunctionsList.add(aggregationFunctions);
                }
                aggregationFunctionsMap.put(variableName, aggregationFunctionsList);

                Metric metric = new Metric(variableName,
                        query,
                        datasource,
                        valueType);
                metric.setAggregationFunctionsMap(aggregationFunctionsMap);
                metric.setKubernetesObject(kubernetes_object);

                metricArrayList.add(metric);
            }

            sloInfo = new SloInfo(slo_class,
                    objectiveFunction,
                    direction,
                    AnalyzerConstants.AutotuneObjectConstants.DEFAULT_HPO_ALGO_IMPL,
                    metricArrayList);
            return new PerformanceProfile(name, profile_version, k8s_type, sloInfo);

        } catch (InvalidValueException | NullPointerException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Delete the performance Profile object corresponding to the passed parameter
     * @param performanceProfileObject - removes the Performance Profile object from the map
     */
    private static void deleteExistingPerformanceProfileObject(String performanceProfileObject) {
        JSONObject performanceProfileObjectJson = new JSONObject(performanceProfileObject);
        String name = performanceProfileObjectJson.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.METADATA)
                .optString(AnalyzerConstants.AutotuneObjectConstants.NAME);

        performanceProfileMap.remove(name);
        LOGGER.info("Deleted performance profile object {}", name);
    }

    private static void addPerformanceProfileObject(PerformanceProfile performanceProfile) {
        performanceProfileMap.put(performanceProfile.getName(), performanceProfile);
        LOGGER.info("PerformanceProfile: ",performanceProfile);
    }

}
