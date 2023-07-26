package com.autotune.analyzer.serviceObjects;

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.kruizeObject.ExperimentUseCaseType;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.kruizeObject.ObjectiveFunction;
import com.autotune.analyzer.kruizeObject.SloInfo;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationNotification;
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

        public static SummarizeAPIObject convertListRecommendationAPIObjToSummarizeAPIObj(List<ListRecommendationsAPIObject> recommendations) {
            SummarizeAPIObject summarize = new SummarizeAPIObject();
            summarize.setClusterName(recommendations.get(0).getClusterName());

            HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> data = new HashMap<>();

            for (ListRecommendationsAPIObject recObj : recommendations) {
                for (KubernetesAPIObject obj : recObj.getKubernetesObjects()) {
                    for (ContainerAPIObject container : obj.getContainerAPIObjects()) {

                        ContainerRecommendations crecs = container.getContainerRecommendations();

                        for (Map.Entry<Timestamp, HashMap<String, HashMap<String, Recommendation>>> entry : crecs.getData().entrySet()) {
                            Timestamp timestamp = entry.getKey();

                            HashMap<String, HashMap<String, RecommendationSummary>> summaries = data.computeIfAbsent(timestamp, k -> new HashMap<>());

                            HashMap<String, HashMap<String, Recommendation>> recs = entry.getValue();

                            for (Map.Entry<String, HashMap<String, Recommendation>> r : recs.entrySet()) {
                                String recKey = r.getKey();

                                HashMap<String, Recommendation> recMap = r.getValue();

                                HashMap<String, RecommendationSummary> recSummaries = summaries.computeIfAbsent(recKey, k -> new HashMap<>());

                                for (Map.Entry<String, Recommendation> recEntry : recMap.entrySet()) {
                                    String key = recEntry.getKey();
                                    Recommendation rec = recEntry.getValue();
                                    if (rec.getConfig() != null ) {
                                        LOGGER.debug("Recommendation = {}", rec);
                                        RecommendationSummary summary = recSummaries.get(key);
                                        if (summary == null) {
                                            summary = createSummaryFromRecommendation(rec);
                                            recSummaries.put(key, summary);
                                        } else {
                                            mergeRecommendationIntoSummary(rec, summary);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            summarize.setData(data);
            return summarize;
        }

        private static RecommendationSummary createSummaryFromRecommendation(Recommendation rec) {

            RecommendationSummary summary = new RecommendationSummary();

            summary.setCurrentConfig(cloneConfig(rec.getCurrentConfig()));
            summary.setConfig(cloneConfig(rec.getConfig()));

            HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> changes =
                    new HashMap<>();

            // Calculate increase/decrease changes
            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> increase = new HashMap<>();
            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> decrease = new HashMap<>();

            for (AnalyzerConstants.ResourceSetting setting : rec.getCurrentConfig().keySet()) {
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> current = rec.getCurrentConfig().get(setting);
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> config = rec.getConfig().get(setting);

                for (AnalyzerConstants.RecommendationItem item : current.keySet()) {
                    RecommendationConfigItem currentValue = current.get(item);
                    RecommendationConfigItem configValue = config.get(item);

                    double diff = configValue.getAmount() - currentValue.getAmount();
                    String format = currentValue.getFormat();
                    String errorMsg = currentValue.getErrorMsg();
                    if (diff < 0) {
                        // Decrease change
                        updateChange(decrease, setting, item, diff, format, errorMsg);
                    } else if (diff > 0) {
                        // Increase change
                        updateChange(increase, setting, item, diff, format, errorMsg);
                    }
                }
            }
            // Calculate variation
            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> variation = new HashMap<>();

            for (AnalyzerConstants.ResourceSetting setting : rec.getCurrentConfig().keySet()) {

                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> currentValues = rec.getCurrentConfig().get(setting);
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> configValues = rec.getConfig().get(setting);

                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> settingVariation = new HashMap<>();

                for (AnalyzerConstants.RecommendationItem item : currentValues.keySet()) {
                    RecommendationConfigItem current = currentValues.get(item);
                    RecommendationConfigItem config = configValues.get(item);

                    RecommendationConfigItem variationValue = new RecommendationConfigItem();
                    variationValue.setAmount(current.getAmount() + config.getAmount());
                    variationValue.setFormat(current.getFormat());
                    variationValue.setErrorMsg(current.getErrorMsg());

                    settingVariation.put(item, variationValue);
                }

                variation.put(setting, settingVariation);
            }

            changes.put(AnalyzerConstants.ResourceChange.increase, increase);
            changes.put(AnalyzerConstants.ResourceChange.decrease, decrease);
            changes.put(AnalyzerConstants.ResourceChange.variation, variation);

            summary.setChange(changes);

            summary.setNotifications(rec.getNotifications());

            return summary;
        }
        private static void updateChange(
                HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> changes,
                AnalyzerConstants.ResourceSetting setting,
                AnalyzerConstants.RecommendationItem item,
                double diff, String format, String errorMsg) {

            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> settingChanges = changes.computeIfAbsent(setting, k -> new HashMap<>());

            RecommendationConfigItem changeValue = settingChanges.get(item);
            if (changeValue == null) {
                changeValue = new RecommendationConfigItem();
                settingChanges.put(item, changeValue);
            }

            changeValue.setAmount(diff);
            changeValue.setFormat(format);
            changeValue.setErrorMsg(errorMsg);
        }

        private static HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> cloneConfig(
                HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config) {

            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> clone =
                    new HashMap<>();

            for (Map.Entry<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> entry : config.entrySet()) {
                AnalyzerConstants.ResourceSetting key = entry.getKey();
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> value = entry.getValue();

                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> clonedValue =
                        new HashMap<>();

                for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> v : value.entrySet()) {
                    AnalyzerConstants.RecommendationItem k = v.getKey();
                    RecommendationConfigItem c = v.getValue();

                    // create deep copy of RecommendationConfigItem
                    RecommendationConfigItem copiedConfig = new RecommendationConfigItem();
                    copiedConfig.setAmount(c.getAmount());
                    copiedConfig.setFormat(c.getFormat());
                    copiedConfig.setErrorMsg(c.getErrorMsg());

                    clonedValue.put(k, copiedConfig);
                }

                clone.put(key, clonedValue);
            }

            return clone;
        }

        private static void mergeRecommendationIntoSummary(Recommendation rec, RecommendationSummary summary) {

            mergeConfig(rec.getCurrentConfig(), summary.getCurrentConfig());
            mergeConfig(rec.getConfig(), summary.getConfig());

            HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> changes =
                    summary.getChange();

            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> increase =
                    changes.get(AnalyzerConstants.ResourceChange.increase);
            mergeConfig(rec.getVariation(), increase);

            mergeNotifications(rec.getNotifications(), summary.getNotifications());

        }

        private static void mergeConfig(
                HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> src,
                HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> dest) {

            for (Map.Entry<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> entry : src.entrySet()) {
                AnalyzerConstants.ResourceSetting key = entry.getKey();
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> srcValue = entry.getValue();

                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> destValue = dest.get(key);
                if (destValue == null) {
                    destValue = new HashMap<>();
                    dest.put(key, destValue);
                }

                for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> v : srcValue.entrySet()) {
                    AnalyzerConstants.RecommendationItem k = v.getKey();
                    RecommendationConfigItem srcConfig = v.getValue();

                    RecommendationConfigItem destConfig = destValue.get(k);
                    if (destConfig == null) {
                        destConfig = new RecommendationConfigItem();
                        destValue.put(k, destConfig);
                    }

                    // merge fields from srcConfig into destConfig
                    destConfig.setAmount(srcConfig.getAmount());
                    destConfig.setFormat(srcConfig.getFormat());
                    destConfig.setErrorMsg(srcConfig.getErrorMsg());
                }
            }
        }

        private static void mergeNotifications(
                HashMap<Integer, RecommendationNotification> src,
                HashMap<Integer, RecommendationNotification> dest) {

            for (Map.Entry<Integer, RecommendationNotification> entry : src.entrySet()) {
                Integer key = entry.getKey();
                RecommendationNotification notification = entry.getValue();

                dest.put(key, notification);
            }
        }
    }
}
