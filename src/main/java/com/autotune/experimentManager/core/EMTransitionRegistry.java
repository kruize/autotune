package com.autotune.experimentManager.core;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.utils.EMUtil;

public class EMTransitionRegistry {
    public static boolean validateStages(EMUtil.EMExpStages fromStage, EMUtil.EMExpStages toStage) {
        if(fromStage.ordinal() >= toStage.ordinal())
            return false;
        return true;
    }

    public static EMUtil.EMExpStages getNextStage(EMUtil.EMExpStages currentStage) {
        if (EMUtil.EMExpStages.getSize() <= (currentStage.ordinal() + 1)){
            return currentStage;
        }
        return EMUtil.EMExpStages.get(currentStage.ordinal() + 1);
    }
    public static EMUtil.EMExpStages getNextStage(EMUtil.EMExpStages currentStage, String runId) {
        if (EMUtil.EMExpStages.getSize() <= (currentStage.ordinal() + 1)){
            return currentStage;
        }
        if (currentStage.isCycle()) {
            // Implement the breaking logic. Might be dependent on ETD
            ExperimentTrialData currentETD = ((ExperimentTrialData) EMMapper.getInstance().getMap().get(runId));
            boolean breakCycle = EMUtil.breakingCondition(currentStage, currentETD);
            if (!breakCycle) {
                return currentStage;
            }
        }
        return EMUtil.EMExpStages.get(currentStage.ordinal() + 1);
    }


    public static EMUtil.EMExpStages getPossibleContinuousStage(EMUtil.EMExpStages currentStage) {
        EMUtil.EMExpStages previousStage = currentStage;
        for (int i = currentStage.ordinal() + 1; i < EMUtil.EMExpStages.getSize(); i++) {
            if (!EMUtil.EMExpStages.get(i).isScheduled()) {
                previousStage = EMUtil.EMExpStages.get(i);
            } else {
                return previousStage;
            }
        }
        return null;
    }
}
