/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.experimentManager.workerimpl;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.parallelengine.worker.AutotuneWorker;
import com.autotune.experimentManager.core.ExperimentTrialHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  This is worker to execute experiments steps.
 */
public class IterationManager implements AutotuneWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(IterationManager.class);

    public IterationManager() {}

    @Override
    public void execute(Object o) {
        ExperimentTrial experimentTrial = (ExperimentTrial) o;
        LOGGER.debug("Experiment name {} started processing", experimentTrial.getExperimentName());
        new ExperimentTrialHandler(experimentTrial).startExperimentTrials();
    }
}
