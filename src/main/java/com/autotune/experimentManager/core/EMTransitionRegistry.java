package com.autotune.experimentManager.core;

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
