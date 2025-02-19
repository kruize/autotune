package com.autotune.analyzer.serviceObjects;

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.kruizeObject.*;
import com.autotune.analyzer.metadataProfiles.MetadataProfile;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.recommendations.NamespaceRecommendations;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForTimestamp;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.metrics.AggregationFunctions;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.data.result.NamespaceData;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Converters {
    private Converters() {

    }

    public static class KruizeObjectConverters {
        private static final Logger LOGGER = LoggerFactory.getLogger(KruizeObjectConverters.class);

        private KruizeObjectConverters() {

        }

        public static CreateExperimentAPIObject convertKruizeObjectToCreateExperimentSO(KruizeObject kruizeObject) {
            // Need to be implemented if needed
            return null;
        }

        public static KruizeObject convertCreateExperimentAPIObjToKruizeObject(CreateExperimentAPIObject createExperimentAPIObject) {
            KruizeObject kruizeObject = new KruizeObject();
            try {
                List<K8sObject> k8sObjectList = new ArrayList<>();
                List<KubernetesAPIObject> kubernetesAPIObjectsList = createExperimentAPIObject.getKubernetesObjects();
                for (KubernetesAPIObject kubernetesAPIObject : kubernetesAPIObjectsList) {
                    K8sObject k8sObject = null;
                    // check if exp type is null to support remote monitoring experiments
                    if (createExperimentAPIObject.isContainerExperiment()) {
                        // container recommendations experiment type
                        k8sObject = createContainerExperiment(kubernetesAPIObject);
                    } else if (createExperimentAPIObject.isNamespaceExperiment()) {
                        // namespace recommendations experiment type
                        k8sObject = createNamespaceExperiment(kubernetesAPIObject);
                    }
                    LOGGER.debug("Experiment Type: {}", createExperimentAPIObject.getExperimentType());
                    k8sObjectList.add(k8sObject);
                }
                kruizeObject.setKubernetes_objects(k8sObjectList);
                kruizeObject.setExperimentName(createExperimentAPIObject.getExperimentName());
                kruizeObject.setApiVersion(createExperimentAPIObject.getApiVersion());
                kruizeObject.setTarget_cluster(createExperimentAPIObject.getTargetCluster());
                kruizeObject.setClusterName(createExperimentAPIObject.getClusterName());
                kruizeObject.setMode(createExperimentAPIObject.getMode());
                kruizeObject.setPerformanceProfile(createExperimentAPIObject.getPerformanceProfile());
                kruizeObject.setDataSource(createExperimentAPIObject.getDatasource());
                kruizeObject.setExperimentType(createExperimentAPIObject.getExperimentType());
                kruizeObject.setSloInfo(createExperimentAPIObject.getSloInfo());
                kruizeObject.setTrial_settings(createExperimentAPIObject.getTrialSettings());
                TermSettings termSettings = createExperimentAPIObject.getRecommendationSettings().getTermSettings();
                RecommendationSettings recommendationSettings = new RecommendationSettings();
                recommendationSettings.setTermSettings(termSettings);
                ModelSettings modelSettings = createExperimentAPIObject.getRecommendationSettings().getModelSettings();
                recommendationSettings.setModelSettings(modelSettings);
                kruizeObject.setRecommendation_settings(recommendationSettings);
                kruizeObject.setExperiment_id(createExperimentAPIObject.getExperiment_id());
                kruizeObject.setStatus(createExperimentAPIObject.getStatus());
                kruizeObject.setExperiment_usecase_type(new ExperimentUseCaseType(kruizeObject));
                if (null != createExperimentAPIObject.getValidationData()) {
                    //Validation already done and it is getting loaded back from db
                    kruizeObject.setValidation_data(createExperimentAPIObject.getValidationData());
                }
            } catch (Exception e) {
                LOGGER.error("failed to convert CreateExperimentAPIObj To KruizeObject due to {} ", e.getMessage());
                LOGGER.debug(createExperimentAPIObject.toString());
                kruizeObject = null;
            }
            return kruizeObject;
        }

        // Generates K8sObject for container type experiments from KubernetesAPIObject
        public static K8sObject createContainerExperiment(KubernetesAPIObject kubernetesAPIObject) {
            K8sObject k8sObject = new K8sObject(kubernetesAPIObject.getName(), kubernetesAPIObject.getType(), kubernetesAPIObject.getNamespace());
            k8sObject.setNamespaceData(new NamespaceData());
            List<ContainerAPIObject> containerAPIObjects = kubernetesAPIObject.getContainerAPIObjects();
            HashMap<String, ContainerData> containerDataHashMap = new HashMap<>();
            for (ContainerAPIObject containerAPIObject : containerAPIObjects) {
                ContainerData containerData = new ContainerData(containerAPIObject.getContainer_name(),
                        containerAPIObject.getContainer_image_name(), new ContainerRecommendations(), null);
                containerDataHashMap.put(containerData.getContainer_name(), containerData);
            }
            k8sObject.setContainerDataMap(containerDataHashMap);
            return k8sObject;
        }

        // Generates K8sObject for namespace type experiments from KubernetesAPIObject
        public static K8sObject createNamespaceExperiment(KubernetesAPIObject kubernetesAPIObject) {
            K8sObject k8sObject = new K8sObject();
            k8sObject.setNamespace(kubernetesAPIObject.getNamespaceAPIObjects().getnamespace_name());
            HashMap<String, ContainerData> containerDataHashMap = new HashMap<>();
            k8sObject.setContainerDataMap(containerDataHashMap);
            NamespaceAPIObject namespaceAPIObject = kubernetesAPIObject.getNamespaceAPIObjects();
            k8sObject.setNamespaceData(new NamespaceData(namespaceAPIObject.getnamespace_name(), new NamespaceRecommendations(), null));
            return k8sObject;
        }


        public static ListRecommendationsAPIObject convertKruizeObjectToListRecommendationSO(
                KruizeObject kruizeObject,
                boolean getLatest,
                boolean checkForTimestamp,
                Timestamp monitoringEndTime) {
            ListRecommendationsAPIObject listRecommendationsAPIObject = new ListRecommendationsAPIObject();
            try {
                listRecommendationsAPIObject.setApiVersion(AnalyzerConstants.VersionConstants.APIVersionConstants.CURRENT_LIST_RECOMMENDATIONS_VERSION);
                listRecommendationsAPIObject.setExperimentName(kruizeObject.getExperimentName());
                listRecommendationsAPIObject.setClusterName(kruizeObject.getClusterName());
                listRecommendationsAPIObject.setExperimentType(kruizeObject.getExperimentType());
                List<KubernetesAPIObject> kubernetesAPIObjects = new ArrayList<>();
                KubernetesAPIObject kubernetesAPIObject;

                for (K8sObject k8sObject : kruizeObject.getKubernetes_objects()) {
                    kubernetesAPIObject = new KubernetesAPIObject(k8sObject.getName(), k8sObject.getType(), k8sObject.getNamespace());
                    // namespace recommendations experiment type
                    if (kruizeObject.isNamespaceExperiment()) {
                        processNamespaceRecommendations(k8sObject, kubernetesAPIObject, checkForTimestamp, getLatest, monitoringEndTime);
                    }
                    processContainerRecommendations(k8sObject, kubernetesAPIObject, checkForTimestamp, getLatest, monitoringEndTime);
                    kubernetesAPIObjects.add(kubernetesAPIObject);
                }
                listRecommendationsAPIObject.setKubernetesObjects(kubernetesAPIObjects);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return listRecommendationsAPIObject;
        }

        private static void processNamespaceRecommendations(K8sObject k8sObject, KubernetesAPIObject kubernetesAPIObject,
                                                            boolean checkForTimestamp, boolean getLatest, Timestamp monitoringEndTime) {
            NamespaceData clonedNamespaceData = Utils.getClone(k8sObject.getNamespaceData(), NamespaceData.class);
            if (clonedNamespaceData != null) {
                HashMap<Timestamp, MappedRecommendationForTimestamp> namespaceRecommendations = clonedNamespaceData.getNamespaceRecommendations().getData();

                if (checkForTimestamp) {
                    filterRecommendationsByTimestamp(namespaceRecommendations, monitoringEndTime);
                } else if (getLatest) {
                    filterRecommendationsByLatest(namespaceRecommendations);
                }

                NamespaceAPIObject namespaceAPIObject = new NamespaceAPIObject(
                        clonedNamespaceData.getNamespace_name(),
                        clonedNamespaceData.getNamespaceRecommendations(),
                        null);
                kubernetesAPIObject.setNamespaceAPIObject(namespaceAPIObject);
            }
        }

        private static void processContainerRecommendations(K8sObject k8sObject, KubernetesAPIObject kubernetesAPIObject,
                                                            boolean checkForTimestamp, boolean getLatest, Timestamp monitoringEndTime) {
            List<ContainerAPIObject> containerAPIObjects = new ArrayList<>();

            for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
                ContainerData clonedContainerData = Utils.getClone(containerData, ContainerData.class);

                if (clonedContainerData != null) {
                    HashMap<Timestamp, MappedRecommendationForTimestamp> recommendations = clonedContainerData.getContainerRecommendations().getData();

                    if (checkForTimestamp) {
                        filterRecommendationsByTimestamp(recommendations, monitoringEndTime);
                    } else if (getLatest) {
                        filterRecommendationsByLatest(recommendations);
                    }

                    ContainerAPIObject containerAPIObject = new ContainerAPIObject(
                            clonedContainerData.getContainer_name(),
                            clonedContainerData.getContainer_image_name(),
                            clonedContainerData.getContainerRecommendations(),
                            null);
                    containerAPIObjects.add(containerAPIObject);
                } else {
                    containerAPIObjects.add(new ContainerAPIObject(
                            containerData.getContainer_name(),
                            containerData.getContainer_image_name(),
                            containerData.getContainerRecommendations(),
                            null));
                }
            }

            kubernetesAPIObject.setContainerAPIObjects(containerAPIObjects);
        }

        private static void filterRecommendationsByTimestamp(HashMap<Timestamp, MappedRecommendationForTimestamp> recommendations,
                                                             Timestamp monitoringEndTime) {
            if (monitoringEndTime != null && recommendations.containsKey(monitoringEndTime)) {
                recommendations.keySet().removeIf(timestamp -> !timestamp.equals(monitoringEndTime));
            }
        }

        private static void filterRecommendationsByLatest(HashMap<Timestamp, MappedRecommendationForTimestamp> recommendations) {
            Timestamp latestTimestamp = null;
            List<Timestamp> timestampsToRemove = new ArrayList<>();

            for (Timestamp timestamp : recommendations.keySet()) {
                if (latestTimestamp == null || timestamp.after(latestTimestamp)) {
                    if (latestTimestamp != null) {
                        timestampsToRemove.add(latestTimestamp);
                    }
                    latestTimestamp = timestamp;
                } else {
                    timestampsToRemove.add(timestamp);
                }
            }

            for (Timestamp timestamp : timestampsToRemove) {
                recommendations.remove(timestamp);
            }
        }

        /**
         * @param containerData
         */
        public static void getLatestResults(ContainerData containerData) {
            if (null != containerData) {
                HashMap<Timestamp, IntervalResults> results = containerData.getResults();
                Timestamp latestTimestamp = null;
                List<Timestamp> tempList = new ArrayList<>();
                for (Timestamp timestamp : results.keySet()) {
                    if (null == latestTimestamp) {
                        latestTimestamp = timestamp;
                    } else {
                        if (timestamp.after(latestTimestamp)) {
                            tempList.add(latestTimestamp);
                            latestTimestamp = timestamp;
                        } else {
                            tempList.add(timestamp);
                        }
                    }
                }
                for (Timestamp timestamp : tempList) {
                    results.remove(timestamp);
                }
                containerData.setResults(results);
            }
        }

        public static ExperimentResultData convertUpdateResultsAPIObjToExperimentResultData(UpdateResultsAPIObject updateResultsAPIObject) {
            ExperimentResultData experimentResultData = new ExperimentResultData();
            experimentResultData.setVersion(updateResultsAPIObject.getApiVersion());
            experimentResultData.setIntervalStartTime(updateResultsAPIObject.getStartTimestamp());
            experimentResultData.setIntervalEndTime(updateResultsAPIObject.getEndTimestamp());
            experimentResultData.setExperiment_name(updateResultsAPIObject.getExperimentName());
            experimentResultData.setCluster_name(updateResultsAPIObject.getKruizeObject().getClusterName());
            List<KubernetesAPIObject> kubernetesAPIObjectList = updateResultsAPIObject.getKubernetesObjects();
            List<K8sObject> k8sObjectList = new ArrayList<>();
            for (KubernetesAPIObject kubernetesAPIObject : kubernetesAPIObjectList) {
                K8sObject k8sObject = new K8sObject(kubernetesAPIObject.getName(), kubernetesAPIObject.getType(), kubernetesAPIObject.getNamespace());
                List<ContainerAPIObject> containersList = kubernetesAPIObject.getContainerAPIObjects();
                HashMap<String, ContainerData> containerDataHashMap = new HashMap<>();
                for (ContainerAPIObject containerAPIObject : containersList) {
                    HashMap<AnalyzerConstants.MetricName, Metric> metricsMap = new HashMap<>();
                    HashMap<Timestamp, IntervalResults> resultsMap = new HashMap<>();
                    ContainerData containerData = new ContainerData(containerAPIObject.getContainer_name(), containerAPIObject.getContainer_image_name(), containerAPIObject.getContainerRecommendations(), metricsMap);
                    HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsHashMap = new HashMap<>();
                    for (Metric metric : containerAPIObject.getMetrics()) {
                        metricsMap.put(AnalyzerConstants.MetricName.valueOf(metric.getName()), metric);
                        MetricResults metricResults = metric.getMetricResult();
                        metricResults.setName(metric.getName());
                        IntervalResults intervalResults = new IntervalResults(updateResultsAPIObject.startTimestamp,
                                updateResultsAPIObject.endTimestamp);
                        metricResultsHashMap.put(AnalyzerConstants.MetricName.valueOf(metric.getName()), metricResults);
                        intervalResults.setMetricResultsMap(metricResultsHashMap);
                        resultsMap.put(updateResultsAPIObject.getEndTimestamp(), intervalResults);
                    }
                    containerData.setResults(resultsMap);
                    containerDataHashMap.put(containerData.getContainer_name(), containerData);
                }
                k8sObject.setContainerDataMap(containerDataHashMap);
                k8sObjectList.add(k8sObject);
            }
            experimentResultData.setKubernetes_objects(k8sObjectList);
            experimentResultData.setValidationOutputData(new ValidationOutputData(true, null, null));
            return experimentResultData;
        }

        public static PerformanceProfile convertInputJSONToCreatePerfProfile(String inputData) throws InvalidValueException {
            PerformanceProfile performanceProfile = null;
            if (inputData != null) {
                JSONObject jsonObject = new JSONObject(inputData);
                String perfProfileName = jsonObject.getString(AnalyzerConstants.AutotuneObjectConstants.NAME);
                Double profileVersion = jsonObject.has(AnalyzerConstants.PROFILE_VERSION) ? jsonObject.getDouble(AnalyzerConstants.PROFILE_VERSION) : null;
                String k8sType = jsonObject.has(AnalyzerConstants.PerformanceProfileConstants.K8S_TYPE) ? jsonObject.getString(AnalyzerConstants.PerformanceProfileConstants.K8S_TYPE) : null;
                JSONObject sloJsonObject = jsonObject.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.SLO);
                JSONArray functionVariableArray = sloJsonObject.getJSONArray(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLES);
                ArrayList<Metric> functionVariablesList = new ArrayList<>();
                for (Object object : functionVariableArray) {
                    JSONObject functionVarObj = (JSONObject) object;
                    String name = functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.NAME);
                    String datasource = functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.DATASOURCE);
                    String query = functionVarObj.has(AnalyzerConstants.AutotuneObjectConstants.QUERY) ? functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.QUERY) : null;
                    String valueType = functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.VALUE_TYPE);
                    String kubeObject = functionVarObj.has(AnalyzerConstants.KUBERNETES_OBJECT) ? functionVarObj.getString(AnalyzerConstants.KUBERNETES_OBJECT) : null;
                    Metric metric = new Metric(name, query, datasource, valueType, kubeObject);
                    JSONArray aggrFunctionArray = functionVarObj.has(AnalyzerConstants.AGGREGATION_FUNCTIONS) ? functionVarObj.getJSONArray(AnalyzerConstants.AGGREGATION_FUNCTIONS) : null;
                    for (Object innerObject : aggrFunctionArray) {
                        JSONObject aggrFuncJsonObject = (JSONObject) innerObject;
                        HashMap<String, AggregationFunctions> aggregationFunctionsMap = new HashMap<>();
                        String function = aggrFuncJsonObject.getString(AnalyzerConstants.FUNCTION);
                        String aggrFuncQuery = aggrFuncJsonObject.getString(KruizeConstants.JSONKeys.QUERY);
                        String version = aggrFuncJsonObject.has(KruizeConstants.JSONKeys.VERSION) ? aggrFuncJsonObject.getString(KruizeConstants.JSONKeys.VERSION) : null;
                        AggregationFunctions aggregationFunctions = new AggregationFunctions(function, aggrFuncQuery, version);
                        aggregationFunctionsMap.put(function, aggregationFunctions);
                        metric.setAggregationFunctionsMap(aggregationFunctionsMap);
                    }
                    functionVariablesList.add(metric);
                }
                String sloClass = sloJsonObject.has(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS) ? sloJsonObject.get(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS).toString() : null;
                String direction = sloJsonObject.has(AnalyzerConstants.AutotuneObjectConstants.DIRECTION) ? sloJsonObject.get(AnalyzerConstants.AutotuneObjectConstants.DIRECTION).toString() : null;
                ObjectiveFunction objectiveFunction = new Gson().fromJson(sloJsonObject.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION).toString(), ObjectiveFunction.class);
                SloInfo sloInfo = new SloInfo(sloClass, objectiveFunction, direction, functionVariablesList);
                performanceProfile = new PerformanceProfile(perfProfileName, profileVersion, k8sType, sloInfo);
            }
            return performanceProfile;
        }

        public static PerformanceProfile convertInputJSONToCreateMetricProfile(String inputData) throws InvalidValueException, Exception {
            PerformanceProfile metricProfile = null;
            if (inputData != null) {
                JSONObject jsonObject = new JSONObject(inputData);
                String apiVersion = jsonObject.getString(AnalyzerConstants.API_VERSION);
                String kind = jsonObject.getString(AnalyzerConstants.KIND);

                JSONObject metadataObject = jsonObject.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.METADATA);
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode metadata = objectMapper.readValue(metadataObject.toString(), ObjectNode.class);
                metadata.put("name", metadataObject.getString("name"));

                Double profileVersion = jsonObject.has(AnalyzerConstants.PROFILE_VERSION) ? jsonObject.getDouble(AnalyzerConstants.PROFILE_VERSION) : null;
                String k8sType = jsonObject.has(AnalyzerConstants.PerformanceProfileConstants.K8S_TYPE) ? jsonObject.getString(AnalyzerConstants.PerformanceProfileConstants.K8S_TYPE) : null;
                JSONObject sloJsonObject = jsonObject.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.SLO);
                JSONArray functionVariableArray = sloJsonObject.getJSONArray(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLES);
                ArrayList<Metric> functionVariablesList = new ArrayList<>();
                for (Object object : functionVariableArray) {
                    JSONObject functionVarObj = (JSONObject) object;
                    String name = functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.NAME);
                    String datasource = functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.DATASOURCE);
                    String query = functionVarObj.has(AnalyzerConstants.AutotuneObjectConstants.QUERY) ? functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.QUERY) : null;
                    String valueType = functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.VALUE_TYPE);
                    String kubeObject = functionVarObj.has(AnalyzerConstants.KUBERNETES_OBJECT) ? functionVarObj.getString(AnalyzerConstants.KUBERNETES_OBJECT) : null;
                    Metric metric = new Metric(name, query, datasource, valueType, kubeObject);
                    JSONArray aggrFunctionArray = functionVarObj.has(AnalyzerConstants.AGGREGATION_FUNCTIONS) ? functionVarObj.getJSONArray(AnalyzerConstants.AGGREGATION_FUNCTIONS) : null;
                    HashMap<String, AggregationFunctions> aggregationFunctionsMap = new HashMap<>();
                    for (Object innerObject : aggrFunctionArray) {
                        JSONObject aggrFuncJsonObject = (JSONObject) innerObject;
                        String function = aggrFuncJsonObject.getString(AnalyzerConstants.FUNCTION);
                        String aggrFuncQuery = aggrFuncJsonObject.getString(KruizeConstants.JSONKeys.QUERY);
                        String version = aggrFuncJsonObject.has(KruizeConstants.JSONKeys.VERSION) ? aggrFuncJsonObject.getString(KruizeConstants.JSONKeys.VERSION) : null;
                        AggregationFunctions aggregationFunctions = new AggregationFunctions(function, aggrFuncQuery, version);
                        aggregationFunctionsMap.put(function, aggregationFunctions);
                    }
                    metric.setAggregationFunctionsMap(aggregationFunctionsMap);
                    functionVariablesList.add(metric);
                }
                String sloClass = sloJsonObject.has(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS) ? sloJsonObject.get(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS).toString() : null;
                String direction = sloJsonObject.has(AnalyzerConstants.AutotuneObjectConstants.DIRECTION) ? sloJsonObject.get(AnalyzerConstants.AutotuneObjectConstants.DIRECTION).toString() : null;
                ObjectiveFunction objectiveFunction = new Gson().fromJson(sloJsonObject.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION).toString(), ObjectiveFunction.class);
                SloInfo sloInfo = new SloInfo(sloClass, objectiveFunction, direction, functionVariablesList);
                metricProfile = new PerformanceProfile(apiVersion, kind, metadata, profileVersion, k8sType, sloInfo);
            }
            return metricProfile;
        }

        public static MetadataProfile convertInputJSONToCreateMetadataProfile(String inputData) throws JsonProcessingException {
            MetadataProfile metadataProfile = null;

            if (inputData != null) {
                JSONObject jsonObject = new JSONObject(inputData);
                String apiVersion = jsonObject.getString(AnalyzerConstants.API_VERSION);
                String kind = jsonObject.getString(AnalyzerConstants.KIND);

                JSONObject metadataObject = jsonObject.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.METADATA);
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode metadata = objectMapper.readValue(metadataObject.toString(), ObjectNode.class);
                metadata.put(AnalyzerConstants.AutotuneObjectConstants.NAME, metadataObject.getString(AnalyzerConstants.AutotuneObjectConstants.NAME));

                Double profileVersion = jsonObject.has(AnalyzerConstants.PROFILE_VERSION) ? jsonObject.getDouble(AnalyzerConstants.PROFILE_VERSION) : null;
                String k8sType = jsonObject.has(AnalyzerConstants.MetadataProfileConstants.K8S_TYPE) ? jsonObject.getString(AnalyzerConstants.MetadataProfileConstants.K8S_TYPE) : null;
                String datasource = jsonObject.getString(AnalyzerConstants.MetadataProfileConstants.DATASOURCE);
                JSONArray queryVariableArray = jsonObject.getJSONArray(AnalyzerConstants.AutotuneObjectConstants.QUERY_VARIABLES);
                ArrayList<Metric> queryVariablesList = new ArrayList<>();
                for (Object object : queryVariableArray) {
                    JSONObject functionVarObj = (JSONObject) object;
                    String name = functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.NAME);
                    datasource = functionVarObj.has(AnalyzerConstants.AutotuneObjectConstants.DATASOURCE) ? functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.DATASOURCE) : datasource;
                    String query = functionVarObj.has(AnalyzerConstants.AutotuneObjectConstants.QUERY) ? functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.QUERY) : null;
                    String valueType = functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.VALUE_TYPE);
                    String kubeObject = functionVarObj.has(AnalyzerConstants.KUBERNETES_OBJECT) ? functionVarObj.getString(AnalyzerConstants.KUBERNETES_OBJECT) : null;
                    Metric metric = new Metric(name, query, datasource, valueType, kubeObject);
                    JSONArray aggrFunctionArray = functionVarObj.getJSONArray(AnalyzerConstants.AGGREGATION_FUNCTIONS);
                    HashMap<String, AggregationFunctions> aggregationFunctionsMap = new HashMap<>();
                    for (Object innerObject : aggrFunctionArray) {
                        JSONObject aggrFuncJsonObject = (JSONObject) innerObject;
                        String function = aggrFuncJsonObject.getString(AnalyzerConstants.FUNCTION);
                        String aggrFuncQuery = aggrFuncJsonObject.getString(KruizeConstants.JSONKeys.QUERY);
                        String version = aggrFuncJsonObject.has(KruizeConstants.JSONKeys.VERSION) ? aggrFuncJsonObject.getString(KruizeConstants.JSONKeys.VERSION) : null;
                        AggregationFunctions aggregationFunctions = new AggregationFunctions(function, aggrFuncQuery, version);
                        aggregationFunctionsMap.put(function, aggregationFunctions);
                    }
                    metric.setAggregationFunctionsMap(aggregationFunctionsMap);
                    queryVariablesList.add(metric);
                }

                metadataProfile = new MetadataProfile(apiVersion, kind, metadata, profileVersion, k8sType, datasource, queryVariablesList);
            }
            return metadataProfile;
        }


        public static ConcurrentHashMap<String, KruizeObject> ConvertUpdateResultDataToAPIResponse(ConcurrentHashMap<String, KruizeObject> mainKruizeExperimentMap) {
            return null;
        }

        public static ConcurrentHashMap<String, KruizeObject> ConvertRecommendationDataToAPIResponse(ConcurrentHashMap<String, KruizeObject> mainKruizeExperimentMap) {
            return null;
        }
    }
}
