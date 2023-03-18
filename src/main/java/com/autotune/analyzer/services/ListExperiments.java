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

import com.autotune.analyzer.KruizeExperiment;
import com.autotune.analyzer.RunExperiment;
import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.annotations.json.KruizeJSONExclusionStrategy;
import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.k8sObjects.KruizeObject;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.AnalyzerConstants;
import com.autotune.utils.TrialHelpers;
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
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.autotune.analyzer.Experimentator.experimentsMap;
import static com.autotune.utils.AnalyzerConstants.ServiceConstants.*;
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
        String gsonStr = "[]";
        if (this.mainKruizeExperimentMap.size() > 0) {
            Gson gsonObj = new GsonBuilder()
                    .disableHtmlEscaping()
                    .setPrettyPrinting()
                    .enableComplexMapKeySerialization()
                    .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                    .setExclusionStrategies(new KruizeJSONExclusionStrategy())
                    .create();
            gsonStr = gsonObj.toJson(this.mainKruizeExperimentMap);
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
