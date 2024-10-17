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

import com.autotune.analyzer.serviceObjects.BulkInput;
import com.autotune.analyzer.serviceObjects.BulkJobStatus;
import com.autotune.analyzer.workerimpl.BulkJobManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.*;
import static com.autotune.utils.KruizeConstants.KRUIZE_BULK_API.*;

/**
 *
 */
@WebServlet(asyncSupported = true)
public class BulkService extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(BulkService.class);
    private ExecutorService executorService = Executors.newFixedThreadPool(10);
    private Map<String, BulkJobStatus> jobStatusMap = new ConcurrentHashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String jobID = req.getParameter(JOB_ID);
        String verboseParam = req.getParameter(VERBOSE);
        // If the parameter is not provided (null), default it to false
        boolean verbose = verboseParam != null && Boolean.parseBoolean(verboseParam);
        BulkJobStatus jobDetails = jobStatusMap.get(jobID);
        resp.setContentType(JSON_CONTENT_TYPE);
        resp.setCharacterEncoding(CHARACTER_ENCODING);
        SimpleFilterProvider filters = new SimpleFilterProvider();

        if (jobDetails == null) {
            sendErrorResponse(
                    resp,
                    null,
                    HttpServletResponse.SC_NOT_FOUND,
                    JOB_NOT_FOUND_MSG
            );
        } else {
            try {
                resp.setStatus(HttpServletResponse.SC_OK);
                // Return the JSON representation of the JobStatus object
                ObjectMapper objectMapper = new ObjectMapper();
                if (!verbose) {
                    filters.addFilter("jobFilter", SimpleBeanPropertyFilter.serializeAllExcept("data"));
                } else {
                    filters.addFilter("jobFilter", SimpleBeanPropertyFilter.serializeAll());
                }
                objectMapper.setFilterProvider(filters);
                String jsonResponse = objectMapper.writeValueAsString(jobDetails);
                resp.getWriter().write(jsonResponse);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Set response type
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);

        // Create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        // Read the request payload and map to RequestPayload class
        BulkInput payload = objectMapper.readValue(request.getInputStream(), BulkInput.class);

        // Generate a unique jobID
        String jobID = UUID.randomUUID().toString();
        BulkJobStatus.Data data = new BulkJobStatus.Data(
                new BulkJobStatus.Experiments(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()),
                new BulkJobStatus.Recommendations(new BulkJobStatus.RecommendationData(
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>()
                ))
        );
        jobStatusMap.put(jobID, new BulkJobStatus(jobID, IN_PROGRESS, data, Instant.now()));
        // Submit the job to be processed asynchronously
        executorService.submit(new BulkJobManager(jobID, jobStatusMap, payload));

        // Just sending a simple success response back
        // Return the jobID to the user
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JOB_ID, jobID);
        response.getWriter().write(jsonObject.toString());
    }


    @Override
    public void destroy() {
        executorService.shutdown();
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
