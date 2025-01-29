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
import com.autotune.utils.MetricsConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.micrometer.core.instrument.Timer;
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
import java.util.*;
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
    private static Map<String, BulkJobStatus> jobStatusMap = new ConcurrentHashMap<>();
    private ExecutorService executorService = Executors.newFixedThreadPool(10);

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
        String statusValue = "failure";
        Timer.Sample timerJobStatus = Timer.start(MetricsConfig.meterRegistry());
        try {
            String jobID = req.getParameter(JOB_ID);
            String verboseParam = req.getParameter(VERBOSE);
            // Read query parameters
            String includeParams = req.getParameter("include");
            String excludeParams = req.getParameter("exclude");
            String experiment_name = req.getParameter("experiment_name");

            LOGGER.info(" include fields: {}", includeParams);
            LOGGER.info(" exclude fields: {}", excludeParams);

            // Parse the include and exclude parameters into lists
            Set<String> includeFields = includeParams != null ? new HashSet<>(Arrays.asList(includeParams.split(","))) : new HashSet<>(Arrays.asList("summary"));
            Set<String> excludeFields = excludeParams != null ? new HashSet<>(Arrays.asList(excludeParams.split(","))) : Collections.emptySet();

            LOGGER.info(" include fields: {}", includeFields);
            LOGGER.info(" exclude fields: {}", excludeFields);
            LOGGER.info(" experiment_name: {}", experiment_name);

            // If the parameter is not provided (null), default it to false
            boolean verbose = verboseParam != null && Boolean.parseBoolean(verboseParam);
            BulkJobStatus jobDetails;
            LOGGER.info("Job ID: " + jobID);
            if (jobStatusMap.isEmpty()) {
                sendErrorResponse(
                        resp,
                        null,
                        HttpServletResponse.SC_NOT_FOUND,
                        JOB_NOT_FOUND_MSG
                );
                return;
            }
            jobDetails = jobStatusMap.get(jobID);
            LOGGER.info("Job Status: {}", jobDetails.getSummary().getStatus());
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
                    // Filter JSON
                    String filteredJson = filterJson(jobDetails, includeFields, excludeFields, experiment_name);
                    resp.getWriter().write(filteredJson);
                    statusValue = "success";
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != timerJobStatus) {
                MetricsConfig.timerJobStatus = MetricsConfig.timerBJobStatus.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerJobStatus.stop(MetricsConfig.timerJobStatus);
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
        String statusValue = "failure";
        Timer.Sample timerCreateBulkJob = Timer.start(MetricsConfig.meterRegistry());
        try {
            // Set response type
            response.setContentType(JSON_CONTENT_TYPE);
            response.setCharacterEncoding(CHARACTER_ENCODING);
            // TODO: Consume from Kruize Input topic (This will be done in the subsequent release)
            //  kafkaExecutorService.execute(new KafkaConsumerTask(bootstrapServers, topic));

            // Create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();

            // Read the request payload and map to RequestPayload class
            BulkInput payload = objectMapper.readValue(request.getInputStream(), BulkInput.class);

            // Generate a unique jobID
            String jobID = UUID.randomUUID().toString();
            BulkJobStatus jobStatus = new BulkJobStatus(jobID, IN_PROGRESS, Instant.now(), payload);
            jobStatusMap.put(jobID, jobStatus);
            // Submit the job to be processed asynchronously
            executorService.submit(new BulkJobManager(jobID, jobStatus, payload)); //TOdo remove payload as it is part of jobStatus object

            // Just sending a simple success response back
            // Return the jobID to the user
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JOB_ID, jobID);
            response.getWriter().write(jsonObject.toString());
            statusValue = "success";
        } finally {
            if (null != timerCreateBulkJob) {
                MetricsConfig.timerCreateBulkJob = MetricsConfig.timerBCreateBulkJob.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerCreateBulkJob.stop(MetricsConfig.timerCreateBulkJob);
            }
        }
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


    public static String filterJson(BulkJobStatus jsonInput, Set<String> includeFields, Set<String> excludeFields, String experiment_name) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        // Include or exclude fields
        SimpleFilterProvider filters = new SimpleFilterProvider();
        jsonInput.copyByPattern(experiment_name);
        if (!includeFields.isEmpty()) {
            LOGGER.debug("includeFields : {}", includeFields);
            Set<String> jobFields = new HashSet<>();
            for (String field : includeFields) {
                if (field.startsWith("summary")) {
                    jobFields.add("summary");
                    if (field.startsWith("summary|")) {
                        Set<String> summaryFields = new HashSet<>();
                        for (String s : field.split("\\|")) {
                            summaryFields.add(s);
                        }
                        filters.addFilter("summaryFilter", SimpleBeanPropertyFilter.filterOutAllExcept(summaryFields));
                    } else {
                        filters.addFilter("summaryFilter", SimpleBeanPropertyFilter.serializeAll());
                    }
                } else if (field.startsWith("experiments")) {
                    jobFields.add("experiments");
                    if (field.startsWith("experiments|")) {
                        Set<String> experimentFields = new HashSet<>();
                        for (String s : field.split("\\|")) {
                            experimentFields.add(s);
                        }
                        filters.addFilter("experimentFilter", SimpleBeanPropertyFilter.filterOutAllExcept(experimentFields));
                    } else {
                        filters.addFilter("experimentFilter", SimpleBeanPropertyFilter.serializeAll());
                    }
                } else {
                    jobFields.add(field);
                }
            }
            filters.addFilter("jobFilter", SimpleBeanPropertyFilter.filterOutAllExcept(jobFields));
        } else if (!excludeFields.isEmpty()) {
            LOGGER.debug("excludeFields : {}", excludeFields);
            filters.addFilter("jobFilter", SimpleBeanPropertyFilter.serializeAllExcept(excludeFields));
        }

        // Assuming the input JSON has a filter identifier like @JsonFilter("dynamicFilter")
        mapper.setFilterProvider(filters);
        synchronized (jsonInput) {

            return mapper.writeValueAsString(jsonInput);
        }

    }
}
