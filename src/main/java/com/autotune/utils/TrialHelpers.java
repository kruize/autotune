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
import java.util.ArrayList;
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
        Gson gsonObj = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        String gsonStr = gsonObj.toJson(experimentTrial);
        return "["+gsonStr+"]";
    }

    /**
     * Update the results obtained from EM to the corresponding AutotuneExperiment object for further processing
     *
     * @param trialNumber
     * @param autotuneExperiment
     * @param trialResultsJson
     * @throws InvalidValueException
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
        TrialDetails trialDetails = experimentTrial.getTrialDetails().get(tracker);
        if (null == trialDetails) {
            LOGGER.error("Invalid results JSON: Deployment tracker: " + tracker + " not found");
            throw new InvalidValueException("Invalid results JSON: Deployment tracker: " + tracker + " not found");
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
            Metric metric = trialDetails.getPodMetrics().get(metricName);
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
        LOGGER.info("*********************~~~~~~~~createDefaultExperimentTrial~~~~~~~~~~***************************************");
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

        ArrayList<String> trackers = new ArrayList<>();
        trackers.add(TRAINING);
        // trackers.add("production");
        DeploymentTracking deploymentTracking = new DeploymentTracking(trackers);
        DeploymentSettings deploymentSettings = new DeploymentSettings(deploymentPolicy,
                deploymentTracking);
        ExperimentSettings experimentSettings = new ExperimentSettings(trialSettings,
                deploymentSettings);

        HashMap<String, TrialDetails> deployments = new HashMap<>();
        // TODO: "runtimeOptions" needs to be interpreted at a runtime level
        // TODO: That means that once we detect a certain layer, it will be associated with a runtime
        // TODO: The runtime layer will know how to pass the options to container through kubernetes
        // This will be the default for Java
        // TODO: The -XX:MaxRAMPercentage will be based on actual observation of the size of the heap

        System.out.println(trialConfigJson);
        HashMap<String, PodContainer> containersHashMap = new HashMap<>();
        HashMap<String, PodContainer> podContainers = new HashMap<>();
        HashMap<String, Metric> podMetrics = new HashMap<>();

        // Create the metrics array
        // First iterate through the objective function variables
        SloInfo sloInfo = autotuneObject.getSloInfo();
        for (Metric metric : sloInfo.getFunctionVariables()) {
            podMetrics.put(metric.getName(), metric);
        }

        // Parse the incoming trialConfigJson for all the tunables
        JSONArray trialConfigArray = new JSONArray(trialConfigJson);
        for (Object trialConfigObject : trialConfigArray) {
            JSONObject trialConfig = (JSONObject) trialConfigObject;
            String tunableName = trialConfig.getString(TUNABLE_NAME);
            Tunable tunable = autotuneExperiment.getApplicationSearchSpace().getTunablesMap().get(tunableName);
            if (tunable == null) {
                System.out.println("ERROR: tunable is null for tunableName: " + tunableName);
            }
            PodContainer podContainer = null;
            if (containersHashMap != null
                    && !containersHashMap.isEmpty()
                    && containersHashMap.containsKey(tunable.getStackName())) {
                podContainer = containersHashMap.get(tunable.getStackName());
            } else {
                ApplicationServiceStack applicationServiceStack = autotuneExperiment.getApplicationDeployment().getApplicationServiceStackMap().get(tunable.getStackName());
                if (applicationServiceStack == null) {
                    // TODO: Can this be null?
                    System.out.println("ERROR: applicationServiceStack is null for stackName: " + tunable.getStackName());
                }
                podContainer = new PodContainer(applicationServiceStack.getStackName(),
                        applicationServiceStack.getContainerName());
                HashMap<String, ContainerConfigData> trialConfigMap = new HashMap<>();
                ContainerConfigData containerConfigData = new ContainerConfigData();
                trialConfigMap.put(String.valueOf(trialNumber), containerConfigData);
                containersHashMap.put(tunable.getStackName(), podContainer);
                podContainer.setTrialConfigs(trialConfigMap);
                podContainers.put(tunable.getStackName(), podContainer);
            }
            Class<Layer> classRef = AutotuneDeploymentInfo.getLayer(tunable.getLayerName());
            try {
                Object inst = classRef.getDeclaredConstructor().newInstance();
                Method method = classRef.getMethod("prepTunable", Tunable.class, JSONObject.class, ContainerConfigData.class);
                method.invoke(inst, tunable, trialConfig, podContainer.getTrialConfigs().get(String.valueOf(trialNumber)));
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            // Now add any queries associated with this tunable into the metrics array
            System.out.println("New Trial: tunable: " + tunableName + " Query Mon agent: " + AutotuneDeploymentInfo.getMonitoringAgent());
            String tunableQuery = tunable.getQueries().get(AutotuneDeploymentInfo.getMonitoringAgent());
            if (tunableQuery != null && !tunableQuery.isEmpty()) {
                HashMap<String, Metric> containerMetrics = podContainer.getContainerMetrics();
                Metric queryMetric = new Metric(tunable.getName(),
                        tunableQuery,
                        AutotuneDeploymentInfo.getMonitoringAgent(),
                        tunable.getValueType());

                if (containerMetrics == null) {
                    containerMetrics = new HashMap<>();
                    podContainer.setContainerMetrics(containerMetrics);
                }
                containerMetrics.put(queryMetric.getName(), queryMetric);
            } else {
                System.out.println("New Trial: tunable: " + tunableName + " No container metrics");
            }
        }

        for (String tracker : trackers) {
            TrialDetails deployment = new TrialDetails(tracker,
                    autotuneExperiment.getDeploymentName(),
                    autotuneExperiment.getApplicationDeployment().getNamespace(),
                    "",
                    TRIAL_RUNNING,
                    TRIAL_RUNNING,
                    TRIAL_RUNNING,
                    podMetrics,
                    podContainers
            );
            deployment.setStartTime(Timestamp.from(Instant.now()));
            deployments.put(tracker, deployment);
        }
        HashMap<String, TrialDetails> trailDetailsMap = new HashMap<>();
        for (String tracker : trackers) {
            TrialDetails trialDetails = new TrialDetails(ROLLING_UPDATE, autotuneExperiment.getDeploymentName(),
                    autotuneExperiment.getApplicationDeployment().getNamespace(), podMetrics, podContainers);
            trailDetailsMap.put(tracker, trialDetails);
        }
        ExperimentTrial experimentTrial = new ExperimentTrial(appSearchSpace.getExperimentName(),
                appSearchSpace.getExperimentId(),
                autotuneObject.getNamespace(),
                trialInfo,
                datasourceInfo,
                experimentSettings,
                deployments
        );

        return experimentTrial;
    }
}
