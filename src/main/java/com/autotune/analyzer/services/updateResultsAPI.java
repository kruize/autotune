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
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.autotune.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * REST API used to update metric results to a given experiments.
 */
@WebServlet(asyncSupported = true)
public class updateResultsAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(updateResultsAPI.class);
    ConcurrentHashMap<String, JsonObject> mainAutoTuneOperatorMap = new ConcurrentHashMap<>();
    KubernetesServices kubernetesServices = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.mainAutoTuneOperatorMap = (ConcurrentHashMap<String, JsonObject>) getServletContext().getAttribute(AnalyzerConstants.AnalyserKeys.ANALYSER_STORAGE_CONTEXT_KEY);
        this.kubernetesServices = new KubernetesServicesImpl();
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String inputData = request.getReader().lines().collect(Collectors.joining());
        List<JsonObject> autoTuneOperatorDataList = Arrays.asList(new Gson().fromJson(inputData, JsonObject[].class));
        for (JsonObject jsonObject : autoTuneOperatorDataList) {
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
        sendSuccessResponse(response);
    }

    private void sendSuccessResponse(HttpServletResponse response) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = response.getWriter();
        out.append(
                new Gson().toJson(
                        new AutotuneResponse("Updated metrics results successfully with Autotune. View update results at /listExperiments \"results\" section.", HttpServletResponse.SC_CREATED, "", "SUCCESS")
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
