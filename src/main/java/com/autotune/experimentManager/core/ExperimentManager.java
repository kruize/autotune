package com.autotune.experimentManager.core;

import com.autotune.experimentManager.services.ListTrialStatus;

import com.autotune.experimentManager.services.CreateExperimentTrial;
import com.autotune.experimentManager.services.ListExperimentTrail;
import com.autotune.experimentManager.settings.EMS;
import com.autotune.experimentManager.utils.EMConstants;

import com.autotune.utils.ServerContext;

import org.eclipse.jetty.servlet.ServletContextHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExperimentManager {
    public static EMExecutorService emExecutorService;
    public static EMStageProcessor emStageProcessor;
    public static EMScheduledStageProcessor emScheduledStageProcessor;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentManager.class);

    public static void initializeEM() {
        emExecutorService = EMExecutorService.getService();
        emStageProcessor = new EMStageProcessor();
        emScheduledStageProcessor = new EMScheduledStageProcessor();
        LOGGER.info(EMConstants.Logs.ExperimentManager.INITIALIZE_EM);
    }

    public static void launch(ServletContextHandler contextHandler) {
        LOGGER.info("EM version: testv1");
        initializeEM();
        addEMServlets(contextHandler);

        if (true != performEnvCheck()) {
            // Raise an exception or error
        }

        if (true != performConfigCheck()) {
            // Raise an exception or error
        }

        // Set the initial executors based on settings
        emExecutorService.createExecutors(EMS.getController().getCurrentExecutors());
        emExecutorService.initiateExperimentStageProcessor(emStageProcessor);
        emExecutorService.initiateExperimentStageProcessor(emScheduledStageProcessor);

    }

    public static boolean performEnvCheck() {
        // TODO: Read the system limit's and detect the environment
        // Parking it for future implementation
        return true;
    }

    public static boolean performConfigCheck() {
        // TODO: Read the config and change settings accordingly
        return true;
    }

    public static void notifyQueueProcessor() {
        emStageProcessor.notifyProcessor();
    }

    public static void notifyScheduledQueueProcessor() {
        emScheduledStageProcessor.notifyProcessor();
    }

    private static void addEMServlets(ServletContextHandler context) {
        LOGGER.info(EMConstants.Logs.ExperimentManager.ADD_EM_SERVLETS);
        context.addServlet(CreateExperimentTrial.class, ServerContext.EXPERIMENT_MANAGER_CREATE_TRIAL);
        context.addServlet(ListExperimentTrail.class, ServerContext.EXPERIMENT_MANAGER_LIST_EXPERIMENT_TRIAL);
        context.addServlet(ListTrialStatus.class, ServerContext.EXPERIMENT_MANAGER_LIST_TRIAL_STATUS);
    }
}
