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

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.experiment.KruizeExperiment;
import com.autotune.analyzer.experiment.RunExperiment;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.trials.ExperimentTrial;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.TrialHelpers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
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
import static com.autotune.utils.TrialHelpers.updateExperimentTrial;

/**
 * Rest API used to list experiments.
 */
public class ListExperiments extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListExperiments.class);
    ConcurrentHashMap<String, KruizeObject> mainKruizeExperimentMap = new ConcurrentHashMap<>();
    KubernetesServices kubernetesServices = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.mainKruizeExperimentMap = (ConcurrentHashMap<String, KruizeObject>) getServletContext().getAttribute(AnalyzerConstants.EXPERIMENT_MAP);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);

        String results = request.getParameter(KruizeConstants.JSONKeys.RESULTS);
        String latest = request.getParameter(AnalyzerConstants.ServiceConstants.LATEST);
        String recommendations = request.getParameter(KruizeConstants.JSONKeys.RECOMMENDATIONS);

        results = (results == null || results.isEmpty()) ? AnalyzerConstants.BooleanString.FALSE: results;
        recommendations = (recommendations == null || recommendations.isEmpty()) ? AnalyzerConstants.BooleanString.FALSE: recommendations;
        latest = (latest == null || latest.isEmpty()) ? AnalyzerConstants.BooleanString.FALSE: latest;

        String gsonStr = "[]";
        if (this.mainKruizeExperimentMap.size() > 0) {
            Gson gsonObj = new GsonBuilder()
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
            gsonStr = gsonObj.toJson(mainKruizeExperimentMap);
            // Modify the JSON response here based on query params.
            gsonStr = buildResponseBasedOnQuery(gsonStr,gsonObj, latest, results, recommendations);
        } else {
            JSONArray experimentTrialJSONArray = new JSONArray();
            for (String deploymentName : experimentsMap.keySet()) {
                KruizeExperiment kruizeExperiment = experimentsMap.get(deploymentName);
                for (int trialNum : kruizeExperiment.getExperimentTrials().keySet()) {
                    ExperimentTrial experimentTrial = kruizeExperiment.getExperimentTrials().get(trialNum);
                    JSONArray experimentTrialJSON = new JSONArray(TrialHelpers.experimentTrialToJSON(experimentTrial));
                    experimentTrialJSONArray.put(experimentTrialJSON.get(0));
                }
            }
            gsonStr = experimentTrialJSONArray.toString(4);
        }
        response.getWriter().println(gsonStr);
        response.getWriter().close();
    }
    private String buildResponseBasedOnQuery(String gsonStr, Gson gsonObj, String latest, String results, String recommendations) throws JsonProcessingException {
        // Case : default
        // return the response without results or recommendations
        if (results.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE) && recommendations.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE)) {
            // remove 'results' from the json
            gsonStr = manipulateResponse(gsonStr, KruizeConstants.JSONKeys.RESULTS);
            // remove 'recommendations' from the json
            gsonStr = manipulateResponse(gsonStr, KruizeConstants.JSONKeys.RECOMMENDATIONS);
        } else {
            if (results.equalsIgnoreCase(AnalyzerConstants.BooleanString.TRUE)) {
                if (recommendations.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE)) {
                    // Case 1: results=true , recommendations=false, latest=false
                    if (latest.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE))
                        // return all results and no recommendations.
                        gsonStr = manipulateResponse(gsonStr, KruizeConstants.JSONKeys.RECOMMENDATIONS);
                    // Case 2: results=true , recommendations=false, latest=true
                    else {
                        // return the latest result and no recommendations.
                        HashMap<String, ContainerData> containerDataMap = new HashMap<>();
                        for (Map.Entry<String, KruizeObject> entry : mainKruizeExperimentMap.entrySet()) {
                            List<K8sObject> k8sObjectList = entry.getValue().getKubernetes_objects();
                            for (K8sObject k8sObject : k8sObjectList) {
                                for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
                                    containerData = Converters.KruizeObjectConverters.getLatestResults(containerData);
                                    containerDataMap.put(containerData.getContainer_name(), containerData);
                                }
                                k8sObject.setContainerDataMap(containerDataMap);
                            }
                            entry.getValue().setKubernetes_objects(k8sObjectList);
                        }
                        gsonStr = gsonObj.toJson(mainKruizeExperimentMap);
                        gsonStr = manipulateResponse(gsonStr, KruizeConstants.JSONKeys.RECOMMENDATIONS);
                    }
                } else {
                    // Case 3 : results=true, recommendations=true, latest=false
                    if (latest.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE))
                        // return everything
                        return gsonStr;
                    // Case 4: results=true , recommendations=true, latest=true
                    else {
                        // return latest results and latest recommendation
                    }
                }
            } else {
                if (recommendations.equalsIgnoreCase(AnalyzerConstants.BooleanString.TRUE)) {
                    // Case 5 : result=false, recommendations=true, latest=false
                    if (latest.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE))
                        // Response : return all recommendations and no results.
                        gsonStr = manipulateResponse(gsonStr, KruizeConstants.JSONKeys.RESULTS);
                    // Case 6 : result=false, recommendations=true, latest=true
                    else {
                        // Response : return the latest recommendation and no results.
                        HashMap<String, ContainerData> containerDataMap = new HashMap<>();
                        for (Map.Entry<String, KruizeObject> entry : mainKruizeExperimentMap.entrySet()) {
                            List<K8sObject> k8sObjectList = entry.getValue().getKubernetes_objects();
                            for (K8sObject k8sObject : k8sObjectList) {
                                for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
                                    containerData = Converters.KruizeObjectConverters.getLatestRecommendations(containerData);
                                    containerDataMap.put(containerData.getContainer_name(), containerData);
                                }
                                k8sObject.setContainerDataMap(containerDataMap);
                            }
                            entry.getValue().setKubernetes_objects(k8sObjectList);
                        }
                        gsonStr = gsonObj.toJson(mainKruizeExperimentMap);
                        gsonStr = manipulateResponse(gsonStr, KruizeConstants.JSONKeys.RESULTS);
                    }
                }
            }
        }
        return gsonStr;
    }

    private String manipulateResponse(String gsonStr, String objectTobeRemoved) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(gsonStr);

        // Find the corresponding object and remove it
        rootNode.findParents(objectTobeRemoved).forEach(parent -> {
            if (parent instanceof ObjectNode) {
                ((ObjectNode) parent).remove(objectTobeRemoved);
            }
        });

        // Convert the modified JsonNode back to a JSON string and return it
        return mapper.writeValueAsString(rootNode);
    }

    //TODO this function no more used.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);

        LOGGER.info("Processing trial result...");
        try {
            String experimentName = request.getParameter(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME);
            // String deploymentName = request.getParameter(AnalyzerConstants.ServiceConstants.DEPLOYMENT_NAME);

            String trialResultsData = request.getReader().lines().collect(Collectors.joining());
            JSONObject trialResultsJson = new JSONObject(trialResultsData);

            // Read in the experiment name and the deployment name in the received JSON from EM
            String experimentNameJson = trialResultsJson.getString(EXPERIMENT_NAME);
            String trialNumber = trialResultsJson.getString("trialNumber");

            JSONArray deploymentsJsonArray = trialResultsJson.getJSONArray("deployments");
            for (Object deploymentObject : deploymentsJsonArray) {
                JSONObject deploymentJsonObject = (JSONObject) deploymentObject;
                String deploymentNameJson = deploymentJsonObject.getString(DEPLOYMENT_NAME);
                KruizeExperiment kruizeExperiment = experimentsMap.get(deploymentNameJson);

                // Check if the passed in JSON has the same info as in the URL
                if (!experimentName.equals(experimentNameJson) || kruizeExperiment == null) {
                    LOGGER.error("Bad results JSON passed: {}", experimentNameJson);
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    break;
                }

                try {
                    updateExperimentTrial(trialNumber, kruizeExperiment, trialResultsJson);
                } catch (InvalidValueException | IncompatibleInputJSONException e) {
                    e.printStackTrace();
                }
                RunExperiment runExperiment = kruizeExperiment.getExperimentThread();
                // Received a metrics JSON from EM after a trial, let the waiting thread know
                LOGGER.info("Received trial result for experiment: " + experimentNameJson + "; Deployment name: " + deploymentNameJson);
                runExperiment.send();
            }
            response.getWriter().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
