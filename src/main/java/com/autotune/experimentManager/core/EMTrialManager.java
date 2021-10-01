package com.autotune.experimentManager.core;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.EMStageProcessQueue;
import com.autotune.experimentManager.data.EMStageTransition;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.utils.EMUtil;

import java.util.LinkedList;

public class EMTrialManager {

    public static void launchWaitingExperiment(String runId) {
        ExperimentTrialData etd = ((ExperimentTrialData) EMMapper.getInstance().getMap().get(runId));
        String nsdkey = EMUtil.formatNSDKey(etd.getConfig().getDeploymentNamespace(), etd.getConfig().getDeploymentName());
        if (EMMapper.getInstance().getDeploymentRunIdMap().containsKey(nsdkey)) {
            LinkedList<String> depList = (LinkedList<String>) EMMapper.getInstance().getDeploymentRunIdMap().get(nsdkey);
            if (runId.equalsIgnoreCase(depList.peek())) {
                depList.removeFirst();
                String nextRunId = depList.getFirst();
                ExperimentTrialData nextEtd = (ExperimentTrialData) EMMapper.getInstance().getMap().get(nextRunId);
                if (nextEtd.getStatus().toString().equalsIgnoreCase(EMUtil.EMExpStatus.WAIT.toString())) {
                    nextEtd.setStatus(EMUtil.EMExpStatus.IN_PROGRESS);
                }
                EMStageTransition transition = new EMStageTransition(nextRunId, EMUtil.EMExpStages.CREATE_CONFIG);
                EMStageProcessQueue.getStageProcessQueueInstance().getQueue().add(transition);
                ExperimentManager.notifyQueueProcessor();
            } else {
                // TODO: Need to handle this scary problem
            }
        }
    }
}
