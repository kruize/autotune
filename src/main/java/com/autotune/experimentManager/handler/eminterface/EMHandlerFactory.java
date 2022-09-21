package com.autotune.experimentManager.handler.eminterface;

import com.autotune.experimentManager.data.result.AutoTuneWorkFlow;
import com.autotune.experimentManager.handler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EMHandlerFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMHandlerFactory.class);
    public <T extends EMHandlerInterface> EMHandlerInterface create(String className) {
        EMHandlerInterface toReturn = null;
        if (PreValidationHandler.class.getName().equals(className)) {
            toReturn = new PreValidationHandler();
        }else if(DeploymentHandler.class.getName().equals(className)){
            toReturn = new DeploymentHandler();
        }else if(PostValidationHandler.class.getName().equals(className)) {
            toReturn = new PostValidationHandler();
        }else if(LoadValidationHandler.class.getName().equals(className)){
                toReturn = new LoadValidationHandler();
        }else if(MetricCollectionHandler.class.getName().equals(className)){
            toReturn = new MetricCollectionHandler();
        }else if(SummarizerHandler.class.getName().equals(className)){
            toReturn = new SummarizerHandler();
        }else if(PostResultsHandler.class.getName().equals(className)){
            toReturn = new PostResultsHandler();
        }else {
            LOGGER.error("Factory class not updated for Hander : {} ",className);
        }
        return toReturn;
    }
}
