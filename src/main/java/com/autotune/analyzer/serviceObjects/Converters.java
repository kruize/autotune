package com.autotune.analyzer.serviceObjects;

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotSupportedException;
import com.autotune.analyzer.kruizeLayer.*;
import com.autotune.analyzer.kruizeObject.*;
import com.autotune.analyzer.metadataProfiles.MetadataProfile;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.recommendations.Config;
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.recommendations.NamespaceRecommendations;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForTimestamp;
import com.autotune.analyzer.recommendations.objects.TermRecommendations;
import com.autotune.analyzer.recommendations.utils.RecommendationUtils;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.metrics.*;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.data.result.NamespaceData;
import com.autotune.common.data.system.info.device.accelerator.NvidiaAcceleratorDeviceData;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import org.json.JSONArray;
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
                    k8sObjectList.add(k8sObject);
                }
                // TODO : some modification to add custom terms and models automatically here

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
                RecommendationSettings recommendationSettings = getRecommendationSettings(createExperimentAPIObject);
                kruizeObject.setRecommendation_settings(recommendationSettings);
                kruizeObject.setExperiment_id(createExperimentAPIObject.getExperiment_id());
                kruizeObject.setStatus(createExperimentAPIObject.getStatus());
                kruizeObject.setExperiment_usecase_type(new ExperimentUseCaseType(kruizeObject));
                String target_cluster = createExperimentAPIObject.getTargetCluster();
                // metadata_profile field is applicable only for local monitoring experiments
                if (KruizeDeploymentInfo.local && target_cluster.equalsIgnoreCase(AnalyzerConstants.LOCAL)) {
                    kruizeObject.setMetadataProfile(createExperimentAPIObject.getMetadataProfile());
                }
                if (null != createExperimentAPIObject.getValidationData()) {
                    // Validation already done, and it is getting loaded back from db
                    kruizeObject.setValidation_data(createExperimentAPIObject.getValidationData());
                }
                kruizeObject.setCreation_date(createExperimentAPIObject.getCreationDate());
                kruizeObject.setUpdate_date(createExperimentAPIObject.getUpdateDate());
            } catch (Exception e) {
                LOGGER.error("failed to convert CreateExperimentAPIObj To KruizeObject due to {} ", e.getMessage());
                LOGGER.debug(createExperimentAPIObject.toString());
                kruizeObject = null;
            }
            return kruizeObject;
        }

        private static RecommendationSettings getRecommendationSettings(CreateExperimentAPIObject createExperimentAPIObject) {
            RecommendationSettings recommendationSettings = new RecommendationSettings();
            RecommendationSettings apiRecommendationSettings = createExperimentAPIObject.getRecommendationSettings();
            if (apiRecommendationSettings != null) {
                if (apiRecommendationSettings.getTermSettings() != null) {
                    recommendationSettings.setTermSettings(apiRecommendationSettings.getTermSettings());
                }
                if (apiRecommendationSettings.getModelSettings() != null) {
                    recommendationSettings.setModelSettings(apiRecommendationSettings.getModelSettings());
                }
                if (apiRecommendationSettings.getThreshold() != null) {
                    recommendationSettings.setThreshold(apiRecommendationSettings.getThreshold());
                }
            }
            return recommendationSettings;
        }

        // Generates K8sObject for container type experiments from KubernetesAPIObject
        public static K8sObject createContainerExperiment(KubernetesAPIObject kubernetesAPIObject) {
            K8sObject k8sObject = new K8sObject(kubernetesAPIObject.getName(), kubernetesAPIObject.getType(), kubernetesAPIObject.getNamespace());
            HashMap<String, NamespaceData> namespaceDataMap = new HashMap<>();
            k8sObject.setNamespaceDataMap(namespaceDataMap);

            HashMap<String, ContainerData> containerDataHashMap = new HashMap<>();
            for (ContainerAPIObject containerAPIObject : kubernetesAPIObject.getContainerAPIObjects()) {
                ContainerData containerData = new ContainerData(containerAPIObject.getContainer_name(),
                        containerAPIObject.getContainer_image_name(), new ContainerRecommendations(), null);
                if (null != containerAPIObject.getLayerMap() && !containerAPIObject.getLayerMap().isEmpty()) {
                    containerData.setLayerMap(containerAPIObject.getLayerMap());
                }
                containerDataHashMap.put(containerData.getContainer_name(), containerData);
            }
            k8sObject.setContainerDataMap(containerDataHashMap);
            return k8sObject;
        }

        // Generates K8sObject for namespace type experiments from KubernetesAPIObject
        public static K8sObject createNamespaceExperiment(KubernetesAPIObject kubernetesAPIObject) {
            K8sObject k8sObject = new K8sObject();
            k8sObject.setContainerDataMap(new HashMap<>());

            NamespaceAPIObject namespaceAPIObject = kubernetesAPIObject.getNamespaceAPIObject();
            k8sObject.setNamespace(namespaceAPIObject.getNamespace());

            HashMap<String, NamespaceData> namespaceDataHashMap = new HashMap<>();
            namespaceDataHashMap.put(namespaceAPIObject.getNamespace(), new NamespaceData(namespaceAPIObject.getNamespace(),
                    new NamespaceRecommendations(), null));
            k8sObject.setNamespaceDataMap(namespaceDataHashMap);
            return k8sObject;
        }


        /**
         * Converts a {@link KruizeObject} into the standard list recommendations service object.
         *
         * <p>The returned object preserves the legacy recommendation response structure used by
         * the existing recommendation APIs. Depending on the input flags, the recommendation data
         * is either reduced to the latest entry or filtered to a specific monitoring end time.
         *
         * @param kruizeObject the experiment data to convert
         * @param getLatest whether only the latest recommendation should be retained when no
         *                  explicit timestamp filtering is requested
         * @param checkForTimestamp whether recommendations should be filtered using
         *                          {@code monitoringEndTime}
         * @param monitoringEndTime the timestamp to retain when {@code checkForTimestamp} is true
         * @return the standard {@link ListRecommendationsAPIObject} representation for the
         *         supplied experiment
         */
        public static ListRecommendationsAPIObject convertKruizeObjectToListRecommendationSO(
                KruizeObject kruizeObject,
                boolean getLatest,
                boolean checkForTimestamp,
                Timestamp monitoringEndTime) {
            ListRecommendationsAPIObject listRecommendationsAPIObject = new ListRecommendationsAPIObject();
            try {
                listRecommendationsAPIObject.setApiVersion(KruizeConstants.KRUIZE_RECOMMENDATION_API_VERSION.LATEST.getVersionNumber());
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

        /**
         * Copies and filters namespace recommendations from a Kubernetes object into its API
         * representation.
         *
         * <p>Namespace recommendation data is cloned before filtering so that response shaping does
         * not mutate the original in-memory experiment state.
         *
         * @param k8sObject the source Kubernetes object
         * @param kubernetesAPIObject the target API object being populated
         * @param checkForTimestamp whether recommendations should be filtered to a single timestamp
         * @param getLatest whether only the latest recommendation should be retained
         * @param monitoringEndTime the monitoring end time used for timestamp filtering
         */
        private static void processNamespaceRecommendations(K8sObject k8sObject, KubernetesAPIObject kubernetesAPIObject,
                                                            boolean checkForTimestamp, boolean getLatest, Timestamp monitoringEndTime) {
            NamespaceData clonedNamespaceData = null;
            if(k8sObject.getNamespaceDataMap() != null && k8sObject.getNamespace() != null && k8sObject.getNamespaceDataMap().containsKey(k8sObject.getNamespace())) {
                clonedNamespaceData = Utils.getClone(k8sObject.getNamespaceDataMap().get(k8sObject.getNamespace()), NamespaceData.class);
            }
            if (clonedNamespaceData != null && clonedNamespaceData.getNamespaceRecommendations() != null
            && clonedNamespaceData.getNamespaceRecommendations().getData() != null) {
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

        /**
         * Copies and filters container recommendations from a Kubernetes object into its API
         * representation.
         *
         * <p>Container recommendation data is cloned before filtering so that the response can be
         * shaped independently of the original experiment object.
         *
         * @param k8sObject the source Kubernetes object
         * @param kubernetesAPIObject the target API object being populated
         * @param checkForTimestamp whether recommendations should be filtered to a single timestamp
         * @param getLatest whether only the latest recommendation should be retained
         * @param monitoringEndTime the monitoring end time used for timestamp filtering
         */
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

        /**
         * Retains only the recommendation entry that matches the requested monitoring end time.
         *
         * @param recommendations the recommendation map to filter in place
         * @param monitoringEndTime the timestamp that should remain in the map
         */
        private static void filterRecommendationsByTimestamp(HashMap<Timestamp, MappedRecommendationForTimestamp> recommendations,
                                                             Timestamp monitoringEndTime) {
            if (monitoringEndTime != null && recommendations.containsKey(monitoringEndTime)) {
                recommendations.keySet().removeIf(timestamp -> !timestamp.equals(monitoringEndTime));
            }
        }

        /**
         * Retains only the most recent recommendation entry in the supplied map.
         *
         * @param recommendations the recommendation map to filter in place
         */
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
                HashMap<String, NamespaceData> namespaceDataHashMap = new HashMap<>();

                if(containersList != null && !containersList.isEmpty()) {
                    for (ContainerAPIObject containerAPIObject : containersList) {
                        HashMap<AnalyzerConstants.MetricName, Metric> metricsMap = new HashMap<>();
                        HashMap<Timestamp, IntervalResults> resultsMap = new HashMap<>();
                        ContainerData containerData = new ContainerData(
                                containerAPIObject.getContainer_name(),
                                containerAPIObject.getContainer_image_name(),
                                containerAPIObject.getContainerRecommendations(),
                                metricsMap);
                        HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsHashMap = new HashMap<>();
                        HashMap<AnalyzerConstants.MetricName, AcceleratorMetricResult> acceleratorMetricResultHashMap = new HashMap<>();
                        for (Metric metric : containerAPIObject.getMetrics()) {
                            boolean isAcceleratorMetric = metric.getName().equalsIgnoreCase(AnalyzerConstants.MetricName.acceleratorCoreUsage.name())
                                    || metric.getName().equalsIgnoreCase(AnalyzerConstants.MetricName.acceleratorMemoryUsage.name())
                                    || metric.getName().equalsIgnoreCase(AnalyzerConstants.MetricName.acceleratorFrameBufferUsage.name());

                            metricsMap.put(AnalyzerConstants.MetricName.valueOf(metric.getName()), metric);
                            MetricResults metricResults = metric.getMetricResult();
                            metricResults.setName(metric.getName());
                            IntervalResults intervalResults = new IntervalResults(updateResultsAPIObject.startTimestamp,
                                    updateResultsAPIObject.endTimestamp);

                            if (isAcceleratorMetric) {
                                if (null != metricResults.getMetadata()
                                        && metricResults.getMetadata() instanceof AcceleratorMetricMetadata acceleratorMetricMetadata) {
                                    if (null != acceleratorMetricMetadata.getModelName()) {
                                        boolean isPartitionSupported = RecommendationUtils.checkIfModelIsKruizeSupportedMIG(acceleratorMetricMetadata.getModelName());
                                        boolean isPartition = (null != acceleratorMetricMetadata.getProfileName());
                                        NvidiaAcceleratorDeviceData acceleratorDeviceData = new NvidiaAcceleratorDeviceData(
                                                acceleratorMetricMetadata.getModelName(),
                                                acceleratorMetricMetadata.getNode(),
                                                null,
                                                null,
                                                acceleratorMetricMetadata.getProfileName(),
                                                isPartitionSupported,
                                                isPartition
                                        );
                                        AcceleratorMetricResult acceleratorMetricResult = new AcceleratorMetricResult(acceleratorDeviceData, metricResults);
                                        acceleratorMetricResultHashMap.put(AnalyzerConstants.MetricName.valueOf(metric.getName()), acceleratorMetricResult);
                                        // Storing in metrics to avoid the data irregular conversion from DB
                                        metricResultsHashMap.put(AnalyzerConstants.MetricName.valueOf(metric.getName()), metricResults);
                                    }
                                }
                            } else {
                                metricResultsHashMap.put(AnalyzerConstants.MetricName.valueOf(metric.getName()), metricResults);
                            }
                            intervalResults.setMetricResultsMap(metricResultsHashMap);
                            intervalResults.setAcceleratorMetricResultHashMap(acceleratorMetricResultHashMap);
                            resultsMap.put(updateResultsAPIObject.getEndTimestamp(), intervalResults);
                        }
                        containerData.setResults(resultsMap);
                        containerDataHashMap.put(containerData.getContainer_name(), containerData);
                    }
                    k8sObject.setContainerDataMap(containerDataHashMap);
                    k8sObjectList.add(k8sObject);
                } else if (kubernetesAPIObject.getNamespaceAPIObject() != null) {
                    NamespaceAPIObject namespaceAPIObject = kubernetesAPIObject.getNamespaceAPIObject();

                    HashMap<AnalyzerConstants.MetricName, Metric> metricsMap = new HashMap<>();
                    HashMap<Timestamp, IntervalResults> resultsMap = new HashMap<>();
                    NamespaceData namespaceData = new NamespaceData(namespaceAPIObject.getNamespace(), namespaceAPIObject.getNamespaceRecommendations(), metricsMap);
                    HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsHashMap = new HashMap<>();
                        for (Metric metric : namespaceAPIObject.getMetrics()) {
                            metricsMap.put(AnalyzerConstants.MetricName.valueOf(metric.getName()), metric);
                            MetricResults metricResults = metric.getMetricResult();
                            metricResults.setName(metric.getName());
                            IntervalResults intervalResults = new IntervalResults(updateResultsAPIObject.getStartTimestamp(),
                                    updateResultsAPIObject.getEndTimestamp());
                            metricResultsHashMap.put(AnalyzerConstants.MetricName.valueOf(metric.getName()), metricResults);
                            intervalResults.setMetricResultsMap(metricResultsHashMap);
                            resultsMap.put(updateResultsAPIObject.getEndTimestamp(), intervalResults);
                        }
                        namespaceData.setResults(resultsMap);
                        namespaceDataHashMap.put(namespaceData.getNamespace_name(), namespaceData);

                    k8sObject.setNamespaceDataMap(namespaceDataHashMap);
                    k8sObjectList.add(k8sObject);
                } else {
                    LOGGER.debug("Missing container/namespace data from the input json {}", kubernetesAPIObject);
                }
            }
            experimentResultData.setKubernetes_objects(k8sObjectList);
            experimentResultData.setValidationOutputData(new ValidationOutputData(true, null, null));
            return experimentResultData;
        }

        public static PerformanceProfile convertInputJSONToCreatePerfProfile(String inputData) throws InvalidValueException, Exception {
            PerformanceProfile performanceProfile = null;
            SloInfo sloInfo = null;
            if (inputData != null) {
                JSONObject jsonObject = new JSONObject(inputData);
                String perfProfileName = jsonObject.has(AnalyzerConstants.AutotuneObjectConstants.NAME) ? jsonObject.getString(AnalyzerConstants.AutotuneObjectConstants.NAME) : null;
                double profileVersion = jsonObject.has(AnalyzerConstants.PROFILE_VERSION) ? jsonObject.getDouble(AnalyzerConstants.PROFILE_VERSION) : AnalyzerConstants.PerformanceProfileConstants.ZERO_VALUE;
                String k8sType = jsonObject.has(AnalyzerConstants.PerformanceProfileConstants.K8S_TYPE) ? jsonObject.getString(AnalyzerConstants.PerformanceProfileConstants.K8S_TYPE) : null;
                JSONObject sloJsonObject = jsonObject.has(AnalyzerConstants.AutotuneObjectConstants.SLO) ? jsonObject.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.SLO) : null;
                if (sloJsonObject != null) {
                    JSONArray functionVariableArray = sloJsonObject.has(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLES) ? sloJsonObject.getJSONArray(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLES) : null;
                    ArrayList<Metric> functionVariablesList = new ArrayList<>();
                    if (functionVariableArray != null) {
                        for (Object object : functionVariableArray) {
                            JSONObject functionVarObj = (JSONObject) object;
                            String name = functionVarObj.optString(AnalyzerConstants.AutotuneObjectConstants.NAME, null);
                            String datasource = functionVarObj.optString(AnalyzerConstants.AutotuneObjectConstants.DATASOURCE, null);
                            String query = functionVarObj.optString(AnalyzerConstants.AutotuneObjectConstants.QUERY, null);
                            String valueType = functionVarObj.optString(AnalyzerConstants.AutotuneObjectConstants.VALUE_TYPE, null);
                            String kubeObject = functionVarObj.optString(AnalyzerConstants.KUBERNETES_OBJECT, null);
                            Metric metric = new Metric(name, query, datasource, valueType, kubeObject);
                            JSONArray aggrFunctionArray = functionVarObj.optJSONArray(AnalyzerConstants.AGGREGATION_FUNCTIONS);
                            if (aggrFunctionArray != null) {
                                HashMap<String, AggregationFunctions> aggregationFunctionsMap = new HashMap<>();
                                for (Object innerObject : aggrFunctionArray) {
                                    try {
                                        JSONObject aggrFuncJsonObject = (JSONObject) innerObject;
                                        String function = aggrFuncJsonObject.optString(AnalyzerConstants.FUNCTION, null);
                                        String aggrFuncQuery = aggrFuncJsonObject.optString(KruizeConstants.JSONKeys.QUERY, null);
                                        String version = aggrFuncJsonObject.optString(KruizeConstants.JSONKeys.VERSION, null);
                                        AggregationFunctions aggregationFunctions = new AggregationFunctions(function, aggrFuncQuery, version);
                                        aggregationFunctionsMap.put(function, aggregationFunctions);
                                    } catch (Exception e) {
                                        LOGGER.info(e.getMessage());
                                        throw new Exception(e.getMessage());
                                    }
                                }
                                metric.setAggregationFunctionsMap(aggregationFunctionsMap);
                            }
                            functionVariablesList.add(metric);
                        }
                    }
                    String sloClass = sloJsonObject.has(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS) ? sloJsonObject.get(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS).toString() : null;
                    String direction = sloJsonObject.has(AnalyzerConstants.AutotuneObjectConstants.DIRECTION) ? sloJsonObject.get(AnalyzerConstants.AutotuneObjectConstants.DIRECTION).toString() : null;
                    JSONObject objectiveFunctionJson = sloJsonObject.optJSONObject(AnalyzerConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION);
                    ObjectiveFunction objectiveFunction = objectiveFunctionJson != null ? new Gson().fromJson(objectiveFunctionJson.toString(), ObjectiveFunction.class) : null;
                    sloInfo = new SloInfo(sloClass, objectiveFunction, direction, functionVariablesList);
                }
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

        public static KruizeLayer convertInputJSONToCreateLayer(String inputData) throws Exception, MonitoringAgentNotSupportedException {
            KruizeLayer kruizeLayer = null;

            if (inputData != null) {
                JSONObject jsonObject = new JSONObject(inputData);
                String apiVersion = jsonObject.getString(AnalyzerConstants.API_VERSION);
                String kind = jsonObject.getString(AnalyzerConstants.KIND);

                // Parse metadata
                JSONObject metadataObject = jsonObject.optJSONObject(AnalyzerConstants.AutotuneObjectConstants.METADATA);
                String name = null;
                if (metadataObject != null) {
                    name = metadataObject.optString(AnalyzerConstants.AutotuneObjectConstants.NAME, null);
                }

                // Parse basic layer fields
                String layerName = jsonObject.optString(AnalyzerConstants.AutotuneConfigConstants.LAYER_NAME, null);
                String details = jsonObject.has(AnalyzerConstants.AutotuneConfigConstants.DETAILS) ? jsonObject.getString(AnalyzerConstants.AutotuneConfigConstants.DETAILS) : null;

                // Parse layer_presence
                String presence = null;
                List<LayerPresenceQuery> queries = null;
                String labelName = null;
                String labelValue = null;

                JSONObject layerPresenceObject = jsonObject.optJSONObject("layer_presence");
                if (layerPresenceObject != null) {
                    presence = layerPresenceObject.has("presence") ? layerPresenceObject.getString("presence") : null;

                    // Parse queries array if present
                    if (layerPresenceObject.has("queries")) {
                        JSONArray queriesArray = layerPresenceObject.getJSONArray("queries");
                        queries = new ArrayList<>();
                        for (Object queryObj : queriesArray) {
                            JSONObject queryJsonObject = (JSONObject) queryObj;
                            String datasource = queryJsonObject.getString("datasource");
                            String query = queryJsonObject.getString("query");
                            String key = queryJsonObject.has("key") ? queryJsonObject.getString("key") : null;
                            LayerPresenceQuery layerPresenceQuery = new LayerPresenceQuery(datasource, query, key);
                            queries.add(layerPresenceQuery);
                        }
                    }

                    // Parse label array if present
                    if (layerPresenceObject.has("label")) {
                        JSONArray labelArray = layerPresenceObject.getJSONArray("label");
                        if (labelArray.length() > 0) {
                            JSONObject labelObject = labelArray.getJSONObject(0);
                            labelName = labelObject.getString("name");
                            labelValue = labelObject.getString("value");
                        }
                    }
                }

                // Parse tunables array
                ArrayList<Tunable> tunables = null;
                JSONArray tunablesArray = jsonObject.optJSONArray("tunables");
                if (tunablesArray != null) {
                    tunables = new ArrayList<>();
                    for (Object tunableObj : tunablesArray) {
                        JSONObject tunableJsonObject = (JSONObject) tunableObj;
                        Tunable tunable = new Gson().fromJson(tunableJsonObject.toString(), Tunable.class);
                        tunables.add(tunable);
                    }
                }

                // Create LayerMetadata object
                LayerMetadata metadata = new LayerMetadata();
                metadata.setName(name);

                // Create LayerPresence object
                LayerPresence layerPresence = new LayerPresence();
                layerPresence.setPresence(presence);
                layerPresence.setQueries(queries);

                // Convert label name/value to List<LayerPresenceLabel> if present
                if (labelName != null && labelValue != null) {
                    List<LayerPresenceLabel> labels = new ArrayList<>();
                    labels.add(new LayerPresenceLabel(labelName, labelValue));
                    layerPresence.setLabel(labels);
                }

                kruizeLayer = new KruizeLayer(apiVersion, kind, metadata, layerName,
                        details, layerPresence, tunables);
            }
            return kruizeLayer;
        }


        public static ConcurrentHashMap<String, KruizeObject> ConvertUpdateResultDataToAPIResponse(ConcurrentHashMap<String, KruizeObject> mainKruizeExperimentMap) {
            return null;
        }

        public static ConcurrentHashMap<String, KruizeObject> ConvertRecommendationDataToAPIResponse(ConcurrentHashMap<String, KruizeObject> mainKruizeExperimentMap) {
            return null;
        }

        /**
         * Converts a {@link KruizeObject} into the V1 recommendations response structure.
         *
         * <p>The V1 conversion starts from the standard list recommendation conversion and then
         * normalizes the resulting object graph so it can be serialized in the new schema. The
         * normalization aligns the payload with the V1 semantics by:
         * <ul>
         *     <li>mapping pod count based recommendation data to {@code replicas}</li>
         *     <li>ensuring nested {@code resources.{requests,limits}} are populated</li>
         *     <li>populating {@code metrics_info.pod_count} from the existing term metrics</li>
         *     <li>preparing variation data to expose replicas in the V1 response</li>
         * </ul>
         *
         * @param ko the experiment data to convert
         * @param getLatest whether only the latest recommendation should be retained when no
         *                  explicit timestamp filtering is requested
         * @param checkForTimestamp whether recommendations should be filtered using
         *                          {@code monitoringEndTime}
         * @param monitoringEndTime the timestamp to retain when {@code checkForTimestamp} is true
         * @return the {@link ListRecommendationsAPIObject} prepared for V1 serialization
         */
        public static ListRecommendationsAPIObject convertKruizeObjectToListRecommendationSOV1(KruizeObject ko,
                                                                                               boolean getLatest,
                                                                                               boolean checkForTimestamp,
                                                                                               Timestamp monitoringEndTime) {
            /*
             * NOTE:
             * The current service object model already contains the core fields required by the
             * V1 response shape:
             * - Config / Variation support replicas + nested resources
             * - TermRecommendations supports metrics_info
             *
             * So the V1 conversion here starts from the standard recommendation conversion and
             * then normalizes the recommendation payload to the V1 semantics:
             * - pods_count -> replicas
             * - metrics_info.pod_count populated from the existing term metrics
             * - nested resources retained under resources.{limits,requests}
             *
             * Final JSON shaping is handled by the existing serialization layer and adapters.
             */
            ListRecommendationsAPIObject listRecommendationsAPIObject =
                    convertKruizeObjectToListRecommendationSO(ko, getLatest, checkForTimestamp, monitoringEndTime);

            try {
                normalizeKubernetesRecommendationsForV1(listRecommendationsAPIObject);
            } catch (Exception e) {
                LOGGER.error("Failed to convert recommendations to V1 schema for experiment {} due to {}",
                        ko.getExperimentName(), e.getMessage());
            }

            return listRecommendationsAPIObject;
        }

        /**
         * Applies V1 normalization to all Kubernetes objects contained in the converted response.
         *
         * @param listRecommendationsAPIObject the converted recommendation response to normalize
         */
        private static void normalizeKubernetesRecommendationsForV1(ListRecommendationsAPIObject listRecommendationsAPIObject) {
            if (listRecommendationsAPIObject == null || listRecommendationsAPIObject.getKubernetesObjects() == null) {
                return;
            }

            for (KubernetesAPIObject kubernetesAPIObject : listRecommendationsAPIObject.getKubernetesObjects()) {
                normalizeContainerRecommendationsForV1(kubernetesAPIObject);
                normalizeNamespaceRecommendationsForV1(kubernetesAPIObject);
            }
        }

        /**
         * Applies V1 normalization to all container recommendations for a Kubernetes object.
         *
         * @param kubernetesAPIObject the Kubernetes API object whose container recommendations are
         *                            to be normalized
         */
        private static void normalizeContainerRecommendationsForV1(KubernetesAPIObject kubernetesAPIObject) {
            if (kubernetesAPIObject == null || kubernetesAPIObject.getContainerAPIObjects() == null) {
                return;
            }

            for (ContainerAPIObject containerAPIObject : kubernetesAPIObject.getContainerAPIObjects()) {
                if (containerAPIObject != null
                        && containerAPIObject.getContainerRecommendations() != null
                        && containerAPIObject.getContainerRecommendations().getData() != null) {
                    normalizeMappedRecommendationsForV1(containerAPIObject.getContainerRecommendations().getData());
                }
            }
        }

        /**
         * Applies V1 normalization to namespace recommendations for a Kubernetes object.
         *
         * @param kubernetesAPIObject the Kubernetes API object whose namespace recommendations are
         *                            to be normalized
         */
        private static void normalizeNamespaceRecommendationsForV1(KubernetesAPIObject kubernetesAPIObject) {
            if (kubernetesAPIObject == null
                    || kubernetesAPIObject.getNamespaceAPIObject() == null
                    || kubernetesAPIObject.getNamespaceAPIObject().getNamespaceRecommendations() == null
                    || kubernetesAPIObject.getNamespaceAPIObject().getNamespaceRecommendations().getData() == null) {
                return;
            }

            normalizeMappedRecommendationsForV1(
                    kubernetesAPIObject.getNamespaceAPIObject().getNamespaceRecommendations().getData());
        }

        /**
         * Applies V1 normalization to all timestamped recommendation entries in a recommendation
         * data map.
         *
         * @param recommendationDataMap the timestamp keyed recommendation data to normalize
         */
        private static void normalizeMappedRecommendationsForV1(
                HashMap<Timestamp, MappedRecommendationForTimestamp> recommendationDataMap) {
            if (recommendationDataMap == null) {
                return;
            }

            for (MappedRecommendationForTimestamp recommendationForTimestamp : recommendationDataMap.values()) {
                if (recommendationForTimestamp == null) {
                    continue;
                }

                normalizeConfigForV1(recommendationForTimestamp.getCurrentConfig());

                HashMap<String, TermRecommendations> recommendationForTermHashMap =
                        recommendationForTimestamp.getRecommendationForTermHashMap();
                if (recommendationForTermHashMap == null) {
                    continue;
                }

                for (TermRecommendations termRecommendations : recommendationForTermHashMap.values()) {
                    normalizeTermRecommendationsForV1(termRecommendations);
                }
            }
        }

        /**
         * Applies V1 normalization to term-level recommendation data.
         *
         * <p>This includes normalizing term metrics information and each model-specific
         * recommendation entry under the term.
         *
         * @param termRecommendations the term recommendation data to normalize
         */
        private static void normalizeTermRecommendationsForV1(TermRecommendations termRecommendations) {
            if (termRecommendations == null) {
                return;
            }

            normalizeMetricsInfoForV1(termRecommendations);

            HashMap<String, com.autotune.analyzer.recommendations.objects.MappedRecommendationForModel>
                    recommendationForModelHashMap = termRecommendations.getRecommendationForModelHashMap();
            if (recommendationForModelHashMap == null) {
                return;
            }

            for (com.autotune.analyzer.recommendations.objects.MappedRecommendationForModel recommendationForModel
                    : recommendationForModelHashMap.values()) {
                normalizeModelRecommendationForV1(recommendationForModel);
            }
        }

        /**
         * Applies V1 normalization to a model-specific recommendation entry.
         *
         * <p>The model recommendation is updated so that pod count is surfaced as replicas in the
         * config and variation sections, nested resource structures are ensured, and the legacy
         * pod count field is cleared from the serialized response path.
         *
         * @param recommendationForModel the model recommendation to normalize
         */
        private static void normalizeModelRecommendationForV1(
                com.autotune.analyzer.recommendations.objects.MappedRecommendationForModel recommendationForModel) {
            if (recommendationForModel == null) {
                return;
            }

            int podsCount = recommendationForModel.getPodsCount();
            if (recommendationForModel.getConfig() != null) {
                recommendationForModel.getConfig().setReplicas(podsCount);
                normalizeConfigForV1(recommendationForModel.getConfig());
            }
            if (recommendationForModel.getVariation() != null) {
                recommendationForModel.getVariation().setReplicas(podsCount);
                normalizeVariationForV1(recommendationForModel.getVariation());
            }

            recommendationForModel.setPodsCount(0);
        }

        /**
         * Ensures that a config object is ready for V1 serialization.
         *
         * <p>If nested resources are not already present, they are populated from the legacy
         * top-level requests and limits maps.
         *
         * @param config the config object to normalize
         */
        private static void normalizeConfigForV1(Config config) {
            if (config == null) {
                return;
            }
            populateResourcesIfMissing(config.getResources(), config.getRequests(), config.getLimits(), true, config);
        }

        /**
         * Ensures that a variation object is ready for V1 serialization.
         *
         * <p>If nested resources are not already present, they are populated from the legacy
         * top-level requests and limits maps.
         *
         * @param variation the variation object to normalize
         */
        private static void normalizeVariationForV1(com.autotune.analyzer.recommendations.Variation variation) {
            if (variation == null) {
                return;
            }
            populateResourcesIfMissing(variation.getResources(), variation.getRequests(), variation.getLimits(), false, variation);
        }

        /**
         * Populates nested resources on a config or variation object when they are absent.
         *
         * <p>This helper preserves existing nested resources and only derives a new
         * {@code resources} map from the legacy {@code requests} and {@code limits} fields when
         * necessary.
         *
         * @param existingResources the already populated resources map, if any
         * @param requests the legacy requests map
         * @param limits the legacy limits map
         * @param isConfig whether the target object is a {@link Config}; otherwise it is treated as
         *                 a {@link com.autotune.analyzer.recommendations.Variation}
         * @param target the config or variation object to update
         */
        private static void populateResourcesIfMissing(
                HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> existingResources,
                java.util.Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requests,
                java.util.Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limits,
                boolean isConfig,
                Object target) {
            if (existingResources != null) {
                return;
            }

            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> resources =
                    new HashMap<>();

            if (requests != null) {
                resources.put(AnalyzerConstants.ResourceSetting.requests, new HashMap<>(requests));
            }
            if (limits != null) {
                resources.put(AnalyzerConstants.ResourceSetting.limits, new HashMap<>(limits));
            }

            if (resources.isEmpty()) {
                return;
            }

            if (isConfig) {
                ((Config) target).setResources(resources);
            } else {
                ((com.autotune.analyzer.recommendations.Variation) target).setResources(resources);
            }
        }

        /**
         * Ensures that term metrics information exposes the V1 {@code pod_count} metric entry.
         *
         * <p>The current term recommendations model stores {@code metricsInfo} internally, so this
         * method accesses it reflectively and augments it with a {@code pod_count} entry derived
         * from the existing metrics when available.
         *
         * @param termRecommendations the term recommendation whose metrics information is to be
         *                            normalized
         */
        private static void normalizeMetricsInfoForV1(TermRecommendations termRecommendations) {
            try {
                java.lang.reflect.Field metricsInfoField =
                        TermRecommendations.class.getDeclaredField("metricsInfo");
                metricsInfoField.setAccessible(true);

                HashMap<String, MetricAggregationInfoResults> metricsInfo =
                        (HashMap<String, MetricAggregationInfoResults>) metricsInfoField.get(termRecommendations);

                if (metricsInfo == null || metricsInfo.isEmpty() || metricsInfo.containsKey("pod_count")) {
                    return;
                }

                MetricAggregationInfoResults podMetric = getPodCountMetric(metricsInfo);
                if (podMetric != null) {
                    termRecommendations.addMetricsInfo("pod_count", podMetric);
                }
            } catch (Exception e) {
                LOGGER.debug("Unable to normalize metrics_info for V1 schema due to {}", e.getMessage());
            }
        }

        private static MetricAggregationInfoResults getPodCountMetric(
                HashMap<String, MetricAggregationInfoResults> metricsInfo) {
            if (metricsInfo.containsKey("pods_count")) {
                return metricsInfo.get("pods_count");
            }
            if (metricsInfo.containsKey("replicas")) {
                return metricsInfo.get("replicas");
            }
            return null;
        }
    }
}
