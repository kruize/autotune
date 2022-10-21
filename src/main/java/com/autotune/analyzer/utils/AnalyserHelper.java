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
package com.autotune.analyzer.utils;

import com.autotune.analyzer.data.ExperimentInterface;
import com.autotune.analyzer.data.ExperimentInterfaceDBImpl;
import com.autotune.analyzer.data.ExperimentInterfaceImpl;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.ServletContext;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * THis is Helper class called from CreateExperiment Servlet basically to CRUD, Validate etc. to experiments either
 * locally or into DataBase.
 */

public class AnalyserHelper {

    //TODO change JsonObject to More specific Object ExperimentMonitoring
    private ConcurrentHashMap<String, JsonObject> mainAutoTuneOperatorMap;
    private ServletContext sce;
    private String errorMessage;
    private int httpResponseCode;
    private ExperimentInterface experimentInterface;

    public AnalyserHelper(ServletContext sce) {
        this.sce = sce;
        this.mainAutoTuneOperatorMap = (ConcurrentHashMap<String, JsonObject>) sce.getAttribute(AnalyzerConstants.AnalyserKeys.ANALYSER_EXPERIMENTS_STORAGE_KEY);
        if (AnalyzerConstants.RunConfig.STORAGE_MODE.equals("local")) {
            experimentInterface = new ExperimentInterfaceImpl();
        } else {
            experimentInterface = new ExperimentInterfaceDBImpl();
        }
    }

    /**
     * Load in_progress Experiments
     */
    public void loadExperiments() {
        if (AnalyzerConstants.RunConfig.STORAGE_MODE.equals("local")) {
            ConcurrentHashMap<String, JsonObject> analyserMainExperimentData = new ConcurrentHashMap<>();
            this.sce.setAttribute(AnalyzerConstants.AnalyserKeys.ANALYSER_EXPERIMENTS_STORAGE_KEY, analyserMainExperimentData);
        } else {
            // TODO Fetch in progress Experiments from DB
            experimentInterface.getALLExperiments(AnalyzerConstants.AnalyserExpStatus.IN_PROGRESS);
            this.sce.setAttribute(AnalyzerConstants.AnalyserKeys.ANALYSER_EXPERIMENTS_STORAGE_KEY, null);
        }
    }

    /**
     * Save newly submitted experiments
     *
     * @param newExperiments
     * @return
     */
    public boolean saveExperiments(List<JsonObject> newExperiments) {
        boolean success = experimentInterface.saveExperiments(newExperiments);
        if (success) {
            for (JsonObject jsonObject : newExperiments) {
                this.mainAutoTuneOperatorMap.put(jsonObject.get("name").toString(), jsonObject);
            }
        } else {
            this.setErrorMessage(experimentInterface.getErrorMessage());
            this.setHttpResponseCode(experimentInterface.getHttpResponseCode());
        }
        return success;
    }

    /**
     * Check if Experiments Data are valid with no duplication in
     * Name, ClusterID, Namespace , DeploymentName
     *      TODO 1 Check if Experiments already exist in map
     *           2 Check If ClusterID,Deployment and namespace already exist in map
     */
    public boolean validateExperiments(List<JsonObject> experimentDataList) {
        return true;
    }


    public boolean updateResults(List<JsonObject> resultsList) {
        boolean success = experimentInterface.updateResults(resultsList);
        if (success) {
            for (JsonObject jsonObject : resultsList) {
                JsonObject exp = this.mainAutoTuneOperatorMap.get(jsonObject.get("name").toString());
                if (exp.get("results") == null) {
                    JsonArray jsonArray = new JsonArray();
                    jsonArray.add(jsonObject);
                    exp.add("results", jsonArray);
                } else {
                    JsonArray jsonArray = exp.getAsJsonArray("results");
                    jsonArray.add(jsonObject);
                    exp.add("results", jsonArray);
                }
            }
        } else {
            this.setErrorMessage(experimentInterface.getErrorMessage());
            this.setHttpResponseCode(experimentInterface.getHttpResponseCode());
        }
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getHttpResponseCode() {
        return httpResponseCode;
    }

    public void setHttpResponseCode(int httpResponseCode) {
        this.httpResponseCode = httpResponseCode;
    }
}
