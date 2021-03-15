package com.autotune.experiment_manager;

import org.json.JSONArray;
import org.json.JSONObject;

public class HotspotConfig
{
	public static Config getConfig(JSONArray updateConfig) {
		for (Object object : updateConfig) {
			JSONObject tunableJson = (JSONObject) object;
			// TODO Go over tunables list and see which java options suggest envs
		}

		Config config = new Config();
		config.configName = "update java envs";
		config.layer = "hotspot";

		JSONObject specJson = new JSONObject();
		JSONObject hotspotJson = new JSONObject();
		JSONObject envJson = new JSONObject();

		envJson.put("JVM_OPTIONS", "--XX:MaxInlineLevel=23");

		hotspotJson.put("env", envJson);

		specJson.put(config.layer, hotspotJson);

		config.specJson = specJson;
		System.out.println(config.specJson.toString(4));

		return config;
	}
}
