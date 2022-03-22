/*******************************************************************************
 * Copyright (c) 2021, 2021 Red Hat, IBM Corporation and others.
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
package com.autotune.common.experiments;

import java.util.HashMap;

/**
 *
 */
public class ExperimentTrial {
    private final String experimentId;
    private final String namespace;
    private final String experimentName;
    private final TrialInfo trialInfo;
    private final DatasourceInfo datasourceInfo;
    private final ExperimentSettings experimentSettings;
    // HashMap of parallel trials being monitored for this trial
    // Eg. training and production
    // uses tracker as key. tracker = "training" or "production"
    private final HashMap<String, TrialDetails> trialDetails;

    public ExperimentTrial(String experimentName,
                           String experimentId,
                           String namespace,
                           TrialInfo trialInfo,
                           DatasourceInfo datasourceInfo,
                           ExperimentSettings experimentSettings,
                           HashMap<String, TrialDetails> trialDetails) {
        this.experimentId = experimentId;
        this.namespace = namespace;
        this.experimentName = experimentName;
        this.trialInfo = trialInfo;
        this.datasourceInfo = datasourceInfo;
        this.experimentSettings = experimentSettings;
        this.trialDetails = trialDetails;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public DatasourceInfo getDatasourceInfo() {
        return datasourceInfo;
    }

    public TrialInfo getTrialInfo() {
        return trialInfo;
    }

    public ExperimentSettings getExperimentSettings() {
        return experimentSettings;
    }

    public HashMap<String, TrialDetails> getTrialDetails() {
        return trialDetails;
    }
}
