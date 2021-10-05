package com.autotune.experimentManager.transitions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitionToCreateResultData extends AbstractBaseTransition{
    private static final Logger LOGGER = LoggerFactory.getLogger(TransitionToCreateResultData.class);
    @Override
    public void transit(String runId) {
        LOGGER.info("Executing transition - TransitionToCreateResultData on thread - {}", Thread.currentThread().getId());
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        processNextTransition(runId);
    }
}
