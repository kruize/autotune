package com.autotune.experimentManager.data;

public class EMStageScheduledTransition {
    private EMStageTransition emStageTransition;
    private int delayInSecs;

    public EMStageScheduledTransition(EMStageTransition emStageTransition, int delayInSecs) {
        this.emStageTransition = emStageTransition;
        this.delayInSecs = delayInSecs;
    }

    public EMStageTransition getEmStageTransition() {
        return emStageTransition;
    }

    public int getDelayInSecs() {
        return delayInSecs;
    }
}
