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

import com.autotune.analyzer.exceptions.AutotuneResponse;
import com.autotune.analyzer.utils.AnalyserHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.autotune.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * REST API to create experiments for monitoring.
 */
@WebServlet(asyncSupported = true)
public class CreateExperimentAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateExperimentAPI.class);

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String inputData = request.getReader().lines().collect(Collectors.joining());
        List<JsonObject> newExperiments = Arrays.asList(new Gson().fromJson(inputData, JsonObject[].class));
        boolean success = false;
        AnalyserHelper analyserHelper = new AnalyserHelper(getServletContext());
        if (analyserHelper.validateExperiments(newExperiments)) {
            success = analyserHelper.saveExperiments(newExperiments);
        }
        if (success) {
            sendSuccessResponse(response);
        } else {
            sendErrorResponse(response, null, analyserHelper.getHttpResponseCode(), analyserHelper.getErrorMessage());
        }
    }

    private void sendSuccessResponse(HttpServletResponse response) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = response.getWriter();
        out.append(
                new Gson().toJson(
                        new AutotuneResponse("Experiment registered successfully with Autotune. View registered experiments at /listExperiments", HttpServletResponse.SC_CREATED, "", "SUCCESS")
                )
        );
        out.flush();
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

}
