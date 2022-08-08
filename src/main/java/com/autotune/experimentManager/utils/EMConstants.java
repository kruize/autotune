package com.autotune.experimentManager.utils;

import java.util.Locale;

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
		public static final String INITIATE_TRIAL_RUN_PHASE = "com.autotune.experimentManager.transitions.TransitionToInitiateTrialRunPhase";
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
        public static final String CONTAINERS = "containers";
		public static final String IMAGE_NAME = "image_name";
		public static final String CONTAINER_NAME = "container_name";
        public static final String ITERATIONS = "iterations";

        // Info section
		public static String INFO = "info";
		public static String TRIAL_INFO = "trial_info";
		public static String DATASOURCE_INFO = "datasource_info";
		public static String TRIAL_ID = "trial_id";
		public static String TRIAL_NUM = "trial_num";
		public static String TRIAL_RESULT_URL = "trial_result_url";
		public static String URL = "url";
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
		public static String EXPERIMENT_NAME = "experiment_name";

		// Deployments Section
		public static String DEPLOYMENTS = "deployments";
		public static String NAMESPACE = "namespace";
		public static String METRICS = "metrics";
		public static String POD_METRICS = "pod_metrics";
		public static String CONTAINER_METRICS = "container_metrics";
		public static String CONFIG = "config";
		public static String NAME = "name";
		public static String QUERY = "query";
		public static String DATASOURCE = "datasource";
		public static String METRIC_INFO = "metric_info";
		public static String METRICS_RESULTS = "metrics_results";
		public static String WARMUP_RESULTS = "warmup_results";
		public static String MEASUREMENT_RESULTS = "measurement_results";
		public static String ITERATION_RESULT = "iteration_result";
		public static String GENERAL_INFO = "general_info";
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
		public static String PERCENTILE_INFO = "percentile_info";
	}

	public static class InputJsonKeys {
		private InputJsonKeys() { }
		public static class ListTrialStatusKeys {
			private ListTrialStatusKeys() { }
			public static String RUN_ID ="runId";
			public static String STATUS = "status";
			public static String ERROR = "error";
			public static String SUMMARY = "summary";
			public static String COMPLETE_STATUS = "completeStatus";
			public static String EXPERIMENT_NAME = "experiment_name";
			public static String TRIAL_NUM = "trial_num";
			public static String VERBOSE = "verbose";
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

	public static class TimeUnitsExt {
		private TimeUnitsExt() { }

		public static String SECOND_LC_SINGULAR = "second";
		public static String SECOND_LC_PLURAL = SECOND_LC_SINGULAR + "s";
		public static String SECOND_UC_SINGULAR = SECOND_LC_SINGULAR.toUpperCase();
		public static String SECOND_UC_PLURAL = SECOND_LC_PLURAL.toUpperCase();

		public static String SECOND_SHORT_LC_SINGULAR = "sec";
		public static String SECOND_SHORT_LC_PLURAL = SECOND_SHORT_LC_SINGULAR = "s";
		public static String SECOND_SHORT_UC_SINGULAR = SECOND_SHORT_LC_SINGULAR.toUpperCase();
		public static String SECOND_SHORT_UC_PLURAL = SECOND_SHORT_LC_PLURAL.toUpperCase();

		public static String SECOND_SINGLE_LC = "s";
		public static String SECOND_SINGLE_UC= SECOND_SINGLE_LC.toUpperCase();

		public static String MINUTE_LC_SINGULAR = "minute";
		public static String MINUTE_LC_PLURAL = MINUTE_LC_SINGULAR = "s";
		public static String MINUTE_UC_SINGULAR = MINUTE_LC_SINGULAR.toUpperCase();
		public static String MINUTE_UC_PLURAL = MINUTE_LC_PLURAL.toUpperCase();

		public static String MINUTE_SHORT_LC_SINGULAR = "min";
		public static String MINUTE_SHORT_LC_PLURAL = MINUTE_SHORT_LC_SINGULAR = "s";
		public static String MINUTE_SHORT_UC_SINGULAR = MINUTE_SHORT_LC_SINGULAR.toUpperCase();
		public static String MINUTE_SHORT_UC_PLURAL = MINUTE_SHORT_LC_PLURAL.toUpperCase();

		public static String MINUTE_SINGLE_LC = "m";
		public static String MINUTE_SINGLE_UC= MINUTE_SINGLE_LC.toUpperCase();

		public static String HOUR_LC_SINGULAR = "hour";
		public static String HOUR_LC_PLURAL = HOUR_LC_SINGULAR = "s";
		public static String HOUR_UC_SINGULAR = HOUR_LC_SINGULAR.toUpperCase();
		public static String HOUR_UC_PLURAL = HOUR_LC_PLURAL.toUpperCase();

		public static String HOUR_SHORT_LC_SINGULAR = "hr";
		public static String HOUR_SHORT_LC_PLURAL = HOUR_SHORT_LC_SINGULAR = "s";
		public static String HOUR_SHORT_UC_SINGULAR = HOUR_SHORT_LC_SINGULAR.toUpperCase();
		public static String HOUR_SHORT_UC_PLURAL = HOUR_SHORT_LC_PLURAL.toUpperCase();

		public static String HOUR_SINGLE_LC = "h";
		public static String HOUR_SINGLE_UC= HOUR_SINGLE_LC.toUpperCase();
	}

	public static class TimeConv {
		private TimeConv() { }
		public static int NO_OF_SECONDS_PER_MINUTE = 60;
		public static int NO_OF_MINUTES_PER_HOUR = 60;
		public static int NO_OF_HOURS_PER_DAY = 12;
	}

	public static class EMJSONValueDefaults {
		private EMJSONValueDefaults() { }
		public static String TRIAL_ID_DEFAULT = "";
		public static int TRIAL_NUM_DEFAULT = -1;
		public static String TRIAL_RESULT_URL_DEFAULT = "";
		public static String DEFAULT_NULL = null;
		public static String DEPLOYMENT_TYPE_DEFAULT = "rollingUpdate";
	}

	public static class StandardDefaults {
		private StandardDefaults() { }
		public static int NEGATIVE_INT_DEFAULT = -1;
		public static String CPU_QUERY_NAME = "cpuRequest";
		public static String MEM_QUERY_NAME = "memRequest";
		public static String THROUGHPUT = "throughput";
		public static String RESPONSE_TIME = "response_time";
	}
}
