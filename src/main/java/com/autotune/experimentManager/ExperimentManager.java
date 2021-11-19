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

package com.autotune.experimentManager;

import com.autotune.experimentManager.core.EMExecutorService;
import com.autotune.experimentManager.settings.EMSettings;
import com.autotune.experimentManager.core.EMScheduledStageProcessor;
import com.autotune.experimentManager.core.EMStageProcessor;
import com.autotune.experimentManager.utils.EMConstants;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;

import org.eclipse.jetty.servlet.ServletContextHandler;

public class ExperimentManager {
    private ExperimentManager() { }

    public static EMExecutorService emExecutorService;
    public static EMStageProcessor emStageProcessor;
    public static EMScheduledStageProcessor emScheduledStageProcessor;

    public static void initializeEM() {
        // Initializes the executor services needed by the EM
        emExecutorService = EMExecutorService.getInstance();
        emStageProcessor = new EMStageProcessor();
        emScheduledStageProcessor = new EMScheduledStageProcessor();
    }

    public static void start(ServletContextHandler contextHandler) {
        // Launches / starts the EM
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.toLevel(EMConstants.Logs.LoggerSettings.DEFAULT_LOG_LEVEL));
        initializeEM();

        // Set the initial executors based on settings
        emExecutorService.createExecutors(EMSettings.getController().getCurrentExecutors());
        emExecutorService.initiateExperimentStageProcessor(emStageProcessor);
        emExecutorService.initiateExperimentStageProcessor(emScheduledStageProcessor);
    }

    public static void notifyQueueProcessor() {
        emStageProcessor.notifyProcessor();
    }

    public static void notifyScheduledQueueProcessor() {
        emScheduledStageProcessor.notifyProcessor();
    }
}
