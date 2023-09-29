/*******************************************************************************
 * Copyright (c) 2023, 2023 Red Hat, IBM Corporation and others.
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

import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.MetricsConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * Rest API used to list cluster names.
 */
public class ListClusters extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListClusters.class);

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Handles HTTP GET requests for retrieving a list of cluster names from the database.
     *
     * @param request  The HttpServletRequest object representing the incoming HTTP request.
     * @param response The HttpServletResponse object representing the outgoing HTTP response.
     * @throws IOException If an I/O error occurs while handling the request or response.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Timer.Sample timerListExp = Timer.start(MetricsConfig.meterRegistry());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        List<String> clusterNamesList;

        // get cluster list from the DB and send JSON array as the response
        try {
            clusterNamesList = new ExperimentDBService().loadAllClusterNames();
            Gson gson = new Gson();
            JsonArray jsonArray = gson.toJsonTree(clusterNamesList).getAsJsonArray();
            response.getWriter().println(jsonArray.toString());
            response.getWriter().close();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            if (null != timerListExp) timerListExp.stop(MetricsConfig.timerListExp);
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


}
