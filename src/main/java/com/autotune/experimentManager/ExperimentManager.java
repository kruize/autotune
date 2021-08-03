/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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
import com.autotune.experimentManager.settings.EMS;
import com.autotune.experimentManager.utils.EMConstants;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;

import org.eclipse.jetty.servlet.ServletContextHandler;

public class ExperimentManager {
    public static EMExecutorService emExecutorService;

    public static void initializeEM() {
        // Initializes the executor services needed by the EM
        emExecutorService = EMExecutorService.getService();
    }

    public static void start(ServletContextHandler contextHandler) {
        // Launches / starts the EM
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.toLevel(EMConstants.Logs.LoggerSettings.DEFUALT_LOG_LEVEL));
        initializeEM();

        // Set the initial executors based on settings
        emExecutorService.createExecutors(EMS.getController().getCurrentExecutors());
    }
}
