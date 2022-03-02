package com.autotune.analyzer.utils;

import com.autotune.analyzer.AutotuneExperiment;
import com.autotune.analyzer.application.ApplicationServiceStack;
import com.autotune.common.data.experiments.ExperimentTrial;
import com.autotune.analyzer.k8sObjects.AutotuneConfig;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 */
public class ExperimentHelpers {

	public static JSONObject experimentToJSON(AutotuneExperiment autotuneExperiment) {

		JSONObject experimentJSON = new JSONObject();
		experimentJSON.put("Id", autotuneExperiment.getDeploymentName());
		experimentJSON.put("Name", autotuneExperiment.getExperimentName());
		experimentJSON.put("Status", autotuneExperiment.getExperimentStatus());

		JSONArray appLayersJSONArray = new JSONArray();
		for (String applicationServiceStackName : autotuneExperiment.getApplicationDeployment().getApplicationServiceStackMap().keySet()) {
			ApplicationServiceStack applicationServiceStack = autotuneExperiment.getApplicationDeployment().getApplicationServiceStackMap().get(applicationServiceStackName);
			for (String layerName : applicationServiceStack.getApplicationServiceStackLayers().keySet()) {
				AutotuneConfig autotuneConfig = applicationServiceStack.getApplicationServiceStackLayers().get(layerName);
				JSONObject appLayersObj = new JSONObject();
				appLayersObj.put(applicationServiceStackName, autotuneConfig.getLevel());
				appLayersJSONArray.put(appLayersObj);
			}
		}
		experimentJSON.put("Application Layers", appLayersJSONArray);

		JSONArray trialsJSONArray = new JSONArray();
		for (int trialNum : autotuneExperiment.getExperimentTrials().keySet()) {
			ExperimentTrial experimentTrial = autotuneExperiment.getExperimentTrials().get(trialNum);
			JSONObject trialsJSON = new JSONObject();
			trialsJSON.put("Trial no", experimentTrial.getTrialInfo().getTrialNum());
			trialsJSONArray.put(trialsJSON);
		}
		experimentJSON.put("Experiment Trials", trialsJSONArray);

		return experimentJSON;
	}
}
