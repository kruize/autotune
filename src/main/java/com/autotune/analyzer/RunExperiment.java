package com.autotune.analyzer;

import com.autotune.analyzer.application.ApplicationSearchSpace;
import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.utils.ServerContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

import static com.autotune.analyzer.loop.EMInterface.ProcessTrialResultFromEM;
import static com.autotune.analyzer.loop.HPOInterface.getNewTrialFromHPO;
import static com.autotune.analyzer.loop.EMInterface.SendTrialToEM;
import static com.autotune.analyzer.loop.HPOInterface.postTrialResultToHPO;
import static com.autotune.analyzer.utils.ServiceHelpers.addApplicationToSearchSpace;
import static com.autotune.utils.AutotuneConstants.HpoOperations.*;
import static com.autotune.utils.AutotuneConstants.JSONKeys.*;
import static com.autotune.utils.AutotuneConstants.JSONKeys.DEPLOYMENT_NAME;
import static com.autotune.utils.AutotuneConstants.JSONKeys.URL;
import static com.autotune.utils.ServerContext.OPTUNA_TRIALS_END_POINT;

/**
 *
 */
public class RunExperiment implements Runnable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(RunExperiment.class);
	private final AutotuneExperiment autotuneExperiment;

	public RunExperiment(AutotuneExperiment autotuneExperiment) {
		this.autotuneExperiment = autotuneExperiment;
	}

	/**
	 *
	 */
	public synchronized void receive() {
		while (true) {
			try {
				wait();
				break;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				LOGGER.info("Thread Interrupted");
			}
		}
	}

	/**
	 *
	 */
	public synchronized void send() {
		notify();
	}

	/**
	 *
	 */
	@Override
	public void run() {
		String experimentName = autotuneExperiment.getAutotuneObject().getExperimentName();
		ApplicationSearchSpace applicationSearchSpace = autotuneExperiment.getApplicationSearchSpace();
		JSONArray searchSpaceJsonArray = new JSONArray();
		addApplicationToSearchSpace(searchSpaceJsonArray, applicationSearchSpace);

		JSONObject hpoTrial = new JSONObject();
		hpoTrial.put(SEARCHSPACE, searchSpaceJsonArray.get(0));
		hpoTrial.put(OPERATION, EXP_TRIAL_GENERATE_NEW);

		URL experimentTrialsURL = null;
		try {
			experimentTrialsURL = new URL(OPTUNA_TRIALS_END_POINT);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		for (int i = 0; i<autotuneExperiment.getExperimentSummary().getTotalTrials(); i++) {
			try {
				// Request a new trial config from HPO and return a trial config
				ExperimentTrial experimentTrial = getNewTrialFromHPO(autotuneExperiment, experimentTrialsURL, hpoTrial);

				// Now send the trial to EM to actually deploy it
				SendTrialToEM(autotuneExperiment, experimentTrial);

				// Now wait for the results to be posted by EM
				receive();

				// Now process the result from EM
				ProcessTrialResultFromEM(autotuneExperiment, experimentTrial);

				// POST the result back to HPO
				postTrialResultToHPO(autotuneExperiment, experimentTrial, experimentTrialsURL);

				// Now get a subsequent config from Optuna for a fresh trial
				hpoTrial.remove(OPERATION);
				hpoTrial.remove(SEARCHSPACE);
				hpoTrial.put(EXPERIMENT_NAME, experimentName);
				hpoTrial.put(OPERATION, EXP_TRIAL_GENERATE_SUBSEQUENT);

				Thread.sleep(1000);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
