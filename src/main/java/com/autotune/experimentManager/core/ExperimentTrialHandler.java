/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.experimentManager.core;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.PodContainer;
import com.autotune.common.experiments.TrialDetails;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import com.autotune.experimentManager.core.interceptor.EMLoadInterceptor;
import com.autotune.experimentManager.data.EMMapper;

import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMUtil;
import com.autotune.utils.HttpUtils;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Service class helper used to control and execute Lifecycle of Experiments using trial numbers.
 */
public class ExperimentTrialHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentTrialHandler.class);
    private ExperimentTrial experimentTrial;

    public ExperimentTrialHandler(ExperimentTrial experimentTrial) {
        this.experimentTrial = experimentTrial;
        String experimentName = experimentTrial.getExperimentName();
        if (null != experimentTrial.getTrialInfo() && experimentTrial.getTrialInfo().getTrialNum() != -1) {
            String trialNum = String.valueOf(experimentTrial.getTrialInfo().getTrialNum());
            ConcurrentHashMap<String, HashMap<String, ExperimentTrial>> expTrialMap = EMMapper.getInstance().getExpTrialMap();
            if (expTrialMap.containsKey(experimentName)) {
                HashMap<String, ExperimentTrial> expMap = expTrialMap.get(experimentName);
                if (!expMap.containsKey(trialNum)) {
                    expMap.put(trialNum, experimentTrial);
                }
            } else {
                HashMap<String, ExperimentTrial> trialMap = new HashMap<String, ExperimentTrial>();
                trialMap.put(trialNum, experimentTrial);
                expTrialMap.put(experimentName, trialMap);
            }
        }
    }

    public static JSONObject getDummyMetricJson(ExperimentTrial experimentTrial) {
        JSONObject percentile_info = new JSONObject().
                put("99p", 82.59).put("97p", 64.75).put("95p", 8.94).put("50p", 0.63).
                put("99.9p", 93.48).put("100p", 30000).put("99.99p", 111.5).put("99.999p", 198.52);
        JSONObject general_info = new JSONObject().
                put("min", 2.15).put("max", 2107.212121).put("mean", 31.91);
        JSONArray podMetrics = new JSONArray();
        podMetrics.put(new JSONObject().
                put("summary_results", new JSONObject().
                        put("percentile_info", percentile_info).
                        put("general_info", general_info)
                ).
                put("name", "request_sum").
                put("datasource", "prometheus"));
        podMetrics.put(new JSONObject().
                put("summary_results", new JSONObject().
                        put("percentile_info", percentile_info).
                        put("general_info", general_info)
                ).
                put("name", "request_count").
                put("datasource", "prometheus"));
        JSONArray containers = new JSONArray();
        String imageName = "";
        String containerName = "";
        HashMap<String, TrialDetails> trialDetailsHashMap = experimentTrial.getTrialDetails();
        Map.Entry<String,TrialDetails> entry = trialDetailsHashMap.entrySet().iterator().next();
        imageName = entry.getValue().getConfigData().getContainerName();
        containerName = entry.getValue().getConfigData().getStackName();
        containers.put(new JSONObject().put(
                "image_name", imageName
        ).put(
                "container_name", containerName
        ).put(
                "container_metrics", new JSONArray().put(
                        new JSONObject().put(
                                "name", "memoryRequest"
                        ).put(
                                "summary_results", new JSONObject().put(
                                        "general_info", general_info
                                )
                        )
                )
        ));
        JSONArray deployments = new JSONArray();
        deployments.put(
                new JSONObject().
                        put("pod_metrics", podMetrics).
                        put("deployment_name", experimentTrial.getResourceDetails().getDeploymentName()).
                        put("namespace", experimentTrial.getResourceDetails().getNamespace()).
                        put("type", "training").
                        put("containers", containers)
        );
        JSONObject retJson = new JSONObject();
        retJson.put("experiment_name", experimentTrial.getExperimentName());
        retJson.put("experiment_id", experimentTrial.getExperimentId());
        retJson.put("deployment_name", experimentTrial.getResourceDetails().getDeploymentName());
        if (null != experimentTrial.getTrialInfo()) {
            retJson.put("info", new JSONObject().put("trial_info",
                    new JSONObject(
                            new Gson().toJson(experimentTrial.getTrialInfo())
                    )
            ));
        }
        retJson.put("deployments", deployments);
        return retJson;
    }

    public ExperimentTrial getExperimentTrial() {
        return experimentTrial;
    }

    public void setExperimentTrial(ExperimentTrial experimentTrial) {
        this.experimentTrial = experimentTrial;
    }

    public void startExperimentTrials() {
        LOGGER.debug("Start Exp Trial");
        int numberOFIterations = Integer.parseInt(this.experimentTrial.getExperimentSettings().getTrialSettings().getTrialIterations());
        EMLoadInterceptor emLoadInterceptor = new EMLoadInterceptor();
        String imageName = "";
        String containerName = "";
        this.experimentTrial.getTrialDetails().forEach((tracker, trialDetails) -> {
            KubernetesServices kubernetesServices = null;
            try {
                kubernetesServices = new KubernetesServicesImpl();
                DeploymentHandler deploymentHandler = new DeploymentHandler(
                        this.experimentTrial.getResourceDetails().getNamespace(),
                        this.experimentTrial.getResourceDetails().getDeploymentName(),
                        trialDetails.getConfigData(),
                        kubernetesServices
                );
                IntStream.rangeClosed(1, numberOFIterations).forEach(
                    i -> {
                        boolean proceedToMetricsCollection = false;
                        deploymentHandler.initiateDeploy();
                        //Check if deployment is ready
                        int deploymentIsReadyWithinMinute = EMConstants.TimeConv.DEPLOYMENT_IS_READY_WITHIN_MINUTE;
                        long millisMinutes = TimeUnit.MINUTES.toMillis(deploymentIsReadyWithinMinute);
                        int count = (int)millisMinutes/EMConstants.TimeConv.DEPLOYMENT_CHECK_INTERVAL_IF_READY_MILLIS;
                        for (int j = count; j > 0 && !deploymentHandler.isDeploymentReady(); j--) {   //Parameterize sleep duration hardcoded for 120
                            try {
                                LOGGER.debug("Still deployment is not ready for ExpName {} trail No {}", this.experimentTrial.getExperimentName(), tracker);
                                Thread.sleep(EMConstants.TimeConv.DEPLOYMENT_CHECK_INTERVAL_IF_READY_MILLIS);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if (!deploymentHandler.isDeploymentReady()) {
                            LOGGER.debug("Giving up for ExpName {} trail No {} for {} attempt", this.experimentTrial.getExperimentName(), this.experimentTrial.getTrialInfo().getTrialNum(), i);
                        } else {
                            //check if load applied to deployment

                            // Checks if the load can be detected with the attributes of experiment trial
                            if (EMUtil.InterceptorDetectionStatus.DETECTED == emLoadInterceptor.detect(this.experimentTrial)) {
                                EMUtil.InterceptorAvailabilityStatus currentAvailability = emLoadInterceptor.isAvailable(this.experimentTrial);
                                // This loop mechanism will be part of an abstraction later (Threshold loop abstraction)
                                // for now just like deployment handler we run a for loop and constantly poll for the load availability
                                for (int j = 0; j < EMConstants.StandardDefaults.BackOffThresholds.CHECK_LOAD_AVAILABILITY_THRESHOLD; j++) {
                                    // Proceed to check if load is available (minimal variation)
                                    if (EMUtil.InterceptorAvailabilityStatus.AVAILABLE == currentAvailability) {
                                        // Will proceed for metric cycles if the load is detected
                                        proceedToMetricsCollection = true;
                                        // breaking the load availability loop
                                        break;
                                    }
                                    try {
                                        LOGGER.debug("The Load is not yet available, will be checking it again");
                                        // Will be replaced by a exponential looper mechanism
                                        Thread.sleep(EMUtil.timeToSleep(j) * 1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    currentAvailability = emLoadInterceptor.isAvailable(this.experimentTrial);
                                }
                                if (EMUtil.InterceptorAvailabilityStatus.AVAILABLE != currentAvailability) {
                                    LOGGER.debug("Load cannot be detected for the particular trial, proceed to collect metrics if it can be ignored");
                                    if (this.experimentTrial.getExperimentSettings().getTrialSettings().isForceCollectMetrics()) {
                                        proceedToMetricsCollection = true;
                                    }
                                }
                            } else {
                                LOGGER.debug("Load cannot be detected for the particular trial, proceed to collect metrics if it can be ignored");
                                if (this.experimentTrial.getExperimentSettings().getTrialSettings().isForceCollectMetrics()) {
                                    proceedToMetricsCollection = true;
                                }
                            }
                            if (!proceedToMetricsCollection) {
                                LOGGER.debug("Proceeding to next trial and this trial cannot be completed due to lack of metrics collection. Will be marked as failed trial");
                                // Need to implement a exiting functionality
                            } else {
                                //collect warmup and measurement cycles metrics
                            }
                        }
                    }
                );
            } catch (Exception e) {
                LOGGER.error(e.toString());
                e.printStackTrace();
            } finally {
                if (kubernetesServices != null)
                    kubernetesServices.shutdownClient();
            }
        });
        //Accumulate and send metrics
        JSONObject retJson = getDummyMetricJson(this.experimentTrial);
        URL trial_result_url = null;
        if (null != this.experimentTrial.getTrialInfo() && null!= this.experimentTrial.getTrialInfo().getTrialResultURL()) {
            try {
                trial_result_url = new URL(this.experimentTrial.getTrialInfo().getTrialResultURL());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            LOGGER.debug("POST to URL. {}", trial_result_url);
            HttpUtils.postRequest(trial_result_url, retJson.toString());
        }
        LOGGER.debug(retJson.toString());
    }
}
