package com.autotune.experimentManager.services.util;

import com.autotune.experimentManager.core.ExperimentManager;
import com.autotune.experimentManager.data.*;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.services.CreateExperimentTrial;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;

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
}
