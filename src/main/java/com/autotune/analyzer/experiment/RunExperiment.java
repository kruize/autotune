package com.autotune.analyzer.experiment;

import com.autotune.analyzer.application.ApplicationSearchSpace;
import com.autotune.common.trials.ExperimentTrial;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

import static com.autotune.analyzer.experiment.loop.EMInterface.*;
import static com.autotune.analyzer.experiment.loop.HPOInterface.getTrialFromHPO;
import static com.autotune.analyzer.experiment.loop.HPOInterface.postTrialResultToHPO;
import static com.autotune.analyzer.utils.ServiceHelpers.addApplicationToSearchSpace;
import static com.autotune.utils.KruizeConstants.HpoOperations.*;
import static com.autotune.utils.KruizeConstants.JSONKeys.*;
import static com.autotune.utils.ServerContext.HPO_TRIALS_END_POINT;

/**
 *
 */
public class RunExperiment implements Runnable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(RunExperiment.class);
	private final KruizeExperiment kruizeExperiment;

	public RunExperiment(KruizeExperiment kruizeExperiment) {
		this.kruizeExperiment = kruizeExperiment;
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
		String experimentName = kruizeExperiment.getAutotuneObject().getExperimentName();
		ApplicationSearchSpace applicationSearchSpace = kruizeExperiment.getApplicationSearchSpace();
		JSONArray searchSpaceJsonArray = new JSONArray();
		addApplicationToSearchSpace(searchSpaceJsonArray, applicationSearchSpace);

		JSONObject hpoTrial = new JSONObject();
		hpoTrial.put(SEARCHSPACE, searchSpaceJsonArray.get(0));
		hpoTrial.put(OPERATION, EXP_TRIAL_GENERATE_NEW);

		URL experimentTrialsURL = null;
		try {
			experimentTrialsURL = new URL(HPO_TRIALS_END_POINT);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		for (int i = 0; i< kruizeExperiment.getExperimentSummary().getTotalTrials(); i++) {
			try {
				// Request a new trial config from HPO and return a trial config
				ExperimentTrial experimentTrial = getTrialFromHPO(kruizeExperiment, experimentTrialsURL, hpoTrial);

				// Now send the trial to EM to actually deploy it
				SendTrialToEM(kruizeExperiment, experimentTrial);

				// Now wait for the results to be posted by EM
				receive();

				// Now process the result from EM
				ProcessTrialResultFromEM(kruizeExperiment, experimentTrial);

				// POST the result back to HPO
				postTrialResultToHPO(kruizeExperiment, experimentTrial, experimentTrialsURL);

				// Now get a subsequent config from HPO for a fresh trial
				hpoTrial.remove(OPERATION);
				hpoTrial.remove(SEARCHSPACE);
				hpoTrial.remove(EXPERIMENT_NAME);
				hpoTrial.put(EXPERIMENT_NAME, experimentName);
				hpoTrial.put(OPERATION, EXP_TRIAL_GENERATE_SUBSEQUENT);

				Thread.sleep(1000);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
