/*******************************************************************************
 * Copyright (c) 2022, 2022 Red Hat, IBM Corporation and others.
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
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationNotification;
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

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

public class Summarize extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(Summarize.class);
    List<String> clusterNamesListCached = new ArrayList<>();
    List<String> namespacesListCached = new ArrayList<>();
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
        List<SummarizeAPIObject> namespacesSummaryInCluster = new ArrayList<>();
        List<String> clusterNamesListLocal = new ArrayList<>();
        List<String> namespacesListLocal = new ArrayList<>();
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
                        clusterNamesListCached = new ArrayList<>();
                        namespacesListCached = new ArrayList<>();
                        clusterSummaryCacheMap = new HashMap<>();
                        namespaceSummaryCacheMap = new HashMap<>();
                    }
                    if (summarizeType.equalsIgnoreCase(KruizeConstants.JSONKeys.CLUSTER)) {
                        if (clusterName != null) {
                            clusterNamesListLocal.add(clusterName);
                        } else if (clusterNamesListCached.isEmpty()) {
                            // get all the unique cluster names
                            clusterNamesListLocal = new ExperimentDBService().loadClusterNames();
                            clusterNamesListCached = clusterNamesListLocal;
                        } else {
                            clusterNamesListLocal = clusterNamesListCached;
                        }
                    } else {
                        if (namespaceName != null) {
                            namespacesListLocal.add(namespaceName);
                        } else if (namespacesListCached.isEmpty()) {
                            // get all the namespaces from the DB
                            namespacesListLocal = new ExperimentDBService().loadNamespaceNames();

                            namespacesListCached = namespacesListLocal;
                        } else {
                            namespacesListLocal = namespacesListCached;
                        }
                    }

                    // check the summarize type and invoke the corresponding method
                    if (summarizeType.equalsIgnoreCase(KruizeConstants.JSONKeys.CLUSTER)) {
                        // do summarization based on cluster
                        summarizeBasedOnClusters(clusterNamesListLocal, namespaceName, namespacesSummaryInCluster);
                    } else {
                        // do summarization based on namespace
                        summarizeBasedOnNamespaces(namespacesListLocal, clusterName, namespacesSummaryInCluster);
                    }

                    String gsonStr = "[]";
                    Gson gsonObj = new GsonBuilder()
                            .disableHtmlEscaping()
                            .setPrettyPrinting()
                            .enableComplexMapKeySerialization()
                            .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                            .create();
                    gsonStr = gsonObj.toJson(namespacesSummaryInCluster);
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

    private void summarizeBasedOnNamespaces(List<String> namespacesList, String clusterName,
                                            List<SummarizeAPIObject> namespacesSummaryInCluster) throws Exception {

        SummarizeAPIObject summarizeAPIObjectForNamespace = new SummarizeAPIObject();
        HashMap<String, KruizeObject> recommendationsMap = new HashMap<>();
        ExperimentDBService experimentDBService = new ExperimentDBService();

        long startTime1 = System.currentTimeMillis();
        for (String namespaceName : namespacesList) {
            LOGGER.debug("namespaceName = {}", namespaceName);
            if (clusterName != null) {
                experimentDBService.loadAllRecommendationsByClusterAndNamespaceName(recommendationsMap, clusterName, namespaceName);
                summarizeAPIObjectForNamespace.setClusterName(clusterName);
            } else {
                if (namespaceSummaryCacheMap.containsKey(namespaceName) && namespaceSummaryCacheMap.get(namespaceName) != null) {
                    summarizeAPIObjectForNamespace = namespaceSummaryCacheMap.get(namespaceName);
                    namespacesSummaryInCluster.add(summarizeAPIObjectForNamespace);
                    continue;
                } else {
                    experimentDBService.loadRecommendationsByNamespaceName(recommendationsMap, namespaceName);
                }
            }
            // check if DB returned any result and then continue to build the summarize object
            if (recommendationsMap.isEmpty())
                continue;
            buildSummarizationObjectForNamespace(recommendationsMap, summarizeAPIObjectForNamespace, clusterName, namespaceName);
            summarizeAPIObjectForNamespace.setNamespace(namespaceName);
            namespacesSummaryInCluster.add(summarizeAPIObjectForNamespace);

            // add the summary in cache
            if (clusterName == null)
                namespaceSummaryCacheMap.put(namespaceName, summarizeAPIObjectForNamespace);

            summarizeAPIObjectForNamespace = new SummarizeAPIObject();
            recommendationsMap = new HashMap<>();
        }
        long endTime1 = System.currentTimeMillis();
        LOGGER.info("Time taken for for all clusters: {} milliseconds", endTime1-startTime1);
    }

    private void summarizeBasedOnClusters(List<String> clusterNamesList, String namespaceName,
                                          List<SummarizeAPIObject> namespacesSummaryInCluster) throws Exception {

        SummarizeAPIObject summarizeAPIObjectForCluster = new SummarizeAPIObject();
        HashMap<String, KruizeObject> recommendationsMap = new HashMap<>();
        ExperimentDBService experimentDBService = new ExperimentDBService();

        long startTime1 = System.currentTimeMillis();
        for (String clusterName : clusterNamesList) {
            if (namespaceName != null) {
                experimentDBService.loadAllRecommendationsByClusterAndNamespaceName(recommendationsMap, clusterName, namespaceName);
                summarizeAPIObjectForCluster.setNamespace(namespaceName);
            } else {
                if (clusterSummaryCacheMap.containsKey(clusterName) && clusterSummaryCacheMap.get(clusterName) != null) {
                    summarizeAPIObjectForCluster = clusterSummaryCacheMap.get(clusterName);
                    namespacesSummaryInCluster.add(summarizeAPIObjectForCluster);
                    continue;
                } else {
                    experimentDBService.loadRecommendationsByClusterName(recommendationsMap, clusterName);
                }
            }
            // check if DB returned any result and then continue to build the summarize object
            if (recommendationsMap.isEmpty())
                continue;
            buildSummarizationObjectForCluster(recommendationsMap, summarizeAPIObjectForCluster, namespaceName);
            summarizeAPIObjectForCluster.setClusterName(clusterName);
            // merge namespace summary for each cluster
            namespacesSummaryInCluster.add(summarizeAPIObjectForCluster);
            // add the summary in cache
            if (namespaceName == null)
                clusterSummaryCacheMap.put(clusterName, summarizeAPIObjectForCluster);
            summarizeAPIObjectForCluster = new SummarizeAPIObject();
            recommendationsMap = new HashMap<>();
        }
        long endTime1 = System.currentTimeMillis();
        LOGGER.info("Time taken for for all clusters: {} milliseconds", endTime1-startTime1);
    }
    private void buildSummarizationObjectForCluster(HashMap<String, KruizeObject> recommendationsMap, SummarizeAPIObject summarizeAPIObjectForCluster, String namespaceName) {

        List<ListRecommendationsAPIObject> recommendationList;
        List<KruizeObject> kruizeObjectList = new ArrayList<>(recommendationsMap.values());
        // get the latest recommendations
        recommendationList = ListRecommendations.buildAPIResponse(kruizeObjectList, false, true,
                null);
        // do the summarization
        clusterSummarization(recommendationList, summarizeAPIObjectForCluster, namespaceName);
    }
    private void buildSummarizationObjectForNamespace(HashMap<String, KruizeObject> recommendationsMap, SummarizeAPIObject summarizeAPIObjectForNamespace, String clusterName, String namespaceName) {

        List<ListRecommendationsAPIObject> recommendationList;
        List<KruizeObject> kruizeObjectList = new ArrayList<>(recommendationsMap.values());
        // get the latest recommendations
        recommendationList = ListRecommendations.buildAPIResponse(kruizeObjectList, false, true,
                null);
        // do the summarization
        namespaceSummarization(recommendationList, summarizeAPIObjectForNamespace, namespaceName, clusterName);
    }

    private boolean isValidValue(String summarizeTypeValue, String fetchFromDBValue) {
        return (summarizeTypeValue.equalsIgnoreCase(KruizeConstants.JSONKeys.CLUSTER) || summarizeTypeValue.equalsIgnoreCase(KruizeConstants.JSONKeys.NAMESPACE)
        && (fetchFromDBValue.equals(AnalyzerConstants.BooleanString.TRUE) || fetchFromDBValue.equals(AnalyzerConstants.BooleanString.FALSE)));
    }

    private void clusterSummarization(List<ListRecommendationsAPIObject> listRecommendationsAPIObjectList, SummarizeAPIObject summarizeAPIObjectForCluster, String namespaceName) {
        Summarize summarize = new Summarize();
        // Get the current system timestamp in UTC and set it for the response
        Timestamp currentTimestamp = Timestamp.from(Instant.now());
        currentTimestamp.setNanos(currentTimestamp.getNanos() / 1000 * 1000);
        // Convert to ISO date format
        currentTimestamp.setTime(currentTimestamp.getTime() + Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.ZONE_OFFSET));

        HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> data = new HashMap<>();
        HashMap<String, HashMap<String, RecommendationSummary>> recommendationsCategoryMap = new HashMap<>();
        HashMap<String, RecommendationSummary> recommendationsPeriodMap = new HashMap<>();
        NotificationsSummary allOuterNotificationsSummary = null;
        Set<String> namespaceSet = new HashSet<>();
        Set<String> workloadsSet = new HashSet<>();
        Summary summary = new Summary();
        RecommendationSummary shortTermSummary = new RecommendationSummary();
        RecommendationSummary mediumTermSummary = new RecommendationSummary();
        RecommendationSummary longTermSummary = new RecommendationSummary();
        RecommendationSummary costBasedSummary = new RecommendationSummary();
        RecommendationSummary performanceBasedSummary = new RecommendationSummary();
        RecommendationSummary balancedSummary = new RecommendationSummary();


        for (ListRecommendationsAPIObject listRecommendationsAPIObject : listRecommendationsAPIObjectList) {
            for (KubernetesAPIObject kubernetesAPIObject : listRecommendationsAPIObject.getKubernetesObjects()) {
                for (ContainerAPIObject containerAPIObject : kubernetesAPIObject.getContainerAPIObjects()) {
                    ContainerRecommendations containerRecommendations = containerAPIObject.getContainerRecommendations();
                    for (Map.Entry<Timestamp, HashMap<String, HashMap<String, Recommendation>>> containerRecommendationMapEntry
                            : containerRecommendations.getData().entrySet()) {
                        for (Map.Entry<String, HashMap<String, Recommendation>> recommPeriodMapEntry :
                                containerRecommendationMapEntry.getValue().entrySet()) {
                            String recommendationPeriod = recommPeriodMapEntry.getKey();// duration, profile

                            for (Map.Entry<String, Recommendation> recommendationsMapEntry : recommPeriodMapEntry.getValue().entrySet()) {
                                String key = recommendationsMapEntry.getKey();// short, medium, long or cost, performance, balanced
                                LOGGER.debug("RecommendationsPeriod = {}", key);
                                RecommendationSummary recommendationSummary;// = summarize.getRecommendationPeriodType(key, shortTermSummary,
                                        //mediumTermSummary, longTermSummary, costBasedSummary, performanceBasedSummary, balancedSummary);
                                Recommendation recommendation = recommendationsMapEntry.getValue();
                                LOGGER.debug("Namespace = {}", kubernetesAPIObject.getNamespace());
                                LOGGER.debug("Workload = {}", kubernetesAPIObject.getName());
                                LOGGER.debug("Notifications = {}", recommendation.getNotifications());
                                if (recommendation.getConfig() != null) {
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
                    }
                    else {
                        allOuterNotificationsSummary = currentNotificationsSummary;
                    }
                }
                workloadsSet.add(kubernetesAPIObject.getName());
                namespaceSet.add(kubernetesAPIObject.getNamespace());
            }
            data.put(currentTimestamp, recommendationsCategoryMap);
            summary.setData(data);
            // set the recommendations level notifications summary
            summary.setNotificationsSummary(allOuterNotificationsSummary);
            // set the namespaces object
            if (namespaceName == null) {
                Namespaces namespaces = new Namespaces(namespaceSet.size(), new ArrayList<>(namespaceSet));
                summary.setNamespaces(namespaces);
            }
            Workloads workloads = new Workloads(workloadsSet.size(), new ArrayList<>(workloadsSet));
            summary.setWorkloads(workloads);

            summarizeAPIObjectForCluster.setSummary(summary);
        }
    }

    private void namespaceSummarization(List<ListRecommendationsAPIObject> listRecommendationsAPIObjectList,
                                        SummarizeAPIObject summarizeAPIObjectForNamespace, String namespaceName, String clusterName) {
        Summarize summarize = new Summarize();
        // Get the current system timestamp in UTC and set it for the response
        Timestamp currentTimestamp = Timestamp.from(Instant.now());
        currentTimestamp.setNanos(currentTimestamp.getNanos() / 1000 * 1000);
        // Convert to ISO date format
        currentTimestamp.setTime(currentTimestamp.getTime() + Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.ZONE_OFFSET));

        HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> data = new HashMap<>();
        HashMap<String, HashMap<String, RecommendationSummary>> recommendationsCategoryMap = new HashMap<>();
        HashMap<String, RecommendationSummary> recommendationsPeriodMap = new HashMap<>();
        NotificationsSummary allOuterNotificationsSummary = null;
        Set<String> workloadsSet = new HashSet<>();
        Set<String> clustersSet = new HashSet<>();
        Set<String> containersSet = new HashSet<>();
        Summary summary = new Summary();
        RecommendationSummary shortTermSummary = new RecommendationSummary();
        RecommendationSummary mediumTermSummary = new RecommendationSummary();
        RecommendationSummary longTermSummary = new RecommendationSummary();
        RecommendationSummary costBasedSummary = new RecommendationSummary();
        RecommendationSummary performanceBasedSummary = new RecommendationSummary();
        RecommendationSummary balancedSummary = new RecommendationSummary();

        for (ListRecommendationsAPIObject listRecommendationsAPIObject : listRecommendationsAPIObjectList) {
            String currentClusterName = listRecommendationsAPIObject.getClusterName();
            clustersSet.add(currentClusterName);
            for (KubernetesAPIObject kubernetesAPIObject : listRecommendationsAPIObject.getKubernetesObjects()) {
                String currentNamespace = kubernetesAPIObject.getNamespace();
                for (ContainerAPIObject containerAPIObject : kubernetesAPIObject.getContainerAPIObjects()) {
                    ContainerRecommendations containerRecommendations = containerAPIObject.getContainerRecommendations();
                    for (Map.Entry<Timestamp, HashMap<String, HashMap<String, Recommendation>>> containerRecommendationMapEntry
                            : containerRecommendations.getData().entrySet()) {
                        for (Map.Entry<String, HashMap<String, Recommendation>> recommPeriodMapEntry :
                                containerRecommendationMapEntry.getValue().entrySet()) {
                            LOGGER.debug("RecommendationType = {}", recommPeriodMapEntry.getKey());
                            String recommendationPeriod = recommPeriodMapEntry.getKey();// duration, profile

                            for (Map.Entry<String, Recommendation> recommendationsMapEntry : recommPeriodMapEntry.getValue().entrySet()) {
                                String key = recommendationsMapEntry.getKey();// short, medium, long or cost, performance, balanced
                                LOGGER.debug("RecommendationsPeriod = {}", key);
                                RecommendationSummary recommendationSummary = summarize.getRecommendationPeriodType(key, shortTermSummary,
                                        mediumTermSummary, longTermSummary, costBasedSummary, performanceBasedSummary, balancedSummary);
                                Recommendation recommendation = recommendationsMapEntry.getValue();
                                LOGGER.debug("Namespace = {}", kubernetesAPIObject.getNamespace());
                                LOGGER.debug("Workload = {}", kubernetesAPIObject.getName());
                                LOGGER.debug("Notifications = {}", recommendation.getNotifications());
                                if (recommendation.getConfig() != null) {
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
                    containersSet.add(containerAPIObject.getContainer_name());
                }
                workloadsSet.add(kubernetesAPIObject.getName());
            }
            data.put(currentTimestamp, recommendationsCategoryMap);
            Workloads workloads = new Workloads(workloadsSet.size(), new ArrayList<>(workloadsSet));
            summary.setData(data);
            summary.setWorkloads(workloads);
            // set the recommendations level notifications summary
            summary.setNotificationsSummary(allOuterNotificationsSummary);
            if (clusterName != null && namespaceName != null) {
                Containers containers = new Containers(containersSet.size(), new ArrayList<>(containersSet));
                summary.setContainers(containers);
            } else if (clusterName == null) {
                Clusters clusters = new Clusters(clustersSet.size(), new ArrayList<>(clustersSet));
                summary.setClusters(clusters);
            }
            summarizeAPIObjectForNamespace.setSummary(summary);
        }
    }
    private ActionSummary createActionSummaryObject(HashMap<Integer, RecommendationNotification> notificationMap, String workloadName,
                                                    HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> variation) {

        ActionSummary actionSummary = new ActionSummary();
        // get the optimizable configs
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
                actionSummary.optimizable.cpu.count++;
                actionSummary.optimizable.cpu.workloadNames.add(workloadName);
                if (recommendationItemMap.containsKey(AnalyzerConstants.RecommendationItem.memory)) {
                    actionSummary.optimizable.memory.count++;
                    actionSummary.optimizable.memory.workloadNames.add(workloadName);
                }
            }
        }

        for (RecommendationNotification notification : notificationMap.values()) {
            int code = notification.getCode();
            ActionSummary.Section cpuSection = null;
            ActionSummary.Section memorySection = null;

            if (code == 323001 || code == 323002) {
                cpuSection = actionSummary.idle.cpu;
            } else if (code == 523001) {
                cpuSection = actionSummary.critical.cpu;
            } else if (code == 524001 || code == 524002) {
                memorySection = actionSummary.critical.memory;
            } else if (code == 323004 || code == 323005) {
                cpuSection = actionSummary.optimized.cpu;
            } else if (code == 324003 || code == 324004) {
                memorySection = actionSummary.optimized.memory;
            }

            if (cpuSection != null && !cpuSection.workloadNames.contains(workloadName)) {
                cpuSection.workloadNames.add(workloadName);
                cpuSection.count++;
            }
            if (memorySection != null && !memorySection.workloadNames.contains(workloadName)) {
                memorySection.workloadNames.add(workloadName);
                memorySection.count++;
            }
        }

        actionSummary.total.cpu.count = actionSummary.idle.cpu.count + actionSummary.optimized.cpu.count
                + actionSummary.critical.cpu.count + actionSummary.optimizable.cpu.count;
        actionSummary.total.memory.count = actionSummary.optimized.memory.count + actionSummary.critical.memory.count
                + actionSummary.optimizable.memory.count;

        actionSummary.total.cpu.workloadNames.addAll(actionSummary.idle.cpu.workloadNames);
        actionSummary.total.cpu.workloadNames.addAll(actionSummary.optimized.cpu.workloadNames);
        actionSummary.total.cpu.workloadNames.addAll(actionSummary.critical.cpu.workloadNames);
        actionSummary.total.cpu.workloadNames.addAll(actionSummary.optimizable.cpu.workloadNames);

        actionSummary.total.memory.workloadNames.addAll(actionSummary.idle.memory.workloadNames);
        actionSummary.total.memory.workloadNames.addAll(actionSummary.optimized.memory.workloadNames);
        actionSummary.total.memory.workloadNames.addAll(actionSummary.critical.memory.workloadNames);
        actionSummary.total.memory.workloadNames.addAll(actionSummary.optimizable.memory.workloadNames);

        LOGGER.debug("actionSummary = {}", actionSummary);
        return actionSummary;
    }

    public RecommendationSummary convertToSummary(Recommendation recommendation, String workloadName) {
        RecommendationSummary summary = new RecommendationSummary();
        try {
            summary.setCurrentConfig(recommendation.getCurrentConfig());
            summary.setConfig(recommendation.getConfig());
            summary.setChange(calculateChange(recommendation));
            summary.setNotificationsSummary(calculateNotificationsSummary(recommendation.getNotifications()));
            summary.setActionSummary(createActionSummaryObject(recommendation.getNotifications(), workloadName, recommendation.getVariation()));
        } catch (Exception e){
            LOGGER.error("Exception occurred while converting recommendation to recommendationSummary: {}", e.getMessage());
            e.getMessage();
        }
        return summary;
    }

    private HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> calculateChange(Recommendation recommendation) {
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
        try {
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
        } catch (Exception e){
            LOGGER.error("Exception occurred while building Change object: {}", e.getMessage());
            e.getMessage();
        }
        return changeMap;
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
        return summary;
    }

    public HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> mergeConfigItems(HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config1, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config2, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> configMap) {
        if (configMap == null) {
            configMap = new HashMap<>();
        }

        // if the incoming config is null, skip merging
        if( config1 != null)
            mergeConfigMap(configMap, config1);
        if( config2 != null)
            mergeConfigMap(configMap, config2);

        return configMap;
    }

    private void mergeConfigMap(HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
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

    public HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> mergeChange(RecommendationSummary summary1, RecommendationSummary summary2, HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> changeMap) {
        if (changeMap == null) {
            changeMap = new HashMap<>();
        }
        HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> changeMap1 = summary1.getChange();
        HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> changeMap2 = summary2.getChange();

        mergeChangeMap(changeMap, changeMap1);
        mergeChangeMap(changeMap, changeMap2);

        return changeMap;
    }

    private void mergeChangeMap(HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> targetMap, HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> sourceMap) {
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

    public void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws
            IOException {
        if (null != e) {
            LOGGER.error(e.getMessage());
            if (null == errorMsg) errorMsg = e.getMessage();
        }
        response.sendError(httpStatusCode, errorMsg);
    }

    public RecommendationSummary getRecommendationPeriodType(String key, RecommendationSummary shortTermSummary,
                                                             RecommendationSummary mediumTermSummary,
                                                             RecommendationSummary longTermSummary,
                                                             RecommendationSummary costBasedSummary,
                                                             RecommendationSummary performanceBasedSummary,
                                                             RecommendationSummary balancedSummary) {
        return switch (key) {
            case "short_term" -> shortTermSummary;
            case "medium_term" -> mediumTermSummary;
            case "long_term" -> longTermSummary;
            case "cost" -> costBasedSummary;
            case "performance" -> performanceBasedSummary;
            default -> balancedSummary;
        };
    }

}
