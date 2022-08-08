package com.autotune.experimentManager.services.util;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.PodContainer;
import com.autotune.experimentManager.core.ExperimentManager;
import com.autotune.experimentManager.data.*;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.services.CreateExperimentTrial;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMUtil;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class EMAPIHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMAPIHandler.class);
    public static ExperimentTrialData createETD(JSONObject json) {
        try {
            LOGGER.info("Creating EMTrailConfig");
            EMTrialConfig config = new EMTrialConfig(json);
            LOGGER.info("EMTrailConfig created");
            ExperimentTrialData trailData = new ExperimentTrialData(config);
            LOGGER.info("ETD created");
            return trailData;
        } catch (IncompatibleInputJSONException | EMInvalidInstanceCreation e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String registerTrial(ExperimentTrialData trialData) {
        String runId = EMUtil.createUUID();
        String nsdKey = EMUtil.formatNSDKey(trialData.getConfig().getDeploymentNamespace(), trialData.getConfig().getDeploymentName());

        if (trialData.getConfig().getDeploymentStrategy().equalsIgnoreCase(EMConstants.DeploymentStrategies.ROLLING_UPDATE)) {
            if (EMMapper.getInstance().getDeploymentRunIdMap().containsKey(nsdKey)) {
                LinkedList<String> depList = (LinkedList<String>) EMMapper.getInstance().getDeploymentRunIdMap().get(nsdKey);
                if (depList.isEmpty()) {
                    // TODO: Need to be handled
                } else {
                    String existingRunId = depList.getLast();
                    ExperimentTrialData lastETD = ((ExperimentTrialData) EMMapper.getInstance().getMap().get(existingRunId));
                    if (lastETD.getStatus().toString().equalsIgnoreCase(EMUtil.EMExpStatus.COMPLETED.toString())) {
                        depList.add(runId);
                        EMMapper.getInstance().getMap().put(runId, trialData);
                        pushTransitionToQueue(runId);
                    } else {
                        depList.add(runId);
                        EMMapper.getInstance().getMap().put(runId, trialData);
                        lastETD.setNotifyTrialCompletion(true);
                        trialData.setStatus(EMUtil.EMExpStatus.WAIT);
                    }
                }
            } else {
                LinkedList<String> runIdList = new LinkedList<String>();
                runIdList.add(runId);
                EMMapper.getInstance().getDeploymentRunIdMap().put(nsdKey, runIdList);
                EMMapper.getInstance().getMap().put(runId, trialData);
                pushTransitionToQueue(runId);
            }
        } else {
            if (EMMapper.getInstance().getDeploymentRunIdMap().containsKey(nsdKey)) {
                Queue<String> depQueue = ((Queue<String>) EMMapper.getInstance().getDeploymentRunIdMap().get(nsdKey));
                depQueue.add(runId);
            } else {
                LinkedList<String> runIdList = new LinkedList<String>();
                runIdList.add(runId);
                EMMapper.getInstance().getDeploymentRunIdMap().put(nsdKey, runIdList);
            }
            EMMapper.getInstance().getMap().put(runId, trialData);
            trialData.setStatus(EMUtil.EMExpStatus.IN_PROGRESS);
            pushTransitionToQueue(runId);
        }
        return runId;
    }

    private static void pushTransitionToQueue(String runId) {
        EMStageTransition transition = new EMStageTransition(runId, EMUtil.EMExpStages.CREATE_CONFIG);
        EMStageProcessQueue.getStageProcessQueueInstance().getQueue().add(transition);
        ExperimentManager.notifyQueueProcessor();
    }

    public static JSONObject getStatusJson(String experimentName, String trialNum, boolean verbose) {
        // Get the experiment map
        ConcurrentHashMap<String, HashMap<String, ExperimentTrial>> expMap = EMMapper.getInstance().getExpTrialMap();

        // Create an empty return JSON which will be populated based on the given requirement
        JSONObject returnJson = new JSONObject();

        // Scenerio - No Experiment name given
        if (null == experimentName) {
            if (expMap.size() == 0) {
                returnJson.put("Error", "No Experiments found");
            } else {
                for (String key : expMap.keySet()) {
                    JSONObject resJson = new JSONObject();
                    resJson.put("Status", "COMPLETED");
                    returnJson.put(key, resJson);
                }
            }
            return returnJson;
        }


        if (null != experimentName && expMap.containsKey(experimentName)) {
            // Scenerio - Experiment name given but no trial given
            if (null == trialNum) {
                HashMap<String, ExperimentTrial> trialHashMap = expMap.get(experimentName);
                for (String key : trialHashMap.keySet()) {
                    JSONObject resJson = getDummyTrialJSON(trialHashMap.get(key), verbose);
                    resJson.put("Status", "COMPLETED");
                    returnJson.put(key, resJson);
                }
                return returnJson;
            } else {
                // Scenerio - Experiment name and trial num are given
                if (expMap.get(experimentName).containsKey(trialNum)) {
                    JSONObject resJson = getDummyTrialJSON(expMap.get(experimentName).get(trialNum), verbose);
                    resJson.put("Status", "COMPLETED");
                    returnJson.put(trialNum, resJson);
                } else {
                    returnJson.put("Error", "Invalid trial number");
                }

                return returnJson;
            }
        } else {
            returnJson.put("Error", "Invalid Experiment Name");
        }

        return returnJson;
    }

    private static JSONObject getDummyTrialJSON(ExperimentTrial experimentTrial, boolean verbose) {
        JSONObject percentile_info = new JSONObject().
                put("99p", 82.59).put("97p", 64.75).put("95p", 8.94).put("50p", 0.63).
                put("99.9p", 93.48).put("100p", 30000).put("99.99p", 111.5).put("99.999p", 198.52);
        JSONObject general_info = new JSONObject().
                put("min", 2.15).put("max", 2107.212121).put("mean", 31.91);
        JSONArray podMetrics = new JSONArray();
        for (int i = 0; i < 2; i++) {
            JSONObject resultJSON = new JSONObject();
            resultJSON.put("summary_results", new JSONObject().
                            put("percentile_info", percentile_info).
                            put("general_info", general_info)
                    ).
                    put("datasource", "prometheus");
            if (verbose) {
                JSONObject iterationResults = new JSONObject();
                int iterations = Integer.parseInt(experimentTrial.getExperimentSettings().getTrialSettings().getTrialIterations());
                JSONObject dummy_result = new JSONObject().put("percentile_info", percentile_info).
                        put("general_info", general_info);
                for (int j = 0; j < iterations; j++) {
                    JSONObject iterationObject = new JSONObject();
                    int warmUpCycles = Integer.parseInt(experimentTrial.getExperimentSettings().getTrialSettings().getTrialWarmupCycles());
                    int measurementCycles = Integer.parseInt(experimentTrial.getExperimentSettings().getTrialSettings().getTrialMeasurementCycles());
                    JSONObject warmResult = new JSONObject();
                    JSONObject measureResult = new JSONObject();
                    for (int warmCycle = 0; warmCycle < warmUpCycles; warmCycle++) {
                        warmResult.put(String.valueOf(warmCycle), dummy_result);
                    }
                    for (int measureCycle = 0; measureCycle < measurementCycles; measureCycle++) {
                        measureResult.put(String.valueOf(measureCycle), dummy_result);
                    }
                    iterationObject.put("warmup_results", warmResult);
                    iterationObject.put("measure_results", measureResult);
                    iterationResults.put(String.valueOf(j), iterationObject);
                }
                resultJSON.put("iteration_results", iterationResults);
            }

            if (i == 0) {
                resultJSON.put("name", "request_sum");
            } else {
                resultJSON.put("name", "request_count");
            }
            podMetrics.put(resultJSON);
        }


        JSONArray containers = new JSONArray();

        HashMap<String, PodContainer> podContainerHashMap = experimentTrial.getTrialDetails().get("training").getPodContainers();
        String imageName = podContainerHashMap.keySet().iterator().next();
        JSONArray containerMetrics = new JSONArray();
        for (int i = 0; i < 2; i++) {
            JSONObject resultJSON = new JSONObject();
            resultJSON.put("summary_results", new JSONObject().
                            put("percentile_info", percentile_info).
                            put("general_info", general_info)
                    ).
                    put("datasource", "prometheus");

            if (verbose) {
                JSONObject iterationResults = new JSONObject();
                int iterations = Integer.parseInt(experimentTrial.getExperimentSettings().getTrialSettings().getTrialIterations());
                JSONObject dummy_result = new JSONObject().put("percentile_info", percentile_info).
                        put("general_info", general_info);
                for (int j = 0; j < iterations; j++) {
                    JSONObject iterationObject = new JSONObject();
                    int warmUpCycles = Integer.parseInt(experimentTrial.getExperimentSettings().getTrialSettings().getTrialWarmupCycles());
                    int measurementCycles = Integer.parseInt(experimentTrial.getExperimentSettings().getTrialSettings().getTrialMeasurementCycles());
                    JSONObject warmResult = new JSONObject();
                    JSONObject measureResult = new JSONObject();
                    for (int warmCycle = 0; warmCycle < warmUpCycles; warmCycle++) {
                        warmResult.put(String.valueOf(warmCycle), dummy_result);
                    }
                    for (int measureCycle = 0; measureCycle < measurementCycles; measureCycle++) {
                        measureResult.put(String.valueOf(measureCycle), dummy_result);
                    }
                    iterationObject.put("warmup_results", warmResult);
                    iterationObject.put("measure_results", measureResult);
                    iterationResults.put(String.valueOf(j), iterationObject);
                }
                resultJSON.put("iteration_results", iterationResults);
            }

            if (i == 0) {
                resultJSON.put("name", "memoryRequest");
            } else {
                resultJSON.put("name", "cpuRequest");
            }
            containerMetrics.put(resultJSON);
        }
        containers.put(new JSONObject().put(
                "image_name", imageName
        ).put(
                "container_name", podContainerHashMap.get(imageName).getContainerName()
        ).put(
                "container_metrics", containerMetrics
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
