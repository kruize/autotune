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
package com.autotune.analyzer.kruizeObject;

import com.autotune.analyzer.utils.AnalyzerConstants;
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
    boolean remote_monitoring;
    boolean local_monitoring;
    boolean local_experiment;

    public ExperimentUseCaseType(KruizeObject kruizeObject) throws Exception {
        if (kruizeObject.getTarget_cluster().equalsIgnoreCase(AnalyzerConstants.REMOTE)) {
            if (kruizeObject.getMode().equalsIgnoreCase(AnalyzerConstants.MONITOR)) {
                setRemote_monitoring(true);
            } else {
                throw new Exception("Invalid Mode " + kruizeObject.getMode() + " for target cluster as Remote.");
            }
        } else if (kruizeObject.getTarget_cluster().equalsIgnoreCase(AnalyzerConstants.LOCAL)) {
            switch (kruizeObject.getMode().toLowerCase()) {
                case AnalyzerConstants.MONITOR:
                case AnalyzerConstants.RECREATE:
                case AnalyzerConstants.AUTO:
                    setLocal_monitoring(true);
                    break;

                case AnalyzerConstants.EXPERIMENT:
                    setLocal_experiment(true);
                    break;

                default:
                    throw new Exception("Invalid Mode " + kruizeObject.getMode() + " for target cluster as Local.");
            }
        } else {
            throw new Exception("Invalid Target cluster type");
        }
    }

    public boolean isRemote_monitoring() {
        return remote_monitoring;
    }

    public void setRemote_monitoring(boolean remote_monitoring) {
        this.remote_monitoring = remote_monitoring;
    }

    public boolean isLocal_monitoring() {
        return local_monitoring;
    }

    public void setLocal_monitoring(boolean local_monitoring) {
        this.local_monitoring = local_monitoring;
    }

    public boolean isLocal_experiment() {
        return local_experiment;
    }

    public void setLocal_experiment(boolean local_experiment) {
        this.local_experiment = local_experiment;
    }

    @Override
    public String toString() {
        return "ExperimentUseCaseType{" +
                "remoteMonitoring=" + remote_monitoring +
                ", localMonitoring=" + local_monitoring +
                ", localExperiment=" + local_experiment +
                '}';
    }
}
