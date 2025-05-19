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

import com.autotune.analyzer.adapters.DeviceDetailsAdapter;
import com.autotune.analyzer.adapters.RecommendationItemAdapter;
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
import com.autotune.common.data.result.NamespaceData;
import com.autotune.common.data.system.info.device.DeviceDetails;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.trials.ExperimentTrial;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.KruizeSupportedTypes;
import com.autotune.utils.MetricsConfig;
import com.autotune.utils.TrialHelpers;
import com.google.gson.*;
import io.micrometer.core.instrument.Timer;
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
import java.util.stream.Collectors;

import static com.autotune.analyzer.experiment.Experimentator.experimentsMap;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.*;

/**
 * Rest API used to list experiments.
 */
public class ListExperiments extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListExperiments.class);
    ConcurrentHashMap<String, KruizeObject> mainKruizeExperimentMap = new ConcurrentHashMap<>();
    KubernetesServices kubernetesServices = null;

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

            // adding namespace recommendations to K8sObject
            NamespaceData namespaceData = new NamespaceData();
            if (kubernetesAPIObject.getNamespaceAPIObjects() != null && kubernetesAPIObject.getNamespaceAPIObjects().get(0).getnamespaceRecommendations() != null) {
                namespaceData.setNamespace_name(kubernetesAPIObject.getNamespace());
                namespaceData.setNamespaceRecommendations(kubernetesAPIObject.getNamespaceAPIObjects().get(0).getnamespaceRecommendations());
                k8sObject.setNamespaceData(namespaceData);
            }

            k8sObjectList.add(k8sObject);
        }
        return k8sObjectList;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String statusValue = "failure";
        Timer.Sample timerListExp = Timer.start(MetricsConfig.meterRegistry());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        String gsonStr;
        String results = request.getParameter(KruizeConstants.JSONKeys.RESULTS);
        String latest = request.getParameter(LATEST);
        String recommendations = request.getParameter(KruizeConstants.JSONKeys.RECOMMENDATIONS);
        String experimentName = request.getParameter(EXPERIMENT_NAME);
        String rm = request.getParameter(AnalyzerConstants.ServiceConstants.RM);
        String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        StringBuilder clusterName = new StringBuilder();
        List<KubernetesAPIObject> kubernetesAPIObjectList = new ArrayList<>();
        boolean isJSONValid = true;
        Map<String, KruizeObject> mKruizeExperimentMap = new ConcurrentHashMap<>();
        boolean error = false;
        boolean rmTable = false;
        // validate Query params
        Set<String> invalidParams = new HashSet<>();
        for (String param : request.getParameterMap().keySet()) {
            if (!KruizeSupportedTypes.QUERY_PARAMS_SUPPORTED.contains(param)) {
                invalidParams.add(param);
            }
        }
        if (null != rm
                && !rm.isEmpty()
                && rm.equalsIgnoreCase(AnalyzerConstants.BooleanString.TRUE)
        ) {
            rmTable = true;
        }
        try {
            if (invalidParams.isEmpty()) {
                // Set default values if absent
                if (results == null || results.isEmpty())
                    results = "false";
                if (recommendations == null || recommendations.isEmpty())
                    recommendations = "false";
                if (latest == null || latest.isEmpty())
                    latest = "true";
                // Validate query parameter values
                if (isValidBooleanValue(results) && isValidBooleanValue(recommendations) && isValidBooleanValue(latest)) {
                    // Check if JSON input is provided in the request body and validate it
                    if (!requestBody.isEmpty()) {
                        isJSONValid = validateInputJSON(requestBody);
                    }
                    if (isJSONValid) {
                        try {
                            // Fetch experiments data based on request body input, if it's present
                            if (!requestBody.isEmpty()) {
                                // parse the requestBody JSON into corresponding classes
                                parseInputJSON(requestBody, clusterName, kubernetesAPIObjectList);
                                try {
                                    if (rmTable)
                                        new ExperimentDBService().loadExperimentFromDBByInputJSON(mKruizeExperimentMap, clusterName, kubernetesAPIObjectList);
                                    else {
                                        new ExperimentDBService().loadLMExperimentFromDBByInputJSON(mKruizeExperimentMap, clusterName, kubernetesAPIObjectList);
                                    }
                                } catch (Exception e) {
                                    LOGGER.error("Failed to load saved experiment data: {} ", e.getMessage());
                                }
                            } else {
                                // Fetch experiments data from the DB and check if the requested experiment exists
                                if (rmTable) {
                                    loadExperimentsFromDatabase(mKruizeExperimentMap, experimentName);
                                } else {
                                    loadLMExperimentsFromDatabase(mKruizeExperimentMap, experimentName);
                                }
                            }
                            // Check if experiment exists
                            if (experimentName != null && !mKruizeExperimentMap.containsKey(experimentName)) {
                                error = true;
                                sendErrorResponse(
                                        response,
                                        new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_EXPERIMENT_NAME_EXCPTN),
                                        HttpServletResponse.SC_BAD_REQUEST,
                                        String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_EXPERIMENT_NAME_MSG, experimentName)
                                );
                            }
                            if (!error) {
                                // create Gson Object
                                Gson gsonObj = createGsonObject();

                                // Modify the JSON response here based on query params.
                                gsonStr = buildResponseBasedOnQuery(mKruizeExperimentMap, gsonObj, results, recommendations, latest, experimentName, rmTable);
                                if (gsonStr.isEmpty()) {
                                    gsonStr = generateDefaultResponse();
                                }
                                response.getWriter().println(gsonStr);
                                response.getWriter().close();
                                statusValue = "success";
                            }
                        } catch (Exception e) {
                            LOGGER.error("Exception: " + e.getMessage());
                            e.printStackTrace();
                            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        }
                    } else {
                        sendErrorResponse(
                                response,
                                new Exception(AnalyzerErrorConstants.AutotuneObjectErrors.JSON_PARSING_ERROR),
                                HttpServletResponse.SC_BAD_REQUEST,
                                String.format(AnalyzerErrorConstants.AutotuneObjectErrors.JSON_PARSING_ERROR)
                        );
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
        } finally {
            if (null != timerListExp) {
                MetricsConfig.timerListExp = MetricsConfig.timerBListExp.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerListExp.stop(MetricsConfig.timerListExp);
            }
        }
    }

    private void parseInputJSON(String requestBody, StringBuilder clusterName, List<KubernetesAPIObject> kubernetesAPIObjectList) {
        // Parse the JSON string into a JsonObject
        JsonObject jsonObject = new Gson().fromJson(requestBody, JsonObject.class);

        // Extract cluster name
        clusterName.append(jsonObject.get(KruizeConstants.JSONKeys.CLUSTER_NAME).getAsString());

        // Extract Kubernetes objects
        JsonArray kubernetesObjectsArray = jsonObject.getAsJsonArray(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS);
        for (JsonElement element : kubernetesObjectsArray) {
            JsonObject kubernetesObjectJson = element.getAsJsonObject();
            String type = kubernetesObjectJson.get(KruizeConstants.JSONKeys.TYPE).getAsString();
            String name = kubernetesObjectJson.get(KruizeConstants.JSONKeys.NAME).getAsString();
            String namespace = kubernetesObjectJson.get(KruizeConstants.JSONKeys.NAMESPACE).getAsString();
            List<ContainerAPIObject> containerAPIObjects = extractContainersFromJson(kubernetesObjectJson);
            KubernetesAPIObject kubernetesAPIObject = new KubernetesAPIObject(name, type, namespace);
            kubernetesAPIObject.setContainerAPIObjects(containerAPIObjects);
            kubernetesAPIObjectList.add(kubernetesAPIObject);
        }
    }

    public List<ContainerAPIObject> extractContainersFromJson(JsonObject jsonObject) {
        JsonArray containersArray = jsonObject.getAsJsonArray(KruizeConstants.JSONKeys.CONTAINERS);
        List<ContainerAPIObject> containerAPIObjects = new ArrayList<>();
        for (JsonElement element : containersArray) {
            JsonObject containerJson = element.getAsJsonObject();
            String containerName = containerJson.get(KruizeConstants.JSONKeys.CONTAINER_NAME).getAsString();
            String containerImageName = containerJson.get(KruizeConstants.JSONKeys.CONTAINER_IMAGE_NAME).getAsString();
            ContainerAPIObject containerAPIObject = new ContainerAPIObject(containerName, containerImageName, null, null);
            containerAPIObjects.add(containerAPIObject);
        }
        return containerAPIObjects;
    }

    private boolean validateInputJSON(String requestBody) {
        //TODO: add validations for the requestBody
        return true;
    }

    private boolean isValidBooleanValue(String value) {
        return value != null && (value.equals("true") || value.equals("false"));
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

    private void loadExperimentsFromDatabase(Map<String, KruizeObject> mKruizeExperimentMap, String experimentName) {
        try {
            if (experimentName == null || experimentName.isEmpty())
                new ExperimentDBService().loadAllExperiments(mKruizeExperimentMap);
            else
                new ExperimentDBService().loadExperimentFromDBByName(mKruizeExperimentMap, experimentName);

        } catch (Exception e) {
            LOGGER.error("Failed to load saved experiment data: {} ", e.getMessage());
        }
    }

    private void loadLMExperimentsFromDatabase(Map<String, KruizeObject> mKruizeExperimentMap, String experimentName) {
        try {
            if (experimentName == null || experimentName.isEmpty())
                new ExperimentDBService().loadAllLMExperiments(mKruizeExperimentMap);
            else
                new ExperimentDBService().loadLMExperimentFromDBByName(mKruizeExperimentMap, experimentName);

        } catch (Exception e) {
            LOGGER.error("Failed to load saved experiment data: {} ", e.getMessage());
        }
    }

    private Gson createGsonObject() {
        return new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
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

    private String buildResponseBasedOnQuery(Map<String, KruizeObject> mKruizeExperimentMap, Gson gsonObj, String results,
                                             String recommendations, String latest, String experimentName, boolean rmTable) {
        // Case : default
        // return the response without results or recommendations
        if (results.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE) && recommendations.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE)) {
            modifyJSONResponse(mKruizeExperimentMap, KruizeConstants.JSONKeys.RECOMMENDATIONS);
            return gsonObj.toJson(new ArrayList<>(mKruizeExperimentMap.values()));
        } else {
            try {
                if (results.equalsIgnoreCase(AnalyzerConstants.BooleanString.TRUE) && recommendations.equalsIgnoreCase(
                        AnalyzerConstants.BooleanString.TRUE)) {
                    // Case: results=true , recommendations=true
                    // fetch results and recomm. from the DB
                    loadRecommendations(mKruizeExperimentMap, experimentName, rmTable);
                    buildRecommendationsResponse(mKruizeExperimentMap, latest);
                    loadResults(mKruizeExperimentMap, experimentName);

                    // filter the latest results when latest = true, else return all
                    if (latest.equalsIgnoreCase(AnalyzerConstants.BooleanString.TRUE)) {
                        getLatestResults(mKruizeExperimentMap);
                    }
                    checkPercentileInfo(mKruizeExperimentMap);
                    return gsonObj.toJson(new ArrayList<>(mKruizeExperimentMap.values()));
                } else if (results.equalsIgnoreCase(AnalyzerConstants.BooleanString.TRUE)) {
                    // Case: results=true , recommendations=false
                    loadResults(mKruizeExperimentMap, experimentName);
                    checkPercentileInfo(mKruizeExperimentMap);
                    // filter the latest results when latest = true, else return all
                    if (latest.equalsIgnoreCase(AnalyzerConstants.BooleanString.TRUE)) {
                        getLatestResults(mKruizeExperimentMap);
                    }
                    modifyJSONResponse(mKruizeExperimentMap, KruizeConstants.JSONKeys.RECOMMENDATIONS);
                    return gsonObj.toJson(new ArrayList<>(mKruizeExperimentMap.values()));
                } else {
                    // Case: results=false , recommendations=true
                    loadRecommendations(mKruizeExperimentMap, experimentName, rmTable);
                    buildRecommendationsResponse(mKruizeExperimentMap, latest);
                    return gsonObj.toJson(new ArrayList<>(mKruizeExperimentMap.values()));
                }
            } catch (Exception e) {
                LOGGER.error("Exception occurred while building response: {}", e.getMessage());
                return "";
            }
        }
    }

    private void loadResults(Map<String, KruizeObject> mKruizeExperimentMap, String experimentName) {
        try {
            if (experimentName == null || experimentName.isEmpty())
                new ExperimentDBService().loadAllResults(mKruizeExperimentMap);
            else
                new ExperimentDBService().loadResultsFromDBByName(mKruizeExperimentMap, experimentName, null, null);

        } catch (Exception e) {
            LOGGER.error("Failed to load saved results data: {} ", e.getMessage());
        }
    }

    private void loadRecommendations(Map<String, KruizeObject> mKruizeExperimentMap, String experimentName, boolean rmTable) {
        try {
            if (rmTable) {
                if (experimentName == null || experimentName.isEmpty())
                    new ExperimentDBService().loadAllRecommendations(mKruizeExperimentMap);
                else
                    new ExperimentDBService().loadRecommendationsFromDBByName(mKruizeExperimentMap, experimentName);
            } else {
                if (experimentName == null || experimentName.isEmpty())
                    new ExperimentDBService().loadAllLMRecommendations(mKruizeExperimentMap, null);
                else
                    new ExperimentDBService().loadLMRecommendationsFromDBByName(mKruizeExperimentMap, experimentName, null);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load saved recommendations data: {} ", e.getMessage());
        }
    }

    private void modifyJSONResponse(Map<String, KruizeObject> mKruizeExperimentMap, String objectTobeRemoved) {

        // Iterate over the map values and remove the corresponding object
        if (objectTobeRemoved.equalsIgnoreCase("results")) {
            mKruizeExperimentMap.values().forEach(kruizeObject -> kruizeObject.getKubernetes_objects()
                    .forEach(cont -> cont.getContainerDataMap().values().forEach(containerData -> containerData.setResults(null))));
            mKruizeExperimentMap.values().forEach(kruizeObject -> kruizeObject.getKubernetes_objects()
                    .forEach(cont -> cont.getNamespaceData().setResults(null)));
        } else if (objectTobeRemoved.equalsIgnoreCase("recommendations")) {
            mKruizeExperimentMap.values().forEach(kruizeObject -> kruizeObject.getKubernetes_objects()
                    .forEach(cont -> cont.getContainerDataMap().values().forEach(containerData -> containerData.setContainerRecommendations(null))));
            mKruizeExperimentMap.values().forEach(kruizeObject -> kruizeObject.getKubernetes_objects()
                    .forEach(cont -> cont.getNamespaceData().setNamespaceRecommendations(null)));
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

    private void buildRecommendationsResponse(Map<String, KruizeObject> mKruizeExperimentMap, String latest) {
        boolean getLatest = Boolean.parseBoolean(latest);
        for (KruizeObject ko : mKruizeExperimentMap.values()) {
            try {
                LOGGER.debug(ko.getKubernetes_objects().toString());
                ListRecommendationsAPIObject listRecommendationsAPIObject = Converters.KruizeObjectConverters.
                        convertKruizeObjectToListRecommendationSO(
                                ko,
                                getLatest,
                                false,
                                null);

                mergeRecommendationsInKruizeObject(listRecommendationsAPIObject, ko);
            } catch (Exception e) {
                LOGGER.error("Not able to generate recommendation for expName : {} due to {}", ko.getExperimentName(), e.getMessage());
            }
        }
    }

    private void mergeRecommendationsInKruizeObject(ListRecommendationsAPIObject listRecommendationsAPIObject, KruizeObject ko) {
        ko.setKubernetes_objects(convertKubernetesAPIObjectListToK8sObjectList(listRecommendationsAPIObject.getKubernetesObjects()));
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
