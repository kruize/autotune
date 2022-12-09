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
package com.autotune.analyzer.workerimpl;

import com.autotune.analyzer.application.ApplicationDeployment;
import com.autotune.analyzer.deployment.AutotuneDeployment;
import com.autotune.common.k8sObjects.AutotuneObject;
import com.autotune.common.parallelengine.executor.AutotuneExecutor;
import com.autotune.common.parallelengine.worker.AutotuneWorker;
import com.autotune.utils.AnalyzerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.util.Map;

import static com.autotune.analyzer.Experimentator.startExperiment;
import static com.autotune.analyzer.deployment.AutotuneDeployment.addLayerInfo;
import static com.autotune.analyzer.deployment.AutotuneDeployment.matchPodsToAutotuneObject;

/**
 * Analyser worker which gets initiated via blocking queue either from rest API or Autotune CRD.
 * Move status from queue to in progress.
 * Start HPO loop if
 * TargetCluster : Local
 * mode : Experiment or Monitoring
 */

public class AnalyserManager implements AutotuneWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyserManager.class);

    @Override
    public void execute(Object o, AutotuneExecutor autotuneExecutor, ServletContext context) {
        AutotuneObject kruizeExperiment = (AutotuneObject) o;
        kruizeExperiment.setStatus(AnalyzerConstants.ExpStatus.IN_PROGRESS);
        if (kruizeExperiment.getTargetCluster().equals(AnalyzerConstants.LOCAL)) {
            if (kruizeExperiment.getMode().equals(AnalyzerConstants.EXPERIMENT) || (kruizeExperiment.getMode().equals(AnalyzerConstants.MONITOR))) {
                matchPodsToAutotuneObject(kruizeExperiment);
                for (String kruizeConfig : AutotuneDeployment.autotuneConfigMap.keySet()) {
                    addLayerInfo(AutotuneDeployment.autotuneConfigMap.get(kruizeConfig), kruizeExperiment);
                }
                if (!AutotuneDeployment.deploymentMap.isEmpty() &&
                        AutotuneDeployment.deploymentMap.get(kruizeExperiment.getExperimentName()) != null) {
                    Map<String, ApplicationDeployment> depMap = AutotuneDeployment.deploymentMap.get(kruizeExperiment.getExperimentName());
                    for (String deploymentName : depMap.keySet()) {
                        startExperiment(kruizeExperiment, depMap.get(deploymentName));
                    }
                    LOGGER.info("Added Kruize object " + kruizeExperiment.getExperimentName());
                } else {
                    LOGGER.error("Kruize object " + kruizeExperiment.getExperimentName() + " not added as no related deployments found!");
                }
            }
        }
    }
}
