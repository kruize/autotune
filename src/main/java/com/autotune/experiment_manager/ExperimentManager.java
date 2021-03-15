package com.autotune.experiment_manager;

import com.autotune.Autotune;
import com.autotune.dependencyAnalyzer.util.DAConstants;
import com.autotune.em.utils.EMUtils;
import com.autotune.queue.AutotuneDTO;
import com.autotune.queueprocessor.QueueProcessorImpl;
import com.autotune.util.HttpUtil;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ExperimentManager implements Runnable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentManager.class);
	public static final Map<String, Trial> trialsMap = new HashMap<>();
	public static void start(ServletContextHandler contextHandler) {
		contextHandler.addServlet(GetTrials.class, "/listTrials");
		ExperimentManager experimentManager = new ExperimentManager();
		Thread exprManagerThread = new Thread(experimentManager);
		exprManagerThread.start();
	}

	@Override
	public void run() {
		while (true) {
			QueueProcessorImpl queueProcessorImpl = new QueueProcessorImpl();
			AutotuneDTO autotuneDTO = queueProcessorImpl.receive(EMUtils.QueueName.EXPMGRQUEUE.name());
			LOGGER.info("Received autotuneDTO: {}", autotuneDTO.toString());
			LOGGER.info("ExperimentManager: Running trials");

			try {
				updateExperimentTrials(autotuneDTO.getUrl());
				//startExperiment(); TODO RUN EXPERIMENT
				sendResultToRecommendationManager();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			//result = sendToExperimentManager(UPDATED_JSON);
			//sendResultToPython();
		}

	}

	private void sendResultToRecommendationManager() {
		QueueProcessorImpl queueProcessorImpl = new QueueProcessorImpl();

		AutotuneDTO autotuneDTO = new AutotuneDTO();
		autotuneDTO.setName("Result of experiment");
		autotuneDTO.setUrl(DAConstants.HTTP_PROTOCOL + "://" + Autotune.server.getURI().getHost() + ":" + Autotune.server.getURI().getPort() + "/getExperiments");
		queueProcessorImpl.send(autotuneDTO, EMUtils.QueueName.RECMGRQUEUE.name());

	}

	private void updateExperimentTrials(String url) throws MalformedURLException {
		String response = HttpUtil.getDataFromURL(new URL(url), "");

		JSONArray jsonArray = new JSONArray(response);

		for (Object experiment : jsonArray) {
			JSONObject experimentJson = (JSONObject) experiment;
			String id = experimentJson.getString("id");
			int trialNum = experimentJson.getInt("trial_num");
			int trialRun = 1; //TODO Get trial run
			String name = experimentJson.getString("application_name");
			JSONArray updateConfig = experimentJson.getJSONArray("update_config");

			Config containerConfig = ContainerConfig.getConfig(updateConfig);
			Config hotspotConfig = EnvConfig.getConfig(updateConfig);

			Trial trial = new Trial();
			trial.id = id;
			trial.trialNumber = trialNum;
			trial.trialRun = trialRun;
			trial.deploymentName = name;
			trial.queries = experimentJson.getJSONArray("queries");
			trial.updateConfig.add(containerConfig);
			trial.updateConfig.add(hotspotConfig);

			trialsMap.put(id, trial);
		}
	}
}
