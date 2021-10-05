package com.autotune.analyzer.utils;

import com.autotune.analyzer.AutotuneExperiment;
import com.autotune.analyzer.experiments.ExperimentTrial;
import com.autotune.analyzer.k8sObjects.AutotuneConfig;
import org.json.JSONArray;
import org.json.JSONObject;

public class ExperimentHelpers {

	public static JSONObject experimentToJSON(AutotuneExperiment autotuneExperiment) {

		JSONObject experimentJSON = new JSONObject();
		experimentJSON.put("Id", autotuneExperiment.getExperimentId());
		experimentJSON.put("Name", autotuneExperiment.getExperimentName());
		experimentJSON.put("Status", autotuneExperiment.getExperimentStatus());

		JSONArray appLayersJSON = new JSONArray();
		for (String applicationServiceStackName : autotuneExperiment.getApplicationServiceStack().getApplicationServiceStackLayers().keySet()) {
			AutotuneConfig autotuneConfig = autotuneExperiment.getApplicationServiceStack().getApplicationServiceStackLayers().get(applicationServiceStackName);
			JSONObject appLayersObj = new JSONObject();
			appLayersObj.put(applicationServiceStackName, autotuneConfig.getLevel());
			appLayersJSON.put(appLayersObj);
		}
		experimentJSON.put("Application Layers", appLayersJSON);

		JSONArray trialsJSONArray = new JSONArray();
		for (ExperimentTrial experimentTrial : autotuneExperiment.getExperimentTrials()) {
			JSONObject trialsJSON = new JSONObject();
			trialsJSON.put("Trial no", experimentTrial.getTrialInfo().getTrialNum());
			trialsJSONArray.put(trialsJSON);
		}
		experimentJSON.put("Experiment Trials", trialsJSONArray);

		return experimentJSON;
	}
}