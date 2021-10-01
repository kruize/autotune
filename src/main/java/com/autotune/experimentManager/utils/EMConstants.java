package com.autotune.experimentManager.utils;

public class EMConstants {
    public static class APIPaths {
        public static final int PORT = 8080;
        public static final String ROOT = "/";
        public static final String CREATE_EXPERIMENT = ROOT + "createExperiment";
        public static final String GET_EXPERIMENTS = ROOT + "getExperiments";
        public static final String LIST_TRIAL_STATUS = ROOT + "listTrialStatus";
    }

    public static class DeploymentConstants {
        public static String NAMESPACE = "default";
    }

    public static class EMEnv {
        public static final String AUTOTUNE_MODE = "AUTOTUNE_MODE";
        public static final String EM_ONLY_MODE = "EM_ONLY";
    }

    public static class TransitionClasses {
        public static final String CREATE_CONFIG = "com.autotune.experimentManager.transitions.TransitionToCreateConfig";
        public static final String DEPLOY_CONFIG = "com.autotune.experimentManager.transitions.TransitionToDeployConfig";
        public static final String INITIATE_TRAIL_RUN_PHASE = "com.autotune.experimentManager.transitions.TransitionToInitiateTrailRunPhase";
        public static final String INITIAL_LOAD_CHECK = "com.autotune.experimentManager.transitions.TransitionToInitialLoadCheck";
        public static final String LOAD_CONSISTENCY_CHECK = "com.autotune.experimentManager.transitions.TransitionToLoadConsistencyCheck";
        public static final String INITIATE_METRICS_COLLECTION_PHASE = "com.autotune.experimentManager.transitions.TransitionToInitiateMetricsCollectionPhase";
        public static final String COLLECT_METRICS = "com.autotune.experimentManager.transitions.TransitionToCollectMetrics";
        public static final String CREATE_RESULT_DATA = "com.autotune.experimentManager.transitions.TransitionToCreateResultData";
        public static final String SEND_RESULT_DATA = "com.autotune.experimentManager.transitions.TransitionToSendResultData";
        public static final String CLEAN_OR_ROLLBACK_DEPLOYMENT = "com.autotune.experimentManager.transitions.TransitionToCleanDeployment";
    }

    public static class DeploymentStrategies {
        public static String ROLLING_UPDATE = "rollingUpdate";
        public static String NEW_DEPLOYMENT = "newDeployment";
    }

    public static class Logs {
        public static class LoggerSettings {
            public static String DEFUALT_LOG_LEVEL = "ALL";
        }
        public static class ExperimentManager {
            public static String INITIALIZE_EM = "Initializing EM";
            public static String ADD_EM_SERVLETS = "Adding EM Servlets";
        }

        public static class RunExperiment {
            public static String START_TRANSITION_FOR_RUNID = "Starting transition {} for RUN ID - {}";
            public static String END_TRANSITION_FOR_RUNID = "Ending transition {} for RUN ID - {}";
            public static String RUNNING_TRANSITION_ON_THREAD_ID = "Running Transition on Thread ID - {}";
        }

        public static class EMExecutorService {
            public static String CREATE_REGULAR_EXECUTOR = "Creating regular executor";
            public static String CREATE_SCHEDULED_EXECUTOR = "Creating scheduled executor";
            public static String START_EXECUTE_TRIAL = "Starting to execute a trial";
            public static String START_SCHEDULED_EXECUTE_TRIAL = "Starting to execute a scheduled trial";
            public static String START_STAGE_PROCESSORS = "Starting stage processors";
        }
    }

    public static class InputJsonKeys {
        public static class ListTrialStatusKeys {
            public static String RUN_ID ="runId";
            public static String STATUS = "status";
            public static String ERROR = "error";
        }

        public static class DeploymentKeys {
            public static String PARENT_DEPLOYMENT_NAME = "parent_deployment_name";
            public static String TRAINING_DEPLOYMENT_NAME = "training_deployment_name";
        }
    }

    public static class EMConfigDeployments {
        public static class DeploymentTypes {
            public static String TRAINING = "training";
            public static String PRODUCTION = "production";
        }
    }

    public static class EMConfigSettings {
        public static class TrialSettings {

        }
    }
}
