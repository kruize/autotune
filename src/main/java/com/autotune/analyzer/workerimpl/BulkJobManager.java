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

import com.autotune.analyzer.kruizeObject.RecommendationSettings;
import com.autotune.analyzer.serviceObjects.*;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.dataSourceMetadata.*;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceManager;
import com.autotune.common.k8sObjects.TrialSettings;
import com.autotune.common.utils.CommonUtils;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.GenericRestApiClient;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.MetricsConfig;
import com.autotune.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import io.micrometer.core.instrument.Timer;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.autotune.operator.KruizeDeploymentInfo.bulk_thread_pool_size;
import static com.autotune.utils.KruizeConstants.KRUIZE_BULK_API.*;
import static com.autotune.utils.KruizeConstants.KRUIZE_BULK_API.NotificationConstants.*;


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
    private BulkInput bulkInput;
    private BulkJobStatus jobData;

    public BulkJobManager(String jobID, BulkJobStatus jobData, BulkInput payload) {
        this.jobID = jobID;
        this.jobData = jobData;
        this.bulkInput = payload;
    }

    public static List<String> appendExperiments(List<String> allExperiments, String experimentName) {
        allExperiments.add(experimentName);
        return allExperiments;
    }

    // Helper method to parse labelString into a map
    private static Map<String, String> parseLabelString(String labelString) {
        Map<String, String> labelsMap = new HashMap<>();
        for (String pair : labelString.split(",\\s*")) { // Split on comma and optional space
            String[] keyValue = pair.split("=", 2); // Split on first "=" only
            if (keyValue.length == 2) {
                String value = keyValue[1].trim();
                // Remove surrounding quotes if present
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                labelsMap.put(keyValue[0].trim(), value);
            }
        }
        return labelsMap;
    }

    @Override
    public void run() {
        String statusValue = "failure";
        MetricsConfig.activeJobs.incrementAndGet();
        io.micrometer.core.instrument.Timer.Sample timerRunJob = Timer.start(MetricsConfig.meterRegistry());
        DataSourceMetadataInfo metadataInfo = null;
        DataSourceManager dataSourceManager = new DataSourceManager();
        DataSourceInfo datasource = null;
        String labelString = null;
        Map<String, String> includeResourcesMap = null;
        Map<String, String> excludeResourcesMap = null;
        try {
            if (this.bulkInput.getFilter() != null) {
                labelString = getLabels(this.bulkInput.getFilter());
                includeResourcesMap = buildRegexFilters(this.bulkInput.getFilter().getInclude());
                excludeResourcesMap = buildRegexFilters(this.bulkInput.getFilter().getExclude());
            }
            if (null == this.bulkInput.getDatasource()) {
                this.bulkInput.setDatasource(CREATE_EXPERIMENT_CONFIG_BEAN.getDatasourceName());
            }
            try {
                datasource = CommonUtils.getDataSourceInfo(this.bulkInput.getDatasource());
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
                e.printStackTrace();
                BulkJobStatus.Notification notification = DATASOURCE_NOT_REG_INFO;
                notification.setMessage(String.format(notification.getMessage(), e.getMessage()));
                setFinalJobStatus(FAILED, String.valueOf(HttpURLConnection.HTTP_BAD_REQUEST), notification, datasource);
            }
            if (null != datasource) {
                JSONObject daterange = processDateRange(this.bulkInput.getTime_range());
                if (null != daterange) {
                    metadataInfo = dataSourceManager.importMetadataFromDataSource(datasource, labelString, (Long) daterange.get(START_TIME),
                            (Long) daterange.get(END_TIME), (Integer) daterange.get(STEPS), includeResourcesMap, excludeResourcesMap);
                } else {
                    metadataInfo = dataSourceManager.importMetadataFromDataSource(datasource, labelString, 0, 0,
                            0, includeResourcesMap, excludeResourcesMap);
                }
                if (null == metadataInfo) {
                    setFinalJobStatus(COMPLETED, String.valueOf(HttpURLConnection.HTTP_OK), NOTHING_INFO, datasource);
                } else {
                    Map<String, CreateExperimentAPIObject> createExperimentAPIObjectMap = getExperimentMap(labelString, jobData, metadataInfo, datasource); //Todo Store this map in buffer and use it if BulkAPI pods restarts and support experiment_type
                    jobData.getSummary().setTotal_experiments(createExperimentAPIObjectMap.size());
                    jobData.getSummary().setProcessed_experiments(0);
                    if (jobData.getSummary().getTotal_experiments() > KruizeDeploymentInfo.bulk_api_limit) {
                        setFinalJobStatus(FAILED, String.valueOf(HttpURLConnection.HTTP_BAD_REQUEST), LIMIT_INFO, datasource);
                    } else {
                        ExecutorService createExecutor = Executors.newFixedThreadPool(bulk_thread_pool_size);
                        ExecutorService generateExecutor = Executors.newFixedThreadPool(bulk_thread_pool_size);
                        try {
                            for (CreateExperimentAPIObject apiObject : createExperimentAPIObjectMap.values()) {
                                DataSourceInfo finalDatasource = datasource;
                                createExecutor.submit(() -> {
                                    String experiment_name = apiObject.getExperimentName();
                                    BulkJobStatus.Experiment experiment = jobData.addExperiment(experiment_name);
                                    try {
                                        // send request to createExperiment API for experiment creation
                                        GenericRestApiClient apiClient = new GenericRestApiClient(finalDatasource);
                                        apiClient.setBaseURL(KruizeDeploymentInfo.experiments_url);
                                        GenericRestApiClient.HttpResponseWrapper responseCode;
                                        boolean experiment_exists = false;
                                        try {
                                            responseCode = apiClient.callKruizeAPI("[" + new Gson().toJson(apiObject) + "]");
                                            LOGGER.debug("API Response code: {}", responseCode);
                                            if (responseCode.getStatusCode() == HttpURLConnection.HTTP_CREATED) {
                                                experiment_exists = true;
                                            } else if (responseCode.getStatusCode() == HttpURLConnection.HTTP_CONFLICT) {
                                                experiment_exists = true;
                                            } else {

                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();

                                        } finally {
                                            if (!experiment_exists) {
                                                LOGGER.info("Processing experiment {}", jobData.getSummary().getProcessed_experiments());
                                                jobData.getSummary().incrementProcessed_experiments();
                                            }
                                            synchronized (jobData) {
                                                if (jobData.getSummary().getTotal_experiments() == jobData.getSummary().getProcessed_experiments().get()) {
                                                    setFinalJobStatus(COMPLETED, null, null, finalDatasource);
                                                }
                                            }
                                        }

                                        if (experiment_exists) {
                                            generateExecutor.submit(() -> {
                                                // send request to generateRecommendations API
                                                GenericRestApiClient recommendationApiClient = new GenericRestApiClient(finalDatasource);
                                                String encodedExperimentName;
                                                encodedExperimentName = URLEncoder.encode(experiment_name, StandardCharsets.UTF_8);
                                                recommendationApiClient.setBaseURL(String.format(KruizeDeploymentInfo.recommendations_url, encodedExperimentName));
                                                GenericRestApiClient.HttpResponseWrapper recommendationResponseCode = null;
                                                try {
                                                    recommendationResponseCode = recommendationApiClient.callKruizeAPI(null);
                                                    LOGGER.debug("API Response code: {}", recommendationResponseCode);
                                                    if (recommendationResponseCode.getStatusCode() == HttpURLConnection.HTTP_CREATED) {
                                                        experiment.setStatus(NotificationConstants.Status.PROCESSED);
                                                    } else {
                                                        experiment.setStatus(NotificationConstants.Status.FAILED);

                                                    }
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                    experiment.setStatus(NotificationConstants.Status.FAILED);
                                                } finally {
                                                    jobData.getSummary().incrementProcessed_experiments();
                                                    synchronized (jobData) {
                                                        if (jobData.getSummary().getTotal_experiments() == jobData.getSummary().getProcessed_experiments().get()) {
                                                            setFinalJobStatus(COMPLETED, null, null, finalDatasource);
                                                        }
                                                    }
                                                }
                                            });
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        jobData.getSummary().incrementProcessed_experiments();
                                        if (jobData.getSummary().getTotal_experiments() == jobData.getSummary().getProcessed_experiments().get()) {
                                            setFinalJobStatus(COMPLETED, null, null, finalDatasource);
                                        }
                                    }
                                });
                            }
                        } finally {
                            // Shutdown createExecutor and wait for it to finish
                            createExecutor.shutdown();
                            while (!createExecutor.isTerminated()) {
                                try {
                                    createExecutor.awaitTermination(1, TimeUnit.MINUTES);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }

                            // Shutdown generateExecutor and wait for it to finish
                            generateExecutor.shutdown();
                            while (!generateExecutor.isTerminated()) {
                                try {
                                    generateExecutor.awaitTermination(1, TimeUnit.MINUTES);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }

                            if (jobData.getSummary().getTotal_experiments() == jobData.getSummary().getProcessed_experiments().get()) {
                                statusValue = "success";
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            BulkJobStatus.Notification notification;
            if (e instanceof SocketTimeoutException) {
                notification = DATASOURCE_GATEWAY_TIMEOUT_INFO;
            } else if (e instanceof ConnectTimeoutException) {
                notification = DATASOURCE_CONNECT_TIMEOUT_INFO;
            } else {
                notification = DATASOURCE_DOWN_INFO;
            }
            notification.setMessage(String.format(notification.getMessage(), e.getMessage()));
            setFinalJobStatus(FAILED, String.valueOf(HttpURLConnection.HTTP_UNAVAILABLE), notification, datasource);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
            setFinalJobStatus(FAILED, String.valueOf(HttpURLConnection.HTTP_INTERNAL_ERROR), new BulkJobStatus.Notification(BulkJobStatus.NotificationType.ERROR, e.getMessage(), HttpURLConnection.HTTP_INTERNAL_ERROR), datasource);
        } finally {
            if (null != timerRunJob) {
                MetricsConfig.timerRunJob = MetricsConfig.timerBRunJob.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerRunJob.stop(MetricsConfig.timerRunJob);
            }
            MetricsConfig.activeJobs.decrementAndGet();
        }
    }

    public void setFinalJobStatus(String status, String notificationKey, BulkJobStatus.Notification notification, DataSourceInfo finalDatasource) {
        jobData.getSummary().setStatus(status);
        jobData.getSummary().setEndTime(Instant.now());
        if (null != notification)
            jobData.getSummary().setNotification(notificationKey, notification);
        GenericRestApiClient apiClient = new GenericRestApiClient(finalDatasource);
        if (null != bulkInput.getWebhook() && null != bulkInput.getWebhook().getUrl()) {
            apiClient.setBaseURL(bulkInput.getWebhook().getUrl());
            GenericRestApiClient.HttpResponseWrapper responseCode;
            BulkJobStatus.Webhook webhook = new BulkJobStatus.Webhook(WebHookStatus.IN_PROGRESS);
            jobData.setWebhook(webhook);
            try {
                responseCode = apiClient.callKruizeAPI("[" + new Gson().toJson(jobData) + "]");
                LOGGER.debug("API Response code: {}", responseCode);
                if (responseCode.getStatusCode() == HttpURLConnection.HTTP_OK) {
                    webhook.setStatus(WebHookStatus.COMPLETED);
                    jobData.setWebhook(webhook);
                } else {
                    BulkJobStatus.Notification webHookNotification = new BulkJobStatus.Notification(BulkJobStatus.NotificationType.ERROR, responseCode.getResponseBody().toString(), responseCode.getStatusCode());
                    webhook.setNotifications(webHookNotification);
                    webhook.setStatus(WebHookStatus.FAILED);
                    jobData.setWebhook(webhook);
                }
            } catch (Exception e) {
                e.printStackTrace();
                BulkJobStatus.Notification webHookNotification = new BulkJobStatus.Notification(BulkJobStatus.NotificationType.ERROR, e.toString(), HttpURLConnection.HTTP_INTERNAL_ERROR);
                webhook.setNotifications(webHookNotification);
                webhook.setStatus(WebHookStatus.FAILED);
                jobData.setWebhook(webhook);
            }
        }
    }

    Map<String, CreateExperimentAPIObject> getExperimentMap(String labelString, BulkJobStatus jobData, DataSourceMetadataInfo metadataInfo, DataSourceInfo datasource) throws Exception {
        String statusValue = "failure";
        Timer.Sample timerGetExpMap = Timer.start(MetricsConfig.meterRegistry());
        try {
            Map<String, CreateExperimentAPIObject> createExperimentAPIObjectMap = new HashMap<>();
            Collection<DataSource> dataSourceCollection = metadataInfo.getDatasources().values();
            for (DataSource ds : dataSourceCollection) {
                HashMap<String, DataSourceCluster> clusterHashMap = ds.getClusters();
                for (DataSourceCluster dsc : clusterHashMap.values()) {
                    HashMap<String, DataSourceNamespace> namespaceHashMap = dsc.getNamespaces();
                    for (DataSourceNamespace namespace : namespaceHashMap.values()) {
                        HashMap<String, DataSourceWorkload> dataSourceWorkloadHashMap = namespace.getWorkloads();
                        if (dataSourceWorkloadHashMap != null) {
                            for (DataSourceWorkload dsw : dataSourceWorkloadHashMap.values()) {
                                HashMap<String, DataSourceContainer> dataSourceContainerHashMap = dsw.getContainers();
                                if (dataSourceContainerHashMap != null) {
                                    for (DataSourceContainer dc : dataSourceContainerHashMap.values()) {
                                        // Experiment name - dynamically constructed
                                        String experiment_name = frameExperimentName(labelString, dsc, namespace, dsw, dc);
                                        // create JSON to be passed in the createExperimentAPI
                                        List<CreateExperimentAPIObject> createExperimentAPIObjectList = new ArrayList<>();
                                        CreateExperimentAPIObject apiObject = prepareCreateExperimentJSONInput(dc, dsc, dsw, namespace,
                                                experiment_name, createExperimentAPIObjectList);
                                        createExperimentAPIObjectMap.put(experiment_name, apiObject);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return createExperimentAPIObjectMap;
        } finally {
            if (null != timerGetExpMap) {
                MetricsConfig.timerGetExpMap = MetricsConfig.timerBGetExpMap.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerGetExpMap.stop(MetricsConfig.timerGetExpMap);
            }
        }
    }

    private String getLabels(BulkInput.FilterWrapper filter) {
        String uniqueKey = null;
        try {
            // Process labels in the 'include' section
            if (filter.getInclude() != null) {
                // Initialize StringBuilder for uniqueKey
                StringBuilder includeLabelsBuilder = new StringBuilder();
                Map<String, String> includeLabels = filter.getInclude().getLabels();
                if (includeLabels != null && !includeLabels.isEmpty()) {
                    includeLabels.forEach((key, value) ->
                            includeLabelsBuilder.append(key).append("=").append("\"" + value + "\"").append(",")
                    );
                    // Remove trailing comma
                    if (!includeLabelsBuilder.isEmpty()) {
                        includeLabelsBuilder.setLength(includeLabelsBuilder.length() - 1);
                    }
                    LOGGER.debug("Include Labels: {}", includeLabelsBuilder);
                    uniqueKey = includeLabelsBuilder.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
        return uniqueKey;
    }

    private Map<String, String> buildRegexFilters(BulkInput.Filter filter) {
        Map<String, String> resourceFilters = new HashMap<>();
        if (filter != null) {
            resourceFilters.put("namespaceRegex", filter.getNamespace() != null ?
                    filter.getNamespace().stream().map(String::trim).collect(Collectors.joining("|")) : "");
            resourceFilters.put("workloadRegex", filter.getWorkload() != null ?
                    filter.getWorkload().stream().map(String::trim).collect(Collectors.joining("|")) : "");
            resourceFilters.put("containerRegex", filter.getContainers() != null ?
                    filter.getContainers().stream().map(String::trim).collect(Collectors.joining("|")) : "");
        }
        return resourceFilters;
    }

    private JSONObject processDateRange(BulkInput.TimeRange timeRange) {
        //TODO: add validations for the time range
        JSONObject dateRange = null;
        if (null != timeRange && timeRange.getStart() != null && timeRange.getEnd() != null) {
            String intervalStartTimeStr = timeRange.getStart();
            String intervalEndTimeStr = timeRange.getEnd();
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
            dateRange.put(START_TIME, interval_start_time_epoc);
            dateRange.put(END_TIME, interval_end_time_epoc);
            dateRange.put(STEPS, steps);
        }
        return dateRange;
    }

    /**
     * @param dc                         DataSourceContainer object to get the container details
     * @param dsc                        DataSourceCluster object to get the cluster details
     * @param dsw                        DataSourceWorkload object to get the workload details
     * @param namespace                  DataSourceNamespace object to get the namespace details
     * @param createExperimentAPIObjects
     * @return Json string to be sent to the createExperimentAPI for experiment creation
     * @throws JsonProcessingException
     */
    private CreateExperimentAPIObject prepareCreateExperimentJSONInput(DataSourceContainer dc, DataSourceCluster dsc, DataSourceWorkload dsw,
                                                                       DataSourceNamespace namespace, String experiment_name, List<CreateExperimentAPIObject> createExperimentAPIObjects) throws IOException {

        CreateExperimentAPIObject createExperimentAPIObject = new CreateExperimentAPIObject();
        createExperimentAPIObject.setMode(CREATE_EXPERIMENT_CONFIG_BEAN.getMode());
        createExperimentAPIObject.setTargetCluster(CREATE_EXPERIMENT_CONFIG_BEAN.getTarget());
        createExperimentAPIObject.setApiVersion(CREATE_EXPERIMENT_CONFIG_BEAN.getVersion());
        createExperimentAPIObject.setExperimentName(experiment_name);
        createExperimentAPIObject.setDatasource(this.bulkInput.getDatasource());
        createExperimentAPIObject.setClusterName(dsc.getDataSourceClusterName());
        createExperimentAPIObject.setPerformanceProfile(CREATE_EXPERIMENT_CONFIG_BEAN.getPerformanceProfile());
        List<KubernetesAPIObject> kubernetesAPIObjectList = new ArrayList<>();
        KubernetesAPIObject kubernetesAPIObject = new KubernetesAPIObject();
        ContainerAPIObject cao = new ContainerAPIObject(dc.getContainerName(),
                dc.getContainerImageName(), null, null);
        kubernetesAPIObject.setContainerAPIObjects(Arrays.asList(cao));
        kubernetesAPIObject.setName(dsw.getWorkloadName());
        kubernetesAPIObject.setType(dsw.getWorkloadType());
        kubernetesAPIObject.setNamespace(namespace.getNamespace());
        kubernetesAPIObjectList.add(kubernetesAPIObject);
        createExperimentAPIObject.setKubernetesObjects(kubernetesAPIObjectList);
        RecommendationSettings rs = new RecommendationSettings();
        rs.setThreshold(CREATE_EXPERIMENT_CONFIG_BEAN.getThreshold());
        createExperimentAPIObject.setRecommendationSettings(rs);
        TrialSettings trialSettings = new TrialSettings();
        trialSettings.setMeasurement_durationMinutes(CREATE_EXPERIMENT_CONFIG_BEAN.getMeasurementDurationStr());
        createExperimentAPIObject.setTrialSettings(trialSettings);

        createExperimentAPIObject.setExperiment_id(Utils.generateID(createExperimentAPIObject.toString()));
        createExperimentAPIObject.setStatus(AnalyzerConstants.ExperimentStatus.IN_PROGRESS);
        createExperimentAPIObject.setExperimentType(AnalyzerConstants.ExperimentType.CONTAINER);

        createExperimentAPIObjects.add(createExperimentAPIObject);

        return createExperimentAPIObject;
    }

    /**
     * @param labelString
     * @param dataSourceCluster
     * @param dataSourceNamespace
     * @param dataSourceWorkload
     * @param dataSourceContainer
     * @return
     */
    public String frameExperimentName(String labelString, DataSourceCluster dataSourceCluster, DataSourceNamespace dataSourceNamespace, DataSourceWorkload dataSourceWorkload, DataSourceContainer dataSourceContainer) {

        String datasource = this.bulkInput.getDatasource();
        String clusterName = dataSourceCluster.getDataSourceClusterName();
        String namespace = dataSourceNamespace.getNamespace();
        String workloadName = dataSourceWorkload.getWorkloadName();
        String workloadType = dataSourceWorkload.getWorkloadType();
        String containerName = dataSourceContainer.getContainerName();

        String experimentName = KruizeDeploymentInfo.experiment_name_format
                .replace("%datasource%", datasource)
                .replace("%clustername%", clusterName)
                .replace("%namespace%", namespace)
                .replace("%workloadname%", workloadName)
                .replace("%workloadtype%", workloadType)
                .replace("%containername%", containerName);

        if (null != labelString) {
            // Parse labelString into a map for quick lookup
            Map<String, String> labelsMap = parseLabelString(labelString);

            // Regular expression to match any %label:labelName% pattern
            Pattern labelPattern = Pattern.compile("%label:([a-zA-Z0-9_]+)%");
            Matcher matcher = labelPattern.matcher(experimentName);

            // Process each label placeholder
            StringBuilder result = new StringBuilder();
            int lastEnd = 0;
            while (matcher.find()) {
                result.append(experimentName, lastEnd, matcher.start());
                String labelKey = matcher.group(1); // Extracts the label name
                String labelValue = labelsMap.getOrDefault(labelKey, "unknown" + labelKey);
                result.append(labelValue != null ? labelValue : "unknown" + labelKey);
                lastEnd = matcher.end();
                experimentName = experimentName.replace(matcher.group().toString(), labelValue);
            }
        }
        LOGGER.debug("Experiment name: {}", experimentName);
        return experimentName;
    }
}
