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

import com.autotune.analyzer.exceptions.KruizeResponse;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.Utils;
import com.google.gson.Gson;
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
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 *
 */
@WebServlet(asyncSupported = true)
public class UpdateRecommendation extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateRecommendation.class);

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Get the values from the request parameters
        String experiment_name = request.getParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME);
        String intervalEndTimeStr = request.getParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME);

        // Check if experiment_name is provided
        if (experiment_name == null || experiment_name.isEmpty()) {
            sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.EXPERIMENT_NAME_MANDATORY);
        }

        // Check if interval_end_time is provided
        if (intervalEndTimeStr == null || intervalEndTimeStr.isEmpty()) {
            sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.INTERVAL_END_TIME_MANDATORY);
        }

        // Convert interval_endtime to UTC date format
        if (!Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, intervalEndTimeStr)) {
            sendErrorResponse(
                    response,
                    new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_EXCPTN),
                    HttpServletResponse.SC_BAD_REQUEST,
                    String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG, intervalEndTimeStr)
            );
        }

        //Check if data exist
        Timestamp interval_end_time = Utils.DateUtils.getTimeStampFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, intervalEndTimeStr);
        boolean dataExists = false;
        try {
            dataExists = new ExperimentDBService().checkIfResultsExists(experiment_name, interval_end_time);
        } catch (Exception e) {
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }

        if (dataExists) {
            //Load KruizeObject and generate recommendation
            Map<String, KruizeObject> mainKruizeExperimentMAP = new HashMap<>();
            try {
                new ExperimentDBService().loadExperimentAndResultsFromDBByName(mainKruizeExperimentMAP, experiment_name, null, interval_end_time);
                KruizeObject kruizeObject = mainKruizeExperimentMAP.get(experiment_name);
                for (K8sObject k8Obj : kruizeObject.getKubernetes_objects()) {
                    for (ContainerData containerData : k8Obj.getContainerDataMap().values()) {
                        for (IntervalResults result : containerData.getResults().values()) {

                        }
                    }
                }
//                List<ExperimentResultData> experimentResultDataList = ;
//                boolean recommendationCheck = new ExperimentInitiator().generateAndAddRecommendations(mainKruizeExperimentMAP, experimentResultDataList);
//                if (!recommendationCheck)
//                    LOGGER.error("Failed to create recommendation for experiment: %s and interval_end_time: %s",
//                            experimentResultDataList.get(0).getExperiment_name(),
//                            experimentResultDataList.get(0).getIntervalEndTime());
//                else {
//                    new ExperimentDBService().addRecommendationToDB(mKruizeExperimentMap, experimentResultDataList);
//                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.DATA_NOT_FOUND);
        }
        sendSuccessResponse(response, "All ok");
    }

    private void sendSuccessResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = response.getWriter();
        out.append(
                new Gson().toJson(
                        new KruizeResponse(message, HttpServletResponse.SC_CREATED, "", "SUCCESS")
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
