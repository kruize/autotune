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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * RestAPI Servlet used to load experiment trail in JSON format using POST, and PUT/PATCH methode is used to update
 * Experiments for more trails under same experiment name.
 */
public class InitiateExperimentTrial extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitiateExperimentTrial.class);

    /**
     * This API supports POST methode which is used to initiate experimental trails.
     * Input payload should be in the format of JSON. Please refer documentation for more details.
     * /initiateExperimentTrial is API endpoint,
     * Experiments are stored in Key:Value format where Key is ExperimentName and Value is ExperimentTrial Object.
     * HTTP STATUS CODE - 201 is returned if experiment loaded successfully.
     * HTTP STATUS CODE - 200 is returned if same experiment posted again.
     * HTTP STATUS CODE - 500 is returned for any error.
     * @param request
     * @param response
     * @throws java.io.IOException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws java.io.IOException {
        Gson gson = new Gson();
        HashMap<String, ExperimentTrial> experimentNameMap = new HashMap<String, ExperimentTrial>();
        try {
            String inputData = request.getReader().lines().collect(Collectors.joining());
            ServletContext servletContext = getServletContext();
            ExperimentTrial experimentTrial = gson.fromJson(inputData, ExperimentTrial.class);
            LOGGER.debug(experimentTrial.toString());
            if (null == servletContext.getAttribute("EM")) {
                servletContext.setAttribute("EM", experimentNameMap);
            }else {
                experimentNameMap = (HashMap<String, ExperimentTrial>) servletContext.getAttribute("EM");
            }
            if(null ==  experimentNameMap.get(experimentTrial.getExperimentName())){
                experimentNameMap.put(experimentTrial.getExperimentName(), experimentTrial);
                servletContext.setAttribute("EM", experimentNameMap);
                LOGGER.debug("Experiment name: {} created!",experimentTrial.getExperimentName());
                response.setStatus(HttpServletResponse.SC_CREATED);
            }else{
                LOGGER.debug("Experiment name: {} already exists!",experimentTrial.getExperimentName());
                response.setStatus(HttpServletResponse.SC_OK);
            }
        }catch (Exception e) {
            LOGGER.error("{}",e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * This API supports PUT/PATCH methode which is used to update experimental trails.
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //TBD
        super.doPut(req, resp);
    }
}
