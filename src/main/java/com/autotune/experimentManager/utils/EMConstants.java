package com.autotune.experimentManager.utils;

public class EMConstants {

	private EMConstants() { }
	public static class DeploymentConstants {
		private DeploymentConstants() { }
		public static String NAMESPACE = "default";
	}

	public static class EMEnv {
		public static final String AUTOTUNE_MODE = "AUTOTUNE_MODE";
		public static final String EM_ONLY_MODE = "EM_ONLY";
	}

	public static class TransitionClasses {
		private TransitionClasses() { }
		public static final String CREATE_CONFIG = "com.autotune.experimentManager.transitions.TransitionToCreateConfig";
		public static final String DEPLOY_CONFIG = "com.autotune.experimentManager.transitions.TransitionToDeployConfig";
		public static final String INITIATE_TRAIL_RUN_PHASE = "com.autotune.experimentManager.transitions.TransitionToInitiateTrialRunPhase";
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

	public static class EMJSONKeys {
		private EMJSONKeys() { }
		// Info section
		public static String INFO = "info";
		public static String TRIAL_ID = "trial_id";
		public static String TRIAL_NUM = "trial_num";
		public static String TRIAL_RESULT_URL = "trial_result_url";
		// Settings section
		public static String TRACKERS = "trackers";
		public static String SETTINGS = "settings";
		public static String DEPLOYMENT_TRACKING = "deployment_tracking";
		public static String DEPLOYMENT_POLICY = "deployment_policy";
		public static String DEPLOYMENT_SETTINGS = "deployment_settings";
		public static String TYPE = "type";
		public static String DEPLOYMENT_INFO = "deployment_info";
		public static String DEPLOYMENT_NAME = "deployment_name";
		public static String TARGET_ENV = "target_env";
		public static String TRIAL_SETTINGS = "trial_settings";
		public static String TOTAL_DURATION = "total_duration";
		public static String WARMUP_CYCLES = "warmup_cycles";
		public static String WARMUP_DURATION = "warmup_duration";
		public static String MEASUREMENT_CYCLES = "measurement_cycles";
		public static String MEASUREMENT_DURATION = "measurement_duration";
		// Metadata Section
		public static String EXPERIMENT_ID = "experiment_id";
		public static String APPLICATION_NAME = "application_name";
		// Deployments Section
		public static String DEPLOYMENTS = "deployments";
		public static String NAMESPACE = "namespace";
		public static String CONTAINER_NAME = "container_name";
		public static String METRICS = "metrics";
		public static String CONFIG = "config";
		public static String NAME = "name";
		public static String QUERY = "query";
		public static String DATASOURCE = "datasource";
		public static String METRICS_RESULTS = "metrics_results";
		public static String WARMUP_RESULTS = "warmup_results";
		public static String MEASUREMENT_RESULTS = "measurement_results";
		public static String RESULTS = "results";
		public static String SCORE = "score";
		public static String ERROR = "error";
		public static String MEAN = "mean";
		public static String MODE = "mode";
		public static String SPIKE = "spike";
		public static String P_50_0 = "50p";
		public static String P_95_0 = "95p";
		public static String P_97_0 = "97p";
		public static String P_99_0 = "99p";
		public static String P_99_9 = "99.9p";
		public static String P_99_99 = "99.99p";
		public static String P_99_999 = "99.999p";
		public static String P_99_9999 = "99.9999p";
		public static String P_100_0 = "100p";
		public static String CYCLES = "cycles";
		public static String DURATION = "duration";
	}

	public static class InputJsonKeys {
		private InputJsonKeys() { }
		public static class ListTrialStatusKeys {
			private ListTrialStatusKeys() { }
			public static String RUN_ID ="runId";
			public static String STATUS = "status";
			public static String ERROR = "error";
			public static String SUMMARY = "summary";
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

	public static class EMConfigSettings {
		private EMConfigSettings() { }
		public static class TrialSettings {
			private TrialSettings() { }
		}
	}

	public static class EMSettings {
		private EMSettings() { }
		// Number of current executors per CPU core
		public static int EXECUTORS_MULTIPLIER = 1;
		// Maximum number of executors per CPU core
		public static int MAX_EXECUTORS_MULTIPLIER = 4;
	}

	public static class EMExecutorService {
		private EMExecutorService() { }
		public static class EMConfigSettings {
			private EMConfigSettings() { }
			public static int MIN_EXECUTOR_POOL_SIZE = 1;
		}
	}
}
