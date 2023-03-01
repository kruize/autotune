package com.autotune.experimentManager.core;

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

    public static EMExecutorService getService() {
        if (emExecutorService == null) {
            emExecutorService = new EMExecutorService();
        }
        return emExecutorService;
    }

    public void createExecutors(int poolSize) {
        if (emExecutor == null) {
            LOGGER.info(EMConstants.Logs.EMExecutorService.CREATE_REGULAR_EXECUTOR);
            emExecutor = Executors.newFixedThreadPool(poolSize);
        }
        if (emScheduledExecutor == null) {
            LOGGER.info(EMConstants.Logs.EMExecutorService.CREATE_SCHEDULED_EXECUTOR);
            emScheduledExecutor = Executors.newScheduledThreadPool(poolSize);
        }
    }

    public void setMaxExecutors(int maxPoolSize) {
        ((ThreadPoolExecutor) emExecutor).setMaximumPoolSize(maxPoolSize);
        ((ThreadPoolExecutor) emScheduledExecutor).setMaximumPoolSize(maxPoolSize);
    }

    public void setCoreExecutors(int currentPoolSize) {
        ((ThreadPoolExecutor) emExecutor).setCorePoolSize(currentPoolSize);
        ((ThreadPoolExecutor) emScheduledExecutor).setCorePoolSize(currentPoolSize);
    }

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
