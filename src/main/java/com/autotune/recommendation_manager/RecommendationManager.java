package com.autotune.recommendation_manager;

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

public class RecommendationManager implements Runnable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationManager.class);
	public static HashMap<String, ApplicationSearchSpace> applicationSearchSpaceMap = new HashMap<>();

	@Override
	public void run() {
		while (true) {
			QueueProcessorImpl queueProcessorImpl = new QueueProcessorImpl();
			AutotuneDTO autotuneDTO = queueProcessorImpl.receive(EMUtils.QueueName.RECMGRQUEUE.name());
			LOGGER.info("Received autotuneDTO: {}", autotuneDTO.toString());
			LOGGER.info("RecommendationManager: Updating searchspace");
			updateSearchSpace(autotuneDTO.getUrl());
		}
	}

	private void updateSearchSpace(String url) {
		try {
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
						tunables);

				applicationSearchSpaceMap.put(applicationName, applicationSearchSpace);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
}
