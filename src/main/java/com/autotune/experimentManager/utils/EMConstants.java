package com.autotune.experimentManager.utils;

public class EMConstants {
    public static class APIPaths {
        public static final int PORT = 8080;
        public static final String ROOT = "/";
        public static final String CREATE_EXPERIMENT = ROOT + "createExperiment";
        public static final String GET_EXPERIMENTS = ROOT + "getExperiments";
    }

    public static class DeploymentConstants {
        public static String NAMESPACE = "default";
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
}
