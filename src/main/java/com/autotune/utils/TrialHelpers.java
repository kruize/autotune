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

import com.autotune.analyzer.AutotuneExperiment;
import com.autotune.analyzer.application.ApplicationSearchSpace;
import com.autotune.analyzer.application.ApplicationServiceStack;
import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.deployment.AutotuneDeploymentInfo;
import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.layer.Layer;
import com.autotune.common.annotations.json.AutotuneJSONExclusionStrategy;
import com.autotune.common.data.metrics.EMMetricResult;
import com.autotune.common.experiments.*;
import com.autotune.common.k8sObjects.AutotuneObject;
import com.autotune.common.k8sObjects.Metric;
import com.autotune.common.k8sObjects.SloInfo;
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

import static com.autotune.analyzer.deployment.AutotuneDeployment.autotuneObjectMap;
import static com.autotune.experimentManager.utils.EMConstants.DeploymentStrategies.ROLLING_UPDATE;
import static com.autotune.utils.AnalyzerConstants.AutotuneConfigConstants.TUNABLE_NAME;
import static com.autotune.utils.AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME;
import static com.autotune.utils.AnalyzerConstants.ServiceConstants.TRAINING;
import static com.autotune.utils.AutotuneConstants.JSONKeys.*;
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
                .setExclusionStrategies(new AutotuneJSONExclusionStrategy())
                .create();
        String gsonStr = gsonObj.toJson(experimentTrial);
        return "[" + gsonStr + "]";
    }

    /**
     * Update the results obtained from EM to the corresponding AutotuneExperiment object for further processing
     */
    public static void updateExperimentTrial(int trialNumber,
                                             AutotuneExperiment autotuneExperiment,
                                             JSONObject trialResultsJson) throws InvalidValueException, IncompatibleInputJSONException {
        String tracker = TRAINING;
        ExperimentTrial experimentTrial = autotuneExperiment.getExperimentTrials().get(trialNumber);
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
            EMMetricResult emMetricResult = new EMMetricResult(podMetric.getJSONObject(SUMMARY_RESULTS));
            String metricName = podMetric.getString(NAME);
            Metric metric = experimentTrial.getPodMetricsHashMap().get(metricName);
            metric.setEmMetricResult(emMetricResult);
        }
        LOGGER.info("Successfully updated results for trialNum: " + trialNumber);
    }

    /**
     * Create a ExperimentTrial object that holds the trial config to be deployed to the k8s cluster
     *
     * @param trialNumber        passed in from HPO
     * @param autotuneExperiment from the user
     * @param trialConfigJson    from HPO
     * @return ExperimentTrial object
     */
    public static ExperimentTrial createDefaultExperimentTrial(int trialNumber,
                                                               AutotuneExperiment autotuneExperiment,
                                                               String trialConfigJson) throws MalformedURLException {
        ApplicationSearchSpace appSearchSpace = autotuneExperiment.getApplicationSearchSpace();
        AutotuneObject autotuneObject = autotuneObjectMap.get(autotuneExperiment.getExperimentName());

        TrialSettings trialSettings = new TrialSettings("1",
                "5sec",
                "1",
                "10sec",
                "1"
        );
        DeploymentPolicy deploymentPolicy = new DeploymentPolicy(ROLLING_UPDATE);

        StringBuilder trialResultUrl = new StringBuilder(LIST_EXPERIMENTS_END_POINT)
                .append("?")
                .append(EXPERIMENT_NAME)
                .append(EQUALS)
                .append(autotuneExperiment.getExperimentName());

        TrialInfo trialInfo = new TrialInfo("",
                trialNumber,
                trialResultUrl.toString());

        DatasourceInfo datasourceInfo = new DatasourceInfo(AutotuneDeploymentInfo.getMonitoringAgent(),
                new URL(AutotuneDeploymentInfo.getMonitoringAgentEndpoint()));
        HashMap<String, DatasourceInfo> datasourceInfoHashMap = new HashMap<>();
        datasourceInfoHashMap.put(AutotuneDeploymentInfo.getMonitoringAgent(), datasourceInfo);  //Change key value as per YAML input
        DeploymentTracking deploymentTracking = new DeploymentTracking();
        DeploymentSettings deploymentSettings = new DeploymentSettings(deploymentPolicy,
                deploymentTracking);
        ExperimentSettings experimentSettings = new ExperimentSettings(trialSettings,
                deploymentSettings, true, true, true);

        // TODO: "runtimeOptions" needs to be interpreted at a runtime level
        // TODO: That means that once we detect a certain layer, it will be associated with a runtime
        // TODO: The runtime layer will know how to pass the options to container through kubernetes
        // This will be the default for Java
        // TODO: The -XX:MaxRAMPercentage will be based on actual observation of the size of the heap

        System.out.println(trialConfigJson);


        String experimentName = appSearchSpace.getExperimentName();
        ResourceDetails resourceDetails = new ResourceDetails(autotuneObject.getNamespace(), autotuneExperiment.getDeploymentName());
        String experimentID = appSearchSpace.getExperimentId();
        HashMap<String, TrialDetails> trialsMap = new HashMap<>();
        ContainerConfigData configData = new ContainerConfigData();
        HashMap<String, Metric> podMetricsHashMap = new HashMap<>();
        HashMap<String, HashMap<String, Metric>> containerMetricsHashMap = new HashMap<>();
        SloInfo sloInfo = autotuneObject.getSloInfo();
        for (Metric metric : sloInfo.getFunctionVariables()) {
            podMetricsHashMap.put(metric.getName(), metric);
        }
        JSONArray trialConfigArray = new JSONArray(trialConfigJson);
        for (Object trialConfigObject : trialConfigArray) {
            JSONObject trialConfig = (JSONObject) trialConfigObject;
            String tunableName = trialConfig.getString(TUNABLE_NAME);
            Tunable tunable = autotuneExperiment.getApplicationSearchSpace().getTunablesMap().get(tunableName);
            if (tunable == null) {
                System.out.println("ERROR: tunable is null for tunableName: " + tunableName);
            }
            ApplicationServiceStack applicationServiceStack = autotuneExperiment.getApplicationDeployment().getApplicationServiceStackMap().get(tunable.getStackName());
            String tunableQuery = tunable.getQueries().get(AutotuneDeploymentInfo.getMonitoringAgent());
            Class<Layer> classRef = AutotuneDeploymentInfo.getLayer(tunable.getLayerName());
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
                        AutotuneDeploymentInfo.getMonitoringAgent(),
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
                System.out.println("New Trial: tunable: " + tunableName + " No container metrics");
            }
        }
        TrialDetails trialDetails = new TrialDetails(String.valueOf(trialNumber), configData);
        trialDetails.setStartTime(Timestamp.from(Instant.now()));
        trialsMap.put(String.valueOf(trialNumber), trialDetails);
        String mode = null;
        String environment = null;
        ExperimentTrial experimentTrial = new ExperimentTrial(experimentName,
                mode, environment, resourceDetails, experimentID,
                podMetricsHashMap, containerMetricsHashMap, trialInfo,
                datasourceInfoHashMap,
                experimentSettings,
                trialsMap
        );
        experimentTrial.setTrialResultURL(trialInfo.getTrialResultURL());
        return experimentTrial;
    }
}
