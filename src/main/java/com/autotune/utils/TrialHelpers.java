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
import com.autotune.analyzer.RunExperiment;
import com.autotune.analyzer.application.ApplicationSearchSpace;
import com.autotune.analyzer.application.ApplicationServiceStack;
import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.deployment.AutotuneDeploymentInfo;
import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.common.data.experiments.*;
import com.autotune.analyzer.k8sObjects.AutotuneObject;
import com.autotune.analyzer.k8sObjects.Metric;
import com.autotune.analyzer.k8sObjects.SloInfo;
import com.autotune.analyzer.layer.Layer;
import com.autotune.common.data.metrics.EMMetricResult;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import static com.autotune.analyzer.deployment.AutotuneDeployment.autotuneObjectMap;
import static com.autotune.utils.AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME;
import static com.autotune.utils.ServerContext.LIST_EXPERIMENTS_END_POINT;

public class TrialHelpers
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TrialHelpers.class);

    /**
     * Convert the given ExperimentTrial object to JSON. This will be sent to the EM module
     *
     * @param experimentTrial object that holds the trial config from Optuna
     * @return Equivalent JSONObject that is accepted by EM
     */
    public static JSONObject experimentTrialToJSON(ExperimentTrial experimentTrial) {
        // Top level experimentTrialJSON Object
        JSONObject experimentTrialJSON = new JSONObject();
        experimentTrialJSON.put("experiment_id", experimentTrial.getExperimentId());
        experimentTrialJSON.put("namespace", experimentTrial.getNamespace());
        experimentTrialJSON.put("experiment_name", experimentTrial.getExperimentName());

        /*
         * Info object
         * experimentTrialJSON -> info
         */
        JSONObject trialInfoValues = new JSONObject();
        trialInfoValues.put("trial_id", experimentTrial.getTrialInfo().getTrialId());
        trialInfoValues.put("trial_num", experimentTrial.getTrialInfo().getTrialNum());
        trialInfoValues.put("trial_result_url", experimentTrial.getTrialInfo().getTrialResultURL());

        JSONObject datasourceInfo = new JSONObject();
        datasourceInfo.put("name", experimentTrial.getDatasourceInfo().getName());
        datasourceInfo.put("url", experimentTrial.getDatasourceInfo().getUrl().toString());

        JSONArray datasourceArray = new JSONArray();
        datasourceArray.put(datasourceInfo);

        JSONObject infoValues = new JSONObject();
        infoValues.put("trial_info", trialInfoValues);
        infoValues.put("datasource_info", datasourceArray);

        experimentTrialJSON.put("info", infoValues);
        /*
         * trialSettings object
         * experimentTrialJSON -> settings -> trail_settings
         */
        JSONObject trialSettingsValues = new JSONObject();
        trialSettingsValues.put("iterations", experimentTrial.getExperimentSettings().getTrialSettings().getTrialIterations());
        trialSettingsValues.put("warmup_duration", experimentTrial.getExperimentSettings().getTrialSettings().getTrialWarmupDuration());
        trialSettingsValues.put("warmup_cycles", experimentTrial.getExperimentSettings().getTrialSettings().getTrialWarmupCycles());
        trialSettingsValues.put("measurement_duration", experimentTrial.getExperimentSettings().getTrialSettings().getTrialMeasurementDuration());
        trialSettingsValues.put("measurement_cycles", experimentTrial.getExperimentSettings().getTrialSettings().getTrialMeasurementCycles());

        /*
         * deploymentPolicy object
         * experimentTrialJSON -> settings -> deployment_settings -> deployment_policy
         */
        JSONObject deploymentPolicyValues = new JSONObject();
        deploymentPolicyValues.put("type", experimentTrial.getExperimentSettings().getDeploymentSettings().getDeploymentPolicy().getDeploymentType());

        /*
         * deployment_tracking object
         * experimentTrialJSON -> settings -> deployment_settings -> deployment_tracking
         */
        JSONArray trackersArray = new JSONArray();
        for (String trackerObjs : experimentTrial.getExperimentSettings().getDeploymentSettings().getDeploymentTracking().getTrackers()) {
            trackersArray.put(trackerObjs);
        }
        JSONObject trackers = new JSONObject();
        trackers.put("trackers", trackersArray);

        JSONObject deploymentSettingsValues = new JSONObject();
        deploymentSettingsValues.put("deployment_policy", deploymentPolicyValues);
        deploymentSettingsValues.put("deployment_tracking", trackers);

        JSONObject experimentSettingsValues = new JSONObject();
        experimentSettingsValues.put("trial_settings", trialSettingsValues);
        experimentSettingsValues.put("deployment_settings", deploymentSettingsValues);
        // Update experimentTrialJSON
        experimentTrialJSON.put("settings", experimentSettingsValues);

        /*
         * deployments object section
         */
        JSONArray deploymentsArrayObjs = new JSONArray();
        for (String deploymentType : experimentTrial.getTrialDetails().keySet()) {
            TrialDetails deployment = experimentTrial.getTrialDetails().get(deploymentType);
            /*
             * com/autotune/analyzer/experiments/Container.java object
             * experimentTrialJSON -> deployments -> container
             */
            JSONArray containersJsonArray = new JSONArray();
            for (String stackName : deployment.getPodContainers().keySet()) {
                PodContainer podContainer = deployment.getPodContainers().get(stackName);
                /*
                 * resources object
                 * experimentTrialJSON -> deployments -> container -> config -> resources
                 */
                JSONObject requestsValues = new JSONObject();
                /* CPU Value should only be */
                requestsValues.put("cpu", podContainer.getRequests().getCpu());
                requestsValues.put("memory", podContainer.getRequests().getMemory());
                JSONObject resourcesValues = new JSONObject();
                resourcesValues.put("requests", requestsValues);
                resourcesValues.put("limits", requestsValues);
                JSONObject resources = new JSONObject();
                resources.put("resources", resourcesValues);

                JSONObject containerJson = new JSONObject();
                containerJson.put("container", resources);

                JSONObject tspec = new JSONObject();
                tspec.put("spec", containerJson);

                JSONObject template = new JSONObject();
                template.put("template", tspec);

                JSONObject resObject = new JSONObject();
                resObject.put("name", "update requests and limits");
                resObject.put("spec", template);

                /*
                 * env object
                 * experimentTrialJSON -> deployments -> config -> env
                 */
                JSONObject envValues = new JSONObject();
                envValues.put("JAVA_OPTIONS", podContainer.getRuntimeOptions());
                envValues.put("JDK_JAVA_OPTIONS", podContainer.getRuntimeOptions());

                JSONObject env = new JSONObject();
                env.put("env", envValues);

                JSONObject containere = new JSONObject();
                containere.put("container", env);

                JSONObject tspece = new JSONObject();
                tspece.put("spec", containere);

                JSONObject templatee = new JSONObject();
                templatee.put("template", tspece);

                JSONObject envObject = new JSONObject();
                envObject.put("name", "update env");
                envObject.put("spec", templatee);

                JSONArray configArrayObjects = new JSONArray();
                configArrayObjects.put(resObject);
                configArrayObjects.put(envObject);

                /*
                 * metrics object
                 * experimentTrialJSON -> deployments -> metrics
                 */
                JSONArray containerMetricArrayObjects = new JSONArray();
                for (String metricName : podContainer.getContainerMetrics().keySet()) {
                    Metric metricObjects = podContainer.getContainerMetrics().get(metricName);
                    JSONObject obj = new JSONObject();
                    obj.put("name", metricObjects.getName());
                    obj.put("query", metricObjects.getQuery());
                    obj.put("datasource", metricObjects.getDatasource());
                    containerMetricArrayObjects.put(obj);
                }

                /*
                 * config object
                 * experimentTrialJSON -> deployments -> config
                 */
                JSONObject configObject = new JSONObject();
                configObject.put("image_name", podContainer.getStackName());
                configObject.put("container_name", podContainer.getContainerName());
                configObject.put("config", configArrayObjects);
                configObject.put("container_metrics", containerMetricArrayObjects);

                containersJsonArray.put(configObject);
            }

            /*
             * metrics object
             * experimentTrialJSON -> deployments -> metrics
             */
            JSONArray podMetricArrayObjects = new JSONArray();
            for (String metricName : deployment.getPodMetrics().keySet()) {
                Metric metricObjects = deployment.getPodMetrics().get(metricName);
                JSONObject obj = new JSONObject();
                obj.put("name", metricObjects.getName());
                obj.put("query", metricObjects.getQuery());
                obj.put("datasource", metricObjects.getDatasource());
                podMetricArrayObjects.put(obj);
            }

            JSONObject deploymentObject = new JSONObject();
            deploymentObject.put("type", deployment.getDeploymentType());
            deploymentObject.put("deployment_name", deployment.getDeploymentName());
            deploymentObject.put("namespace", deployment.getDeploymentNameSpace());
            deploymentObject.put("pod_metrics", podMetricArrayObjects);
            deploymentObject.put("containers", containersJsonArray);

            // Add this deployment tracker object to the deployment object array
            deploymentsArrayObjs.put(deploymentObject);
        }
        experimentTrialJSON.put("deployments", deploymentsArrayObjs);

        return experimentTrialJSON;
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
        String tracker = "training";
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
        JSONArray podMetricsArray = deployment.getJSONArray("pod_metrics");
        if (null == podMetricsArray) {
            LOGGER.error("Invalid results JSON: pod_metrics Array not found");
            throw new InvalidValueException("Invalid results JSON: pod_metrics Array not found");
        }
        for (Object metricPodObject : podMetricsArray) {
            JSONObject podMetric = (JSONObject) metricPodObject;
            EMMetricResult emMetricResult = new EMMetricResult(podMetric.getJSONObject("summary_results"));
            String metricName = podMetric.getString("name");
            Metric metric = trialDetails.getPodMetrics().get(metricName);
            metric.setEmMetricResult(emMetricResult);
        }
        LOGGER.info("Successfully updated results for trialNum: " + trialNumber);
    }

    /**
     * Create a ExperimentTrial object that holds the trial config to be deployed to the k8s cluster
     *
     * @param trialNumber passed in from Optuna
     * @param autotuneExperiment from the user
     * @param trialConfigJson from Optuna
     * @return ExperimentTrial object
     */
    public static ExperimentTrial createDefaultExperimentTrial(int trialNumber,
                                                               AutotuneExperiment autotuneExperiment,
                                                               String trialConfigJson) throws MalformedURLException {

        ApplicationSearchSpace appSearchSpace = autotuneExperiment.getApplicationSearchSpace();
        AutotuneObject autotuneObject = autotuneObjectMap.get(autotuneExperiment.getExperimentName());

        TrialSettings trialSettings = new TrialSettings("3",
                "1min",
                "3",
                "1min",
                "3"
        );
        DeploymentPolicy deploymentPolicy = new DeploymentPolicy("rollingUpdate");

        StringBuilder trialResultUrl = new StringBuilder(LIST_EXPERIMENTS_END_POINT)
                .append("?")
                .append(EXPERIMENT_NAME)
                .append("=")
                .append(autotuneExperiment.getExperimentName());

        TrialInfo trialInfo = new TrialInfo("",
                trialNumber,
                trialResultUrl.toString());

        DatasourceInfo datasourceInfo = new DatasourceInfo(AutotuneDeploymentInfo.getMonitoringAgent(),
                new URL(AutotuneDeploymentInfo.getMonitoringAgentEndpoint()));

        ArrayList<String> trackers = new ArrayList<>();
        trackers.add("training");
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
            String tunableName = trialConfig.getString("tunable_name");
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
                containersHashMap.put(tunable.getStackName(), podContainer);
                podContainers.put(tunable.getStackName(), podContainer);
            }
            Class<Layer> classRef = AutotuneDeploymentInfo.getLayer(tunable.getLayerName());
            try {
                Object inst = classRef.getDeclaredConstructor().newInstance();
                Method method = classRef.getMethod("prepTunable", Tunable.class, JSONObject.class, PodContainer.class);
                method.invoke(inst, tunable, trialConfig, podContainer);
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

        for (String tracker : trackers ) {
            TrialDetails deployment = new TrialDetails(tracker,
                    autotuneExperiment.getDeploymentName(),
                    autotuneExperiment.getApplicationDeployment().getNamespace(),
                    "",
                    "",
                    "",
                    "",
                    podMetrics,
                    podContainers
            );
            deployments.put(tracker, deployment);
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
