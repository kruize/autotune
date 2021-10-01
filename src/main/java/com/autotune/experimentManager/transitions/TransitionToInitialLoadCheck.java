package com.autotune.experimentManager.transitions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitionToInitialLoadCheck extends AbstractBaseTransition{
    private static final Logger LOGGER = LoggerFactory.getLogger(TransitionToInitialLoadCheck.class);
    @Override
    public void transit(String runId) {
        LOGGER.info("Executing transition - TransitionToInitialLoadCheck on thread - {}", Thread.currentThread().getId());
        System.out.println("Executing transition - TransitionToInitialLoadCheck on thread - {}" + Thread.currentThread().getId() + "For RunId - " + runId);
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        processNextTransition(runId);
    }
}
