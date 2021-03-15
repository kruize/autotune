package com.autotune.recommendation_manager;

import com.autotune.Autotune;
import com.autotune.DeploymentInfo;
import com.autotune.dependencyAnalyzer.util.DAConstants;
import com.autotune.em.utils.EMUtils;
import com.autotune.queue.AutotuneDTO;
import com.autotune.queueprocessor.QueueProcessorImpl;
import com.autotune.recommendation_manager.service.GetExperimentJson;
import com.autotune.util.HttpUtil;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RecommendationManager implements Runnable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationManager.class);
	private static final int MAX_NUMBER_OF_TRIALS = 5;
	public static HashMap<String, ApplicationSearchSpace> applicationSearchSpaceMap = new HashMap<>();
	public static Map<String, Map <String, Double>> tunablesMap = new HashMap<>();
	private static int trialNumber;

	public static void start(ServletContextHandler contextHandler) {
		contextHandler.addServlet(GetExperimentJson.class, "/listExperiments");
		RecommendationManager recommendationManager = new RecommendationManager();
		Thread recMgrThread = new Thread(recommendationManager);
		recMgrThread.start();
	}

	@Override
	public void run() {
		while (true) {
			QueueProcessorImpl queueProcessorImpl = new QueueProcessorImpl();
			AutotuneDTO autotuneDTO = queueProcessorImpl.receive(EMUtils.QueueName.RECMGRQUEUE.name());
			LOGGER.info("Received autotuneDTO: {}", autotuneDTO.toString());
			if (autotuneDTO.getName().equals("ListAppTunables")) {
				LOGGER.info("RecommendationManager: Updating searchspace");
				updateSearchSpace(autotuneDTO.getUrl());
				try {
					startExperiment();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}

			else {
				// Obtained success message from experiment manager, send back POST request to python module with status
				LOGGER.info("RecommendationManager: Sending result of trial to ML module");
				JSONObject jsonObject = null;
				for (String id : RecommendationManager.tunablesMap.keySet()) {
					jsonObject = new JSONObject();
					jsonObject.put("id", id);
					jsonObject.put("trial_number", trialNumber);
					jsonObject.put("trial_result", "success");
					jsonObject.put("result_value_type", "double");
					jsonObject.put("result_value", 120.41);
					jsonObject.put("operation", "EXP_TRIAL_RESULT");
				}

				final String pythonEndpoint = "http://localhost:8085";
				final String experimentTrials = pythonEndpoint + "/experiment_trials";

				try {
					String result = HttpUtil.postRequest(new URL(experimentTrials), jsonObject.toString());
					LOGGER.info("API 2 POST response: {}", result);
					if (trialNumber < MAX_NUMBER_OF_TRIALS - 1) {
						startExperiment();
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}

			}
		}

	}

	private static void startExperiment() throws MalformedURLException {
		for (String id : applicationSearchSpaceMap.keySet()) {
			String operation;
			if (tunablesMap.containsKey(id)) {
				operation = "EXP_TRIAL_GENERATE_SUBSEQUENT";
			} else {
				operation = "EXP_TRIAL_GENERATE_NEW";
			}

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("id", id);
			jsonObject.put("url", "http://" + Autotune.server.getURI().getHost() + ":" + Autotune.server.getURI().getPort() + "/searchSpace");
			jsonObject.put("operation", operation);

			final String pythonEndpoint = "http://localhost:8085";
			final String experimentTrials = pythonEndpoint + "/experiment_trials";

			RecommendationManager.trialNumber = Integer.parseInt(HttpUtil.postRequest(new URL(experimentTrials), jsonObject.toString()));
			LOGGER.info("TrialNumber is: {}", trialNumber);
			if (trialNumber >= 0) {
				// success
				String tunablesJson = HttpUtil.getDataFromURL(new URL(experimentTrials + "?id=" + id + "&trial_number=" + trialNumber), "");
				LOGGER.info("Trial json received from python: {}" + tunablesJson);
				parseTunablesForID(tunablesJson, id);
				runExperiment();
			}

			/*
			HPO_TRIAL_JSON -> getDataFromURL
			containerModule(HPO_TRIAL_JSON)
			hotspot(HPO_TRIAL_JSON)
			.
			.
			.
			EM_TRIAL_JSON
			 */
		}
	}

	// Send URL of /getExperiments to experiment manager to run all experiments in list
	private static void runExperiment() {
		QueueProcessorImpl queueProcessorImpl = new QueueProcessorImpl();

		AutotuneDTO autotuneDTO = new AutotuneDTO();
		autotuneDTO.setName("Run experiment");
		autotuneDTO.setUrl(DAConstants.HTTP_PROTOCOL + "://" + Autotune.server.getURI().getHost() + ":" + Autotune.server.getURI().getPort() + "/getExperiments");
		queueProcessorImpl.send(autotuneDTO, EMUtils.QueueName.EXPMGRQUEUE.name());
	}

	private static void parseTunablesForID(String tunablesJson, String id) {
		JSONArray tunablesArray = new JSONArray(tunablesJson);
		if (!tunablesMap.containsKey(id)) {
			tunablesMap.put(id, new HashMap<>());
		}
		for (Object tunableObject : tunablesArray) {
			JSONObject tunable = (JSONObject) tunableObject;
			String tunableName = tunable.getString("tunable_name");
			// TODO What if value is a string? Change to object
			double tunableValue = tunable.getDouble("tunable_value");

			tunablesMap.get(id).put(tunableName, tunableValue);
		}
	}

	private void updateSearchSpace(String url) {
		try {
			LOGGER.info("URL for listAppTunables is {} ", url);
			URL listAppTunables = new URL(url);
			String response = HttpUtil.getDataFromURL(listAppTunables, DeploymentInfo.getAuthToken());

			JSONArray jsonArray = new JSONArray(response);
			for (Object jsonObject : jsonArray) {
				JSONObject applicationJson = (JSONObject) jsonObject;
				String applicationName = applicationJson.getString(RMConstants.APPLICATION_NAME);
				String id = applicationJson.getString("id");
				String objectiveFunction = applicationJson.getString("objective_function");
				String hpoAlgoImpl = applicationJson.getString("hpo_algo_impl");
				String direction = applicationJson.getString("direction");
				String valueTypeMain = "double";

				ArrayList<ApplicationTunable> tunables = new ArrayList<>();
				JSONArray layersArray = applicationJson.getJSONArray(RMConstants.LAYERS);
				for (Object layer : layersArray) {
					JSONObject layerJson = (JSONObject) layer;
					JSONArray tunablesArray = layerJson.getJSONArray("tunables");
					for (Object tunablesObject : tunablesArray) {
						JSONObject tunablesJson = (JSONObject) tunablesObject;
						String valueType = tunablesJson.getString("value_type");
						double lowerBound = tunablesJson.getDouble("lower_bound");
						double upperBound = tunablesJson.getDouble("upper_bound");
						String name = tunablesJson.getString("name");
						double step = tunablesJson.getDouble("step");
						String queryURL = tunablesJson.getString("query_url");

						ApplicationTunable applicationTunable = new ApplicationTunable(name,
								valueType,
								lowerBound,
								upperBound,
								step, queryURL);

						tunables.add(applicationTunable);
					}
				}

				ApplicationSearchSpace applicationSearchSpace = new ApplicationSearchSpace(id,
						applicationName,
						objectiveFunction,
						hpoAlgoImpl,
						direction,
						valueTypeMain,
						tunables);

				applicationSearchSpaceMap.put(id, applicationSearchSpace);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
}
