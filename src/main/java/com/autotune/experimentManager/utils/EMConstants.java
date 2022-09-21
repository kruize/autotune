package com.autotune.experimentManager.utils;

public class EMConstants {

	private EMConstants() {
	}

	public static final class DeploymentConstants {
		private DeploymentConstants() {
		}

		public static final String NAMESPACE = "default";
	}

	public static final class TransitionClasses {
		private TransitionClasses() {
		}

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

	public static final class DeploymentStrategies {
		private DeploymentStrategies() {
		}

		public static final String ROLLING_UPDATE = "rollingUpdate";
		public static final String NEW_DEPLOYMENT = "newDeployment";
	}

	public static final class Logs {
		private Logs() {
		}

		public static final class LoggerSettings {
			private LoggerSettings() {
			}

			public static final String DEFAULT_LOG_LEVEL = "ALL";
		}

		public static final class ExperimentManager {
			private ExperimentManager() {
			}

			public static final String INITIALIZE_EM = "Initializing EM";
			public static final String ADD_EM_SERVLETS = "Adding EM Servlets";
		}

		public static final class RunExperiment {
			private RunExperiment() {
			}

			public static final String START_TRANSITION_FOR_RUNID = "Starting transition {} for RUN ID - {}";
			public static final String END_TRANSITION_FOR_RUNID = "Ending transition {} for RUN ID - {}";
			public static final String RUNNING_TRANSITION_ON_THREAD_ID = "Running Transition on Thread ID - {}";
		}

		public static final class EMExecutorService {
			private EMExecutorService() {
			}

			public static final String CREATE_REGULAR_EXECUTOR = "Creating regular executor";
			public static final String CREATE_SCHEDULED_EXECUTOR = "Creating scheduled executor";
			public static final String START_EXECUTE_TRIAL = "Starting to execute a trial";
			public static final String START_SCHEDULED_EXECUTE_TRIAL = "Starting to execute a scheduled trial";
			public static final String START_STAGE_PROCESSORS = "Starting stage processors";
		}
	}

	public static final class InputJsonKeys {
		private InputJsonKeys() {
		}

		public static final class ListTrialStatusKeys {
			private ListTrialStatusKeys() {
			}

			public static final String RUN_ID = "runId";
			public static final String STATUS = "status";
			public static final String ERROR = "error";
			public static final String SUMMARY = "summary";
			public static final String COMPLETE_STATUS = "completeStatus";
			public static final String EXPERIMENT_NAME = "experiment_name";
			public static final String TRIAL_NUM = "trial_num";
			public static final String VERBOSE = "verbose";
		}

		public static final class DeploymentKeys {
			private DeploymentKeys() {
			}

			public static final String PARENT_DEPLOYMENT_NAME = "parent_deployment_name";
			public static final String TRAINING_DEPLOYMENT_NAME = "training_deployment_name";
		}
	}

	public static final class EMConfigDeployments {
		private EMConfigDeployments() {
		}

		public static final class DeploymentTypes {
			private DeploymentTypes() {
			}

			public static final String TRAINING = "training";
			public static final String PRODUCTION = "production";
		}
	}

	public static final class EMConfigSettings {
		private EMConfigSettings() {
		}

		public static final class TrialSettings {
			private TrialSettings() {
			}
		}
	}

	public static final class EMSettings {
		private EMSettings() {
		}

		// Number of current executors per CPU core
		public static final int EXECUTORS_MULTIPLIER = 1;
		// Maximum number of executors per CPU core
		public static final int MAX_EXECUTORS_MULTIPLIER = 4;
	}

	public static final class EMExecutorService {
		private EMExecutorService() {
		}

		public static final class EMConfigSettings {
			private EMConfigSettings() {
			}

			public static final int MIN_EXECUTOR_POOL_SIZE = 1;
		}
	}

	public static final class TimeUnitsExt {
		private TimeUnitsExt() {
		}

		public static final String SECOND_LC_SINGULAR = "second";
		public static final String SECOND_LC_PLURAL = SECOND_LC_SINGULAR + "s";
		public static final String SECOND_UC_SINGULAR = SECOND_LC_SINGULAR.toUpperCase();
		public static final String SECOND_UC_PLURAL = SECOND_LC_PLURAL.toUpperCase();

		public static final String SECOND_SHORT_LC_SINGULAR = "sec";
		public static final String SECOND_SHORT_LC_PLURAL = SECOND_SHORT_LC_SINGULAR + "s";
		public static final String SECOND_SHORT_UC_SINGULAR = SECOND_SHORT_LC_SINGULAR.toUpperCase();
		public static final String SECOND_SHORT_UC_PLURAL = SECOND_SHORT_LC_PLURAL.toUpperCase();

		public static final String SECOND_SINGLE_LC = "s";
		public static final String SECOND_SINGLE_UC = SECOND_SINGLE_LC.toUpperCase();

		public static final String MINUTE_LC_SINGULAR = "minute";
		public static final String MINUTE_LC_PLURAL = MINUTE_LC_SINGULAR + "s";
		public static final String MINUTE_UC_SINGULAR = MINUTE_LC_SINGULAR.toUpperCase();
		public static final String MINUTE_UC_PLURAL = MINUTE_LC_PLURAL.toUpperCase();

