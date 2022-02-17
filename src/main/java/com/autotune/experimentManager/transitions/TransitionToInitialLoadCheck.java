package com.autotune.experimentManager.transitions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitionToInitialLoadCheck extends AbstractBaseTransition{
    private static final Logger LOGGER = LoggerFactory.getLogger(TransitionToInitialLoadCheck.class);
    @Override
    public void transit(String runId) {
        LOGGER.info("Executing transition - TransitionToInitialLoadCheck on thread - {} For RunId - ", Thread.currentThread().getId(), runId);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        processNextTransition(runId);
    }
}
