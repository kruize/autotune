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
package com.autotune.service;

import com.autotune.analyzer.deployment.KruizeDeployment;
import com.autotune.analyzer.workerimpl.CreateExperimentManager;
import com.autotune.analyzer.workerimpl.UpdateResultManager;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.parallelengine.executor.AutotuneExecutor;
import com.autotune.common.parallelengine.queue.AutotuneQueue;
import com.autotune.common.parallelengine.worker.AutotuneWorker;
import com.autotune.common.parallelengine.worker.CallableFactory;
import com.autotune.experimentManager.data.ExperimentDetailsMap;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMConstants.ParallelEngineConfigs;
import com.autotune.experimentManager.workerimpl.IterationManager;
import com.autotune.utils.AnalyzerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Context Initializer to initialize variables used across modules.
 */
public class InitiateListener implements ServletContextListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitiateListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        /**
         * Kruize Experiment Manager thread configuration
         */
        ExperimentDetailsMap<String, ExperimentTrial> experimentDetailsMap = (ExperimentDetailsMap<String, ExperimentTrial>) sce.getServletContext().getAttribute(EMConstants.EMKeys.EM_STORAGE_CONTEXT_KEY);
        if (experimentDetailsMap == null) {
            experimentDetailsMap = new ExperimentDetailsMap<>();
            /**
             * LocalStorage declaration for experiments.
             */
            sce.getServletContext().setAttribute(EMConstants.EMKeys.EM_STORAGE_CONTEXT_KEY, experimentDetailsMap);
            sce.getServletContext().setAttribute(EMConstants.EMKeys.EM_REGISTERED_DEPLOYMENTS, new ArrayList<String>());
        }
        /**
         * Thread pool executor declaration for Experiment Manager
         */
        AutotuneExecutor EMExecutor = new AutotuneExecutor(ParallelEngineConfigs.EM_CORE_POOL_SIZE,
                ParallelEngineConfigs.EM_MAX_POOL_SIZE,
                ParallelEngineConfigs.EM_CORE_POOL_KEEPALIVETIME_IN_SECS,
                TimeUnit.SECONDS,
                new AutotuneQueue<>(ParallelEngineConfigs.EM_QUEUE_SIZE),
                new ThreadPoolExecutor.AbortPolicy(),
                IterationManager.class
        );
        sce.getServletContext().setAttribute(ParallelEngineConfigs.EM_EXECUTOR, EMExecutor);

        /**
         * Kruize Create Experiment thread configuration
         */
        sce.getServletContext().setAttribute(AnalyzerConstants.EXPERIMENT_MAP, KruizeDeployment.autotuneObjectMap);
        AutotuneExecutor analyserExecutor = new AutotuneExecutor(AnalyzerConstants.createExperimentParallelEngineConfigs.CORE_POOL_SIZE,
                AnalyzerConstants.createExperimentParallelEngineConfigs.MAX_POOL_SIZE,
                AnalyzerConstants.createExperimentParallelEngineConfigs.CORE_POOL_KEEPALIVETIME_IN_SECS,
                TimeUnit.SECONDS,
                new AutotuneQueue<>(AnalyzerConstants.createExperimentParallelEngineConfigs.QUEUE_SIZE),
                new ThreadPoolExecutor.AbortPolicy(),
                CreateExperimentManager.class
        );
        sce.getServletContext().setAttribute(AnalyzerConstants.createExperimentParallelEngineConfigs.EXECUTOR, analyserExecutor);

        ScheduledThreadPoolExecutor createExperimentExecutorScheduled = new ScheduledThreadPoolExecutor(1);
        Runnable checkForNewExperiment = () -> {
            KruizeDeployment.autotuneObjectMap.forEach(           //TOdo do pre filter where status=QUEUED before loop
                    (name, ko) -> {
                        if (ko.getStatus().equals(AnalyzerConstants.ExperimentStatus.QUEUED)) {
                            analyserExecutor.submit(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            AutotuneWorker theWorker = new CallableFactory().create(analyserExecutor.getWorker());
                                            theWorker.execute(ko, null, analyserExecutor, null);
                                        }
                                    }
                            );
                        }
                    }
            );
        };
        createExperimentExecutorScheduled.scheduleAtFixedRate(checkForNewExperiment, 5, 5, TimeUnit.SECONDS);

        /**
         *  Kruize Update results thread Configuration
         *
         */

        AutotuneExecutor updateResultExecutor = new AutotuneExecutor(AnalyzerConstants.updateResultsParallelEngineConfigs.CORE_POOL_SIZE,
                AnalyzerConstants.updateResultsParallelEngineConfigs.MAX_POOL_SIZE,
                AnalyzerConstants.updateResultsParallelEngineConfigs.CORE_POOL_KEEPALIVETIME_IN_SECS,
                TimeUnit.SECONDS,
                new AutotuneQueue<>(AnalyzerConstants.updateResultsParallelEngineConfigs.QUEUE_SIZE),
                new ThreadPoolExecutor.AbortPolicy(),
                UpdateResultManager.class
        );
        sce.getServletContext().setAttribute(AnalyzerConstants.updateResultsParallelEngineConfigs.EXECUTOR, updateResultExecutor);

        ScheduledThreadPoolExecutor updateResultsExecutorScheduled = new ScheduledThreadPoolExecutor(1);
        Runnable checkForNewResults = () -> {
            KruizeDeployment.autotuneObjectMap.forEach(           //TOdo do pre filter where status=IN_PROGRESS before loop
                    (name, ko) -> {
                        if (ko.getStatus().equals(AnalyzerConstants.ExperimentStatus.IN_PROGRESS)) {
                            if (null != ko.getResultData()) {
                                Set<ExperimentResultData> experimentResultDataSet = ko.getResultData().stream().filter((resObj) -> resObj.getStatus().equals(AnalyzerConstants.ExperimentStatus.QUEUED))
                                        .collect(Collectors.toSet());
                                experimentResultDataSet.forEach((resultDataObj) -> {
                                    updateResultExecutor.submit(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    AutotuneWorker theWorker = new CallableFactory().create(updateResultExecutor.getWorker());
                                                    theWorker.execute(ko, resultDataObj, updateResultExecutor, null);
                                                }
                                            }
                                    );
                                });
                            }
                        }
                    }
            );
        };
        updateResultsExecutorScheduled.scheduleAtFixedRate(checkForNewResults, 5, 5, TimeUnit.SECONDS);


    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
