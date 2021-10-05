package com.autotune.analyzer;

import com.autotune.analyzer.experiments.ExperimentTrial;
import com.autotune.analyzer.utils.HttpUtils;
import com.autotune.utils.ServerContext;
import com.autotune.utils.TrialHelpers;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URL;

import static com.autotune.utils.ServerContext.EXPERIMENT_MANAGER_CREATE_TRIAL_END_POINT;
import static com.autotune.utils.ServerContext.OPTUNA_TRIALS_END_POINT;

public class RunExperiment implements Runnable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(RunExperiment.class);
	private static final int MAX_NUMBER_OF_TRIALS = 1;
	AutotuneExperiment autotuneExperiment;

	public RunExperiment(AutotuneExperiment autotuneExperiment) {
		this.autotuneExperiment = autotuneExperiment;
	}

	@Override
	public void run() {

		String operation = "EXP_TRIAL_GENERATE_NEW";
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", autotuneExperiment.getExperimentId());
		jsonObject.put("url", ServerContext.SEARCH_SPACE_END_POINT);
		jsonObject.put("operation", operation);

		for (int i = 0; i<MAX_NUMBER_OF_TRIALS; i++) {
			try {
				URL experimentTrialsURL = new URL(OPTUNA_TRIALS_END_POINT);
				autotuneExperiment.setExperimentStatus("[ ]: Getting Experiment Trial Config");
				/* STEP 1: Send a request for a trail config from Optuna */
				int trialNumber = Integer.parseInt(HttpUtils.postRequest(experimentTrialsURL, jsonObject.toString()));
				autotuneExperiment.setExperimentStatus("[ " + trialNumber + " ]: Received Experiment Trial Config");
				System.out.println("Optuna Trial No :" + trialNumber);
				String addExperimentId = "?id=" + autotuneExperiment.getExperimentId();
				String addTrialNumber = "&trial_number=" + trialNumber;
				String trialConfigEP = OPTUNA_TRIALS_END_POINT + addExperimentId + addTrialNumber;
				URL trialConfigURL = new URL(trialConfigEP);

				/* STEP 2: We got a trial id from Optuna, now use that to get the actual config */
				String trialConfigJson = HttpUtils.getDataFromURL(trialConfigURL, "");
				autotuneExperiment.setExperimentStatus("[ " + trialNumber + " ]: Received Experiment Trial Config Info");
				System.out.println(trialConfigJson);

				/* STEP 3: Now create a trial to be passed to experiment manager to run */
				ExperimentTrial experimentTrial = TrialHelpers.createDefaultExperimentTrial(trialNumber,
						autotuneExperiment,
						trialConfigJson);
				autotuneExperiment.experimentTrials.add(experimentTrial);
				JSONObject experimentTrialJSON = TrialHelpers.experimentTrialToJSON(experimentTrial);

				autotuneExperiment.setExperimentStatus("[ " + trialNumber + " ]: Sending Experiment Trial Config Info to EM");
				System.out.println(experimentTrialJSON.toString(4));
				URL createExperimentTrialURL = new URL(EXPERIMENT_MANAGER_CREATE_TRIAL_END_POINT);
				String runId = HttpUtils.postRequest(createExperimentTrialURL, experimentTrialJSON.toString());
				autotuneExperiment.setExperimentStatus("[ " + trialNumber + " ]: Received Run Id: " + runId);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}