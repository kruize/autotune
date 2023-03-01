package com.autotune.utils;

/**
 *
 */
public class ExperimentMessages {
	public static class RunExperiment {
		private RunExperiment() { }

		public static final String INITIAL_STATUS = "Pending Provisioning";
		public static final String STATUS_TRIAL_NUMBER = "Trial Number: ";
		public static final String STATUS_GET_TRIAL_CONFIG = "NEW: Getting Experiment Trial Config";
		public static final String STATUS_RECEIVED_TRIAL_CONFIG = ": Received Experiment Trial Config";
		public static final String STATUS_RECEIVED_TRIAL_CONFIG_INFO = ": Received Experiment Trial Config Info";
		public static final String STATUS_SENDING_TRIAL_CONFIG_INFO = ": Sending Experiment Trial Config Info to EM";
		public static final String STATUS_RUNNING_TRIAL = ": Running trial with EM Run Id: ";
		public static final String STATUS_SENT_RESULT_TO_HPO = ": Successfully sent result to HPO";
	}
}
