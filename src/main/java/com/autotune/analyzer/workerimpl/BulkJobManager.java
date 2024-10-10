/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.workerimpl;


import com.autotune.analyzer.kruizeObject.CreateExperimentConfigBean;
import com.autotune.analyzer.serviceObjects.*;
import com.autotune.common.data.dataSourceMetadata.*;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceManager;
import com.autotune.common.utils.CommonUtils;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.autotune.operator.KruizeDeploymentInfo.bulk_thread_pool_size;
import static com.autotune.utils.KruizeConstants.KRUIZE_BULK_API.*;


/**
 * The `run` method processes bulk input to create experiments and generates resource optimization recommendations.
 * It handles the creation of experiment names based on various data source components, makes HTTP POST requests
 * to generate recommendations, and updates job statuses based on the progress of the recommendations.
 *
 * <p>
 * Key operations include:
 * <ul>
 *     <li>Processing 'include' filter labels to generate a unique key.</li>
 *     <li>Validating and setting the data source if not provided in the input.</li>
 *     <li>Extracting time range from the input and converting it to epoch time format.</li>
 *     <li>Fetching metadata information from the data source for the specified time range and labels.</li>
 *     <li>Creating experiments for each data source component such as clusters, namespaces, workloads, and containers.</li>
 *     <li>Submitting HTTP POST requests to retrieve recommendations for each created experiment.</li>
 *     <li>Updating the job status and progress based on the completion of recommendations.</li>
 * </ul>
 * </p>
 *
 * <p>
 * In case of an exception during the process, error messages are logged, and the exception is printed for debugging.
 * </p>
 *
 * @throws RuntimeException if URL or HTTP connection setup fails.
 * @throws IOException if an error occurs while sending HTTP requests.
 */
