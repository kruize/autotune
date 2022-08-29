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
package com.autotune.experimentManager.data.dao;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.experimentManager.data.ExperimentDetailsMap;

import java.util.List;

/**
 * Experiment trial Object's Data access object.
 */
public interface ExperimentTrialDao {
    // Add experiment trial object.
    public void addExperiments();
    // List all experiments trial.
    public ExperimentDetailsMap<String, ExperimentTrial> listExperiments();
    // List all deployments names for experiments
    public List<String> getAllDeploymentNames();
    // List all trial for given experiment.
    // List status of experiments.
    // Get error message.
    public String getErrorMessage();
    // Get HTTP Response code.
    public int getHttpResponseCode();
}
