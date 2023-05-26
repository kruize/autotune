/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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

import com.autotune.analyzer.experiment.KruizeExperiment;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.KubernetesAPIObject;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.trials.ExperimentTrial;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.TrialHelpers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.autotune.analyzer.experiment.Experimentator.experimentsMap;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * Rest API used to list experiments.
 */
public class ListExperiments extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListExperiments.class);
    KubernetesServices kubernetesServices = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);

        String results = request.getParameter(KruizeConstants.JSONKeys.RESULTS);
        String latest = request.getParameter(AnalyzerConstants.ServiceConstants.LATEST);
        String recommendations = request.getParameter(KruizeConstants.JSONKeys.RECOMMENDATIONS);
        String experimentName = request.getParameter(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME);
        Map<String, KruizeObject> mKruizeExperimentMap = new ConcurrentHashMap<>();

        results = (results == null || results.isEmpty()) ? AnalyzerConstants.BooleanString.FALSE: results;
        recommendations = (recommendations == null || recommendations.isEmpty()) ? AnalyzerConstants.BooleanString.FALSE: recommendations;
        latest = (latest == null || latest.isEmpty()) ? AnalyzerConstants.BooleanString.TRUE: latest;

        // Fetch experiments data from the DB and check if the requested experiment exists
        loadExperimentsFromDatabase(mKruizeExperimentMap, experimentName, response);
        // create Gson Object
        Gson gsonObj = createGsonObject();

        // Modify the JSON response here based on query params.
        String gsonStr = buildResponseBasedOnQuery(mKruizeExperimentMap,gsonObj, latest, results, recommendations, experimentName);
        if (gsonStr.isEmpty()) {
            gsonStr = generateDefaultResponse();
        }
        response.getWriter().println(gsonStr);
        response.getWriter().close();
    }

    private String generateDefaultResponse() {
        JSONArray experimentTrialJSONArray = new JSONArray();
        for (String deploymentName : experimentsMap.keySet()) {
            KruizeExperiment kruizeExperiment = experimentsMap.get(deploymentName);
            for (int trialNum : kruizeExperiment.getExperimentTrials().keySet()) {
                ExperimentTrial experimentTrial = kruizeExperiment.getExperimentTrials().get(trialNum);
                JSONArray experimentTrialJSON = new JSONArray(TrialHelpers.experimentTrialToJSON(experimentTrial));
                experimentTrialJSONArray.put(experimentTrialJSON.get(0));
            }
        }
        return experimentTrialJSONArray.toString(4);
    }

    private void loadExperimentsFromDatabase(Map<String, KruizeObject> mKruizeExperimentMap, String experimentName, HttpServletResponse response) throws IOException {
        try {
            if (experimentName == null || experimentName.isEmpty())
                new ExperimentDBService().loadAllExperiments(mKruizeExperimentMap);
            else
                new ExperimentDBService().loadExperimentFromDBByName(mKruizeExperimentMap, experimentName);

        } catch (Exception e) {
            LOGGER.error("Failed to load saved experiment data: {} ", e.getMessage());
        }
        // Check if experiment exists
        if (experimentName != null && !mKruizeExperimentMap.containsKey(experimentName)) {
            sendErrorResponse(
                    response,
                    new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_EXPERIMENT_NAME_EXCPTN),
                    HttpServletResponse.SC_BAD_REQUEST,
                    String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_EXPERIMENT_NAME_MSG, experimentName)
            );
        }
    }
    private Gson createGsonObject() {
        return new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getDeclaringClass() == Metric.class && (
                                f.getName().equals("trialSummaryResult")
                                        || f.getName().equals("cycleDataMap")
                        ) ||
                                f.getDeclaringClass() == ContainerData.class && (
                                        f.getName().equalsIgnoreCase("metrics")
                                );
                    }
                    @Override
                    public boolean shouldSkipClass(Class<?> aClass) {
                        return false;
                    }
                })
                .create();
    }

    private void checkPercentileInfo(Map<String, KruizeObject> mainKruizeExperimentMap) {
        try {
            for (Map.Entry<String, KruizeObject> entry : mainKruizeExperimentMap.entrySet()) {
                List<K8sObject> k8sObjectList = entry.getValue().getKubernetes_objects();
                for (K8sObject k8sObject : k8sObjectList) {
                    for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
                        Collection<IntervalResults> intervalResultsList;
                        try {
                            intervalResultsList = containerData.getResults().values();
                        } catch (NullPointerException npe) {
                            LOGGER.debug("Result data unavailable. Skipping container..");
                            continue;
                        }
                        for (IntervalResults intervalResult : intervalResultsList) {
                            for (Map.Entry<AnalyzerConstants.MetricName, MetricResults> innerEntry : intervalResult.getMetricResultsMap().entrySet()) {
                                MetricResults metricResult = innerEntry.getValue();
                                if (!metricResult.isPercentile_results_available()) {
                                    metricResult.setMetricPercentileResults(null);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred while checking the percentile: {}", e.getMessage());
        }
    }

    private String buildResponseBasedOnQuery(Map<String, KruizeObject> mKruizeExperimentMap, Gson gsonObj, String latest, String results, String recommendations, String experimentName) throws JsonProcessingException {
        String gsonStr = "[]";
        // Case : default
        // return the response without results or recommendations
        if (results.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE) && recommendations.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE)) {
            modifyJSONResponse(mKruizeExperimentMap, KruizeConstants.JSONKeys.RECOMMENDATIONS, gsonObj);
            return gsonObj.toJson(mKruizeExperimentMap);
        } else {
            // fetch results from the DB
            try {
                for (Map.Entry<String, KruizeObject> entry : mKruizeExperimentMap.entrySet()) {
                    experimentName = entry.getValue().getExperimentName();
                    if (results.equalsIgnoreCase(AnalyzerConstants.BooleanString.TRUE)) {
                        new ExperimentDBService().loadResultsFromDBByName(mKruizeExperimentMap, experimentName);
                        if (recommendations.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE)) {
                            // Case: results=true , recommendations=false
                            // filter the latest results when latest = true, else return all
                            if (latest.equalsIgnoreCase(AnalyzerConstants.BooleanString.TRUE)) {
                                getLatestResults(mKruizeExperimentMap);
                            }
                            checkPercentileInfo(mKruizeExperimentMap);
                            modifyJSONResponse(mKruizeExperimentMap, KruizeConstants.JSONKeys.RECOMMENDATIONS, gsonObj);
                            gsonStr = gsonObj.toJson(mKruizeExperimentMap);
                        } else {
                            // fetch recommendations from the DB
                            try {
                                new ExperimentDBService().loadRecommendationsFromDBByName(mKruizeExperimentMap, experimentName);
                            } catch (Exception e) {
                                LOGGER.error("Failed to load result data from DB: {} ", e.getMessage());
                                return "";
                            }
                            // Case: results=true, recommendations=true, latest=true
                            // get only latest results and recommendations
                            checkPercentileInfo(mKruizeExperimentMap);
                            if (latest.equalsIgnoreCase(AnalyzerConstants.BooleanString.TRUE)) {
                                getLatestResults(mKruizeExperimentMap);
                            }
                            gsonStr = getRecommendations(mKruizeExperimentMap, gsonObj, latest);
                            return gsonStr;
                        }
                    } else {
                        if (recommendations.equalsIgnoreCase(AnalyzerConstants.BooleanString.TRUE)) {
                            // fetch recommendations from the DB
                            try {
                                new ExperimentDBService().loadRecommendationsFromDBByName(mKruizeExperimentMap, experimentName);
                            } catch (Exception e) {
                                LOGGER.error("Failed to load result data from DB: {} ", e.getMessage());
                                return "";
                            }
                            // Case: result=false, recommendations=true, latest=true
                            // filter the latest recommendations when latest = true, else return all
                            modifyJSONResponse(mKruizeExperimentMap, KruizeConstants.JSONKeys.RESULTS, gsonObj);
                            gsonStr = getRecommendations(mKruizeExperimentMap, gsonObj, latest);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load result data from DB: {} ", e.getMessage());
                return "";
            }
        }
        return gsonStr;
    }

    private void modifyJSONResponse(Map<String, KruizeObject> mKruizeExperimentMap, String objectTobeRemoved, Gson gsonObj) {

        // Iterate over the map values and remove the corresponding object
        if (objectTobeRemoved.equalsIgnoreCase("results")) {
            mKruizeExperimentMap.values().forEach(kruizeObject -> kruizeObject.getKubernetes_objects()
                    .forEach(cont -> cont.getContainerDataMap().values().forEach(containerData -> containerData.setResults(null))));
        } else if (objectTobeRemoved.equalsIgnoreCase("recommendations")) {
            mKruizeExperimentMap.values().forEach(kruizeObject -> kruizeObject.getKubernetes_objects()
                    .forEach(cont -> cont.getContainerDataMap().values().forEach(containerData -> containerData.setContainerRecommendations(null))));
        } else
            LOGGER.error("Unsupported Object passed!");

    }
    private void getLatestResults(Map<String, KruizeObject> mKruizeExperimentMap) {
        try {
            for (Map.Entry<String, KruizeObject> entry : mKruizeExperimentMap.entrySet()) {
                List<K8sObject> k8sObjectList = entry.getValue().getKubernetes_objects();
                for (K8sObject k8sObject : k8sObjectList) {
                    for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
                        if (null == containerData.getResults()) {
                            LOGGER.debug("Result data is missing for the container: {}", containerData.getContainer_name());
                            continue;
                        }
                        Converters.KruizeObjectConverters.getLatestResults(containerData);
                    }
                }
            }
        } catch (NullPointerException npe) {
            LOGGER.error("Exception occurred while fetching results for the : {}", npe.getMessage());
        }
    }
    private String getRecommendations(Map<String, KruizeObject> mKruizeExperimentMap, Gson gsonObj, String latest) {
        List<ListRecommendationsAPIObject> recommendationList = new ArrayList<>();
        Boolean getLatest = Boolean.valueOf(latest);
        List<KruizeObject> kruizeObjectList = new ArrayList<>(mKruizeExperimentMap.values());
        List<KruizeObject> updatedKruizeObjectList = new ArrayList<>();
        for (KruizeObject ko : kruizeObjectList) {
            try {
                LOGGER.debug(ko.getKubernetes_objects().toString());
                ListRecommendationsAPIObject listRecommendationsAPIObject = Converters.KruizeObjectConverters.
                        convertKruizeObjectToListRecommendationSO(
                                ko,
                                getLatest,
                                false,
                                null);

                updatedKruizeObjectList.add(mergeRecommendationsInKruizeObject(listRecommendationsAPIObject, ko));
            } catch (Exception e) {
                LOGGER.error("Not able to generate recommendation for expName : {} due to {}", ko.getExperimentName(), e.getMessage());
            }
        }
        return gsonObj.toJson(kruizeObjectList);
    }

    public void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws
            IOException {
        if (null != e) {
            LOGGER.error(e.toString());
            e.printStackTrace();
            if (null == errorMsg) errorMsg = e.getMessage();
        }
        response.sendError(httpStatusCode, errorMsg);
    }
    private KruizeObject mergeRecommendationsInKruizeObject(ListRecommendationsAPIObject listRecommendationsAPIObject, KruizeObject ko) {
        ko.setKubernetes_objects(convertKubernetesAPIObjectListToK8sObjectList(listRecommendationsAPIObject.getKubernetesObjects()));
        return ko;
    }

    private static List<K8sObject> convertKubernetesAPIObjectListToK8sObjectList(List<KubernetesAPIObject> kubernetesAPIObjects) {
        List<K8sObject> k8sObjectList = new ArrayList<>();
        for (KubernetesAPIObject kubernetesAPIObject : kubernetesAPIObjects) {
            K8sObject k8sObject = new K8sObject(
                    kubernetesAPIObject.getName(),
                    kubernetesAPIObject.getType(),
                    kubernetesAPIObject.getNamespace()
            );
            HashMap<String, ContainerData> containerDataMap = new HashMap<>();
            for (ContainerAPIObject containerAPIObject : kubernetesAPIObject.getContainerAPIObjects()) {
                ContainerData containerData = new ContainerData(
                        containerAPIObject.getContainer_name(),
                        containerAPIObject.getContainer_image_name(),
                        containerAPIObject.getContainerRecommendations(),
                        null);
                containerDataMap.put(containerAPIObject.getContainer_name(), containerData);
            }
            k8sObject.setContainerDataMap(containerDataMap);
            k8sObjectList.add(k8sObject);
        }
        return k8sObjectList;
    }
}
