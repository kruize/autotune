package com.autotune.experiment_manager;

import org.json.JSONArray;
import org.json.JSONObject;

public class ContainerConfig
{
	public static Config getConfig(JSONArray updateConfig) {
		double cpuRequest = -1;
		double memRequest = -1;

		for (Object object : updateConfig) {
			JSONObject tunableJson = (JSONObject) object;

			String tunableName = tunableJson.getString("tunable_name");
			double tunableValue = tunableJson.getDouble("tunable_value");

			if (tunableName.equals("memoryRequest")) {
				memRequest = tunableValue;
			}

			if (tunableName.equals("cpuRequest")) {
				cpuRequest = tunableValue;
			}
		}

		Config config = new Config();
		if (cpuRequest > 0 && memRequest > 0) {
			config.configName = "update requests and limits";
			config.layer = "container";

			JSONObject specJson = new JSONObject();
			JSONObject containerJson = new JSONObject();
			JSONObject resourceJSON = new JSONObject();
			JSONObject requestsJson = new JSONObject();
			JSONObject limitsJson = new JSONObject();

			requestsJson.put("cpu", cpuRequest);
			requestsJson.put("mem", memRequest);

			limitsJson.put("cpu", cpuRequest);
			limitsJson.put("mem", memRequest);

			resourceJSON.put("requests", requestsJson);
			resourceJSON.put("limits", limitsJson);

			containerJson.put("resources", resourceJSON);

			specJson.put(config.layer, containerJson);

			config.specJson = specJson;
			System.out.println(config.specJson.toString(4));
		}

		return config;
	}
}
