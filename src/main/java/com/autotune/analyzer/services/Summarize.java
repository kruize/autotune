/*******************************************************************************
 * Copyright (c) 2022, 2022 Red Hat, IBM Corporation and others.
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

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.analyzer.serviceObjects.SummarizeAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.KruizeSupportedTypes;
import com.autotune.utils.MetricsConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.micrometer.core.instrument.Timer;
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

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

public class Summarize extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(Summarize.class);

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Timer.Sample timerListRec = Timer.start(MetricsConfig.meterRegistry());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        List<ListRecommendationsAPIObject> recommendationList;
        Map<String, KruizeObject> mKruizeExperimentMap = new ConcurrentHashMap<>();
        String clusterName = request.getParameter(KruizeConstants.JSONKeys.CLUSTER_NAME);
        String nsName = request.getParameter(AnalyzerConstants.ServiceConstants.NAMESPACE);

        // validate Query params
        Set<String> invalidParams = new HashSet<>();
        for (String param : request.getParameterMap().keySet()) {
            if (!KruizeSupportedTypes.CLUSTER_PARAMS_SUPPORTED.contains(param)) {
                invalidParams.add(param);
            }
        }
        // load recommendations based on params
        try {
            ExperimentDBService experimentDBService = new ExperimentDBService();
            if (clusterName != null) {
                clusterName = clusterName.trim();
                if (nsName != null) {
                    nsName = nsName.trim();
                    experimentDBService.loadAllRecommendationsByClusterAndNSName(mKruizeExperimentMap, clusterName, nsName);
                } else {
                    experimentDBService.loadAllRecommendationsByClusterName(mKruizeExperimentMap, clusterName);
                }
            } else if (nsName != null) {
                nsName = nsName.trim();
                experimentDBService.loadAllRecommendationsByNSName(mKruizeExperimentMap, nsName);
            } else {
                experimentDBService.loadAllExperimentsAndRecommendations(mKruizeExperimentMap);
            }
        } catch (Exception e) {
            LOGGER.error("Loading saved recommendations failed: {} ", e.getMessage());
        }
        // Add all experiments to list
        List<KruizeObject> kruizeObjectList = new ArrayList<>(mKruizeExperimentMap.values());
        try {
            // get the latest recommendations
            recommendationList = ListRecommendations.buildAPIResponse(kruizeObjectList, false,
                    true, null);
            // convert the recommendation response to summarizeAPI Object
            SummarizeAPIObject summarizeAPIObject = Converters.KruizeObjectConverters.
                    convertListRecommendationAPIObjToSummarizeAPIObj(recommendationList);

            String gsonStr;
            Gson gsonObj = new GsonBuilder()
                    .disableHtmlEscaping()
                    .setPrettyPrinting()
                    .enableComplexMapKeySerialization()
                    .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                    .create();
            gsonStr = gsonObj.toJson(summarizeAPIObject);
            response.getWriter().println(gsonStr);
            response.getWriter().close();
        } catch (Exception e) {
            LOGGER.error("Exception: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            timerListRec.stop(MetricsConfig.timerListRec);
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
