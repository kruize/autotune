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
package com.autotune.analyzer.data;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

/**
 * Database interaction implementations.
 */
public class ExperimentInterfaceDBImpl implements ExperimentInterface {
    private String errorMessage;
    private int httpResponseCode;

    public ExperimentInterfaceDBImpl() {
    }


    @Override
    public Map<String, JsonObject> getALLExperiments(AnalyzerConstants.AnalyserExpStatus status) {
        return null;
    }

    @Override
    public JsonObject getExperimentBy(String clusterID, String namespace, String deployment) {
        return null;
    }

    @Override
    public boolean saveExperiments(List<JsonObject> experimentList) {
        return false;
    }

    @Override
    public boolean updateResults(List<JsonObject> resultsList) {
        return false;
    }

    @Override
    public String getErrorMessage() {
        return this.errorMessage;
    }

    @Override
    public int getHttpResponseCode() {
        return this.httpResponseCode;
    }
}
