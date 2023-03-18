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

import com.autotune.common.annotations.json.KruizeJSONExclusionStrategy;
import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.TrialDetails;
import com.google.gson.GsonBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Service class helper used to control and execute Lifecycle of Experiments using trial numbers.
 */
public class ExperimentTrialHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentTrialHandler.class);
    private ExperimentTrial experimentTrial;


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
        Map.Entry<String, TrialDetails> entry = trialDetailsHashMap.entrySet().iterator().next();
        imageName = entry.getValue().getConfigData().getStackName();
        containerName = entry.getValue().getConfigData().getContainerName();
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
                        ).put(
                                "datasource", "prometheus"
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

}
