/*******************************************************************************
 * Copyright (c)  2022 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.experimentManager.data.result;

import com.autotune.experimentManager.handler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;

/**
 * Configuration class used to set workflow
 * if do_experiments steps are prevalidation,deployment,postvalidation,load,metrics,summarizer,PostResults
 * if do_monitoring only then prevalidation,load,metrics,summarizer,PostResults
 * Load depends on up flag wait_for_load
 */
public class AutoTuneWorkFlow {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoTuneWorkFlow.class);
    private final boolean wait_for_load;
    private final LinkedHashMap<String, String> iterationWorkflowMap;
    private final LinkedHashMap<String, String> trialWorkflowMap;

    public AutoTuneWorkFlow(boolean do_experiments, boolean do_monitoring, boolean wait_for_load, String trialResultURL) throws Exception {
        LOGGER.debug("Do_experiments : {} , Do_monitoring : {}", do_experiments, do_monitoring);
        this.wait_for_load = wait_for_load;
        if (do_experiments == true) {
            iterationWorkflowMap = new LinkedHashMap<String, String>();
            iterationWorkflowMap.put(PreValidationHandler.class.getSimpleName().replace("Handler", ""), PreValidationHandler.class.getName());
            iterationWorkflowMap.put(DeploymentHandler.class.getSimpleName().replace("Handler", ""), DeploymentHandler.class.getName());
            iterationWorkflowMap.put(PostValidationHandler.class.getSimpleName().replace("Handler", ""), PostValidationHandler.class.getName());
            if (wait_for_load)
                iterationWorkflowMap.put(LoadValidationHandler.class.getSimpleName().replace("Handler", ""), LoadValidationHandler.class.getName());
            iterationWorkflowMap.put(MetricCollectionHandler.class.getSimpleName().replace("Handler", ""), MetricCollectionHandler.class.getName());
            iterationWorkflowMap.put(SummarizerHandler.class.getSimpleName().replace("Handler", ""), SummarizerHandler.class.getName());
        } else if (do_monitoring == true) {
            iterationWorkflowMap = new LinkedHashMap<String, String>();
            iterationWorkflowMap.put(PreValidationHandler.class.getSimpleName().replace("Handler", ""), PreValidationHandler.class.getName());
            if (wait_for_load)
                iterationWorkflowMap.put(LoadValidationHandler.class.getSimpleName().replace("Handler", ""), LoadValidationHandler.class.getName());
            iterationWorkflowMap.put(MetricCollectionHandler.class.getSimpleName().replace("Handler", ""), MetricCollectionHandler.class.getName());
            iterationWorkflowMap.put(SummarizerHandler.class.getSimpleName().replace("Handler", ""), SummarizerHandler.class.getName());
        } else {
            throw new Exception("Workflow not defined");
        }
        trialWorkflowMap = new LinkedHashMap<String, String>();
        trialWorkflowMap.put(SummarizerHandler.class.getSimpleName().replace("Handler", ""), SummarizerHandler.class.getName());
        if (null != trialResultURL)
            trialWorkflowMap.put(PostResultsHandler.class.getSimpleName().replace("Handler", ""), PostResultsHandler.class.getName());
        LOGGER.debug("Workflow steps are iterations {}", iterationWorkflowMap.keySet());
        LOGGER.debug("Workflow steps are Trials {}", trialWorkflowMap.keySet());
    }

    public boolean isWait_for_load() {
        return wait_for_load;
    }

    public LinkedHashMap<String, String> getIterationWorkflowMap() {
        return iterationWorkflowMap;
    }

    public LinkedHashMap<String, String> getTrialWorkflowMap() {
        return trialWorkflowMap;
    }


}
