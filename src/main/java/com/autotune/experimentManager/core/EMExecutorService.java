package com.autotune.experimentManager.core;

import com.autotune.experimentManager.data.ExperimentTrialData;

import java.util.concurrent.*;

public class EMExecutorService {

    private static EMExecutorService emExecutorService = null;
    private ExecutorService emExecutor;
    private ScheduledExecutorService emScheduledExecutor;

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
            emExecutor = Executors.newFixedThreadPool(poolSize);
        }
        if (emScheduledExecutor == null) {
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
            return emExecutor.submit(trial);
        }
        return null;
    }

    public Future<String> scheduledExecute(Callable<String> trial, int delayInSecs, TimeUnit seconds) {
        if (null != emScheduledExecutor) {
            return emScheduledExecutor.schedule(trial, delayInSecs, seconds);
        }
        return null;
    }

    public Future initiateExperimentStageProcessor(Callable stageProcessor) {
        if (null != emExecutor) {
            return emExecutor.submit(stageProcessor);
        }
        return null;
    }

}
