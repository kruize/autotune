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

package com.autotune.experimentManager.core;

import com.autotune.experimentManager.settings.EMSettings;
import com.autotune.experimentManager.utils.EMConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class EMExecutorService {

    private static EMExecutorService emExecutorService = null;
    private ExecutorService emExecutor;
    private ScheduledExecutorService emScheduledExecutor;

    private static final Logger LOGGER = LoggerFactory.getLogger(EMExecutorService.class);

    private EMExecutorService() {
        emExecutor = null;
    }

    /**
     * Initialises a singleton class object for EMExecutorService class
     * object is created in a synchronized block as we allow only one
     * object to be created
     *
     * @return emExecutorService
     */
    public static EMExecutorService getInstance() {
        if (null == emExecutorService) {
            synchronized (EMExecutorService.class) {
                if (null == emExecutorService) {
                    emExecutorService = new EMExecutorService();
                }
            }
        }
        return emExecutorService;
    }

    /**
     * Creates the Regular and Scheduled executors
     *
     * @param poolSize
     */

    public void createExecutors(int poolSize) {
        if (null == emExecutor) {
            LOGGER.info(EMConstants.Logs.EMExecutorService.CREATE_REGULAR_EXECUTOR);
            synchronized (EMExecutorService.class) {
                if (null == emExecutor) {
                    emExecutor = Executors.newFixedThreadPool(poolSize);
                }
            }
        }
        if (null == emScheduledExecutor) {
            LOGGER.info(EMConstants.Logs.EMExecutorService.CREATE_SCHEDULED_EXECUTOR);
            synchronized (EMExecutorService.class) {
                if (null == emScheduledExecutor) {
                    emScheduledExecutor = Executors.newScheduledThreadPool(poolSize);
                }
            }
        }
    }

    /**
     * Sets Max Pool Size for the executors
     * @param maxPoolSize
     */
    public void setMaxExecutors(int maxPoolSize) {
        if (maxPoolSize >= EMConstants.EMExecutorService.MIN_EXECUTOR_POOL_SIZE && maxPoolSize <= EMSettings.getController().getMaxExecutors()) {
            ((ThreadPoolExecutor) emExecutor).setMaximumPoolSize(maxPoolSize);
            ((ThreadPoolExecutor) emScheduledExecutor).setMaximumPoolSize(maxPoolSize);
        }
    }

    /**
     * Sets Current Pool Size for executors
     * @param currentPoolSize
     */
    public void setCoreExecutors(int currentPoolSize) {
        if (currentPoolSize >= EMConstants.EMExecutorService.MIN_EXECUTOR_POOL_SIZE && currentPoolSize <= EMSettings.getController().getMaxExecutors()) {
            ((ThreadPoolExecutor) emExecutor).setCorePoolSize(currentPoolSize);
            ((ThreadPoolExecutor) emScheduledExecutor).setCorePoolSize(currentPoolSize);
        }
    }

    /**
     * Executes the given callable in the regular executor
     * @param trial
     */
    public Future<String> execute(Callable<String> trial) {
        if (null != emExecutor) {
            LOGGER.info(EMConstants.Logs.EMExecutorService.START_EXECUTE_TRIAL);
            return emExecutor.submit(trial);
        }
        return null;
    }

    public Future<String> scheduledExecute(Callable<String> trial, int delayInSecs, TimeUnit seconds) {
        if (null != emScheduledExecutor) {
            LOGGER.info(EMConstants.Logs.EMExecutorService.START_SCHEDULED_EXECUTE_TRIAL);
            return emScheduledExecutor.schedule(trial, delayInSecs, seconds);
        }
        return null;
    }

    public Future initiateExperimentStageProcessor(Callable stageProcessor) {
        if (null != emExecutor) {
            LOGGER.info(EMConstants.Logs.EMExecutorService.START_STAGE_PROCESSORS);
            return emExecutor.submit(stageProcessor);
        }
        return null;
    }
}
