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

package com.autotune.experimentManager.utils;

import com.autotune.analyzer.utils.ServerContext;

public class EMConstants {
    private EMConstants() { }

    public static class APIPaths {
        private APIPaths() { }
        public static final String CREATE_EXPERIMENT_TRIAL = ServerContext.ROOT_CONTEXT + "createExperimentTrial";
        public static final String LIST_TRIAL_STATUS = ServerContext.ROOT_CONTEXT + "listTrialStatus";
    }

    public static class DeploymentConstants {
        private DeploymentConstants() { }

        public static String NAMESPACE = "default";
    }

    public static class TransitionClasses {
        private TransitionClasses() { }

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
        private DeploymentStrategies() { }

        public static String ROLLING_UPDATE = "rollingUpdate";
        public static String NEW_DEPLOYMENT = "newDeployment";
    }

    public static class Logs {
        private Logs() { }

        public static class LoggerSettings {
            private LoggerSettings() { }

            public static String DEFAULT_LOG_LEVEL = "ALL";
        }
        public static class ExperimentManager {
            private ExperimentManager() { }

            public static String INITIALIZE_EM = "Initializing EM";
            public static String ADD_EM_SERVLETS = "Adding EM Servlets";
        }

        public static class RunExperiment {
            private RunExperiment() { }

            public static String START_TRANSITION_FOR_RUNID = "Starting transition {} for RUN ID - {}";
            public static String END_TRANSITION_FOR_RUNID = "Ending transition {} for RUN ID - {}";
            public static String RUNNING_TRANSITION_ON_THREAD_ID = "Running Transition on Thread ID - {}";
        }

        public static class EMExecutorService {
            private EMExecutorService() { }

            public static String CREATE_REGULAR_EXECUTOR = "Creating regular executor";
            public static String CREATE_SCHEDULED_EXECUTOR = "Creating scheduled executor";
            public static String START_EXECUTE_TRIAL = "Starting to execute a trial";
            public static String START_SCHEDULED_EXECUTE_TRIAL = "Starting to execute a scheduled trial";
            public static String START_STAGE_PROCESSORS = "Starting stage processors";
        }
    }

    public static class InputJsonKeys {
        private InputJsonKeys() { }

        public static class GetTrailStatusInputKeys {
            private GetTrailStatusInputKeys() { }

            public static String RUN_ID = "runId";
        }

        public static class DeploymentKeys {
            private DeploymentKeys() { }

            public static String PARENT_DEPLOYMENT_NAME = "parent_deployment_name";
            public static String TRAINING_DEPLOYMENT_NAME = "training_deployment_name";
        }
    }

    public static class EMConfigDeployments {
        private EMConfigDeployments() { }

        public static class DeploymentTypes {
            private DeploymentTypes() { }

            public static String TRAINING = "training";
            public static String PRODUCTION = "production";
        }
    }

    /**
     * Constants for EMSettings class
     */
    public static class EMSettings {
        private EMSettings() { }

        // Number of current executors per CPU core
        public static int EXECUTORS_MULTIPLIER = 1;
        // Maximum number of executors per CPU core
        public static int MAX_EXECUTORS_MULTIPLIER = 4;
    }

    public static class EMExecutorService {
        private EMExecutorService() { }

        public static int MIN_EXECUTOR_POOL_SIZE = 1;
    }
}