public class BulkJobManager implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(BulkJobManager.class);

    private String jobID;
    private Map<String, BulkJobStatus> jobStatusMap;
    private BulkInput bulkInput;


    public BulkJobManager(String jobID, Map<String, BulkJobStatus> jobStatusMap, BulkInput payload) {
        this.jobID = jobID;
        this.jobStatusMap = jobStatusMap;
        this.bulkInput = payload;
    }

    public static List<String> appendExperiments(List<String> allExperiments, String experimentName) {
        allExperiments.add(experimentName);
        return allExperiments;
    }

    @Override
    public void run() {
        try {
            BulkJobStatus jobData = jobStatusMap.get(jobID);
            String uniqueKey = getLabels(this.bulkInput.getFilter());
            if (null == this.bulkInput.getDatasource()) {
                this.bulkInput.setDatasource(CREATE_EXPERIMENT_CONFIG_BEAN.getDatasource());
            }
            DataSourceMetadataInfo metadataInfo = null;
            DataSourceManager dataSourceManager = new DataSourceManager();
            DataSourceInfo datasource = CommonUtils.getDataSourceInfo(this.bulkInput.getDatasource());
            JSONObject daterange = processDateRange(this.bulkInput.getTime_range());
            if (null != daterange)
                metadataInfo = dataSourceManager.importMetadataFromDataSource(datasource, uniqueKey, (Long) daterange.get("start_time"), (Long) daterange.get("end_time"), (Integer) daterange.get("steps"));
            else {
                metadataInfo = dataSourceManager.importMetadataFromDataSource(datasource, uniqueKey, 0, 0, 0);
            }
            if (null == metadataInfo) {
                jobData.setStatus(COMPLETED);
                jobData.setMessage(NOTHING);
            } else {
                Map<String, CreateExperimentAPIObject> createExperimentAPIObjectMap = getExperimentMap(metadataInfo); //Todo Store this map in buffer and use it if BulkAPI pods restarts and support experiment_type
                jobData.setTotal_experiments(createExperimentAPIObjectMap.size());
                jobData.setProcessed_experiments(0);
                if (jobData.getTotal_experiments() > KruizeDeploymentInfo.BULK_API_LIMIT) {
                    jobStatusMap.get(jobID).setStatus(FAILED);
                    jobStatusMap.get(jobID).setMessage(String.format(LIMIT_MESSAGE, KruizeDeploymentInfo.BULK_API_LIMIT));
                } else {
                    ExecutorService createExecutor = Executors.newFixedThreadPool(bulk_thread_pool_size);
                    ExecutorService generateExecutor = Executors.newFixedThreadPool(bulk_thread_pool_size);
                    for (CreateExperimentAPIObject apiObject : createExperimentAPIObjectMap.values()) {
                        createExecutor.submit(() -> {
                            String experiment_name = apiObject.getExperimentName();
                            BulkJobStatus.Experiments newExperiments = jobData.getData().getExperiments();
                            BulkJobStatus.RecommendationData recommendationData = jobData.getData().getRecommendations().getData();
                            try {
                                jobData.getData().getExperiments().setNewExperiments(
                                            appendExperiments(newExperiments.getNewExperiments(), experiment_name));
                                generateExecutor.submit(() -> {

                                    jobData.getData().getRecommendations().getData().setUnprocessed(
                                            appendExperiments(recommendationData.getUnprocessed(), experiment_name)
                                    );

                                    URL url;
                                    HttpURLConnection connection = null;
                                    int statusCode = 0;
                                    try {
                                        url = new URL(String.format(KruizeDeploymentInfo.recommendations_url, experiment_name));
                                        connection = (HttpURLConnection) url.openConnection();
                                        connection.setRequestMethod("POST");

                                        recommendationData.moveToProgress(experiment_name);

                                        statusCode = connection.getResponseCode();
                                    } catch (IOException e) {
                                        LOGGER.error(e.getMessage());

                                        recommendationData.moveToFailed(experiment_name);

                                        throw new RuntimeException(e);
                                    } finally {
                                        if (null != connection) connection.disconnect();
                                    }
                                    if (statusCode == HttpURLConnection.HTTP_CREATED) {

                                        recommendationData.moveToCompleted(experiment_name);
                                        jobData.setProcessed_experiments(jobData.getProcessed_experiments() + 1);

                                        if (jobData.getTotal_experiments() == jobData.getProcessed_experiments()) {
                                            jobData.setStatus(COMPLETED);
                                            jobStatusMap.get(jobID).setEndTime(Instant.now());
                                        }

                                    } else {

                                        recommendationData.moveToFailed(experiment_name);

                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                recommendationData.moveToFailed(experiment_name);
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
            jobStatusMap.get(jobID).setStatus("FAILED");
        }
    }


    Map<String, CreateExperimentAPIObject> getExperimentMap(DataSourceMetadataInfo metadataInfo) throws IOException {
        Map<String, CreateExperimentAPIObject> createExperimentAPIObjectMap = new HashMap<>();
        Collection<DataSource> dataSourceCollection = metadataInfo.getDataSourceHashMap().values();
        for (DataSource ds : dataSourceCollection) {
            HashMap<String, DataSourceCluster> clusterHashMap = ds.getDataSourceClusterHashMap();
            for (DataSourceCluster dsc : clusterHashMap.values()) {
                HashMap<String, DataSourceNamespace> namespaceHashMap = dsc.getDataSourceNamespaceHashMap();
                for (DataSourceNamespace namespace : namespaceHashMap.values()) {
                    HashMap<String, DataSourceWorkload> dataSourceWorkloadHashMap = namespace.getDataSourceWorkloadHashMap();
                    if (dataSourceWorkloadHashMap != null) {
                        for (DataSourceWorkload dsw : dataSourceWorkloadHashMap.values()) {
                            HashMap<String, DataSourceContainer> dataSourceContainerHashMap = dsw.getDataSourceContainerHashMap();
                            if (dataSourceContainerHashMap != null) {
                                for (DataSourceContainer dc : dataSourceContainerHashMap.values()) {
                                    // Experiment name - dynamically constructed
                                    String experiment_name = this.bulkInput.getDatasource() + "|" + dsc.getDataSourceClusterName() + "|"
                                            + namespace.getDataSourceNamespaceName() + "|" + dsw.getDataSourceWorkloadName() + "("
                                            + dsw.getDataSourceWorkloadType() + ")" + "|" + dc.getDataSourceContainerName();
                                    // create JSON to be passed in the createExperimentAPI
                                    String createExpJSON = prepareCreateExperimentJSONInput(dc, dsc, dsw, namespace,
                                            experiment_name, this.bulkInput.getDatasource());
                                    // send request to createExperiment API for experiment creation
                                    int responseCode = sendCreateExperimentRequest(createExpJSON);
                                    // if the experiment is successfully created, add it in the map
                                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                                        // Parse and extract the first JSON object from the array
                                        JsonObject jsonObject = JsonParser.parseString(createExpJSON).getAsJsonArray().get(0).getAsJsonObject();
                                        // Convert JSON object to CreateExperimentAPIObject
                                        CreateExperimentAPIObject createExperimentAPIObject = new Gson().fromJson(jsonObject, CreateExperimentAPIObject.class);
                                        // Add to the map using the experiment_name
                                        createExperimentAPIObjectMap.put(jsonObject.get("experiment_name").getAsString(), createExperimentAPIObject);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return createExperimentAPIObjectMap;
    }

    private String getLabels(BulkInput.FilterWrapper filter) {
        String uniqueKey = null;
        try {
            // Process labels in the 'include' section
            if (filter != null && filter.getInclude() != null) {
                // Initialize StringBuilder for uniqueKey
                StringBuilder includeLabelsBuilder = new StringBuilder();
                Map<String, String> includeLabels = filter.getInclude().getLabels();
                if (includeLabels != null && !includeLabels.isEmpty()) {
                    includeLabels.forEach((key, value) ->
                            includeLabelsBuilder.append(key).append("=").append("\"" + value + "\"").append(",")
                    );
                    // Remove trailing comma
                    if (includeLabelsBuilder.length() > 0) {
                        includeLabelsBuilder.setLength(includeLabelsBuilder.length() - 1);
                    }
                    LOGGER.debug("Include Labels: " + includeLabelsBuilder);
                    uniqueKey = includeLabelsBuilder.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
        return uniqueKey;
    }

    private JSONObject processDateRange(BulkInput.TimeRange timeRange) {
        JSONObject dateRange = null;
        if (null != timeRange && timeRange.getStart() != null && timeRange.getEnd() != null) {
            String intervalEndTimeStr = timeRange.getStart();
            String intervalStartTimeStr = timeRange.getEnd();
            long interval_end_time_epoc = 0;
            long interval_start_time_epoc = 0;
            LocalDateTime localDateTime = LocalDateTime.parse(intervalEndTimeStr, DateTimeFormatter.ofPattern(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT));
            interval_end_time_epoc = localDateTime.toEpochSecond(ZoneOffset.UTC);
            Timestamp interval_end_time = Timestamp.from(localDateTime.toInstant(ZoneOffset.UTC));
            localDateTime = LocalDateTime.parse(intervalStartTimeStr, DateTimeFormatter.ofPattern(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT));
            interval_start_time_epoc = localDateTime.toEpochSecond(ZoneOffset.UTC);
            Timestamp interval_start_time = Timestamp.from(localDateTime.toInstant(ZoneOffset.UTC));
            int steps = CREATE_EXPERIMENT_CONFIG_BEAN.getMeasurementDuration() * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE; // todo fetch experiment recommendations setting measurement
            dateRange = new JSONObject();
            dateRange.put("start_time", interval_start_time_epoc);
            dateRange.put("end_time", interval_end_time_epoc);
            dateRange.put("steps", steps);
        }
        return dateRange;
    }



    /**
     * @param dc         DataSourceContainer object to get the container details
     * @param dsc        DataSourceCluster object to get the cluster details
     * @param dsw        DataSourceWorkload object to get the workload details
     * @param namespace  DataSourceNamespace object to get the namespace details
     * @param datasource Datasource name to be set
     * @return Json string to be sent to the createExperimentAPI for experiment creation
     * @throws JsonProcessingException
     */
    private String prepareCreateExperimentJSONInput(DataSourceContainer dc, DataSourceCluster dsc, DataSourceWorkload dsw,
                                                    DataSourceNamespace namespace, String experiment_name, String datasource) throws IOException {

        CreateExperimentConfigBean createExperimentConfigBean = CREATE_EXPERIMENT_CONFIG_BEAN;
        // Experiment name
        createExperimentConfigBean.setExperiment_name(experiment_name);
        // Datasource
        createExperimentConfigBean.setDatasource(datasource);
        // Cluster name
        createExperimentConfigBean.setCluster_name(dsc.getDataSourceClusterName());
        // Kubernetes objects
        List<KubernetesAPIObject> kubernetesAPIObjectList = new ArrayList<>();
        KubernetesAPIObject kubernetesAPIObject = new KubernetesAPIObject();
        kubernetesAPIObject.setType(dsw.getDataSourceWorkloadType());
        kubernetesAPIObject.setName(dsw.getDataSourceWorkloadName());
        kubernetesAPIObject.setNamespace(namespace.getDataSourceNamespaceName());
        // Containers
        ContainerAPIObject containerAPIObject = new ContainerAPIObject(dc.getDataSourceContainerName(),
                dc.getDataSourceContainerImageName(), null, null);
        // Add container to the Kubernetes object
        kubernetesAPIObject.setContainerAPIObjects(Arrays.asList(containerAPIObject));
        kubernetesAPIObjectList.add(kubernetesAPIObject);
        // Add the Kubernetes objects to the createExperimentConfigBean
        createExperimentConfigBean.setKubernetes_objects(kubernetesAPIObjectList);

        // list to hold CreateExperimentConfigBean objects
        List<CreateExperimentConfigBean> createExperimentConfigBeanList = new ArrayList<>();

        // Add CreateExperimentConfigBean object to the list
        createExperimentConfigBeanList.add(createExperimentConfigBean);

        // Convert to JSON
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(createExperimentConfigBeanList);
        LOGGER.debug("CreateExp JSON: {}", json);
        return json;
    }

    public static int sendCreateExperimentRequest(String jsonInput) throws IOException {
        URL url;
        try {
            url = new URL(KruizeDeploymentInfo.experiments_url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
        try {
            connection.setRequestMethod("POST");
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
        } catch (ProtocolException e) {
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
        // Write JSON input to the connection output stream
        try(OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Check for the response code to verify if request was successful
        int responseCode = connection.getResponseCode();
        System.out.println("POST Response Code : " + responseCode);
        return responseCode;
    }
}
