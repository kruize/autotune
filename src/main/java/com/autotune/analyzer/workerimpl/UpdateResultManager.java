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

import com.autotune.analyzer.experiment.KruizeExperiment;
import com.autotune.analyzer.experiment.RunExperiment;
import com.autotune.analyzer.experiment.ExperimentInterface;
import com.autotune.analyzer.experiment.ExperimentInterfaceImpl;
import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.recommendations.GenerateRecommendation;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.DeploymentResultData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.common.parallelengine.executor.KruizeExecutor;
import com.autotune.common.parallelengine.worker.KruizeWorker;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.google.gson.Gson;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.util.List;

import static com.autotune.analyzer.experiment.Experimentator.experimentsMap;
import static com.autotune.utils.TrialHelpers.updateExperimentTrial;

/**
 * Worker that gets executed from ParallelEngine when new Experiment results are received.
 */
public class UpdateResultManager implements KruizeWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateResultManager.class);

    @Override
    public void execute(KruizeObject kruizeObject, Object o, KruizeExecutor kruizeExecutor, ServletContext context) {
        ExperimentResultData resultData = (ExperimentResultData) o;
        ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
        experimentInterface.addResultsToDB(kruizeObject, resultData);
        if (kruizeObject.getExperimentUseCaseType().isLocalExperiment()) {
            String trialNumber = resultData.getTrialNumber();
            List<DeploymentResultData> deploymentResultDataList = resultData.getDeployments();
            deploymentResultDataList.forEach(
                    (deploymentResultData -> {
                        String deploymentName = deploymentResultData.getDeployment_name();
                        KruizeExperiment kruizeExperiment = experimentsMap.get(deploymentName);
                        try {
                            updateExperimentTrial(trialNumber, kruizeExperiment, new JSONObject(new Gson().toJson(resultData)));
                        } catch (InvalidValueException e) {
                            e.printStackTrace();
                        } catch (IncompatibleInputJSONException e) {
                            e.printStackTrace();
                        }
                        RunExperiment runExperiment = kruizeExperiment.getExperimentThread();
                        // Received a metrics JSON from EM after a trial, let the waiting thread know
                        LOGGER.info("Received trial result for experiment: " + resultData.getExperiment_name() + "; Deployment name: " + deploymentName);
                        runExperiment.send();
                    })
            );
        }
    }
}
