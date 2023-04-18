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
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.common.parallelengine.executor.KruizeExecutor;
import com.autotune.common.parallelengine.worker.KruizeWorker;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.operator.KruizeOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.util.Map;

import static com.autotune.analyzer.experiment.Experimentator.startExperiment;
import static com.autotune.operator.KruizeOperator.addLayerInfo;
import static com.autotune.operator.KruizeOperator.matchPodsToAutotuneObject;

/**
 * Analyser worker which gets initiated via blocking queue either from rest API or Autotune CRD.
 * Move status from queue to in progress.
 * Start HPO loop if
 * TargetCluster : Local
 * mode : Experiment or Monitoring
 */

public class CreateExperimentManager implements KruizeWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateExperimentManager.class);

    @Override
    public void execute(KruizeObject kruizeObject, Object o, KruizeExecutor kruizeExecutor, ServletContext context) {
        new ExperimentDBService().addExperimentToDB(kruizeObject);
        //experimentInterface.updateExperimentStatus(kruizeExperiment, AnalyzerConstants.ExpStatus.IN_PROGRESS);

        if (kruizeObject.getExperimentUseCaseType().isLocalExperiment() || kruizeObject.getExperimentUseCaseType().isLocalMonitoring()) {
            matchPodsToAutotuneObject(kruizeObject);
            for (String kruizeConfig : KruizeOperator.autotuneConfigMap.keySet()) {
                addLayerInfo(KruizeOperator.autotuneConfigMap.get(kruizeConfig), kruizeObject);
            }
            if (kruizeObject.getExperimentUseCaseType().isLocalExperiment()) {
                if (!KruizeOperator.deploymentMap.isEmpty() &&
                        KruizeOperator.deploymentMap.get(kruizeObject.getExperimentName()) != null) {
                    Map<String, ApplicationDeployment> depMap = KruizeOperator.deploymentMap.get(kruizeObject.getExperimentName());
                    for (String deploymentName : depMap.keySet()) {
                        startExperiment(kruizeObject, depMap.get(deploymentName));
                    }
                    LOGGER.info("Added Kruize object " + kruizeObject.getExperimentName());
                } else {
                    LOGGER.error("Kruize object " + kruizeObject.getExperimentName() + " not added as no related deployments found!");
                }
            }
        }
    }
}
