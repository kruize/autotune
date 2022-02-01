package com.autotune.analyzer.services;

import com.autotune.analyzer.AutotuneExperiment;
import com.autotune.analyzer.Experimentator;
import com.autotune.analyzer.deployment.AutotuneDeployment;
import com.autotune.analyzer.experiments.ExperimentTrial;
import com.autotune.analyzer.application.ApplicationSearchSpace;
import com.autotune.analyzer.application.Tunable;
import com.autotune.utils.TrialHelpers;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

public class ListExperiments extends HttpServlet
{
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(JSON_CONTENT_TYPE);
		response.setCharacterEncoding(CHARACTER_ENCODING);

		JSONArray experimentTrialJSONArray = new JSONArray();
		for (String experimentId : Experimentator.experimentsMap.keySet()) {
			AutotuneExperiment autotuneExperiment = Experimentator.experimentsMap.get(experimentId);
			for (ExperimentTrial experimentTrial : autotuneExperiment.getExperimentTrials()) {
				JSONObject experimentTrialJSON = TrialHelpers.experimentTrialToJSON(experimentTrial);
				experimentTrialJSONArray.put(experimentTrialJSON);
			}
		}
		response.getWriter().println(experimentTrialJSONArray.toString(4));
		response.getWriter().close();

	}

	public void getExperiment(JSONArray outputJsonArray, String experimentName) {
		if (experimentName == null) {
			// No application parameter, generate search space for all applications
			for (String expName : Experimentator.applicationSearchSpaceMap.keySet()) {
				addExperiment(outputJsonArray, expName);
			}
		} else {
			if (Experimentator.applicationSearchSpaceMap.containsKey(experimentName)) {
				addExperiment(outputJsonArray, experimentName);
			}
		}

		if (outputJsonArray.isEmpty()) {
			if (AutotuneDeployment.autotuneObjectMap.isEmpty())
				outputJsonArray.put("Error: No objects of kind Autotune found!");
			else
				outputJsonArray.put("Error: Application " + experimentName + " not found!");
		}
	}

	private void addExperiment(JSONArray outputJsonArray, String experimentName) {
		JSONObject jsonObject = new JSONObject();
		ApplicationSearchSpace applicationSearchSpace = Experimentator.applicationSearchSpaceMap.get(experimentName);

		String experimentId = applicationSearchSpace.getExperimentId();
		String name = applicationSearchSpace.getExperimentName();

		//TODO Replace trialNum hardcoding
		int trialNum = 1;

		jsonObject.put("id", experimentId);
		jsonObject.put("application_name", name);
		jsonObject.put("trial_num", trialNum);

		JSONArray updateConfigJson = new JSONArray();

		for (String tunableName : Experimentator.tunablesMap.get(experimentName).keySet()) {
			JSONObject tunableJson = new JSONObject();
			tunableJson.put("tunable_name", tunableName);
			tunableJson.put("tunable_value", Experimentator.tunablesMap.get(experimentName).get(tunableName));
			updateConfigJson.put(tunableJson);
		}

		JSONArray queriesJsonArray = new JSONArray();
		for (String applicationTunableName : applicationSearchSpace.getTunablesMap().keySet()) {
			Tunable applicationTunable = applicationSearchSpace.getTunablesMap().get(applicationTunableName);
			JSONObject queryJson = new JSONObject();
			queryJson.put("tunable_name", applicationTunable.getName());
			queriesJsonArray.put(queryJson);
		}

		jsonObject.put("update_config", updateConfigJson);
		jsonObject.put("queries", queriesJsonArray);
		outputJsonArray.put(jsonObject);
	}
}
