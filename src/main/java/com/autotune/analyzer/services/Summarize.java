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
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.KubernetesAPIObject;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.analyzer.serviceObjects.SummarizeAPIObject;
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

/**
 * Rest API to build and return the summarized response based on the parameters passed.
 */
public class Summarize extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(Summarize.class);
    HashMap<String, SummarizeAPIObject> clusterSummaryCacheMap = new HashMap<>();
    HashMap<String, SummarizeAPIObject> namespaceSummaryCacheMap = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Handles HTTP GET requests for retrieving summarized API objects based on query parameters.
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Timer.Sample timerListRec = Timer.start(MetricsConfig.meterRegistry());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        List<SummarizeAPIObject> summarizeAPIObjectList = new ArrayList<>();
        // Extract query parameters from the request
        String summarizeType = request.getParameter(KruizeConstants.JSONKeys.SUMMARIZE_TYPE);
        String clusterName = request.getParameter(KruizeConstants.JSONKeys.CLUSTER_NAME);
        String namespaceName = request.getParameter(KruizeConstants.JSONKeys.NAMESPACE_NAME);
        String fetchFromDB = request.getParameter(KruizeConstants.JSONKeys.FETCH_FROM_DB);

        // Validate Query params
        Set<String> invalidParams = new HashSet<>();
        for (String param : request.getParameterMap().keySet()) {
            if (!KruizeSupportedTypes.SUMMARIZE_PARAMS_SUPPORTED.contains(param)) {
                invalidParams.add(param);
            }
        }
        if (invalidParams.isEmpty()) {
            // Set default values if absent
            if (summarizeType == null || summarizeType.isEmpty())
                summarizeType = KruizeConstants.JSONKeys.CLUSTER;
            // by default, fetchFromDB will be false so the data will be fetched from cache only
            if (fetchFromDB == null || fetchFromDB.isEmpty())
                fetchFromDB = AnalyzerConstants.BooleanString.FALSE;
            if (isValidValue(summarizeType, fetchFromDB)) {
                // load recommendations based on params
                try {
                    // Reset cache maps if fetching from the database
                    if (fetchFromDB.equals(AnalyzerConstants.BooleanString.TRUE)) {
                        clusterSummaryCacheMap = new HashMap<>();
                        namespaceSummaryCacheMap = new HashMap<>();
                    }
                    // Load namespaces with clusters from the database
                    HashMap<String, List<String>> ClusterNamespaceAssociationMap = new ExperimentDBService().loadAllClusterNamespaceAssociationMap();

                    if (summarizeType.equalsIgnoreCase(KruizeConstants.JSONKeys.CLUSTER)) {
                        // Summarize based on cluster
                        summarizeAPIObjectList = summarizeBasedOnClusters(ClusterNamespaceAssociationMap, namespaceName, clusterName);
                    } else if (summarizeType.equalsIgnoreCase(KruizeConstants.JSONKeys.NAMESPACE)){
                        // Summarize based on namespace
                        summarizeAPIObjectList = summarizeBasedOnNamespaces(ClusterNamespaceAssociationMap, clusterName, namespaceName);
                    } else {
                        LOGGER.error("Summarize Type: {} not supported!", summarizeType);
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

    /**
     * Checks if the provided values for summarizeType and fetchFromDB are valid.
     *
     * @param summarizeTypeValue The value of the summarizeType parameter.
     * @param fetchFromDBValue   The value of the fetchFromDB parameter.
     * @return True if both summarizeTypeValue and fetchFromDBValue are valid; otherwise, false.
     */
    private boolean isValidValue(String summarizeTypeValue, String fetchFromDBValue) {
        return (summarizeTypeValue.equalsIgnoreCase(KruizeConstants.JSONKeys.CLUSTER) || summarizeTypeValue.equalsIgnoreCase(KruizeConstants.JSONKeys.NAMESPACE)
                && (fetchFromDBValue.equals(AnalyzerConstants.BooleanString.TRUE) || fetchFromDBValue.equals(AnalyzerConstants.BooleanString.FALSE)));
    }

    /**
     * Retrieves a set of cluster names based on the provided clusterName parameter.
     * If the clusterName parameter is null, returns the entire set of allClusterNames.
     * Otherwise, returns a set containing only the provided clusterNameParam.
     *
     * @param clusterNameParam The specific cluster name to retrieve, or null if all cluster names are desired.
     * @param allClusterNames  The set of all available cluster names.
     * @return A set of cluster names based on the provided parameter, or the full set of allClusterNames if clusterNameParam is null.
     */
    private Set<String> getClusterNames(String clusterNameParam, Set<String> allClusterNames) {
        return clusterNameParam == null ? allClusterNames : Collections.singleton(clusterNameParam);
    }

    /**
     * Retrieves a set of unique namespace names based on the provided namespaceName parameter.
     * If the namespaceName parameter is null, returns a set containing all unique namespaces from namespacesWithClustersMapSorted.
     * Otherwise, returns a set containing only the provided namespaceNameParam.
     *
     * @param namespaceNameParam           The specific namespace name to retrieve, or null if all unique namespaces are desired.
     * @param ClusterNamespaceAssociationMap A mapping of cluster names to lists of associated namespace names.
     * @return A set of unique namespace names based on the provided parameter, or a set of all unique namespaces if namespaceNameParam is null.
     */
    private Set<String> getUniqueNamespaces(String namespaceNameParam, HashMap<String, List<String>> ClusterNamespaceAssociationMap) {
        if (namespaceNameParam == null) {
            Set<String> uniqueNamespaces = new HashSet<>();
            for (List<String> namespaces : ClusterNamespaceAssociationMap.values()) {
                uniqueNamespaces.addAll(namespaces);
            }
            return uniqueNamespaces;
        } else {
            return Collections.singleton(namespaceNameParam);
        }
    }

    /**
     * Loads experiments and recommendations from the database based on the provided parameters.
     *
     * @param recommendationsMap A map to store loaded recommendations.
     * @param clusterName        The specific cluster name to filter by, or null if not specified.
     * @param namespaceName      The specific namespace name to filter by, or null if not specified.
     * @throws Exception If an error occurs while loading data from the database.
     */
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

    /**
     * Summarizes API objects based on cluster names and optional namespace filtering.
     *
     * @param ClusterNamespaceAssociationMap A mapping of cluster names to lists of associated namespace names.
     * @param namespaceName                 The specific namespace name to filter by, or null if not specified.
     * @param clusterNameParam              The specific cluster name to summarize, or null if not specified.
     * @return A list of SummarizeAPIObject representing the summarized data for the specified clusters and namespace.
     * @throws Exception If an error occurs during summarization or data retrieval.
     */
    private List<SummarizeAPIObject> summarizeBasedOnClusters(HashMap<String, List<String>> ClusterNamespaceAssociationMap,
                                                              String namespaceName, String clusterNameParam) throws Exception {
        Set<String> clusterNamesSet = getClusterNames(clusterNameParam, ClusterNamespaceAssociationMap.keySet());
        List<SummarizeAPIObject> namespacesSummaryInCluster = new ArrayList<>();

        for (String clusterName : clusterNamesSet) {
            if (namespaceName == null) {
                SummarizeAPIObject summarizeFromCache = getSummaryFromCache(clusterName, clusterSummaryCacheMap);
                if (summarizeFromCache != null) {
                    namespacesSummaryInCluster.add(summarizeFromCache);
                    continue;
                }
            }
            SummarizeAPIObject summarizeClusterObject = new SummarizeAPIObject();
            summarization(KruizeConstants.JSONKeys.CLUSTER_NAME, clusterName, namespaceName, summarizeClusterObject, namespacesSummaryInCluster);
            if (namespaceName == null)
                clusterSummaryCacheMap.put(clusterName, summarizeClusterObject);
        }
        return namespacesSummaryInCluster;
    }

    /**
     * Summarizes API objects based on namespace names and optional cluster names filtering.
     *
     * @param ClusterNamespaceAssociationMap A mapping of cluster names to lists of associated namespace names.
     * @param namespaceNameParam                 The specific namespace name to filter by, or null if not specified.
     * @param clusterName              The specific cluster name to summarize, or null if not specified.
     * @return A list of SummarizeAPIObject representing the summarized data for the specified clusters and namespace.
     * @throws Exception If an error occurs during summarization or data retrieval.
     */
    private List<SummarizeAPIObject> summarizeBasedOnNamespaces(HashMap<String, List<String>> ClusterNamespaceAssociationMap,
                                                                String clusterName, String namespaceNameParam) throws Exception {
        Set<String> uniqueNamespaces = getUniqueNamespaces(namespaceNameParam, ClusterNamespaceAssociationMap);
        List<SummarizeAPIObject> clusterSummaryInNamespace = new ArrayList<>();
        for (String namespaceName : uniqueNamespaces) {
            if (clusterName == null) {
                SummarizeAPIObject summarizeFromCache = getSummaryFromCache(namespaceName, namespaceSummaryCacheMap);
                if (summarizeFromCache != null) {
                    clusterSummaryInNamespace.add(summarizeFromCache);
                    continue;
                }
            }
            SummarizeAPIObject summarizeNamespaceObject = new SummarizeAPIObject();
            summarization(KruizeConstants.JSONKeys.NAMESPACE_NAME, clusterName, namespaceName, summarizeNamespaceObject, clusterSummaryInNamespace);
            if (clusterName == null)
                namespaceSummaryCacheMap.put(namespaceName, summarizeNamespaceObject);
        }
        return clusterSummaryInNamespace;
    }

    /**
     * Performs summarization of API objects based on the provided summarizeType, cluster name, and namespace name.
     *
     * @param summarizeType            The type of summarization ("cluster" or "namespace").
     * @param clusterName              The specific cluster name to summarize, or null if not specified.
     * @param namespaceName            The specific namespace name to summarize, or null if not specified.
     * @param summarizeObject          The SummarizeAPIObject to populate with summarized data.
     * @param namespacesSummaryInCluster A list to which the populated summarizeObject will be added.
     * @throws Exception If an error occurs during summarization or data retrieval.
     */
    private void summarization(String summarizeType, String clusterName, String namespaceName,
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
            if (summarizeType.equalsIgnoreCase(KruizeConstants.JSONKeys.CLUSTER_NAME)) {
                clusterSummarization(recommendations, summarizeObject, namespaceName, currentTimestamp);
                summarizeObject.setClusterName(clusterName);
                if (namespaceName != null)
                    summarizeObject.setNamespace(namespaceName);
            } else {
                namespaceSummarization(recommendations, summarizeObject, namespaceName, clusterName, currentTimestamp);
                summarizeObject.setNamespace(namespaceName);
                if (clusterName != null)
                    summarizeObject.setClusterName(clusterName);
            }
            namespacesSummaryInCluster.add(summarizeObject);
        }
    }

    /**
     * Performs summarization of Kubernetes API objects at the cluster level and populates the SummarizeAPIObject.
     *
     * @param listRecommendationsAPIObjectList A list of ListRecommendationsAPIObject containing recommendations for various Kubernetes API objects.
     * @param summarizeAPIObjectForCluster     The SummarizeAPIObject to populate with the summarized data.
     * @param namespaceName                   The specific namespace name to filter by, or null if not specified.
     * @param currentTimestamp                The current timestamp in UTC used for the response.
     */
    private void clusterSummarization(List<ListRecommendationsAPIObject> listRecommendationsAPIObjectList, SummarizeAPIObject summarizeAPIObjectForCluster,
                                      String namespaceName, Timestamp currentTimestamp) {
        Summarize summarize = new Summarize();
        HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> data = new HashMap<>();
        HashMap<String, HashMap<String, RecommendationSummary>> recommendationsCategoryMap = new HashMap<>();
        HashMap<String, RecommendationSummary> recommendationsPeriodMap = new HashMap<>();
        HashMap<String, Object> namespaces = new HashMap<>();
        HashMap<String, Object> workloads = new HashMap<>();
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
                    } else {
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
                namespaces.put(KruizeConstants.JSONKeys.COUNT, namespaceSet.size());
                namespaces.put(KruizeConstants.JSONKeys.NAMES, namespaceSet);
                summarizeAPIObjectForCluster.setNamespaces(namespaces);
            }
            workloads.put(KruizeConstants.JSONKeys.COUNT, workloadsSet.size());
            workloads.put(KruizeConstants.JSONKeys.NAMES, workloadsSet);
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

    /**
     * Performs summarization of Kubernetes API objects at the namespace level and populates the SummarizeAPIObject.
     *
     * @param listRecommendationsAPIObjectList A list of ListRecommendationsAPIObject containing recommendations for various Kubernetes API objects.
     * @param summarizeAPIObjectForNamespace   The SummarizeAPIObject to populate with the summarized data.
     * @param namespaceName                   The specific namespace name to filter by, or null if not specified.
     * @param clusterName                     The specific cluster name to filter by, or null if not specified.
     * @param currentTimestamp                The current timestamp in UTC used for the response.
     */
    private void namespaceSummarization(List<ListRecommendationsAPIObject> listRecommendationsAPIObjectList,
                                        SummarizeAPIObject summarizeAPIObjectForNamespace, String namespaceName, String clusterName,
                                        Timestamp currentTimestamp) {
        Summarize summarize = new Summarize();
        HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> data = new HashMap<>();
        HashMap<String, HashMap<String, RecommendationSummary>> recommendationsCategoryMap = new HashMap<>();
        HashMap<String, RecommendationSummary> recommendationsPeriodMap = new HashMap<>();
        NotificationsSummary allOuterNotificationsSummary = null;
        HashMap<String, Object> workloads = new HashMap<>();
        HashMap<String, Object> containers = new HashMap<>();
        HashMap<String, Object> clusters = new HashMap<>();

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
            workloads.put(KruizeConstants.JSONKeys.COUNT, workloadsSet.size());
            workloads.put(KruizeConstants.JSONKeys.NAMES, workloadsSet);
            summarizeAPIObjectForNamespace.setWorkloads(workloads);
            // set clusters and namespaces
            if (clusterName != null && namespaceName != null) {
                containers.put(KruizeConstants.JSONKeys.COUNT, containersSet.size());
                containers.put(KruizeConstants.JSONKeys.NAMES, containersSet);
                summarizeAPIObjectForNamespace.setContainers(containers);
            } else if (clusterName == null) {
                clusters.put(KruizeConstants.JSONKeys.COUNT, clustersSet.size());
                clusters.put(KruizeConstants.JSONKeys.NAMES, clustersSet);
                summarizeAPIObjectForNamespace.setClusters(clusters);
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

    /**
     * Builds a list of ListRecommendationsAPIObject from a list of KruizeObject.
     *
     * @param kruizeObjects The list of KruizeObject to build recommendations from.
     * @return A list of ListRecommendationsAPIObject containing recommendations based on the provided KruizeObject list.
     */
    private List<ListRecommendationsAPIObject> buildRecommendationsList(List<KruizeObject> kruizeObjects) {
        return ListRecommendations.buildAPIResponse(kruizeObjects, false, true, null);
    }

    /**
     * Retrieves a SummarizeAPIObject from the provided cacheMap using the specified id.
     *
     * @param id       The id used as the key to retrieve the SummarizeAPIObject from the cache.
     * @param cacheMap The map representing the cache containing SummarizeAPIObject instances.
     * @return The SummarizeAPIObject associated with the provided id in the cacheMap, or null if not found.
     */
    private SummarizeAPIObject getSummaryFromCache(String id, Map<String, SummarizeAPIObject> cacheMap) {
        if (cacheMap.containsKey(id) && cacheMap.get(id) != null) {
            return cacheMap.get(id);
        }
        return null;
    }

    /**
     * Loads KruizeObject recommendations from the database based on the provided cluster and namespace names.
     *
     * @param clusterName   The specific cluster name to filter by, or null if not specified.
     * @param namespaceName The specific namespace name to filter by, or null if not specified.
     * @return A HashMap containing KruizeObject recommendations loaded from the database.
     * @throws Exception If an error occurs while loading data from the database.
     */
    private HashMap<String, KruizeObject> loadDBRecommendations(String clusterName, String namespaceName) throws Exception {

        HashMap<String, KruizeObject> recommendationsMap = new HashMap<>();
        // load data from the DB based on the params
        loadDBBasedOnParams(recommendationsMap, clusterName, namespaceName);
        return recommendationsMap;
    }

    /**
     * Creates an ActionSummary object based on the provided notification map, variation map, and workload name.
     *
     * @param notificationMap A map containing recommendation notifications.
     * @param variation       A map representing the variation of recommendation items.
     * @param workloadName    The name of the workload associated with the ActionSummary.
     * @return An ActionSummary object containing summarized information about recommendations and notifications.
     */
    private ActionSummary createActionSummaryObject(HashMap<Integer, RecommendationNotification> notificationMap,
                                                    HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
                                                            RecommendationConfigItem>> variation, String workloadName) {

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

    /**
     * Converts a Recommendation into a RecommendationSummary object containing summarized information.
     *
     * @param recommendation The Recommendation object to be converted.
     * @param workloadName   The name of the workload associated with the Recommendation.
     * @return A RecommendationSummary object containing summarized information about the Recommendation.
     */
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
        } catch (Exception e) {
            LOGGER.error("Exception occurred while converting recommendation to recommendationSummary: {}", e.getMessage());
        }
        return summary;
    }

    /**
     * Calculates and populates a change map based on the provided Recommendation object's variation.
     *
     * @param recommendation The Recommendation object to calculate changes for.
     * @return A change map containing information about increases, decreases, and variations in resource settings.
     */
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

    /**
     * Populates the provided change map with default values for the change objects.
     *
     * @param changeMap The change map to populate with default values.
     */
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

    /**
     * Calculates the summary of different types of recommendations notifications.
     *
     * @param notifications The map containing recommendation notifications.
     * @return The summary of notifications categorized by type.
     */
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

    /**
     * Merges two configuration maps and populates a target configuration map with the merged values.
     *
     * @param config1    The first configuration map to be merged.
     * @param config2    The second configuration map to be merged.
     * @param configMap  The target configuration map to populate with merged values.
     * @return The merged configuration map.
     */
    public HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> mergeConfigItems(
            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config1,
            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config2,
            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> configMap) {
        if (configMap == null) {
            configMap = new HashMap<>();
        }

        // if the incoming config is null, skip merging
        if (config1 != null) {
            mergeConfigObjects(configMap, config1);
        }
        if (config2 != null) {
            mergeConfigObjects(configMap, config2);
        }

        return configMap;
    }

    /**
     * Merges two configuration maps and populates a target configuration map with the merged values.
     *
     * @param targetMap  The target configuration map to populate with merged values.
     * @param sourceMap  The source configuration map to merge into the target map.
     */
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

    /**
     * Merges two sets of change objects representing resource change details from different recommendation summaries.
     *
     * @param existingSummary The existing recommendation summary to merge from.
     * @param currentSummary  The current recommendation summary to merge from.
     * @return A merged map of resource change objects.
     */
    public HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
            RecommendationConfigItem>>> mergeChangeObjects(RecommendationSummary existingSummary, RecommendationSummary currentSummary) {
        HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
                RecommendationConfigItem>>> changeMapExisting = existingSummary.getChange();
        HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
                RecommendationConfigItem>>> changeMapCurrent = currentSummary.getChange();

        return mergeChangeObjectMaps(changeMapExisting, changeMapCurrent);
    }

    /**
     * Merges two maps of change objects representing resource change details from different recommendation summaries.
     *
     * @param changeMapExisting The map of change objects from the existing recommendation summary.
     * @param changeMapCurrent  The map of change objects from the current recommendation summary.
     * @return A merged map of resource change objects.
     */
    public static HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting,
            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> mergeChangeObjectMaps(HashMap<AnalyzerConstants.ResourceChange,
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
                            Double mergedAmount = existingItem.getAmount() + currentItem.getAmount();
                            String format = existingItem.getFormat();
                            RecommendationConfigItem mergedItem = new RecommendationConfigItem(mergedAmount, format);
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
    /**
     * Sets default values for a configuration map of recommendation items.
     *
     * @param config The configuration map to set default values for.
     * @return The configuration map with default values populated.
     */
    private HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>
    setDefaultValuesForConfigs(HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config) {
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
