/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.experimentManager.services;

import com.autotune.common.experiments.ExperimentTrial;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;

public class InitiateExperimentTrial extends HttpServlet {
    /**
     * This API supports POST methode which is used to initiate experimental trails.
     * Input payload should be in the format of JSON. Please refer documentation for more details.
     * /initiateExperimentTrial is API endpoint,
     * Experiments are stored in Key:Value format where Key is Experiment Name and Value is ExperimentTrial Object
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(InitiateExperimentTrial.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws java.io.IOException {
        Gson gson = new Gson();
        String inputData = req.getReader().lines().collect(Collectors.joining());
        ExperimentTrial experimentTrial = gson.fromJson(inputData, ExperimentTrial.class);
        LOGGER.debug(experimentTrial.toString());
        resp.setStatus(HttpServletResponse.SC_CREATED);
    }
}
