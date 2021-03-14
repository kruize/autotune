package com.autotune.recommendation_manager;

import com.autotune.Autotune;
import com.autotune.DeploymentInfo;
import com.autotune.em.utils.EMUtils;
import com.autotune.queue.AutotuneDTO;
import com.autotune.queueprocessor.QueueProcessorImpl;
import com.autotune.util.HttpUtil;
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
	public static HashMap<String, ApplicationSearchSpace> applicationSearchSpaceMap = new HashMap<>();
	public static Map<String, Trial> trialMap = new HashMap<>();

	@Override
	public void run() {
		while (true) {
			QueueProcessorImpl queueProcessorImpl = new QueueProcessorImpl();
			AutotuneDTO autotuneDTO = queueProcessorImpl.receive(EMUtils.QueueName.RECMGRQUEUE.name());
			LOGGER.info("Received autotuneDTO: {}", autotuneDTO.toString());
			LOGGER.info("RecommendationManager: Updating searchspace");
			updateSearchSpace(autotuneDTO.getUrl());

			try {
				startExperiment();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			//result = sendToExperimentManager(UPDATED_JSON);
			//sendResultToPython();
		}

	}

	private void startExperiment() throws MalformedURLException {
		for (String id : applicationSearchSpaceMap.keySet()) {
			String operation;
			if (trialMap.containsKey(id)) {
				operation = "EXP_TRIAL_GENERATE_SUBSEQUENT";
			} else {
				operation = "EXP_TRIAL_GENERATE_NEW";
			}

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("id", id);
			jsonObject.put("url", "http://" + Autotune.server.getURI().getHost() + ":" + Autotune.server.getURI().getPort() + "/searchSpace");
			jsonObject.put("operation", operation);

			String trialNumber = HttpUtil.postRequest(new URL("http://localhost:8085/experiment_trials"), jsonObject.toString());
			System.out.println("TrialNumber is: " + trialNumber);
			if (Integer.getInteger(trialNumber) > 0) {
				// success
				//String trialJson = HttpUtil.getDataFromURL(new URL("http://localhost:8085/experiment_trials"), "");
				//trialMap.put(id, trialMap);
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

	private void updateSearchSpace(String url) {
		try {
			LOGGER.info("URL for listAppTunables is {} ", url);
			URL listAppTunables = new URL(url);
			String response = HttpUtil.getDataFromURL(listAppTunables, DeploymentInfo.getAuthToken());

			JSONArray jsonArray = new JSONArray(response);
			for (Object jsonObject : jsonArray) {
				JSONObject applicationJson = (JSONObject) jsonObject;
				String id = "ID";
				String value_type = "VALUE_TYPE";
				String applicationName = applicationJson.getString(RMConstants.APPLICATION_NAME);
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
						String lowerBound = tunablesJson.getString("lower_bound");
						String upperBound = tunablesJson.getString("upper_bound");
						String name = tunablesJson.getString("name");
						double step = tunablesJson.getDouble("step");

						ApplicationTunable applicationTunable = new ApplicationTunable(name,
								valueType,
								lowerBound,
								upperBound,
								step);

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
