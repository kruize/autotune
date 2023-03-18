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
package com.autotune.utils;

import com.autotune.analyzer.experiment.KruizeExperiment;
import com.autotune.analyzer.application.ApplicationSearchSpace;
import com.autotune.analyzer.application.ApplicationServiceStack;
import com.autotune.analyzer.application.Tunable;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.kruizeLayer.layers.Layer;
import com.autotune.common.annotations.json.KruizeJSONExclusionStrategy;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.trials.*;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.common.data.metrics.Metric;
import com.autotune.analyzer.kruizeObject.SloInfo;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.performanceProfiles.PerformanceProfilesDeployment;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;

import static com.autotune.operator.KruizeOperator.autotuneObjectMap;
import static com.autotune.experimentManager.utils.EMConstants.DeploymentStrategies.ROLLING_UPDATE;
import static com.autotune.analyzer.utils.AnalyzerConstants.AutotuneConfigConstants.TUNABLE_NAME;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.TRAINING;
import static com.autotune.utils.KruizeConstants.JSONKeys.*;
import static com.autotune.utils.ServerContext.LIST_EXPERIMENTS_END_POINT;

public class TrialHelpers {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrialHelpers.class);

    /**
     * Convert the given ExperimentTrial object to JSON. This will be sent to the EM module
     *
     * @param experimentTrial object that holds the trial config from HPO
     * @return Equivalent JSONObject that is accepted by EM
     */
    public static String experimentTrialToJSON(ExperimentTrial experimentTrial) {
        Gson gsonObj = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .setExclusionStrategies(new KruizeJSONExclusionStrategy())
                .create();
        String gsonStr = gsonObj.toJson(experimentTrial);
        return "[" + gsonStr + "]";
    }

    /**
     * Update the results obtained from EM to the corresponding KruizeExperiment object for further processing
     */
    public static void updateExperimentTrial(String trialNumber,
                                             KruizeExperiment kruizeExperiment,
                                             JSONObject trialResultsJson) throws InvalidValueException, IncompatibleInputJSONException {
        String tracker = TRAINING;
        ExperimentTrial experimentTrial = kruizeExperiment.getExperimentTrials().get(Integer.parseInt(trialNumber));
        if (null == experimentTrial) {
            LOGGER.error("Invalid results JSON: Invalid trialNumber: " + trialNumber);
            throw new InvalidValueException("Invalid results JSON: Invalid trialNumber: " + trialNumber);
        }
        HashMap<String, TrialDetails> trialDetails = experimentTrial.getTrialDetails();
        if (null == trialDetails) {
            LOGGER.error("Invalid results JSON: trialDetails: not found");
            throw new InvalidValueException("Invalid results JSON: trialDetails not found");
        }

        JSONArray deploymentsArray = trialResultsJson.getJSONArray("deployments");
        if (null == deploymentsArray) {
            LOGGER.error("Invalid results JSON: Deployments Array not found");
            throw new InvalidValueException("Invalid results JSON: Deployments Array not found");
        }
        JSONObject deployment = deploymentsArray.getJSONObject(0);
        JSONArray podMetricsArray = deployment.getJSONArray(POD_METRICS);
        if (null == podMetricsArray) {
            LOGGER.error("Invalid results JSON: pod_metrics Array not found");
            throw new InvalidValueException("Invalid results JSON: pod_metrics Array not found");
        }
        for (Object metricPodObject : podMetricsArray) {
            JSONObject podMetric = (JSONObject) metricPodObject;
            MetricResults metricResults = new MetricResults(podMetric.getJSONObject(SUMMARY_RESULTS));
            String metricName = podMetric.getString(NAME);
            Metric metric = experimentTrial.getPodMetricsHashMap().get(metricName);
            metric.setEmMetricResult(metricResults);
        }
        LOGGER.info("Successfully updated results for trialNum: " + trialNumber);
    }

    /**
     * Create a ExperimentTrial object that holds the trial config to be deployed to the k8s cluster
     *
     * @param trialNumber        passed in from HPO
     * @param kruizeExperiment from the user
     * @param trialConfigJson    from HPO
     * @return ExperimentTrial object
     */
    public static ExperimentTrial createDefaultExperimentTrial(int trialNumber,
                                                               KruizeExperiment kruizeExperiment,
                                                               String trialConfigJson) throws MalformedURLException {
        ApplicationSearchSpace appSearchSpace = kruizeExperiment.getApplicationSearchSpace();
        KruizeObject kruizeObject = autotuneObjectMap.get(kruizeExperiment.getExperimentName());

        TrialSettings trialSettings = new TrialSettings("1",
                "1min",
                "1",
                "1min",
                "1"
        );
        DeploymentPolicy deploymentPolicy = new DeploymentPolicy(ROLLING_UPDATE);

        StringBuilder trialResultUrl = new StringBuilder(LIST_EXPERIMENTS_END_POINT)
                .append("?")
                .append(EXPERIMENT_NAME)
                .append(EQUALS)
                .append(kruizeExperiment.getExperimentName());

        TrialInfo trialInfo = new TrialInfo("",
                trialNumber,
                trialResultUrl.toString());

        DataSourceInfo datasourceInfo = new DataSourceInfo(KruizeDeploymentInfo.getMonitoringAgent(),
                new URL(KruizeDeploymentInfo.getMonitoringAgentEndpoint()));
        HashMap<String, DataSourceInfo> datasourceInfoHashMap = new HashMap<>();
        datasourceInfoHashMap.put(KruizeDeploymentInfo.getMonitoringAgent(), datasourceInfo);  //Change key value as per YAML input
        DeploymentTracking deploymentTracking = new DeploymentTracking();
        DeploymentSettings deploymentSettings = new DeploymentSettings(deploymentPolicy,
                deploymentTracking);

        boolean do_experiments = true;
        if (kruizeObject.getMode().equals("monitor")) {
            do_experiments = false;
        }
        boolean do_monitoring = true;
        if (kruizeObject.getTarget_cluster().equals("remote")) {
            do_monitoring = false;
        }
        /**
         * Currently wait_for_load will be set to false if we have to monitor a remote cluster
         * TODO: In the future this might need its own flag
         */
        boolean wait_for_load = true;
        if (kruizeObject.getMode().equals("monitor") && kruizeObject.getTarget_cluster().equals("remote")) {
            wait_for_load = false;
        }
        ExperimentSettings experimentSettings = new ExperimentSettings(trialSettings,
                deploymentSettings, do_experiments, do_monitoring, wait_for_load);

        // TODO: "runtimeOptions" needs to be interpreted at a runtime level
        // TODO: That means that once we detect a certain layer, it will be associated with a runtime
        // TODO: The runtime layer will know how to pass the options to container through kubernetes
        // This will be the default for Java
        // TODO: The -XX:MaxRAMPercentage will be based on actual observation of the size of the heap

        LOGGER.info(trialConfigJson);

        String experimentName = appSearchSpace.getExperimentName();
        ResourceDetails resourceDetails = new ResourceDetails(kruizeObject.getNamespace(), kruizeExperiment.getDeploymentName());
        String experimentID = appSearchSpace.getExperimentId();
        HashMap<String, TrialDetails> trialsMap = new HashMap<>();
        ContainerConfigData configData = new ContainerConfigData();
        HashMap<String, Metric> podMetricsHashMap = new HashMap<>();
        HashMap<String, HashMap<String, Metric>> containerMetricsHashMap = new HashMap<>();
        PerformanceProfile performanceProfile = PerformanceProfilesDeployment.performanceProfilesMap
                .get(kruizeObject.getPerformanceProfile());
        SloInfo sloInfo = performanceProfile.getSloInfo();
        for (Metric metric : sloInfo.getFunctionVariables()) {
            podMetricsHashMap.put(metric.getName(), metric);
        }
        JSONArray trialConfigArray = new JSONArray(trialConfigJson);
        for (Object trialConfigObject : trialConfigArray) {
            JSONObject trialConfig = (JSONObject) trialConfigObject;
            String tunableName = trialConfig.getString(TUNABLE_NAME);
            Tunable tunable = kruizeExperiment.getApplicationSearchSpace().getTunablesMap().get(tunableName);
            if (tunable == null) {
                LOGGER.error("ERROR: tunable is null for tunableName: " + tunableName);
            }
            ApplicationServiceStack applicationServiceStack = kruizeExperiment.getApplicationDeployment().getApplicationServiceStackMap().get(tunable.getStackName());
            String tunableQuery = tunable.getQueries().get(KruizeDeploymentInfo.getMonitoringAgent());
            Class<Layer> classRef = KruizeDeploymentInfo.getLayer(tunable.getLayerName());
            try {
                Object inst = classRef.getDeclaredConstructor().newInstance();
                Method method = classRef.getMethod("prepTunable", Tunable.class, JSONObject.class, ContainerConfigData.class);
                method.invoke(inst, tunable, trialConfig, configData);
                configData.setContainerName(applicationServiceStack.getContainerName());
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (tunableQuery != null && !tunableQuery.isEmpty()) {
                Metric queryMetric = new Metric(tunable.getName(),
                        tunableQuery,
                        KruizeDeploymentInfo.getMonitoringAgent(),
                        tunable.getValueType());
                if (containerMetricsHashMap != null
                        && !containerMetricsHashMap.isEmpty()
                        && containerMetricsHashMap.containsKey(applicationServiceStack.getContainerName())) {
                    containerMetricsHashMap.get(applicationServiceStack.getContainerName())
                            .put(queryMetric.getName(), queryMetric);
                } else {
                    HashMap<String, Metric> localMetricMap = new HashMap<>();
                    localMetricMap.put(queryMetric.getName(), queryMetric);
                    containerMetricsHashMap.put(applicationServiceStack.getContainerName(), localMetricMap);
                }
            } else {
                LOGGER.error("New Trial: tunable: " + tunableName + " No container metrics");
            }
        }
        TrialDetails trialDetails = new TrialDetails(String.valueOf(trialNumber), configData);
        trialDetails.setStartTime(Timestamp.from(Instant.now()));
        trialsMap.put(String.valueOf(trialNumber), trialDetails);
        String mode = null;
        ExperimentTrial experimentTrial = new ExperimentTrial(experimentName,
                mode, resourceDetails, experimentID,
                podMetricsHashMap, containerMetricsHashMap, trialInfo,
                datasourceInfoHashMap,
                experimentSettings,
                trialsMap
        );
        experimentTrial.setTrialResultURL(ServerContext.UPDATE_RESULTS_END_POINT);
        return experimentTrial;
    }
}
