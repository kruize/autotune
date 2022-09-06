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

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.parallelengine.executor.AutotuneExecutor;
import com.autotune.common.parallelengine.queue.AutotuneQueue;
import com.autotune.experimentManager.data.ExperimentDetailsMap;
import com.autotune.experimentManager.utils.EMConstants.EMJSONKeys;
import com.autotune.experimentManager.utils.EMConstants.ParallelEngineConfigs;
import com.autotune.experimentManager.workerimpl.IterationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Context Initializer to initialize variables used across modules.
 */
public class InitiateListener implements ServletContextListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitiateListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ExperimentDetailsMap<String, ExperimentTrial> experimentDetailsMap = (ExperimentDetailsMap<String, ExperimentTrial>) sce.getServletContext().getAttribute(EMJSONKeys.EM_STORAGE_CONTEXT_KEY);
        if (experimentDetailsMap == null) {
            experimentDetailsMap = new ExperimentDetailsMap<>();
            /**
             * LocalStorage declaration for experiments.
             */
            sce.getServletContext().setAttribute(EMJSONKeys.EM_STORAGE_CONTEXT_KEY, experimentDetailsMap);
            sce.getServletContext().setAttribute(EMJSONKeys.EM_REGISTERED_DEPLOYMENTS, new ArrayList<String>());
        }
        AutotuneQueue<ExperimentTrial> emQueue = new AutotuneQueue<>(20000);
        /**
         * Thread pool executor declaration for Experiment Manager
         */
        AutotuneExecutor EMExecutor = new AutotuneExecutor(ParallelEngineConfigs.EM_CORE_POOL_SIZE,
                ParallelEngineConfigs.EM_MAX_POOL_SIZE,
                ParallelEngineConfigs.EM_CORE_POOL_KEEPALIVETIME_IN_SECS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.AbortPolicy(),
                emQueue,
                IterationManager.class,
                ParallelEngineConfigs.EM_DELAY_IN_SECS);
        sce.getServletContext().setAttribute(ParallelEngineConfigs.EM_EXECUTOR, EMExecutor);

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