		public static final String MINUTE_SHORT_LC_SINGULAR = "min";
		public static final String MINUTE_SHORT_LC_PLURAL = MINUTE_SHORT_LC_SINGULAR + "s";
		public static final String MINUTE_SHORT_UC_SINGULAR = MINUTE_SHORT_LC_SINGULAR.toUpperCase();
		public static final String MINUTE_SHORT_UC_PLURAL = MINUTE_SHORT_LC_PLURAL.toUpperCase();

		public static final String MINUTE_SINGLE_LC = "m";
		public static final String MINUTE_SINGLE_UC = MINUTE_SINGLE_LC.toUpperCase();

		public static final String HOUR_LC_SINGULAR = "hour";
		public static final String HOUR_LC_PLURAL = HOUR_LC_SINGULAR + "s";
		public static final String HOUR_UC_SINGULAR = HOUR_LC_SINGULAR.toUpperCase();
		public static final String HOUR_UC_PLURAL = HOUR_LC_PLURAL.toUpperCase();

		public static final String HOUR_SHORT_LC_SINGULAR = "hr";
		public static final String HOUR_SHORT_LC_PLURAL = HOUR_SHORT_LC_SINGULAR + "s";
		public static final String HOUR_SHORT_UC_SINGULAR = HOUR_SHORT_LC_SINGULAR.toUpperCase();
		public static final String HOUR_SHORT_UC_PLURAL = HOUR_SHORT_LC_PLURAL.toUpperCase();

		public static final String HOUR_SINGLE_LC = "h";
		public static final String HOUR_SINGLE_UC = HOUR_SINGLE_LC.toUpperCase();
	}

	public static final class TimeConv {
		private TimeConv() {
		}

		public static final int NO_OF_SECONDS_PER_MINUTE = 60;
		public static final int NO_OF_MINUTES_PER_HOUR = 60;
		public static final int NO_OF_HOURS_PER_DAY = 12;
		public static final int DEPLOYMENT_IS_READY_WITHIN_MINUTE = 2;
		public static final int DEPLOYMENT_CHECK_INTERVAL_IF_READY_MILLIS = 1000;
	}

	public static final class EMJSONValueDefaults {
		private EMJSONValueDefaults() {
		}

		public static final String TRIAL_ID_DEFAULT = "";
		public static final int TRIAL_NUM_DEFAULT = -1;
		public static final String TRIAL_RESULT_URL_DEFAULT = "";
		public static final String DEFAULT_NULL = null;
		public static final String DEPLOYMENT_TYPE_DEFAULT = "rollingUpdate";
	}

	public static final class StandardDefaults {
		private StandardDefaults() {
		}

		public static final int NEGATIVE_INT_DEFAULT = -1;
		public static final String CPU_QUERY_NAME = "cpuRequest";
		public static final String MEM_QUERY_NAME = "memRequest";
		public static final String THROUGHPUT = "throughput";
		public static final String RESPONSE_TIME = "response_time";

		public static final class BackOffThresholds {
			private BackOffThresholds() {
			}

			public static final int CHECK_LOAD_AVAILABILITY_THRESHOLD = 10;
			public static final int DEPLOYMENT_READINESS_THRESHOLD = 10;
			public static final int[] EXPONENTIAL_BACKOFF_INTERVALS = {1, 3, 4, 7, 11};
			public static final int DEFAULT_LINEAR_BACKOFF_INTERVAL = 1;
		}

		public static class EMFlowFlags {
			private EMFlowFlags() {
			}

			public static boolean DEFAULT_NEEDS_DEPLOYMENT = false;
			public static boolean DEFAULT_CHECK_LOAD = false;
			public static boolean DEFAULT_COLLECT_METRICS = false;
		}
	}
	public static class EMKeys {
		private EMKeys() { }
		public static final String EM_STORAGE_CONTEXT_KEY = "experimentDetailsMap";
		public static final String EM_REGISTERED_DEPLOYMENTS = "registeredDeployments";
		public static final String EM_KUBERNETES_SERVICE = "kubernetesService";
	}

	public static class ParallelEngineConfigs {
		private ParallelEngineConfigs() { }
		/**
		 *  MAX Queue size to stack experiments
		 */
		public static int EM_QUEUE_SIZE = 20000;
		/**
		 * Core pool size is the minimum number of workers to keep alive
		 */
		public static int EM_CORE_POOL_SIZE = 100;
		/**
		 * Maximum number of workers limit
		 */
		public static int EM_MAX_POOL_SIZE = 1000;
		/**
		 * Timeout for idle threads waiting for work. Threads use this timeout when there are more than corePoolSize present or if allowCoreThreadTimeOut. Otherwise they wait forever for new work.
		 */
		public static int EM_CORE_POOL_KEEPALIVETIME_IN_SECS = 5;
		/**
		 * the time between successive executions
		 */
		public static int EM_DELAY_IN_SECS = 2;
		public static String EM_EXECUTOR = "EM_EXECUTOR";


	}
}


