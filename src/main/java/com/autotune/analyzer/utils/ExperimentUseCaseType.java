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
package com.autotune.analyzer.utils;

import com.autotune.common.k8sObjects.KruizeObject;
import com.autotune.utils.AnalyzerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class helps in identifying the valid use cases
 * like Remote Monitoring
 * Local Monitoring or Experiment mode.
 */
public class ExperimentUseCaseType {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentUseCaseType.class);
    boolean remoteMonitoring;
    boolean localMonitoring;
    boolean localExperiment;

    public ExperimentUseCaseType(KruizeObject kruizeObject) throws Exception {
        if (kruizeObject.getTarget_cluster().equalsIgnoreCase(AnalyzerConstants.REMOTE)) {
            if (kruizeObject.getMode().equalsIgnoreCase(AnalyzerConstants.MONITOR)) {
                setRemoteMonitoring(true);
            } else {
                throw new Exception("Invalid Mode " + kruizeObject.getMode() + " for target cluster as Remote.");
            }
        } else if (kruizeObject.getTarget_cluster().equalsIgnoreCase(AnalyzerConstants.LOCAL)) {
            if (kruizeObject.getMode().equalsIgnoreCase(AnalyzerConstants.MONITOR)) {
                setLocalMonitoring(true);
            } else if (kruizeObject.getMode().equalsIgnoreCase(AnalyzerConstants.EXPERIMENT)) {
                setLocalExperiment(true);
            } else {
                throw new Exception("Invalid Mode " + kruizeObject.getMode() + " for target cluster as Local.");
            }
        } else {
            throw new Exception("Invalid Target cluster type");
        }
    }

    public boolean isRemoteMonitoring() {
        return remoteMonitoring;
    }

    public void setRemoteMonitoring(boolean remoteMonitoring) {
        this.remoteMonitoring = remoteMonitoring;
    }

    public boolean isLocalMonitoring() {
        return localMonitoring;
    }

    public void setLocalMonitoring(boolean localMonitoring) {
        this.localMonitoring = localMonitoring;
    }

    public boolean isLocalExperiment() {
        return localExperiment;
    }

    public void setLocalExperiment(boolean localExperiment) {
        this.localExperiment = localExperiment;
    }

    @Override
    public String toString() {
        return "ExperimentUseCaseType{" +
                "remoteMonitoring=" + remoteMonitoring +
                ", localMonitoring=" + localMonitoring +
                ", localExperiment=" + localExperiment +
                '}';
    }
}
