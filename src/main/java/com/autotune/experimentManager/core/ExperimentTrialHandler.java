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
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.utils.HttpUtils;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.IntStream;

import static com.autotune.analyzer.loop.HPOInterface.postTrialResultToHPO;
import static com.autotune.utils.ServerContext.OPTUNA_TRIALS_END_POINT;

/**
 * Service class helper used to control and execute Lifecycle of Experiments using trial numbers.
 */
public class ExperimentTrialHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentTrialHandler.class);
    private ExperimentTrial experimentTrial;

    public ExperimentTrialHandler(ExperimentTrial experimentTrial) {
        this.experimentTrial = experimentTrial;
    }

    public ExperimentTrial getExperimentTrial() {
        return experimentTrial;
    }

    public void setExperimentTrial(ExperimentTrial experimentTrial) {
        this.experimentTrial = experimentTrial;
    }

    public ArrayList<String> getTrackers() {
        return this.experimentTrial.getExperimentSettings().getDeploymentSettings().getDeploymentTracking().getTrackers();
    }

    public void startExperimentTrials() {
        LOGGER.debug("Start Exp Trial");
        int numberOFIterations = Integer.parseInt(this.experimentTrial.getExperimentSettings().getTrialSettings().getTrialIterations());
        this.experimentTrial.getTrialDetails().forEach((tracker, trialDetails) -> {
            trialDetails.getPodContainers().forEach((imageName, podContainer) -> {
                podContainer.getTrialConfigs().forEach((trialNumber, containerConfigData) -> {
                    DeploymentHandler deploymentHandler = new DeploymentHandler(
                            trialDetails.getDeploymentNameSpace(),
                            trialDetails.getDeploymentName(),
                            containerConfigData
                            );
                    IntStream.rangeClosed(1, numberOFIterations).forEach(
                            i -> {
                                deploymentHandler.initiateDeploy();
                                //check if load applied to deployment
                                //collect warmup and measurement cycles metrics
                            }
                    );
                });
            });
        });
        //Accumulate and send metrics
        JSONObject retJson = getDummyMetricJson(this.experimentTrial);
        // POST the result back to HPO
        URL experimentTrialsURL = null;
        URL trial_result_url = null;
        try {
            experimentTrialsURL = new URL(OPTUNA_TRIALS_END_POINT);
            trial_result_url = new URL(this.experimentTrial.getTrialInfo().getTrialResultURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        postTrialResultToHPO(this.experimentTrial, experimentTrialsURL);  // this will be called inside analyser api
        try {
            Thread.sleep(1000 * 10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        HttpUtils.postRequest(trial_result_url, retJson.toString());
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
        JSONArray containers = new JSONArray();

        HashMap<String, PodContainer> podContainerHashMap = experimentTrial.getTrialDetails().get("training").getPodContainers();
        String imageName = podContainerHashMap.keySet().iterator().next();

        containers.put(new JSONObject().put(
                "image_name", imageName
        ).put(
                "container_name", podContainerHashMap.get(imageName).getContainerName()
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
                        put("deployment_name", experimentTrial.getTrialDetails().get("training").getDeploymentName()).
                        put("namespace", experimentTrial.getTrialDetails().get("training").getDeploymentNameSpace()).
                        put("type", "training").
                        put("containers", containers)
        );
        JSONObject retJson = new JSONObject();
        retJson.put("experiment_name", experimentTrial.getExperimentName());
        retJson.put("experiment_id", experimentTrial.getExperimentId());
        retJson.put("deployment_name", experimentTrial.getTrialDetails().get("training").getDeploymentName());
        retJson.put("info", new JSONObject().put("trial_info",
                new JSONObject(
                        new Gson().toJson(experimentTrial.getTrialInfo())
                )
        ));
        System.out.println(deployments);
        retJson.put("deployments", deployments);
        return retJson;
    }

}
