/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.analyzer.services;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.recommendations.*;
import com.autotune.analyzer.recommendations.summary.*;
import com.autotune.analyzer.serviceObjects.*;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.KruizeSupportedTypes;
import com.autotune.utils.MetricsConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

public class Summarize extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(Summarize.class);
    HashMap<String, SummarizeAPIObject> clusterSummaryCacheMap = new HashMap<>();
    HashMap<String, SummarizeAPIObject> namespaceSummaryCacheMap = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Timer.Sample timerListRec = Timer.start(MetricsConfig.meterRegistry());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        List<SummarizeAPIObject> summarizeAPIObjectList = new ArrayList<>();
        String summarizeType = request.getParameter(KruizeConstants.JSONKeys.SUMMARIZE_TYPE);
        String clusterName = request.getParameter(KruizeConstants.JSONKeys.CLUSTER_NAME);
        String namespaceName = request.getParameter(KruizeConstants.JSONKeys.NAMESPACE_NAME);
        String fetchFromDB = request.getParameter(KruizeConstants.JSONKeys.FETCH_FROM_DB);

        // validate Query params
        Set<String> invalidParams = new HashSet<>();
        for (String param : request.getParameterMap().keySet()) {
            if (!KruizeSupportedTypes.SUMMARIZE_PARAMS_SUPPORTED.contains(param)) {
                invalidParams.add(param);
            }
        }
        if (invalidParams.isEmpty()) {
            // Set default value if absent
            if (summarizeType == null || summarizeType.isEmpty())
                summarizeType = KruizeConstants.JSONKeys.CLUSTER;
            // by default, db_flag will be false so the data will be fetched from cache only
            if (fetchFromDB == null || fetchFromDB.isEmpty())
                fetchFromDB = AnalyzerConstants.BooleanString.FALSE;
            if (isValidValue(summarizeType, fetchFromDB)) {
                // load recommendations based on params
                try {
                    if (fetchFromDB.equals(AnalyzerConstants.BooleanString.TRUE)) {
                        clusterSummaryCacheMap = new HashMap<>();
                        namespaceSummaryCacheMap = new HashMap<>();
                    }
                    // get the clusters and namespaces
                    HashMap<String, List<String>> namespacesWithClustersMap = new ExperimentDBService().loadNamespacesByClusterNames();

                    if (summarizeType.equalsIgnoreCase(KruizeConstants.JSONKeys.CLUSTER)) {
                        // do summarization based on cluster
                        summarizeBasedOnClusters(namespacesWithClustersMap, namespaceName, clusterName, summarizeAPIObjectList);
                    } else {
                        // do summarization based on namespace
                        summarizeBasedOnNamespaces(namespacesWithClustersMap, namespaceName, clusterName, summarizeAPIObjectList);
                    }

                    String gsonStr = "[]";
                    Gson gsonObj = new GsonBuilder()
                            .disableHtmlEscaping()
                            .setPrettyPrinting()
                            .enableComplexMapKeySerialization()
                            .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                            .create();
                    gsonStr = gsonObj.toJson(summarizeAPIObjectList);
                    response.getWriter().println(gsonStr);
                    response.getWriter().close();
                } catch (Exception e) {
                    LOGGER.error("Loading saved recommendations failed: {} ", e.getMessage());
                    sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                } finally {
                    timerListRec.stop(MetricsConfig.timerListRec);
                }
            } else {
                sendErrorResponse(
                        response,
                        new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_QUERY_PARAM_VALUE),
                        HttpServletResponse.SC_BAD_REQUEST,
                        String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_QUERY_PARAM_VALUE)
                );
            }

        } else {
            sendErrorResponse(
                    response,
                    new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_QUERY_PARAM),
                    HttpServletResponse.SC_BAD_REQUEST,
                    String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_QUERY_PARAM, invalidParams)
            );
        }
    }

    private boolean isValidValue(String summarizeTypeValue, String fetchFromDBValue) {
        return (summarizeTypeValue.equalsIgnoreCase(KruizeConstants.JSONKeys.CLUSTER) || summarizeTypeValue.equalsIgnoreCase(KruizeConstants.JSONKeys.NAMESPACE)
        && (fetchFromDBValue.equals(AnalyzerConstants.BooleanString.TRUE) || fetchFromDBValue.equals(AnalyzerConstants.BooleanString.FALSE)));
    }
    private void loadDBBasedOnParams(HashMap<String, KruizeObject> recommendationsMap,
                                     String clusterName, String namespaceName) throws Exception {

        ExperimentDBService experimentDBService = new ExperimentDBService();
        if (clusterName != null && namespaceName != null) {
            experimentDBService.loadExperimentsAndRecommendationsByClusterAndNamespaceName(
                    recommendationsMap, clusterName, namespaceName);
        } else if (namespaceName != null) {
            experimentDBService.loadExperimentsAndRecommendationsByNamespaceName(
                    recommendationsMap, namespaceName);
        } else if (clusterName != null) {
            experimentDBService.loadExperimentsAndRecommendationsByClusterName(
                    recommendationsMap, clusterName);
        }
    }

    private void summarizeBasedOnClusters(HashMap<String, List<String>> namespacesWithClustersMapSorted,
                                      String namespaceName, String clusterNameParam,
                                      List<SummarizeAPIObject> namespacesSummaryInCluster) throws Exception {
    Set<String> clusterNamesSet = getClusterNames(clusterNameParam, namespacesWithClustersMapSorted.keySet());

    for (String clusterName : clusterNamesSet) {
        SummarizeAPIObject summarizeFromCache = getSummaryFromCache(clusterName, clusterSummaryCacheMap);
        if (summarizeFromCache != null) {
            namespacesSummaryInCluster.add(summarizeFromCache);
            continue;
        }
        SummarizeAPIObject summarizeClusterObject = new SummarizeAPIObject();
        summarization(clusterName, namespaceName, summarizeClusterObject, namespacesSummaryInCluster);
        clusterSummaryCacheMap.put(clusterName, summarizeClusterObject);
    }
}
    private void summarizeBasedOnNamespaces(HashMap<String, List<String>> namespacesWithClustersMapSorted,
                                            String clusterName, String namespaceNameParam,
                                            List<SummarizeAPIObject> namespacesSummaryInCluster) throws Exception {
        Set<String> uniqueNamespaces = getUniqueNamespaces(namespaceNameParam, namespacesWithClustersMapSorted);

        for (String namespaceName : uniqueNamespaces) {
            SummarizeAPIObject summarizeFromCache = getSummaryFromCache(namespaceName, namespaceSummaryCacheMap);
            if (summarizeFromCache != null) {
                namespacesSummaryInCluster.add(summarizeFromCache);
                continue;
            }

            SummarizeAPIObject summarizeNamespaceObject = new SummarizeAPIObject();
            summarization(clusterName, namespaceName, summarizeNamespaceObject, namespacesSummaryInCluster);
            namespaceSummaryCacheMap.put(namespaceName, summarizeNamespaceObject);
        }
    }
    private Set<String> getClusterNames(String clusterNameParam, Set<String> allClusterNames) {
        return clusterNameParam == null ? allClusterNames : Collections.singleton(clusterNameParam);
    }
    private Set<String> getUniqueNamespaces(String namespaceNameParam, HashMap<String, List<String>> namespacesWithClustersMapSorted) {
        if (namespaceNameParam == null) {
            Set<String> uniqueNamespaces = new HashSet<>();
            for (List<String> namespaces : namespacesWithClustersMapSorted.values()) {
                uniqueNamespaces.addAll(namespaces);
            }
            return uniqueNamespaces;
        } else {
            return Collections.singleton(namespaceNameParam);
        }
    }

    private void summarization(String clusterName, String namespaceName,
                               SummarizeAPIObject summarizeObject,
                               List<SummarizeAPIObject> namespacesSummaryInCluster) throws Exception {
        HashMap<String, KruizeObject> recommendationsMap = loadDBRecommendations(clusterName, namespaceName);
        List<KruizeObject> kruizeObjectList = new ArrayList<>(recommendationsMap.values());
        // Get the current system timestamp in UTC and set it for the response
        Timestamp currentTimestamp = Timestamp.from(Instant.now());
        currentTimestamp.setNanos(currentTimestamp.getNanos() / 1000 * 1000);
        // Convert to ISO date format
        currentTimestamp.setTime(currentTimestamp.getTime() + Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.ZONE_OFFSET));

        if (!recommendationsMap.isEmpty()) {
            List<ListRecommendationsAPIObject> recommendations = buildRecommendationsList(kruizeObjectList);
            if (namespaceName == null) {
                clusterSummarization(recommendations, summarizeObject, clusterName, currentTimestamp);
                summarizeObject.setClusterName(clusterName);
            } else {
                namespaceSummarization(recommendations, summarizeObject, namespaceName, clusterName, currentTimestamp);
                summarizeObject.setNamespace(namespaceName);
            }
            namespacesSummaryInCluster.add(summarizeObject);
        }
    }

    private List<ListRecommendationsAPIObject> buildRecommendationsList(List<KruizeObject> kruizeObjects) {
        return ListRecommendations.buildAPIResponse(kruizeObjects, false, true, null);
    }

    private SummarizeAPIObject getSummaryFromCache(String id, Map<String, SummarizeAPIObject> cacheMap) {
        if (cacheMap.containsKey(id) && cacheMap.get(id) != null) {
            return cacheMap.get(id);
        }
        return null;
    }
    private HashMap<String, KruizeObject> loadDBRecommendations(String clusterName, String namespaceName) throws Exception {

        HashMap<String, KruizeObject> recommendationsMap = new HashMap<>();
        // load data from the DB based on the params
        loadDBBasedOnParams(recommendationsMap, clusterName, namespaceName);
        return recommendationsMap;
    }

    private void clusterSummarization(List<ListRecommendationsAPIObject> listRecommendationsAPIObjectList, SummarizeAPIObject summarizeAPIObjectForCluster,
                                      String namespaceName, Timestamp currentTimestamp) {
        Summarize summarize = new Summarize();
        HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> data = new HashMap<>();
        HashMap<String, HashMap<String, RecommendationSummary>> recommendationsCategoryMap = new HashMap<>();
        HashMap<String, RecommendationSummary> recommendationsPeriodMap = new HashMap<>();
        HashMap<String, ResourceInfo> namespaces = new HashMap<>();
        HashMap<String, ResourceInfo> workloads = new HashMap<>();
        NotificationsSummary allOuterNotificationsSummary = null;
        Set<String> namespaceSet = new HashSet<>();
        Set<String> workloadsSet = new HashSet<>();
        Set<String> workloadsWithoutRecommendation = new HashSet<>();
        Summary summary = new Summary();
        RecommendationSummary recommendationSummary;
        ResourceInfo resourceInfo;

        for (ListRecommendationsAPIObject listRecommendationsAPIObject : listRecommendationsAPIObjectList) {
            for (KubernetesAPIObject kubernetesAPIObject : listRecommendationsAPIObject.getKubernetesObjects()) {
                for (ContainerAPIObject containerAPIObject : kubernetesAPIObject.getContainerAPIObjects()) {
                    ContainerRecommendations containerRecommendations = containerAPIObject.getContainerRecommendations();
                    for (Map.Entry<Timestamp, HashMap<String, HashMap<String, Recommendation>>> containerRecommendationMapEntry
                            : containerRecommendations.getData().entrySet()) {
                        for (Map.Entry<String, HashMap<String, Recommendation>> recommPeriodMapEntry :
                                containerRecommendationMapEntry.getValue().entrySet()) {
                            String recommendationPeriod = recommPeriodMapEntry.getKey();

                            for (Map.Entry<String, Recommendation> recommendationsMapEntry : recommPeriodMapEntry.getValue().entrySet()) {
                                String key = recommendationsMapEntry.getKey();
                                LOGGER.debug("Recommendation term : {}", key);
                                Recommendation recommendation = recommendationsMapEntry.getValue();
                                RecommendationSummary recommendationSummaryCurrent = summarize.convertToSummary(recommendation,
                                        kubernetesAPIObject.getName());
                                if (recommendationsPeriodMap.containsKey(key)) {
                                    recommendationSummary = recommendationSummaryCurrent.mergeSummaries(
                                            recommendationsPeriodMap.get(key), recommendationSummaryCurrent);
                                } else {
                                    recommendationSummary = recommendationSummaryCurrent;
                                }
                                recommendationsPeriodMap.put(key, recommendationSummary);
                            }
                            recommendationsCategoryMap.put(recommendationPeriod, recommendationsPeriodMap);
                        }
                    }
                    // get the outer notifications summary here
                    NotificationsSummary currentNotificationsSummary = summarize.calculateNotificationsSummary(containerRecommendations.getNotificationMap());
                    if (allOuterNotificationsSummary != null) {
                        allOuterNotificationsSummary = currentNotificationsSummary.mergeNotificationsSummary(allOuterNotificationsSummary, currentNotificationsSummary);
                    }
                    else {
                        allOuterNotificationsSummary = currentNotificationsSummary;
                    }

                    // get the top level action summary
                    for (RecommendationNotification recommendationNotification : containerRecommendations.getNotificationMap().values()) {
                        if (recommendationNotification.getCode() == 120001) {
                            workloadsWithoutRecommendation.add(kubernetesAPIObject.getName());
                        }
                    }

                }
                workloadsSet.add(kubernetesAPIObject.getName());
                namespaceSet.add(kubernetesAPIObject.getNamespace());
            }
            data.put(currentTimestamp, recommendationsCategoryMap);
            summary.setData(data);

            // set the recommendations level notifications summary
            summarizeAPIObjectForCluster.setNotificationsSummary(allOuterNotificationsSummary);
            // set the namespaces object
            if (namespaceName == null) {
                resourceInfo = new ResourceInfo(namespaceSet.size(), namespaceSet);
                namespaces.put(KruizeConstants.JSONKeys.NAMESPACES, resourceInfo);
                summarizeAPIObjectForCluster.setNamespaces(namespaces);
            }
            resourceInfo = new ResourceInfo(workloadsSet.size(), workloadsSet);
            workloads.put(KruizeConstants.JSONKeys.WORKLOADS, resourceInfo);
            summarizeAPIObjectForCluster.setWorkloads(workloads);

            // set the top level action summary
            resourceInfo = new ResourceInfo(workloadsWithoutRecommendation.size(), workloadsWithoutRecommendation);
            HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> info = new HashMap<>();
            info.put(AnalyzerConstants.ActionSummaryRecommendationItem.general, resourceInfo);
            HashMap<String, HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo>> actionSummaryTopLevel = new HashMap<>();
            actionSummaryTopLevel.put(KruizeConstants.JSONKeys.INFO, info);

            summarizeAPIObjectForCluster.setActionSummaryTopLevel(actionSummaryTopLevel);

            summarizeAPIObjectForCluster.setSummary(summary);
        }
    }
    private void namespaceSummarization(List<ListRecommendationsAPIObject> listRecommendationsAPIObjectList,
                                        SummarizeAPIObject summarizeAPIObjectForNamespace, String namespaceName, String clusterName,
                                        Timestamp currentTimestamp) {
        Summarize summarize = new Summarize();
        HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> data = new HashMap<>();
        HashMap<String, HashMap<String, RecommendationSummary>> recommendationsCategoryMap = new HashMap<>();
        HashMap<String, RecommendationSummary> recommendationsPeriodMap = new HashMap<>();
        NotificationsSummary allOuterNotificationsSummary = null;
        HashMap<String, ResourceInfo> workloads = new HashMap<>();
        HashMap<String, ResourceInfo> containers = new HashMap<>();
        HashMap<String, ResourceInfo> clusters = new HashMap<>();

        Set<String> workloadsSet = new HashSet<>();
        Set<String> clustersSet = new HashSet<>();
        Set<String> containersSet = new HashSet<>();
        Set<String> workloadsWithoutRecommendation = new HashSet<>();
        Summary summary = new Summary();
        RecommendationSummary recommendationSummary;
        ResourceInfo resourceInfo;

        for (ListRecommendationsAPIObject listRecommendationsAPIObject : listRecommendationsAPIObjectList) {
            String currentClusterName = listRecommendationsAPIObject.getClusterName();
            clustersSet.add(currentClusterName);
            for (KubernetesAPIObject kubernetesAPIObject : listRecommendationsAPIObject.getKubernetesObjects()) {
                for (ContainerAPIObject containerAPIObject : kubernetesAPIObject.getContainerAPIObjects()) {
                    ContainerRecommendations containerRecommendations = containerAPIObject.getContainerRecommendations();
                    for (Map.Entry<Timestamp, HashMap<String, HashMap<String, Recommendation>>> containerRecommendationMapEntry
                            : containerRecommendations.getData().entrySet()) {
                        for (Map.Entry<String, HashMap<String, Recommendation>> recommPeriodMapEntry :
                                containerRecommendationMapEntry.getValue().entrySet()) {
                            String recommendationPeriod = recommPeriodMapEntry.getKey();
                            for (Map.Entry<String, Recommendation> recommendationsMapEntry : recommPeriodMapEntry.getValue().entrySet()) {
                                String key = recommendationsMapEntry.getKey();
                                LOGGER.debug("RecommendationsPeriod = {}", key);
                                Recommendation recommendation = recommendationsMapEntry.getValue();
                                if (recommendation.getVariation() != null) {
                                    RecommendationSummary recommendationSummaryCurrent = summarize.convertToSummary(recommendation, kubernetesAPIObject.getName());
                                    if (recommendationsPeriodMap.containsKey(key)) {
                                        recommendationSummary = recommendationSummaryCurrent.mergeSummaries(recommendationsPeriodMap.get(key), recommendationSummaryCurrent);
                                    } else {
                                        recommendationSummary = recommendationSummaryCurrent;
                                    }
                                    recommendationsPeriodMap.put(key, recommendationSummary);
                                }
                            }
                            recommendationsCategoryMap.put(recommendationPeriod, recommendationsPeriodMap);
                        }
                    }
                    // get the outer notifications summary here
                    NotificationsSummary currentNotificationsSummary = summarize.calculateNotificationsSummary(containerRecommendations.getNotificationMap());
                    if (allOuterNotificationsSummary != null) {
                        allOuterNotificationsSummary = currentNotificationsSummary.mergeNotificationsSummary(allOuterNotificationsSummary, currentNotificationsSummary);
                    } else {
                        allOuterNotificationsSummary = currentNotificationsSummary;
                    }
                    // get the top level action summary
                    for (RecommendationNotification recommendationNotification : containerRecommendations.getNotificationMap().values()) {
                        if (recommendationNotification.getCode() == 120001) {
                            workloadsWithoutRecommendation.add(kubernetesAPIObject.getName());
                        }
                    }
                    containersSet.add(containerAPIObject.getContainer_name());
                }
                workloadsSet.add(kubernetesAPIObject.getName());
            }
            data.put(currentTimestamp, recommendationsCategoryMap);
            summary.setData(data);

            // set the recommendations level notifications summary
            summarizeAPIObjectForNamespace.setNotificationsSummary(allOuterNotificationsSummary);

            // set the workloads
            resourceInfo = new ResourceInfo(workloadsSet.size(), workloadsSet);
            workloads.put(KruizeConstants.JSONKeys.WORKLOADS, resourceInfo);
            summarizeAPIObjectForNamespace.setWorkloads(workloads);
            if (clusterName != null && namespaceName != null) {
                resourceInfo = new ResourceInfo(containersSet.size(), containersSet);
                containers.put(KruizeConstants.JSONKeys.CONTAINERS, resourceInfo);
                summarizeAPIObjectForNamespace.setContainers(containers);
            } else if (clusterName == null) {
                resourceInfo = new ResourceInfo(clustersSet.size(), clustersSet);
                clusters.put(KruizeConstants.JSONKeys.CLUSTERS, resourceInfo);
                summarizeAPIObjectForNamespace.setContainers(clusters);
            }
            // set the top level action summary
            resourceInfo = new ResourceInfo(workloadsWithoutRecommendation.size(), workloadsWithoutRecommendation);
            HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> info = new HashMap<>();
            info.put(AnalyzerConstants.ActionSummaryRecommendationItem.general, resourceInfo);
            HashMap<String, HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo>> actionSummaryTopLevel = new HashMap<>();
            actionSummaryTopLevel.put(KruizeConstants.JSONKeys.INFO, info);

            summarizeAPIObjectForNamespace.setActionSummaryTopLevel(actionSummaryTopLevel);
            summarizeAPIObjectForNamespace.setSummary(summary);
        }
    }
    private ActionSummary createActionSummaryObject(HashMap<Integer, RecommendationNotification> notificationMap,
                                                    HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
                                                    RecommendationConfigItem>> variation , String workloadName) {

        ActionSummary actionSummary = new ActionSummary();
        HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> optimizable = new HashMap<>();
        HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> idle = new HashMap<>();
        HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> error = new HashMap<>();
        HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> optimized = new HashMap<>();
        HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> info = new HashMap<>();
        HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> critical = new HashMap<>();
        HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> total = new HashMap<>();

        Set<Integer> commonErrorValues = new HashSet<>();
        commonErrorValues.add(RecommendationConstants.NotificationCodes.ERROR_NUM_PODS_CANNOT_BE_ZERO);
        commonErrorValues.add(RecommendationConstants.NotificationCodes.ERROR_NUM_PODS_CANNOT_BE_NEGATIVE);
        commonErrorValues.add(RecommendationConstants.NotificationCodes.ERROR_HOURS_CANNOT_BE_NEGATIVE);

        Set<Integer> cpuErrorValues = new HashSet<>();
        cpuErrorValues.add(RecommendationConstants.NotificationCodes.ERROR_AMOUNT_MISSING_IN_CPU_SECTION);
        cpuErrorValues.add(RecommendationConstants.NotificationCodes.ERROR_INVALID_FORMAT_IN_CPU_SECTION);
        cpuErrorValues.add(RecommendationConstants.NotificationCodes.ERROR_INVALID_AMOUNT_IN_CPU_SECTION);
        cpuErrorValues.add(RecommendationConstants.NotificationCodes.ERROR_FORMAT_MISSING_IN_CPU_SECTION);

        Set<Integer> memoryErrorValues = new HashSet<>();
        memoryErrorValues.add(RecommendationConstants.NotificationCodes.ERROR_AMOUNT_MISSING_IN_MEMORY_SECTION);
        memoryErrorValues.add(RecommendationConstants.NotificationCodes.ERROR_INVALID_FORMAT_IN_MEMORY_SECTION);
        memoryErrorValues.add(RecommendationConstants.NotificationCodes.ERROR_INVALID_AMOUNT_IN_MEMORY_SECTION);
        memoryErrorValues.add(RecommendationConstants.NotificationCodes.ERROR_FORMAT_MISSING_IN_MEMORY_SECTION);

        LOGGER.debug("Workload = {}", workloadName);
        // set the actionSummary as optimizable in case of no notifications
        try {
            if (notificationMap.isEmpty()) {
                HashMap<AnalyzerConstants.RecommendationItem, Double> recommendationItemMap = new HashMap<>();
                for (Map.Entry<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>
                        settingEntry : variation.entrySet()) {
                    AnalyzerConstants.ResourceSetting resourceSetting = settingEntry.getKey();
                    if (resourceSetting.equals(AnalyzerConstants.ResourceSetting.requests)) {
                        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> itemMap = settingEntry.getValue();
                        for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> itemEntry : itemMap.entrySet()) {
                            AnalyzerConstants.RecommendationItem recommendationItem = itemEntry.getKey();
                            RecommendationConfigItem configItem = itemEntry.getValue();
                            double amount = configItem.getAmount();
                            if (amount != 0.0)
                                recommendationItemMap.put(recommendationItem, amount);
                        }
                    }
                }
                if (recommendationItemMap.containsKey(AnalyzerConstants.RecommendationItem.cpu)) {
                    ResourceInfo resourceInfo = new ResourceInfo();
                    resourceInfo.setCount(resourceInfo.getCount() + 1);
                    resourceInfo.getWorkloadNames().add(workloadName);
                    optimizable.put(AnalyzerConstants.ActionSummaryRecommendationItem.cpu, resourceInfo);
                    actionSummary.setOptimizable(optimizable);
                    if (recommendationItemMap.containsKey(AnalyzerConstants.RecommendationItem.memory)) {
                        resourceInfo = new ResourceInfo();
                        resourceInfo.setCount(resourceInfo.getCount() + 1);
                        resourceInfo.getWorkloadNames().add(workloadName);
                        optimizable.put(AnalyzerConstants.ActionSummaryRecommendationItem.memory, resourceInfo);
                        actionSummary.setOptimizable(optimizable);

                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred while getting optimizable configs: {}", e.getMessage());
        }

        // create the actionSummary for the rest of the cases
        ResourceInfo cpuSection = null;
        ResourceInfo memorySection = null;
        ResourceInfo generalSection;
        try {
            for (RecommendationNotification notification : notificationMap.values()) {
                int code = notification.getCode();

                if (code == RecommendationConstants.NotificationCodes.CRITICAL_CPU_REQUEST_NOT_SET) {
                    cpuSection = new ResourceInfo();
                    cpuSection.getWorkloadNames().add(workloadName);
                    cpuSection.setCount(cpuSection.getWorkloadNames().size());
                    critical.put(AnalyzerConstants.ActionSummaryRecommendationItem.cpu, cpuSection);
                    actionSummary.setCritical(critical);
                } else if (code == RecommendationConstants.NotificationCodes.NOTICE_CPU_RECORDS_ARE_IDLE ||
                        code == RecommendationConstants.NotificationCodes.NOTICE_CPU_RECORDS_ARE_ZERO) {
                    cpuSection = new ResourceInfo();
                    cpuSection.getWorkloadNames().add(workloadName);
                    cpuSection.setCount(cpuSection.getWorkloadNames().size());
                    idle.put(AnalyzerConstants.ActionSummaryRecommendationItem.cpu, cpuSection);
                    actionSummary.setIdle(idle);
                } else if (code == RecommendationConstants.NotificationCodes.NOTICE_CPU_REQUESTS_OPTIMISED ||
                        code == RecommendationConstants.NotificationCodes.NOTICE_CPU_LIMITS_OPTIMISED) {
                    cpuSection = new ResourceInfo();
                    cpuSection.getWorkloadNames().add(workloadName);
                    cpuSection.setCount(cpuSection.getWorkloadNames().size());
                    optimized.put(AnalyzerConstants.ActionSummaryRecommendationItem.cpu, cpuSection);
                    actionSummary.setOptimized(optimized);
                } else if (code == RecommendationConstants.NotificationCodes.WARNING_CPU_LIMIT_NOT_SET) {
                    cpuSection = new ResourceInfo();
                    cpuSection.getWorkloadNames().add(workloadName);
                    cpuSection.setCount(cpuSection.getWorkloadNames().size());
                    optimizable.put(AnalyzerConstants.ActionSummaryRecommendationItem.cpu, cpuSection);
                    actionSummary.setOptimizable(optimizable);
                } else if (cpuErrorValues.contains(code)) {
                    System.out.println("at 575");
                    cpuSection = new ResourceInfo();
                    cpuSection.getWorkloadNames().add(workloadName);
                    cpuSection.setCount(cpuSection.getWorkloadNames().size());
                    error.put(AnalyzerConstants.ActionSummaryRecommendationItem.cpu, cpuSection);
                    actionSummary.setError(error);
                } else if (code == RecommendationConstants.NotificationCodes.NOTICE_MEMORY_REQUESTS_OPTIMISED ||
                        code == RecommendationConstants.NotificationCodes.NOTICE_MEMORY_LIMITS_OPTIMISED) {
                    memorySection = new ResourceInfo();
                    memorySection.getWorkloadNames().add(workloadName);
                    memorySection.setCount(memorySection.getWorkloadNames().size());
                    optimizable.put(AnalyzerConstants.ActionSummaryRecommendationItem.memory, memorySection);
                    actionSummary.setOptimizable(optimizable);
                } else if (code == RecommendationConstants.NotificationCodes.CRITICAL_MEMORY_REQUEST_NOT_SET ||
                        code == RecommendationConstants.NotificationCodes.CRITICAL_MEMORY_LIMIT_NOT_SET) {
                    memorySection = new ResourceInfo();
                    memorySection.getWorkloadNames().add(workloadName);
                    memorySection.setCount(memorySection.getWorkloadNames().size());
                    critical.put(AnalyzerConstants.ActionSummaryRecommendationItem.memory, memorySection);
                    actionSummary.setCritical(critical);
                } else if (memoryErrorValues.contains(code)) {
                    memorySection = new ResourceInfo();
                    memorySection.getWorkloadNames().add(workloadName);
                    memorySection.setCount(memorySection.getWorkloadNames().size());
                    error.put(AnalyzerConstants.ActionSummaryRecommendationItem.memory, memorySection);
                    actionSummary.setError(error);
                } else if (code == RecommendationConstants.NotificationCodes.INFO_NOT_ENOUGH_DATA) {
                    generalSection = new ResourceInfo();
                    generalSection.getWorkloadNames().add(workloadName);
                    generalSection.setCount(generalSection.getWorkloadNames().size());
                    info.put(AnalyzerConstants.ActionSummaryRecommendationItem.general, generalSection);
                    actionSummary.setInfo(info);
                } else if (commonErrorValues.contains(code)) {
                    generalSection = new ResourceInfo();
                    generalSection.getWorkloadNames().add(workloadName);
                    generalSection.setCount(generalSection.getWorkloadNames().size());
                    error.put(AnalyzerConstants.ActionSummaryRecommendationItem.general, generalSection);
                    actionSummary.setError(error);
                } else {
                    LOGGER.warn("Code {} not present in the list!", code);
                }
            }
        } catch (Exception e) {
            LOGGER.info("Exception occurred while building actionSummary : {}", e.getMessage());
        }

        // check for case when only CPU/Memory notification is present, if yes, set the other one as optimizable
        if (cpuSection == null && memorySection != null) {
            cpuSection = new ResourceInfo();
            cpuSection.getWorkloadNames().add(workloadName);
            cpuSection.setCount(cpuSection.getWorkloadNames().size());
            optimizable.put(AnalyzerConstants.ActionSummaryRecommendationItem.memory, cpuSection);
            actionSummary.setOptimizable(optimizable);
        } else if (cpuSection != null && memorySection == null) {
            memorySection = new ResourceInfo();
            memorySection.getWorkloadNames().add(workloadName);
            memorySection.setCount(memorySection.getWorkloadNames().size());
            optimizable.put(AnalyzerConstants.ActionSummaryRecommendationItem.memory, memorySection);
            actionSummary.setOptimizable(optimizable);
        }

        // set the total value
        ResourceInfo totalCpuResource = new ResourceInfo();
        ResourceInfo totalMemoryResource = new ResourceInfo();
        ResourceInfo totalGeneralResource = new ResourceInfo();
        try {
            Set<String> allCpuWorkloads = Optional.of(actionSummary).stream()
                    .flatMap(summary -> Stream.of(summary.getIdle(), summary.getOptimized(), summary.getCritical(),
                            summary.getOptimizable(), summary.getError(), summary.getInfo()))
                    .flatMap(map -> Optional.ofNullable(map.get(AnalyzerConstants.ActionSummaryRecommendationItem.cpu))
                            .stream().flatMap(item -> item.getWorkloadNames().stream()))
                    .collect(Collectors.toCollection(HashSet::new));

            totalCpuResource.setWorkloadNames(allCpuWorkloads);
            totalCpuResource.setCount(allCpuWorkloads.size());

            Set<String> allMemoryWorkloads = Optional.of(actionSummary).stream()
                    .flatMap(summary -> Stream.of(summary.getIdle(), summary.getOptimized(), summary.getCritical(),
                            summary.getOptimizable(), summary.getError(), summary.getInfo()))
                    .flatMap(map -> Optional.ofNullable(map.get(AnalyzerConstants.ActionSummaryRecommendationItem.memory))
                            .stream().flatMap(item -> item.getWorkloadNames().stream()))
                    .collect(Collectors.toCollection(HashSet::new));

            totalMemoryResource.setWorkloadNames(allMemoryWorkloads);
            totalMemoryResource.setCount(allMemoryWorkloads.size());

            Set<String> allGeneralWorkloads = Optional.of(actionSummary).stream()
                    .flatMap(summary -> Stream.of(summary.getIdle(), summary.getOptimized(), summary.getCritical(),
                            summary.getOptimizable(), summary.getError(), summary.getInfo()))
                    .flatMap(map -> Optional.ofNullable(map.get(AnalyzerConstants.ActionSummaryRecommendationItem.general))
                            .stream().flatMap(item -> item.getWorkloadNames().stream()))
                    .collect(Collectors.toCollection(HashSet::new));

            totalGeneralResource.setWorkloadNames(allGeneralWorkloads);
            totalGeneralResource.setCount(allGeneralWorkloads.size());
        } catch (Exception e) {
            LOGGER.error("Exception occurred while computing the total value: {}", e.getMessage());
        }
        total.put(AnalyzerConstants.ActionSummaryRecommendationItem.cpu, totalCpuResource);
        total.put(AnalyzerConstants.ActionSummaryRecommendationItem.memory, totalMemoryResource);
        total.put(AnalyzerConstants.ActionSummaryRecommendationItem.general, totalGeneralResource);

        actionSummary.setTotal(total);

        LOGGER.debug("actionSummary Final = {}", actionSummary);
        return actionSummary;
    }

    public RecommendationSummary convertToSummary(Recommendation recommendation, String workloadName) {
        RecommendationSummary summary = new RecommendationSummary();
        try {
            // if the recommendation is null, skip adding current config in the summary as well
            if (recommendation.getCurrentConfig() != null) {
                if (recommendation.getConfig().get(AnalyzerConstants.ResourceSetting.requests).get(AnalyzerConstants.
                        RecommendationItem.cpu) == null)
                    recommendation.getCurrentConfig().get(AnalyzerConstants.ResourceSetting.requests).get(AnalyzerConstants.
                            RecommendationItem.cpu).setAmount(0.0);
                if (recommendation.getConfig().get(AnalyzerConstants.ResourceSetting.requests).get(AnalyzerConstants.
                        RecommendationItem.memory) == null)
                    recommendation.getCurrentConfig().get(AnalyzerConstants.ResourceSetting.requests).get(AnalyzerConstants.
                            RecommendationItem.memory).setAmount(0.0);

                // check if recommendation CPU/Memory limit is null and set current CPU/Memory limits as null as well to skip adding it in the summary
                if (recommendation.getConfig().get(AnalyzerConstants.ResourceSetting.limits).get(AnalyzerConstants.
                        RecommendationItem.cpu) == null)
                    recommendation.getCurrentConfig().get(AnalyzerConstants.ResourceSetting.limits).get(AnalyzerConstants.
                            RecommendationItem.cpu).setAmount(0.0);
                if (recommendation.getConfig().get(AnalyzerConstants.ResourceSetting.limits).get(AnalyzerConstants.
                        RecommendationItem.memory) == null)
                    recommendation.getCurrentConfig().get(AnalyzerConstants.ResourceSetting.limits).get(AnalyzerConstants.
                            RecommendationItem.memory).setAmount(0.0);
            }
            recommendation.setCurrentConfig(setDefaultValuesForConfigs(recommendation.getCurrentConfig()));
            recommendation.setConfig(setDefaultValuesForConfigs(recommendation.getConfig()));
            recommendation.setVariation(setDefaultValuesForConfigs(recommendation.getVariation()));
            summary.setCurrentConfig(recommendation.getCurrentConfig());
            summary.setConfig(recommendation.getConfig());
            summary.setChange(calculateChange(recommendation));
            summary.setNotificationsSummary(calculateNotificationsSummary(recommendation.getNotifications()));
            summary.setActionSummary(createActionSummaryObject(recommendation.getNotifications(), recommendation.getVariation(), workloadName));
        } catch (Exception e){
            LOGGER.error("Exception occurred while converting recommendation to recommendationSummary: {}", e.getMessage());
        }
        return summary;
    }
    private HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting,
            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> calculateChange(Recommendation recommendation) {
        HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
                RecommendationConfigItem>>> changeMap = new HashMap<>();
        changeMap.put(AnalyzerConstants.ResourceChange.increase, new HashMap<>());
        changeMap.put(AnalyzerConstants.ResourceChange.decrease, new HashMap<>());
        changeMap.put(AnalyzerConstants.ResourceChange.variation, new HashMap<>());

        // Populate the changeMap with default values
        setDefaultValuesForChangeObject(changeMap);

        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>
                variationConfig = recommendation.getVariation();

        // set the increase and decrease values
        for (Map.Entry<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>
                settingEntry : variationConfig.entrySet()) {
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

        // Set the variation
        changeMap.put(AnalyzerConstants.ResourceChange.variation, variationConfig);
        return changeMap;
    }

    private void setDefaultValuesForChangeObject(HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting,
            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> changeMap) {
        for (AnalyzerConstants.ResourceChange change : AnalyzerConstants.ResourceChange.values()) {
            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> settingMap = new HashMap<>();
            changeMap.put(change, settingMap);

            for (AnalyzerConstants.ResourceSetting setting : AnalyzerConstants.ResourceSetting.values()) {
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> recommendationMap = new HashMap<>();
                settingMap.put(setting, recommendationMap);

                for (AnalyzerConstants.RecommendationItem item : AnalyzerConstants.RecommendationItem.values()) {
                    RecommendationConfigItem configItem = new RecommendationConfigItem();

                    if (setting == AnalyzerConstants.ResourceSetting.requests || setting == AnalyzerConstants.ResourceSetting.limits) {
                        configItem.setAmount(0.0);
                        configItem.setFormat(item == AnalyzerConstants.RecommendationItem.memory ? "MiB" : "cores");

                        recommendationMap.put(item, configItem);
                    }
                }
            }
        }
    }

    public NotificationsSummary calculateNotificationsSummary(HashMap<Integer, RecommendationNotification> notifications) {
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
        LOGGER.debug("Notif summary = {}", notifications.values());
        return summary;
    }

    public HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> mergeConfigItems(
            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config1,
            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config2,
            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> configMap) {
        if (configMap == null) {
            configMap = new HashMap<>();
        }

        // if the incoming config is null, skip merging
        if( config1 != null) {
            mergeConfigObjects(configMap, config1);
        }
        if( config2 != null) {
            mergeConfigObjects(configMap, config2);
        }

        return configMap;
    }

    private void mergeConfigObjects(HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
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

    public HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
            RecommendationConfigItem>>> mergeChangeObjects(RecommendationSummary existingSummary, RecommendationSummary currentSummary) {
        HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
                RecommendationConfigItem>>> changeMapExisting = existingSummary.getChange();
        HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
                RecommendationConfigItem>>> changeMapCurrent = currentSummary.getChange();

        return mergeMaps(changeMapExisting, changeMapCurrent);
    }
    public static HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting,
            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> mergeMaps(HashMap<AnalyzerConstants.ResourceChange,
            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> changeMapExisting,
                                                                                                HashMap<AnalyzerConstants.ResourceChange,
                                                                                                        HashMap<AnalyzerConstants.ResourceSetting,
                                                                                                HashMap<AnalyzerConstants.RecommendationItem,
                                                                                                        RecommendationConfigItem>>> changeMapCurrent) {

        HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
                RecommendationConfigItem>>> mergedMap = new HashMap<>();
        try {
            for (AnalyzerConstants.ResourceChange change : AnalyzerConstants.ResourceChange.values()) {
                mergedMap.put(change, new HashMap<>());
                for (AnalyzerConstants.ResourceSetting setting : AnalyzerConstants.ResourceSetting.values()) {
                    mergedMap.get(change).put(setting, new HashMap<>());
                    for (AnalyzerConstants.RecommendationItem item : AnalyzerConstants.RecommendationItem.values()) {
                        RecommendationConfigItem existingItem = changeMapExisting.get(change).get(setting).get(item);
                        RecommendationConfigItem currentItem = changeMapCurrent.get(change).get(setting).get(item);
                        if (existingItem != null && currentItem != null) {
                            RecommendationConfigItem mergedItem = new RecommendationConfigItem(existingItem.getAmount() + currentItem.getAmount());
                            mergedMap.get(change).get(setting).put(item, mergedItem);
                        } else if (existingItem != null) {
                            mergedMap.get(change).get(setting).put(item, existingItem);
                        } else if (currentItem != null) {
                            mergedMap.get(change).get(setting).put(item, currentItem);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred while merging Change Maps : {}", e.getMessage());
        }
        return mergedMap;
    }
    private HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>
    setDefaultValuesForConfigs(HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,RecommendationConfigItem>> config) {
        if (config == null) {
            config = new HashMap<>();
            // Initialize inner maps
            config.put(AnalyzerConstants.ResourceSetting.requests, new HashMap<>());
            config.put(AnalyzerConstants.ResourceSetting.limits, new HashMap<>());
            // Add default config items
            for (AnalyzerConstants.ResourceSetting resourceSetting : AnalyzerConstants.ResourceSetting.values()) {
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> innerMap = config.get(resourceSetting);

                for (AnalyzerConstants.RecommendationItem key : AnalyzerConstants.RecommendationItem.values()) {
                    innerMap.put(key, new RecommendationConfigItem());
                }
            }
        }
        // Check inner maps and config items for null
        for (Map.Entry<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>
                entry : config.entrySet()) {

            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> innerMap = entry.getValue();

            for (AnalyzerConstants.RecommendationItem key : AnalyzerConstants.RecommendationItem.values()) {
                if (!innerMap.containsKey(key)) {
                    // Item not present, add with defaults
                    if (key.equals(AnalyzerConstants.RecommendationItem.cpu))
                        innerMap.put(key, new RecommendationConfigItem(0.0, "cores"));
                    else
                        innerMap.put(key, new RecommendationConfigItem(0.0, "MiB"));
                } else {
                    RecommendationConfigItem configItem = innerMap.get(key);
                    if (configItem.getAmount() == null) {
                        configItem.setAmount(0.0);
                    }
                    if (configItem.getFormat() == null) {
                        if (key.equals(AnalyzerConstants.RecommendationItem.cpu))
                            configItem.setFormat("cores");
                        else
                            configItem.setFormat("MiB");
                    }
                }
            }

        }
        return config;
    }


    public void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws
            IOException {
        if (null != e) {
            LOGGER.error(e.getMessage());
            if (null == errorMsg) errorMsg = e.getMessage();
        }
        response.sendError(httpStatusCode, errorMsg);
    }
}
