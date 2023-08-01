package com.autotune.analyzer.serviceObjects;

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.kruizeObject.ExperimentUseCaseType;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.kruizeObject.ObjectiveFunction;
import com.autotune.analyzer.kruizeObject.SloInfo;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.recommendations.*;
import com.autotune.analyzer.recommendations.summary.Namespaces;
import com.autotune.analyzer.recommendations.summary.NotificationsSummary;
import com.autotune.analyzer.recommendations.summary.RecommendationSummary;
import com.autotune.analyzer.recommendations.summary.Summary;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.AggregationFunctions;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.Utils;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.*;
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
                    K8sObject k8sObject = new K8sObject(kubernetesAPIObject.getName(), kubernetesAPIObject.getType(), kubernetesAPIObject.getNamespace());
                    List<ContainerAPIObject> containerAPIObjects = kubernetesAPIObject.getContainerAPIObjects();
                    HashMap<String, ContainerData> containerDataHashMap = new HashMap<>();
                    for (ContainerAPIObject containerAPIObject : containerAPIObjects) {
                        ContainerData containerData = new ContainerData(containerAPIObject.getContainer_name(),
                                containerAPIObject.getContainer_image_name(), new ContainerRecommendations(), null);
                        containerDataHashMap.put(containerData.getContainer_name(), containerData);
                    }
                    k8sObject.setContainerDataMap(containerDataHashMap);
                    k8sObjectList.add(k8sObject);
                }
                kruizeObject.setKubernetes_objects(k8sObjectList);
                kruizeObject.setExperimentName(createExperimentAPIObject.getExperimentName());
                kruizeObject.setApiVersion(createExperimentAPIObject.getApiVersion());
                kruizeObject.setTarget_cluster(createExperimentAPIObject.getTargetCluster());
                kruizeObject.setClusterName(createExperimentAPIObject.getClusterName());
                kruizeObject.setMode(createExperimentAPIObject.getMode());
                kruizeObject.setPerformanceProfile(createExperimentAPIObject.getPerformanceProfile());
                kruizeObject.setSloInfo(createExperimentAPIObject.getSloInfo());
                kruizeObject.setTrial_settings(createExperimentAPIObject.getTrialSettings());
                kruizeObject.setRecommendation_settings(createExperimentAPIObject.getRecommendationSettings());
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

        public static ListRecommendationsAPIObject convertKruizeObjectToListRecommendationSO(
                KruizeObject kruizeObject,
                boolean getLatest,
                boolean checkForTimestamp,
                Timestamp monitoringEndTime) {
            ListRecommendationsAPIObject listRecommendationsAPIObject = new ListRecommendationsAPIObject();
            try {
                listRecommendationsAPIObject.setApiVersion(kruizeObject.getApiVersion());
                listRecommendationsAPIObject.setExperimentName(kruizeObject.getExperimentName());
                listRecommendationsAPIObject.setClusterName(kruizeObject.getClusterName());
                List<KubernetesAPIObject> kubernetesAPIObjects = new ArrayList<>();
                KubernetesAPIObject kubernetesAPIObject;

                for (K8sObject k8sObject : kruizeObject.getKubernetes_objects()) {
                    kubernetesAPIObject = new KubernetesAPIObject(k8sObject.getName(), k8sObject.getType(), k8sObject.getNamespace());
                    HashMap<String, ContainerData> containerDataMap = new HashMap<>();
                    List<ContainerAPIObject> containerAPIObjects = new ArrayList<>();
                    for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
                        ContainerAPIObject containerAPIObject;
                        // if a Time stamp is passed it holds the priority than latest
                        if (checkForTimestamp) {
                            // This step causes a performance degradation, need to be replaced with a better flow of creating SO's
                            ContainerData clonedContainerData = Utils.getClone(containerData, ContainerData.class);
                            if (null != clonedContainerData) {
                                HashMap<Timestamp, HashMap<String, HashMap<String, Recommendation>>> recommendations
                                        = clonedContainerData.getContainerRecommendations().getData();
                                if (null != monitoringEndTime && recommendations.containsKey(monitoringEndTime)) {
                                    List<Timestamp> tempList = new ArrayList<>();
                                    for (Timestamp timestamp : recommendations.keySet()) {
                                        if (!timestamp.equals(monitoringEndTime))
                                            tempList.add(timestamp);
                                    }
                                    for (Timestamp timestamp : tempList) {
                                        recommendations.remove(timestamp);
                                    }
                                    clonedContainerData.getContainerRecommendations().setData(recommendations);
                                    containerAPIObject = new ContainerAPIObject(clonedContainerData.getContainer_name(),
                                            clonedContainerData.getContainer_image_name(),
                                            clonedContainerData.getContainerRecommendations(),
                                            null);
                                    containerAPIObjects.add(containerAPIObject);
                                }
                            }
                        } else if (getLatest) {
                            // This step causes a performance degradation, need to be replaced with a better flow of creating SO's
                            ContainerData clonedContainerData = Utils.getClone(containerData, ContainerData.class);
                            if (null != clonedContainerData) {
                                HashMap<Timestamp, HashMap<String, HashMap<String, Recommendation>>> recommendations
                                        = clonedContainerData.getContainerRecommendations().getData();
                                Timestamp latestTimestamp = null;
                                List<Timestamp> tempList = new ArrayList<>();
                                for (Timestamp timestamp : recommendations.keySet()) {
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
                                    recommendations.remove(timestamp);
                                }
                                clonedContainerData.getContainerRecommendations().setData(recommendations);
                                containerAPIObject = new ContainerAPIObject(clonedContainerData.getContainer_name(),
                                        clonedContainerData.getContainer_image_name(),
                                        clonedContainerData.getContainerRecommendations(),
                                        null);
                                containerAPIObjects.add(containerAPIObject);
                            }
                        } else {
                            containerAPIObject = new ContainerAPIObject(containerData.getContainer_name(),
                                    containerData.getContainer_image_name(),
                                    containerData.getContainerRecommendations(),
                                    null);
                            containerAPIObjects.add(containerAPIObject);
                            containerDataMap.put(containerData.getContainer_name(), containerData);
                        }
                    }
                    kubernetesAPIObject.setContainerAPIObjects(containerAPIObjects);
                    kubernetesAPIObjects.add(kubernetesAPIObject);
                }
                listRecommendationsAPIObject.setKubernetesObjects(kubernetesAPIObjects);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return listRecommendationsAPIObject;
        }

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

        public static ConcurrentHashMap<String, KruizeObject> ConvertUpdateResultDataToAPIResponse(ConcurrentHashMap<String, KruizeObject> mainKruizeExperimentMap) {
            return null;
        }

        public static ConcurrentHashMap<String, KruizeObject> ConvertRecommendationDataToAPIResponse(ConcurrentHashMap<String, KruizeObject> mainKruizeExperimentMap) {
            return null;
        }

        public static SummarizeAPIObject convertListRecommendationAPIObjToSummarizeAPIObj(List<ListRecommendationsAPIObject> listRecommendationsAPIObjectList) {
            SummarizeAPIObject summarize = new SummarizeAPIObject();
            summarize.setClusterName(listRecommendationsAPIObjectList.get(0).getClusterName());
            Summary summary = new Summary();

            Recommendation recommendation;
            NotificationsSummary allOuterNotificationsSummary = null;
            RecommendationSummary allRecommendationSummary = new RecommendationSummary();
            HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> data = new HashMap<>();
            HashMap<String, HashMap<String, RecommendationSummary>> recommendationsCategoryMap = null;
            Set<String> namespaceSet = new HashSet<>();

            for (ListRecommendationsAPIObject listRecommendationsAPIObject : listRecommendationsAPIObjectList) {
                for (KubernetesAPIObject obj : listRecommendationsAPIObject.getKubernetesObjects()) {
                    for (ContainerAPIObject containerAPIObject : obj.getContainerAPIObjects()) {

                        ContainerRecommendations containerRecommendations = containerAPIObject.getContainerRecommendations();

                        for (Map.Entry<Timestamp, HashMap<String, HashMap<String, Recommendation>>> containerRecommendationMapEntry
                                : containerRecommendations.getData().entrySet()) {
                            recommendationsCategoryMap = new HashMap<>();
                            for (Map.Entry<String, HashMap<String, Recommendation>> recommPeriodMapEntry :
                                    containerRecommendationMapEntry.getValue().entrySet()) {
                                String recommendationPeriod = recommPeriodMapEntry.getKey();// duration, profile
                                HashMap<String, RecommendationSummary> recommendationsPeriodMap = new HashMap<>();

                                for (Map.Entry<String, Recommendation> recommendationsMapEntry : recommPeriodMapEntry.getValue().entrySet()) {
                                    String key = recommendationsMapEntry.getKey();// short, medium, long or cost, performance, balanced
                                    LOGGER.debug("RecommendationsMapEntry Key = {}", key);
                                    recommendation = recommendationsMapEntry.getValue();
                                    if (recommendation.getCurrentConfig() != null) {
                                        RecommendationSummary recommendationSummaryCurrent = convertToSummary(recommendation);
                                        if (!allRecommendationSummary.isEmpty())
                                            allRecommendationSummary = mergeSummaries(allRecommendationSummary, recommendationSummaryCurrent);
                                        else
                                            allRecommendationSummary = recommendationSummaryCurrent;

                                        recommendationsPeriodMap.put(key, allRecommendationSummary);
                                    }
                                }
                                recommendationsCategoryMap.put(recommendationPeriod, recommendationsPeriodMap);
                            }
                        }
                        // get the outer notifications summary here
                        NotificationsSummary currentNotificationsSummary = calculateNotificationsSummary(containerRecommendations.getNotificationMap());
                        if (allOuterNotificationsSummary != null)
                            allOuterNotificationsSummary = mergeNotificationsSummary(allOuterNotificationsSummary, currentNotificationsSummary);
                        else
                            allOuterNotificationsSummary = currentNotificationsSummary;
                    }
                    namespaceSet.add(obj.getNamespace());
                }
            }
            summary.setNotificationsSummary(allOuterNotificationsSummary);
            // Get the current system time in UTC
            Instant currentTime = Instant.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT);
            String formattedTime = currentTime.atZone(java.time.ZoneOffset.UTC).format(formatter);

            data.put(Timestamp.from(Instant.parse(formattedTime)), recommendationsCategoryMap);
            summary.setData(data);

            // set the namespaces count
            Namespaces namespaces = new Namespaces(namespaceSet.size(), new ArrayList<>(namespaceSet));
            summary.setNamespaces(namespaces);

            summarize.setSummary(summary);
            return summarize;
        }

        public static RecommendationSummary convertToSummary(Recommendation recommendation) {
            RecommendationSummary summary = new RecommendationSummary();
            summary.setCurrentConfig(recommendation.getCurrentConfig());
            summary.setConfig(recommendation.getConfig());
            summary.setChange(calculateChange(recommendation));
            summary.setNotificationsSummary(calculateNotificationsSummary(recommendation.getNotifications()));
            return summary;
        }

        private static HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> calculateChange(Recommendation recommendation) {
            HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> changeMap = new HashMap<>();
            changeMap.put(AnalyzerConstants.ResourceChange.increase, new HashMap<>());
            changeMap.put(AnalyzerConstants.ResourceChange.decrease, new HashMap<>());
            changeMap.put(AnalyzerConstants.ResourceChange.variation, new HashMap<>());

            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> variationConfig = recommendation.getVariation();

            for (Map.Entry<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> settingEntry : variationConfig.entrySet()) {
                AnalyzerConstants.ResourceSetting resourceSetting = settingEntry.getKey();
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> itemMap = settingEntry.getValue();

                for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> itemEntry : itemMap.entrySet()) {
                    AnalyzerConstants.RecommendationItem recommendationItem = itemEntry.getKey();
                    RecommendationConfigItem configItem = itemEntry.getValue();
                    double amount = configItem.getAmount();

                    if (amount > 0) {
                        changeMap.get(AnalyzerConstants.ResourceChange.increase)
                                .computeIfAbsent(resourceSetting, k -> new HashMap<>())
                                .put(recommendationItem, configItem);
                    } else if (amount < 0) {
                        changeMap.get(AnalyzerConstants.ResourceChange.decrease)
                                .computeIfAbsent(resourceSetting, k -> new HashMap<>())
                                .put(recommendationItem, configItem);
                    }
                }
            }

            // Calculate the variation
            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> increaseConfig = changeMap.get(AnalyzerConstants.ResourceChange.increase);
            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> decreaseConfig = changeMap.get(AnalyzerConstants.ResourceChange.decrease);

            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> variationMap = new HashMap<>();
            for (AnalyzerConstants.ResourceSetting resourceSetting : variationConfig.keySet()) {
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> variationItemMap = new HashMap<>();
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> increaseItemMap = increaseConfig.getOrDefault(resourceSetting, new HashMap<>());
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> decreaseItemMap = decreaseConfig.getOrDefault(resourceSetting, new HashMap<>());

                for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> entry : variationConfig.get(resourceSetting).entrySet()) {
                    AnalyzerConstants.RecommendationItem recommendationItem = entry.getKey();
                    RecommendationConfigItem variationConfigItem = entry.getValue();

                    RecommendationConfigItem increaseConfigItem = increaseItemMap.get(recommendationItem);
                    if (increaseConfigItem == null) {
                        increaseConfigItem = new RecommendationConfigItem(0.0, null);
                    }

                    RecommendationConfigItem decreaseConfigItem = decreaseItemMap.get(recommendationItem);
                    if (decreaseConfigItem == null) {
                        decreaseConfigItem = new RecommendationConfigItem(0.0, null);
                    }

                    double variationValue = increaseConfigItem.getAmount() + decreaseConfigItem.getAmount();
                    variationConfigItem.setAmount(variationValue);
                    variationItemMap.put(recommendationItem, variationConfigItem);
                }

                variationMap.put(resourceSetting, variationItemMap);
            }
            changeMap.put(AnalyzerConstants.ResourceChange.variation, variationMap);
            return changeMap;
        }

        private static NotificationsSummary calculateNotificationsSummary(HashMap<Integer, RecommendationNotification> notifications) {
            NotificationsSummary summary = new NotificationsSummary();
            int infoCount = 0;
            int noticeCount = 0;
            int warningCount = 0;
            int errorCount = 0;
            int criticalCount = 0;

            for (RecommendationNotification notification : notifications.values()) {
                switch (notification.getType()) {
                    case "info" -> infoCount++;
                    case "notice" -> noticeCount++;
                    case "warning" -> warningCount++;
                    case "error" -> errorCount++;
                    case "critical" -> criticalCount++;
                }
            }
            summary.setInfo(infoCount);
            summary.setNotice(noticeCount);
            summary.setWarning(warningCount);
            summary.setError(errorCount);
            summary.setCritical(criticalCount);
            return summary;
        }
        public static RecommendationSummary mergeSummaries(RecommendationSummary summary1, RecommendationSummary summary2) {
            RecommendationSummary mergedSummary = new RecommendationSummary();

            mergedSummary.setCurrentConfig(mergeConfigItems(summary1.getCurrentConfig(), summary2.getCurrentConfig(), mergedSummary.getCurrentConfig()));
            mergedSummary.setConfig(mergeConfigItems(summary1.getConfig(), summary2.getConfig(), mergedSummary.getConfig()));
            mergedSummary.setChange(mergeChange(summary1, summary2, mergedSummary.getChange()));
            mergedSummary.setNotificationsSummary(mergeNotificationsSummary(summary1.getNotificationsSummary(), summary2.getNotificationsSummary()));
            return mergedSummary;
        }

        private static HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> mergeConfigItems(HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config1, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config2, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> configMap) {
            if (configMap == null) {
                configMap = new HashMap<>();
            }

            mergeConfigMap(configMap, config1);
            mergeConfigMap(configMap, config2);

            return configMap;
        }

        private static void mergeConfigMap(HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
                RecommendationConfigItem>> targetMap, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
                RecommendationConfigItem>> sourceMap) {
            for (Map.Entry<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> entry :
                    sourceMap.entrySet()) {
                AnalyzerConstants.ResourceSetting resourceSetting = entry.getKey();
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> itemMap = entry.getValue();

                for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> itemEntry : itemMap.entrySet()) {
                    AnalyzerConstants.RecommendationItem recommendationItem = itemEntry.getKey();
                    RecommendationConfigItem configItem = itemEntry.getValue();

                    targetMap.computeIfAbsent(resourceSetting, k -> new HashMap<>())
                            .merge(recommendationItem, configItem, (existingItem, newItem) -> {
                                // Sum the amount values for existing and new RecommendationConfigItems
                                existingItem.setAmount(existingItem.getAmount() + newItem.getAmount());
                                return existingItem;
                            });
                }
            }
        }

        private static HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> mergeChange(RecommendationSummary summary1, RecommendationSummary summary2, HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> changeMap) {
            if (changeMap == null) {
                changeMap = new HashMap<>();
            }
            HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> changeMap1 = summary1.getChange();
            HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> changeMap2 = summary2.getChange();

            mergeChangeMap(changeMap, changeMap1);
            mergeChangeMap(changeMap, changeMap2);

            return changeMap;
        }

        private static void mergeChangeMap(HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> targetMap, HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> sourceMap) {
            for (Map.Entry<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> entry : sourceMap.entrySet()) {
                AnalyzerConstants.ResourceChange resourceChange = entry.getKey();
                HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> settingMap = entry.getValue();

                for (Map.Entry<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> settingEntry : settingMap.entrySet()) {
                    AnalyzerConstants.ResourceSetting resourceSetting = settingEntry.getKey();
                    HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> itemMap = settingEntry.getValue();

                    for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> itemEntry : itemMap.entrySet()) {
                        AnalyzerConstants.RecommendationItem recommendationItem = itemEntry.getKey();
                        RecommendationConfigItem configItem = itemEntry.getValue();

                        targetMap.computeIfAbsent(resourceChange, k -> new HashMap<>())
                                .computeIfAbsent(resourceSetting, k -> new HashMap<>())
                                .merge(recommendationItem, configItem, (existingItem, newItem) -> {
                                    // Sum the amount values for existing and new RecommendationConfigItems
                                    existingItem.setAmount(existingItem.getAmount() + newItem.getAmount());
                                    return existingItem;
                                });
                    }
                }
            }
        }

        private static NotificationsSummary mergeNotificationsSummary(NotificationsSummary notifications1, NotificationsSummary notifications2) {

            int infoCount = notifications1.getInfo() + notifications2.getInfo();
            int noticeCount = notifications1.getNotice() + notifications2.getNotice();
            int warningCount = notifications1.getWarning() + notifications2.getWarning();
            int errorCount = notifications1.getError() + notifications2.getError();
            int criticalCount = notifications1.getCritical() + notifications2.getCritical();

            NotificationsSummary mergedSummary = new NotificationsSummary();
            mergedSummary.setInfo(infoCount);
            mergedSummary.setNotice(noticeCount);
            mergedSummary.setWarning(warningCount);
            mergedSummary.setError(errorCount);
            mergedSummary.setCritical(criticalCount);
            return mergedSummary;
        }
    }
}
