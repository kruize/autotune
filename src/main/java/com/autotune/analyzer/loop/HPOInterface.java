package com.autotune.analyzer.loop;

import com.autotune.analyzer.AutotuneExperiment;
import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.TrialDetails;
import com.autotune.utils.HttpUtils;
import com.autotune.utils.TrialHelpers;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

import static com.autotune.utils.AnalyzerConstants.ServiceConstants.*;
import static com.autotune.utils.AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME;
import static com.autotune.utils.AutotuneConstants.HpoOperations.*;
import static com.autotune.utils.AutotuneConstants.JSONKeys.*;
import static com.autotune.utils.AutotuneConstants.JSONKeys.EQUALS;
import static com.autotune.utils.ExperimentMessages.RunExperiment.*;
import static com.autotune.utils.ServerContext.HPO_TRIALS_END_POINT;

/**
 *
 */
public class HPOInterface {
	private static final Logger LOGGER = LoggerFactory.getLogger(HPOInterface.class);

	/**
	 *
	 * @param autotuneExperiment
	 * @param experimentTrialsURL
	 * @param hpoTrial
	 * @return
	 */
	public static ExperimentTrial getTrialFromHPO(AutotuneExperiment autotuneExperiment,
												  URL experimentTrialsURL,
												  JSONObject hpoTrial) {
		try {
			String experimentName = autotuneExperiment.getAutotuneObject().getExperimentName();
			autotuneExperiment.setExperimentStatus(STATUS_TRIAL_NUMBER + STATUS_GET_TRIAL_CONFIG);
			LOGGER.info(hpoTrial.toString());

			/* STEP 1: Send a request for a trial config from HPO */
			int trialNumber = Integer.parseInt(HttpUtils.postRequest(experimentTrialsURL, hpoTrial.toString()));

			autotuneExperiment.initializeTrial(trialNumber);
			autotuneExperiment.setExperimentStatus(STATUS_TRIAL_NUMBER + trialNumber + STATUS_RECEIVED_TRIAL_CONFIG);
			LOGGER.info("HPO Trial No: " + trialNumber);

			StringBuilder trialConfigUrl = new StringBuilder(HPO_TRIALS_END_POINT)
					.append(QUESTION_MARK).append(EXPERIMENT_NAME)
					.append(EQUALS).append(experimentName)
					.append(AMPERSAND).append(TRIAL_NUMBER)
					.append(EQUALS).append(trialNumber);
			java.net.URL trialConfigURL = null;
			trialConfigURL = new URL(trialConfigUrl.toString());

			/* STEP 2: We got a trial id from HPO, now use that to get the actual config */
			String trialConfigJson = HttpUtils.getDataFromURL(trialConfigURL, "");
			autotuneExperiment.setExperimentStatus(STATUS_TRIAL_NUMBER + trialNumber + STATUS_RECEIVED_TRIAL_CONFIG_INFO);
			LOGGER.debug(trialConfigJson);

			/* STEP 3: Now create a trial to be passed to experiment manager to run */
			ExperimentTrial experimentTrial = null;
			experimentTrial = TrialHelpers.createDefaultExperimentTrial(trialNumber,
						autotuneExperiment,
						trialConfigJson);

			autotuneExperiment.getExperimentTrials().put(trialNumber, experimentTrial);

			return experimentTrial;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 *
	 * @param autotuneExperiment
	 * @param experimentTrial
	 * @param experimentTrialsURL
	 */
	public static void postTrialResultToHPO(AutotuneExperiment autotuneExperiment,
											ExperimentTrial experimentTrial,
											URL experimentTrialsURL) {

		int trialNumber = experimentTrial.getTrialInfo().getTrialNum();
		int min = 1;
		int max = 10;
		double rand;

		String experimentName = autotuneExperiment.getAutotuneObject().getExperimentName();
		JSONObject sendTrialResult = new JSONObject();
		sendTrialResult.put(EXPERIMENT_NAME, experimentName);
		sendTrialResult.put(TRIAL_NUMBER, trialNumber);
		sendTrialResult.put(TRIAL_RESULT, "success");
		sendTrialResult.put(RESULT_VALUE_TYPE, "double");
		rand = Math.random() * (max - min + 1) + min;
		sendTrialResult.put(RESULT_VALUE, rand);
		sendTrialResult.put(OPERATION, EXP_TRIAL_RESULT);

		/* STEP 7: Now send the calculated result back to HPO */
		String response = HttpUtils.postRequest(experimentTrialsURL, sendTrialResult.toString());
		LOGGER.info("HPO Trial No: " + trialNumber + " result response: " + response);
		autotuneExperiment.setExperimentStatus(STATUS_TRIAL_NUMBER + trialNumber + STATUS_SENT_RESULT_TO_HPO);

		/* STEP 8: Compare and Summarize the result just obtained */
		TrialDetails trialDetails = experimentTrial.getTrialDetails().get(String.valueOf(trialNumber));
		autotuneExperiment.summarizeTrial(trialDetails);
	}

}
