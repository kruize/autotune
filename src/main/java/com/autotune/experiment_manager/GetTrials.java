package com.autotune.experiment_manager;

import com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment;
import com.autotune.dependencyAnalyzer.util.DAConstants;
import com.autotune.recommendation_manager.RecommendationManager;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class GetTrials extends HttpServlet
{
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String id = req.getParameter(DAConstants.AutotuneObjectConstants.ID);

		JSONArray outputJsonArray = new JSONArray();
		resp.setContentType("application/json");

		getTrial(outputJsonArray, id);
		resp.getWriter().println(outputJsonArray.toString(4));
	}

	public void getTrial(JSONArray outputJsonArray, String id) {
		if (id == null) {
			//No application parameter, generate search space for all applications
			for (String applicationID : RecommendationManager.applicationSearchSpaceMap.keySet()) {
				addTrial(outputJsonArray, applicationID);
			}
		} else {
			if (RecommendationManager.applicationSearchSpaceMap.containsKey(id)) {
				addTrial(outputJsonArray, id);
			}
		}

		if (outputJsonArray.isEmpty()) {
			if (AutotuneDeployment.autotuneObjectMap.isEmpty())
				outputJsonArray.put("Error: No objects of kind Autotune found!");
			else
				outputJsonArray.put("Error: Application " + id + " not found!");
		}
	}

	private void addTrial(JSONArray outputJsonArray, String id) {
		JSONObject jsonObject = new JSONObject();
		Trial trial = ExperimentManager.trialsMap.get(id);

		String applicationID = trial.getId();
		String name = trial.getDeploymentName();

		//TODO Replace trialNum hardcoding
		int trialNum = trial.getTrialNumber();

		jsonObject.put("id", applicationID);
		jsonObject.put("application_name", name);
		jsonObject.put("trial_num", trialNum);

		JSONArray updateConfigJson = new JSONArray();

		for (Config config : ExperimentManager.trialsMap.get(id).updateConfig) {
			JSONObject tunableJson = new JSONObject();
			tunableJson.put("config", config.configName);
			JSONObject specJson = new JSONObject();
			JSONObject templateJson = new JSONObject();

			templateJson.put("spec", config.specJson);
			specJson.put("template", templateJson);
			tunableJson.put("spec", specJson);
			updateConfigJson.put(tunableJson);
		}

		JSONArray queriesJsonArray = trial.getQueries();

		jsonObject.put("update_config", updateConfigJson);
		jsonObject.put("queries", queriesJsonArray);
		outputJsonArray.put(jsonObject);
	}

}
