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

package com.autotune.experimentManager.handler.eminterface;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.TrialDetails;
import com.autotune.common.parallelengine.executor.AutotuneExecutor;
import com.autotune.experimentManager.data.result.StepsMetaData;
import com.autotune.experimentManager.data.result.TrialIterationMetaData;

import javax.servlet.ServletContext;

/**
 * Execute methode executes core business logic and submit a task to IterationManger to execute next task.
 * DeploymentHandler , MetricCollectionHandler will
 */
public interface EMHandlerInterface {
    void execute(ExperimentTrial experimentTrial, TrialDetails trialDetails,
                 TrialIterationMetaData iterationMetaData,
                 StepsMetaData stepsMeatData,
                 AutotuneExecutor autotuneExecutor, ServletContext context);
}
