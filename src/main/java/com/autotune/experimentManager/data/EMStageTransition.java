package com.autotune.experimentManager.data;

import com.autotune.experimentManager.utils.EMUtil.EMExpStages;

public class EMStageTransition {
    private String runId;
    private EMExpStages targetStage;

    public EMStageTransition(String runId, EMExpStages targetStage) {
        this.runId = runId;
        this.targetStage = targetStage;
    }

    public String getRunId() {
        return runId;
    }

    public EMExpStages getTargetStage() {
        return targetStage;
    }
}
