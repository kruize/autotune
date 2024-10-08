package com.autotune.analyzer.workerimpl;


import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.kruizeObject.RecommendationSettings;
import com.autotune.analyzer.serviceObjects.*;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.dataSourceMetadata.*;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceManager;
import com.autotune.common.k8sObjects.TrialSettings;
import com.autotune.common.utils.CommonUtils;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.autotune.operator.KruizeDeploymentInfo.bulk_thread_pool_size;
import static com.autotune.utils.KruizeConstants.KRUIZE_BULK_API.*;

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

            String uniqueKey = null;
            // Process labels in the 'include' section
            if (this.bulkInput.getFilter() != null && this.bulkInput.getFilter().getInclude() != null) {
                // Initialize StringBuilder for uniqueKey
                StringBuilder includeLabelsBuilder = new StringBuilder();
                Map<String, String> includeLabels = this.bulkInput.getFilter().getInclude().getLabels();
                if (includeLabels != null && !includeLabels.isEmpty()) {
                    includeLabels.forEach((key, value) ->
                            includeLabelsBuilder.append(key).append("=").append("\"" + value + "\"").append(",")
                    );
                    // Remove trailing comma
                    if (includeLabelsBuilder.length() > 0) {
                        includeLabelsBuilder.setLength(includeLabelsBuilder.length() - 1);
                    }
                    LOGGER.info("Include Labels: " + includeLabelsBuilder.toString());
                    uniqueKey = includeLabelsBuilder.toString();
                }
            }
            if (null == this.bulkInput.getDatasource()) {
                this.bulkInput.setDatasource(CREATE_EXPERIMENT_CONFIG_BEAN.getDatasourceName());
            }
            DataSourceMetadataInfo metadataInfo = null;
            DataSourceManager dataSourceManager = new DataSourceManager();
            DataSourceInfo datasource = CommonUtils.getDataSourceInfo(this.bulkInput.getDatasource());


            if (null != this.bulkInput.getTime_range() && this.bulkInput.getTime_range().getStart() != null && this.bulkInput.getTime_range().getEnd() != null) {
                // Extract interval start and end times
                String intervalEndTimeStr = this.bulkInput.getTime_range().getStart();
                String intervalStartTimeStr = this.bulkInput.getTime_range().getEnd();
                long interval_end_time_epoc = 0;
                long interval_start_time_epoc = 0;
                LocalDateTime localDateTime = LocalDateTime.parse(intervalEndTimeStr, DateTimeFormatter.ofPattern(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT));
                interval_end_time_epoc = localDateTime.toEpochSecond(ZoneOffset.UTC);
                Timestamp interval_end_time = Timestamp.from(localDateTime.toInstant(ZoneOffset.UTC));
                localDateTime = LocalDateTime.parse(intervalStartTimeStr, DateTimeFormatter.ofPattern(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT));
                interval_start_time_epoc = localDateTime.toEpochSecond(ZoneOffset.UTC);
                Timestamp interval_start_time = Timestamp.from(localDateTime.toInstant(ZoneOffset.UTC));
                int steps = CREATE_EXPERIMENT_CONFIG_BEAN.getMeasurementDuration() * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE; // todo fetch experiment recommendations setting measurement
                //TODO Get metaData
                //example metadataInfo = dataSourceManager.importMetadataFromDataSource(datasource, uniqueKey, interval_start_time_epoc, interval_end_time_epoc, steps);
            } else {
                //TODO Get metaData
                //metadataInfo = dataSourceManager.importMetadataFromDataSource(datasource, uniqueKey, 0, 0, 0);
            }
            List<String> recommendationsRequiredExperiments = new CopyOnWriteArrayList<>();
            if (null == metadataInfo) {
                jobStatusMap.get(jobID).setStatus(COMPLETED);
            } else {
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
                                            CreateExperimentAPIObject createExperimentAPIObject = new CreateExperimentAPIObject();
                                            createExperimentAPIObject.setMode(CREATE_EXPERIMENT_CONFIG_BEAN.getMode());
                                            createExperimentAPIObject.setTargetCluster(CREATE_EXPERIMENT_CONFIG_BEAN.getTarget());
                                            createExperimentAPIObject.setApiVersion(CREATE_EXPERIMENT_CONFIG_BEAN.getVersion());
                                            String experiment_name = this.bulkInput.getDatasource() + "|" + dsc.getDataSourceClusterName() + "|" + namespace.getDataSourceNamespaceName()
                                                    + "|" + dsw.getDataSourceWorkloadName() + "(" + dsw.getDataSourceWorkloadType() + ")" + "|" + dc.getDataSourceContainerName();
                                            createExperimentAPIObject.setExperimentName(experiment_name);
                                            createExperimentAPIObject.setDatasource(this.bulkInput.getDatasource());
                                            createExperimentAPIObject.setClusterName(dsc.getDataSourceClusterName());
                                            createExperimentAPIObject.setPerformanceProfile(CREATE_EXPERIMENT_CONFIG_BEAN.getPerformanceProfile());
                                            List<KubernetesAPIObject> kubernetesAPIObjectList = new ArrayList<>();
                                            KubernetesAPIObject kubernetesAPIObject = new KubernetesAPIObject();
                                            ContainerAPIObject cao = new ContainerAPIObject(dc.getDataSourceContainerName(),
                                                    dc.getDataSourceContainerImageName(), null, null);
                                            kubernetesAPIObject.setContainerAPIObjects(Arrays.asList(cao));
                                            kubernetesAPIObject.setName(dsw.getDataSourceWorkloadName());
                                            kubernetesAPIObject.setType(dsw.getDataSourceWorkloadType());
                                            kubernetesAPIObject.setNamespace(namespace.getDataSourceNamespaceName());
                                            kubernetesAPIObjectList.add(kubernetesAPIObject);
                                            createExperimentAPIObject.setKubernetesObjects(kubernetesAPIObjectList);
                                            RecommendationSettings rs = new RecommendationSettings();
                                            rs.setThreshold(CREATE_EXPERIMENT_CONFIG_BEAN.getThreshold());
                                            createExperimentAPIObject.setRecommendationSettings(rs);
                                            TrialSettings trialSettings = new TrialSettings();
                                            trialSettings.setMeasurement_durationMinutes(CREATE_EXPERIMENT_CONFIG_BEAN.getMeasurementDurationStr());
                                            createExperimentAPIObject.setTrialSettings(trialSettings);
                                            List<KruizeObject> kruizeExpList = new ArrayList<>();

                                            createExperimentAPIObject.setExperiment_id(Utils.generateID(createExperimentAPIObject.toString()));
                                            createExperimentAPIObject.setStatus(AnalyzerConstants.ExperimentStatus.IN_PROGRESS);

                                            try {
                                                ValidationOutputData output = new ExperimentDBService().addExperimentToDB(createExperimentAPIObject);
                                                if (output.isSuccess()) {
                                                    jobStatusMap.get(jobID).getData().getExperiments().setNewExperiments(
                                                            appendExperiments(jobStatusMap.get(jobID).getData().getExperiments().getNewExperiments(), experiment_name)
                                                    );
                                                }
                                                recommendationsRequiredExperiments.add(experiment_name);
                                            } catch (Exception e) {
                                                LOGGER.info(e.getMessage());
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
                jobStatusMap.get(jobID).setStatus(IN_PROGRESS);
                jobStatusMap.get(jobID).getData().getRecommendations().getData().setInqueue(recommendationsRequiredExperiments);
                jobStatusMap.get(jobID).getData().getRecommendations().setTotalCount(recommendationsRequiredExperiments.size());

            }
            ExecutorService executor = Executors.newFixedThreadPool(bulk_thread_pool_size);
            for (String name : recommendationsRequiredExperiments) {
                executor.submit(() -> {
                    URL url = null;
                    try {
                        url = new URL(String.format(KruizeDeploymentInfo.recommendations_url, name));
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                    HttpURLConnection connection = null;
                    try {
                        connection = (HttpURLConnection) url.openConnection();
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage());
                        throw new RuntimeException(e);
                    }
                    try {
                        connection.setRequestMethod("POST");
                    } catch (ProtocolException e) {
                        LOGGER.error(e.getMessage());
                        throw new RuntimeException(e);
                    }
                    // Get the response code from /helloworld
                    int statusCode = 0;
                    try {
                        jobStatusMap.get(jobID).getData().getRecommendations().getData().moveToProgress(name);
                        LOGGER.info(String.format(KruizeDeploymentInfo.recommendations_url, name));
                        statusCode = connection.getResponseCode();
                        LOGGER.info(String.format(KruizeDeploymentInfo.recommendations_url, name));
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage());
                        throw new RuntimeException(e);
                    }

                    if (statusCode == HttpURLConnection.HTTP_CREATED) {
                        jobStatusMap.get(jobID).getData().getRecommendations().getData().moveToCompleted(name);
                    } else {
                        jobStatusMap.get(jobID).getData().getRecommendations().getData().moveToFailed(name);
                    }
                    jobStatusMap.get(jobID).setProgress(jobStatusMap.get(jobID).getData().getRecommendations().getData().completionPercentage());
                    if (jobStatusMap.get(jobID).getProgress() == 100) {
                        jobStatusMap.get(jobID).setStatus(COMPLETED); // Mark the job as completed
                        jobStatusMap.get(jobID).setEndTime(Instant.now());
                        jobStatusMap.get(jobID).getData().getRecommendations().setCompletedCount(
                                jobStatusMap.get(jobID).getData().getRecommendations().getData().getCompleted().size()
                        );
                    }
                    // Close the connection
                    connection.disconnect();
                });
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();

        }
    }
}
