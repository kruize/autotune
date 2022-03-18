package com.autotune.analyzer;

import com.autotune.analyzer.k8sObjects.Metric;
import com.autotune.common.data.experiments.ExperimentTrial;
import com.autotune.common.data.experiments.TrialDetails;
import com.autotune.utils.HttpUtils;
import com.autotune.utils.ServerContext;
import com.autotune.utils.TrialHelpers;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Instant;

import static com.autotune.utils.AnalyzerConstants.ServiceConstants.DEPLOYMENT_NAME;
import static com.autotune.utils.AutotuneConstants.HpoOperations.*;
import static com.autotune.utils.ServerContext.EXPERIMENT_MANAGER_CREATE_TRIAL_END_POINT;
import static com.autotune.utils.ServerContext.OPTUNA_TRIALS_END_POINT;

public class RunExperiment implements Runnable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(RunExperiment.class);
	private final AutotuneExperiment autotuneExperiment;

	public RunExperiment(AutotuneExperiment autotuneExperiment) {
		this.autotuneExperiment = autotuneExperiment;
	}

	public synchronized void receive() {
		while (true) {
			try {
				wait();
				break;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				System.out.println("Thread Interrupted");
			}
		}
	}

	public synchronized void send() {
		notify();
	}

	@Override
	public void run() {
		int trialNumber;

		String experimentId = autotuneExperiment.getAutotuneObject().getExperimentId();
		StringBuilder searchSpaceUrl = new StringBuilder(ServerContext.SEARCH_SPACE_END_POINT)
				.append("?")
				.append(DEPLOYMENT_NAME)
				.append("=")
				.append(autotuneExperiment.getDeploymentName());
		JSONObject hpoTrial = new JSONObject();
		hpoTrial.put("id", experimentId);
		hpoTrial.put("url", searchSpaceUrl.toString());
		hpoTrial.put("operation", NEW_TRIAL);

		URL experimentTrialsURL = null;
		try {
			experimentTrialsURL = new URL(OPTUNA_TRIALS_END_POINT);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		int min = 1;
		int max = 10;
		double rand;

		for (int i = 0; i<autotuneExperiment.getExperimentSummary().getTotalTrials(); i++) {
			try {
				autotuneExperiment.setExperimentStatus("[ ]: Getting Experiment Trial Config");
				LOGGER.debug(hpoTrial.toString());

				/* STEP 1: Send a request for a trail config from Optuna */
				trialNumber = Integer.parseInt(HttpUtils.postRequest(experimentTrialsURL, hpoTrial.toString()));
				autotuneExperiment.initializeTrial(trialNumber);
				autotuneExperiment.setExperimentStatus("[ " + trialNumber + " ]: Received Experiment Trial Config");
				LOGGER.info("Optuna Trial No: " + trialNumber);
				StringBuilder trialConfigUrl = new StringBuilder(OPTUNA_TRIALS_END_POINT)
						.append("?id=")
						.append(experimentId)
						.append("&trial_number=")
						.append(trialNumber);
				URL trialConfigURL = new URL(trialConfigUrl.toString());

				/* STEP 2: We got a trial id from Optuna, now use that to get the actual config */
				String trialConfigJson = HttpUtils.getDataFromURL(trialConfigURL, "");
				autotuneExperiment.setExperimentStatus("[ " + trialNumber + " ]: Received Experiment Trial Config Info");
				LOGGER.info(trialConfigJson);

				/* STEP 3: Now create a trial to be passed to experiment manager to run */
				ExperimentTrial experimentTrial = TrialHelpers.createDefaultExperimentTrial(trialNumber,
						autotuneExperiment,
						trialConfigJson);
				autotuneExperiment.getExperimentTrials().put(trialNumber, experimentTrial);
				JSONObject experimentTrialJSON = TrialHelpers.experimentTrialToJSON(experimentTrial);

				/* STEP 4: Send trial to EM */
				autotuneExperiment.setExperimentStatus("[ " + trialNumber + " ]: Sending Experiment Trial Config Info to EM");
				LOGGER.info(experimentTrialJSON.toString(4));
				URL createExperimentTrialURL = new URL(EXPERIMENT_MANAGER_CREATE_TRIAL_END_POINT);
				String runId = HttpUtils.postRequest(createExperimentTrialURL, experimentTrialJSON.toString());
				autotuneExperiment.setExperimentStatus("[ " + trialNumber + " ]: Running trial with EM Run Id: " + runId);

				/* STEP 5: Now wait for the results to be posted by EM */
				receive();

				/* STEP 6: Received the results from EM */
				LOGGER.info("Processing trial result for " + trialNumber);

				TrialDetails trialDetails = experimentTrial.getTrialDetails().get("training");
				Metric reqSum = trialDetails.getPodMetrics().get("request_sum");
				Metric reqCount = trialDetails.getPodMetrics().get("request_count");
				trialDetails.setEndTime(Timestamp.from(Instant.now()));

				double reqSumMean = reqSum.getEmMetricResult().getEmMetricGenericResults().getMean();
				double reqCountMean = reqCount.getEmMetricResult().getEmMetricGenericResults().getMean();
				double rspTime = reqSumMean / reqCountMean;
				LOGGER.info("Calculated rspTime (" + rspTime + ") = reqSumMean (" + reqSumMean + ") / reqCountMean (" + reqCountMean + ");");

				trialDetails.setResult(String.valueOf(rspTime));
				trialDetails.setResultError("None");

				JSONObject sendTrialResult = new JSONObject();
				sendTrialResult.put("id", experimentId);
				sendTrialResult.put("trial_number", trialNumber);
				sendTrialResult.put("trial_result", "success");
				sendTrialResult.put("result_value_type", "double");
				rand = Math.random() * (max - min + 1) + min;
				sendTrialResult.put("result_value", rand);
				sendTrialResult.put("operation", TRIAL_RESULT);

				/* STEP 7: Now send the calculated result back to Optuna */
				int response = Integer.parseInt(HttpUtils.postRequest(experimentTrialsURL, sendTrialResult.toString()));
				LOGGER.info("Optuna Trial No: " + trialNumber + " result response: " + response);
				autotuneExperiment.setExperimentStatus("[ " + trialNumber + " ]: Successfully sent result to HPO");

				/* STEP 8: Compare and Summarize the result just obtained */
				autotuneExperiment.summarizeTrial(trialDetails);

				/* STEP 9: Now get a subsequent config from Optuna for a fresh trial */
				hpoTrial.remove("operation");
				hpoTrial.put("operation", CONTINUE_TRIAL);

				Thread.sleep(1000);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
}
