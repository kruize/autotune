package com.autotune.experimentManager.transitions;

public interface BaseTransition {
    public void transit(String runId);
    public void processNextTransition(String runId);
}
