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
package com.autotune.experimentManager.utils;

import com.autotune.common.annotations.json.KruizeJSONExclusionStrategy;
import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.*;
import com.autotune.common.trials.ExperimentTrial;
import com.autotune.common.trials.TrialDetails;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.data.input.EMMetricInput;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

public class EMUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMUtil.class);

    /**
     * Utility to return a Unique ID
     */
    public static String createUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the NSD (NameSpace and Deployment) key by concating two strings
     * `namespace` and `deploymentName` with a colon `:`
     */
    public static String formatNSDKey(String namespace, String deploymentName) {
        return (new StringBuilder())
                .append(namespace)
                .append(":")
                .append(deploymentName)
                .toString();
    }

    public static int timeToSleep(int iteration, ThresholdIntervalType type) {
        int time = 0;
        if (type == null) {
            type = ThresholdIntervalType.LINEAR;
        }
        if (0 <= iteration) {
            int index = 0;
            if (type == ThresholdIntervalType.LINEAR) {
                time = EMConstants.StandardDefaults.BackOffThresholds.DEFAULT_LINEAR_BACKOFF_INTERVAL;
            } else if (type == ThresholdIntervalType.EXPONENTIAL) {
                index = iteration % EMConstants.StandardDefaults.BackOffThresholds.EXPONENTIAL_BACKOFF_INTERVALS.length;
                time = EMConstants.StandardDefaults.BackOffThresholds.EXPONENTIAL_BACKOFF_INTERVALS[index];
            }
        }
        return time;
    }

    // Parse the experiment trial settings and set the flags accordingly
    public static void setFlowFlagsBasedOnTrial(ExperimentTrial experimentTrial) {
        // Needs to be implemented
        // Currently setting all flags to be true
        for (Map.Entry<EMUtil.EMFlowFlags, Boolean> flagEntry : experimentTrial.getFlagsMap().entrySet()) {
            flagEntry.setValue(true);
        }
    }

    public static String replaceQueryVars(String query, ArrayList<Map<String, String>> queryVariablesList) {
        if (null != query) {
            if (null != queryVariablesList) {
                for (Map<String, String> variableMap : queryVariablesList) {
                    String key = variableMap.get(EMConstants.QueryMapConstants.NAME);
                    String value = variableMap.get(EMConstants.QueryMapConstants.VALUE);
                    query = query.replace(key, value);
                }
            }
        }
        return query;
    }

    public static String formatQueryByPodName(String rawQuery, String podName) {
        if (null == rawQuery || null == podName) {
            return rawQuery;
        }

        if (null != rawQuery && null != podName) {
            rawQuery = rawQuery.replace(EMConstants.QueryMapConstants.QueryVar.POD_NAME, podName);
        }
        return rawQuery;
    }

    public static String formatQueryByContainerName(String rawQuery, String containerName) {
        if (null == rawQuery || null == containerName) {
            return rawQuery;
        }

        if (null != rawQuery && null != containerName) {
            rawQuery = rawQuery.replace(EMConstants.QueryMapConstants.QueryVar.CONTAINER_NAME, containerName);
        }
        return rawQuery;
    }

    public static String getCurrentPodNameOfTrial(ExperimentTrial experimentTrial) {
        // Needs to be implemented
        KubernetesServices kubernetesServices = null;
        try {
            kubernetesServices = new KubernetesServicesImpl();
            // Need to call the platform specific client to get the pod name
            return kubernetesServices.getPodsBy("default", "app", "tfb-qrh-deployment").get(0).getMetadata().getName();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != kubernetesServices) {
                kubernetesServices.shutdownClient();
            }
        }
        return null;
    }

    public static ExperimentResultData getRealMetricsJSON(ExperimentTrial experimentTrial, boolean verbose, String triaLNumber) {
        HashMap<String, Metric> podMetricsMap = experimentTrial.getPodMetricsHashMap();
        HashMap<String, HashMap<String, Metric>> containersMap = experimentTrial.getContainerMetricsHashMap();
        DeploymentResultData deploymentResultData = new DeploymentResultData();
        deploymentResultData.setDeployment_name(experimentTrial.getResourceDetails().getDeploymentName());
        deploymentResultData.setNamespace(experimentTrial.getResourceDetails().getNamespace());
        List<PodResultData> podResultDataList = new ArrayList<>();
        for (Map.Entry<String, Metric> podMetricEntry : podMetricsMap.entrySet()) {
            Metric podMetric = podMetricEntry.getValue();
            if (null != podMetric.getMetricResult() && Float.MIN_VALUE != podMetric.getMetricResult().getAggregationInfoResult().getAvg()) {
                MetricAggregationInfoResults metricAggregationInfoResults = new MetricAggregationInfoResults();
                metricAggregationInfoResults.setAvg(podMetric.getMetricResult().getAggregationInfoResult().getAvg());
                HashMap<String, MetricAggregationInfoResults> generalInfoResultHashMap = new HashMap<>();
                generalInfoResultHashMap.put("general_info", metricAggregationInfoResults);
                PodResultData podResultData = new PodResultData();
                podResultData.setName(podMetric.getName());
                podResultData.setDatasource(podMetric.getDatasource());
                podResultData.setSummary_results(generalInfoResultHashMap);
                podResultDataList.add(podResultData);
            }
        }
        HashMap<String, ContainerData> containerDataMap = new HashMap<>();
        for (Map.Entry<String, HashMap<String, Metric>> containerMapEntry : containersMap.entrySet()) {
            ContainerData containerData = new ContainerData();
            containerData.setContainer_name(containerMapEntry.getKey());
            containerData.setContainer_image_name(null);

            HashMap<AnalyzerConstants.MetricName, Metric> containerMetrics = new HashMap<>();
            for (Map.Entry<String, Metric> containerMetricEntry : containerMapEntry.getValue().entrySet()) {
                Metric containerMetric = containerMetricEntry.getValue();
                Metric metric = new Metric();
                if (null != containerMetric.getMetricResult() && Float.MIN_VALUE != containerMetric.getMetricResult().getAggregationInfoResult().getAvg()) {
                    MetricResults metricResults = new MetricResults();
                    MetricAggregationInfoResults metricAggregationInfoResults = new MetricAggregationInfoResults();
                    metricAggregationInfoResults.setAvg(containerMetric.getMetricResult().getAggregationInfoResult().getAvg());
                    metricAggregationInfoResults.setFormat(containerMetric.getMetricResult().getAggregationInfoResult().getFormat());
                    metricResults.setAggregationInfoResult(metricAggregationInfoResults);
                    metricResults.setName(AnalyzerConstants.MetricName.valueOf(containerMetric.getName()).toString());
                    metric.setMetricResult(metricResults);
                    containerMetrics.put(AnalyzerConstants.MetricName.valueOf(containerMetric.getName()), metric);
                }
            }
            containerData.setMetrics(containerMetrics);
            containerDataMap.put(containerData.getContainer_name(), containerData);
        }
        deploymentResultData.setPod_metrics(podResultDataList);
        deploymentResultData.setContainerDataMap(containerDataMap);
        ExperimentResultData experimentResultData = new ExperimentResultData();
        experimentResultData.setExperiment_name(experimentTrial.getExperimentName());
        experimentResultData.setIntervalEndTime(new Timestamp(System.currentTimeMillis()));
        experimentResultData.setIntervalStartTime(new Timestamp(System.currentTimeMillis()));
        experimentResultData.setTrialNumber(triaLNumber);
        List<DeploymentResultData> deploymentResultDataList = new ArrayList<>();
        deploymentResultDataList.add(deploymentResultData);
        experimentResultData.setDeployments(deploymentResultDataList);

        return experimentResultData;

    }

    public static JSONObject getLiveMetricData(ExperimentTrial experimentTrial, String trialNum) {
        JSONArray podMetrics = new JSONArray();
        JSONArray containers = new JSONArray();
        JSONObject retJson = new JSONObject();
        HashMap<String, Metric> podMetricsMap = experimentTrial.getPodMetricsHashMap();
        for (Map.Entry<String, Metric> podMetricEntry : podMetricsMap.entrySet()) {
            Metric podMetric = podMetricEntry.getValue();
            LinkedHashMap<String, LinkedHashMap<Integer, com.autotune.common.data.metrics.MetricResults>> iterationDataMap = podMetric.getCycleDataMap().get(trialNum);
            try {
                if (null != iterationDataMap) {
                    LOGGER.debug(iterationDataMap.toString());
                    JSONObject iteration_results = new JSONObject((new Gson()).toJson(iterationDataMap));
                    LOGGER.debug("Iteration result - " + iteration_results.toString(2));
                    JSONObject podMetricJSON = new JSONObject();
                    podMetricJSON.put("name", podMetric.getName());
                    podMetricJSON.put("datasource", podMetric.getDatasource());
                    podMetricJSON.put("iteration_results", iteration_results);
                    podMetrics.put(podMetricJSON);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        HashMap<String, HashMap<String, Metric>> containersMap = experimentTrial.getContainerMetricsHashMap();
        for (Map.Entry<String, HashMap<String, Metric>> containerMapEntry : containersMap.entrySet()) {
            String containerName = containerMapEntry.getKey();
            JSONArray containerMetrics = new JSONArray();
            for (Map.Entry<String, Metric> containerMetricEntry : containerMapEntry.getValue().entrySet()) {
                Metric containerMetric = containerMetricEntry.getValue();
                LinkedHashMap<String, LinkedHashMap<Integer, com.autotune.common.data.metrics.MetricResults>> iterationDataMap = containerMetric.getCycleDataMap().get(trialNum);
                if (null != iterationDataMap) {
                    JSONObject iteration_results = new JSONObject((new Gson()).toJson(iterationDataMap));
                    JSONObject containerMetricJSON = new JSONObject();
                    containerMetricJSON.put("name", containerMetric.getName());
                    containerMetricJSON.put("datasource", containerMetric.getDatasource());
                    containerMetricJSON.put("iteration_results", iteration_results);
                    containerMetrics.put(containerMetricJSON);
                }
            }
            containers.put(new JSONObject().put(
                            "container_name", containerName
                    ).put(
                            "container_metrics", containerMetrics
                    )
            );
        }
        HashMap<String, TrialDetails> trialDetailsHashMap = experimentTrial.getTrialDetails();
        JSONArray deployments = new JSONArray();
        deployments.put(
                new JSONObject().
                        put("pod_metrics", podMetrics).
                        put("deployment_name", experimentTrial.getResourceDetails().getDeploymentName()).
                        put("namespace", experimentTrial.getResourceDetails().getNamespace()).
                        put("type", "training").
                        put("containers", containers)
        );
        retJson.put("experiment_name", experimentTrial.getExperimentName());
        retJson.put("experiment_id", experimentTrial.getExperimentId());
        retJson.put("deployment_name", experimentTrial.getResourceDetails().getDeploymentName());
        if (null != experimentTrial.getTrialInfo()) {
            retJson.put("info", new JSONObject().put("trial_info",
                    new JSONObject(
                            new GsonBuilder()
                                    .setExclusionStrategies(new KruizeJSONExclusionStrategy())
                                    .create()
                                    .toJson(experimentTrial.getTrialInfo())
                    )
            ));
        }
        retJson.put("deployments", deployments);
        return retJson;
    }

    public static double convertToMiB(double value, MemoryUnits memoryUnits) {
        if (value <= 0)
            return 0;
        if (memoryUnits == MemoryUnits.BYTES) {
            LOGGER.debug("Calcuclated val - " + value * KruizeConstants.ConvUnits.Memory.BYTES_TO_KIBIBYTES * KruizeConstants.ConvUnits.Memory.KIBIBYTES_TO_MEBIBYTES);
            return value * KruizeConstants.ConvUnits.Memory.BYTES_TO_KIBIBYTES * KruizeConstants.ConvUnits.Memory.KIBIBYTES_TO_MEBIBYTES;
        } else if (memoryUnits == MemoryUnits.KIBIBYTES) {
            return value * KruizeConstants.ConvUnits.Memory.KIBIBYTES_TO_MEBIBYTES;
        } else if (memoryUnits == MemoryUnits.MEBIBYTES) {
            return value;
        } else if (memoryUnits == MemoryUnits.GIBIBYTES) {
            return value * KruizeConstants.ConvUnits.Memory.MEBIBYTES_IN_GIBIBYTES;
        }
        return 0.0;
    }

    public static double convertToMB(double value, MemoryUnits memoryUnits) {
        if (value <= 0)
            return 0;
        if (memoryUnits == MemoryUnits.BYTES) {
            return value * KruizeConstants.ConvUnits.Memory.BYTES_TO_KILOBYTES * KruizeConstants.ConvUnits.Memory.KILOBYTES_IN_MEGABYTES;
        } else if (memoryUnits == MemoryUnits.KILOBYTES) {
            return value * KruizeConstants.ConvUnits.Memory.KILOBYTES_TO_MEGABYTES;
        } else if (memoryUnits == MemoryUnits.MEGABYTES) {
            return value;
        } else if (memoryUnits == MemoryUnits.GIGABYTES) {
            return value * KruizeConstants.ConvUnits.Memory.MEGABYTES_IN_GIGABYTES;
        }
        return 0.0;
    }

    public JSONObject generateMetricsMap(ExperimentTrialData etd) {
        ArrayList<EMMetricInput> emMetricInputs = etd.getConfig().getEmConfigObject().getDeployments().getTrainingDeployment().getMetrics();
        return null;
    }

    /**
     * EMExpStages is a collection of all stages that an experiment trial goes through to complete its lifecycle.
     * <p>
     * Each trial starts at INIT stage and ends at EXIT stage
     * <p>
     * Each subsequent stage between INIT and EXIT represent a particular portion of trial lifecycle as described below
     * <p>
     * CREATE_CONFIG:
     * Description: Stage in EM Trial Lifecycle where the configuration for a particular trial is being created
     * Predecessor Stage: INIT
     * Successor Stage: DEPLOY_CONFIG
     * Stage Type: Regular (Regular stages will be processed instantly by EM without any wait)
     * <p>
     * DEPLOY_CONFIG:
     * Description: Stage in EM Trial Lifecycle where the created configuration for a trial is deployed in the node
     * Predecessor Stage: CREATE_CONFIG
     * Successor Stage: INITIATE_TRAIL_RUN_PHASE
     * Stage Type: Regular (Regular stages will be processed instantly by EM without any wait)
     * <p>
     * Details of respective stages will be added once they are implemented
     */
    public enum EMExpStages {
        /**
         * Initial stage
         */
        INIT(0, 1, false, null),
        /**
         * CREATE_CONFIG:
         * Description: Stage in EM Trial Lifecycle where the configuration for a particular trial is being created
         * Predecessor Stage: INIT
         * Successor Stage: DEPLOY_CONFIG
         * Stage Type: Regular (Regular stages will be processed instantly by EM without any wait)
         */
        CREATE_CONFIG(1, 1, false, EMConstants.TransitionClasses.CREATE_CONFIG),
        /**
         * DEPLOY_CONFIG:
         * Description: Stage in EM Trial Lifecycle where the created configuration for a trial is deployed in the node
         * Predecessor Stage: CREATE_CONFIG
         * Successor Stage: INITIATE_TRAIL_RUN_PHASE
         * Stage Type: Regular (Regular stages will be processed instantly by EM without any wait)
         */
        DEPLOY_CONFIG(2, 1, false, EMConstants.TransitionClasses.DEPLOY_CONFIG),
        INITIATE_TRIAL_RUN_PHASE(3, 1, false, EMConstants.TransitionClasses.INITIATE_TRIAL_RUN_PHASE),
        INITIAL_LOAD_CHECK(3, 2, true, EMConstants.TransitionClasses.INITIAL_LOAD_CHECK),
        LOAD_CONSISTENCY_CHECK(3, 3, true, EMConstants.TransitionClasses.LOAD_CONSISTENCY_CHECK),
        INITIATE_METRICS_COLLECTION_PHASE(4, 1, false, EMConstants.TransitionClasses.INITIATE_METRICS_COLLECTION_PHASE),
        COLLECT_METRICS(4, 1, true, EMConstants.TransitionClasses.COLLECT_METRICS),
        CREATE_RESULT_DATA(5, 1, false, EMConstants.TransitionClasses.CREATE_RESULT_DATA),
        SEND_RESULT_DATA(5, 2, false, EMConstants.TransitionClasses.SEND_RESULT_DATA),
        CLEAN_OR_ROLLBACK_DEPLOYMENT(6, 1, true, EMConstants.TransitionClasses.CLEAN_OR_ROLLBACK_DEPLOYMENT),
        /**
         * Final or exiting stage
         */
        EXIT(7, 1, false, null);

        private static final EMExpStages values[] = values();
        private int stage;
        private int intermediate_stage;
        private boolean isScheduled;
        private String className;

        private EMExpStages(final int stage, final int intermediate_stage, final boolean isScheduled, final String className) {
            this.stage = stage;
            this.intermediate_stage = intermediate_stage;
            this.isScheduled = isScheduled;
            this.className = className;
        }

        public static EMExpStages get(int ordinal) {
            return values[ordinal];
        }

        public static int getSize() {
            return values().length;
        }

        /**
         * Returns stage id
         */
        public int getStage() {
            return stage;
        }

        /**
         * Returns intermediate stage id
         */
        public int getIntermediate_stage() {
            return intermediate_stage;
        }

        /**
         * Returns if the stage is a scheduled stage
         */
        public boolean isScheduled() {
            return isScheduled;
        }

        /**
         * Returns the name of the class which is responsible for processing a particular stage
         */
        public String getClassName() {
            return className;
        }

    }

    /**
     * Statuses are the informative responses to the user at a high level about the life cycle of a trial
     * <p>
     * If a trial is just created the status would be `CREATED`
     * If a trial is waiting for load or any other param to start or continue the status would be `WAIT`
     * If a trial is undergoing the metric collection or getting run to collect metrics the status would be `IN_PROGRESS`
     * If a trial has completed its iterations and sent metrics to USER and exited the status would be `COMPLETED`
     */
    public enum EMExpStatus {
        // Set at the time of creation
        CREATED,
        // Set if the trial is in conflict with on going trial
        QUEUED,
        // Set if the trial starts running
        IN_PROGRESS,
        // Set if deployment is needed
        DEPLOYING,
        // Set if deployment is successful
        DEPLOYMENT_SUCCESSFUL,
        // Set if deployment is failed
        DEPLOYMENT_FAILED,
        // Set if load check is needed
        WAITING_FOR_LOAD,
        // Set if load check is successful
        LOAD_CHECK_SUCCESSFUL,
        // Set if load check is failed
        LOAD_CHECK_FAILED,
        // Set if the collection of metrics is needed
        COLLECTING_METRICS,
        // Set if metric collection is successful
        METRIC_COLLECTION_SUCCESSFUL,
        // Set if metric collection failed
        METRIC_COLLECTION_FAILED,
        // Set if the trial reached end of its cycle
        TRIAL_COMPLETED,
        // Set if the results are posted successfully
        TRIAL_RESULT_SENT_SUCCESSFULLY,
        // Set if the results are not sent to analyser / trail creator
        TRIAL_RESULT_SEND_FAILED,
        COMPLETED,
        WAIT,
        FAILED;

    }

    public enum InterceptorDetectionStatus {
        DETECTED,
        NOT_DETECTED
    }

    public enum InterceptorAvailabilityStatus {
        AVAILABLE,
        NOT_AVAILABLE
    }

    public enum LoadAvailabilityStatus {
        LOAD_AVAILABLE,
        LOAD_NOT_AVAILABLE
    }

    public enum ThresholdIntervalType {
        EXPONENTIAL,
        LINEAR
    }

    public enum DeploymentReadinessStatus {
        READY,
        NOT_READY
    }

    public enum EMFlowFlags {
        NEEDS_DEPLOYMENT, // To check if deployment needed
        CHECK_LOAD, // To check if load detection to be done
        COLLECT_METRICS, // To check if metrics needed to be collected
    }

    public enum MemoryUnits {
        BYTES,
        KILOBYTES,
        KIBIBYTES,
        MEGABYTES,
        MEBIBYTES,
        GIGABYTES,
        GIBIBYTES,
        TERABYTES,
        TEBIBYTES
    }
}
