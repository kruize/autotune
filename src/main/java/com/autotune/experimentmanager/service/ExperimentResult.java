/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.experimentmanager.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.autotune.experimentmanager.core.ExperimentManager;
import com.autotune.experimentmanager.fsm.object.ExperimentTrialObject;
import com.autotune.experimentmanager.fsm.object.MetricObject;
import com.autotune.experimentmanager.utils.EMUtils;

/**
 * This class generate the experiment result as service endpoint, you can get the
 * result by trailId and application name.
 * @author Bipin Kumar
 *
 * Mar 31, 2021
 */
public class ExperimentResult extends HttpServlet
{
	private static final long serialVersionUID = 1L;


	/**
	 * Returns the experiment result of performed for a recommendation.
	 *
	 * Request:
	 * `GET /experiment_result` returns the list of all applications monitored by autotune, and their layers.
	 *
	 * `GET /experiment_result?application_name=<APP_NAME>` returns result for a given experiment name.
	 *
	 * Example JSON:
	 *{
	 *  "trials": {
	 *     "id": "101383821",
	 *     "app-version": "v1",
	 *     "deployment_name": "petclinic_deployment",
	 *     "trial_num": 1,
	 *     "trial_run": "15mins",
	 *     "trial_measurement_time": "3mins",
	 *     "trial_result": "",
	 *     "trial_result_info": "",
	 *     "trial_result_error": "",
	 *     "metrics": [
	 *        {
	 *           "name": "obj_fun_var1",
	 *           "query": "obj_fun_var1_query",
	 *           "datasource": "prometheus",
	 *           "score": "",
	 *           "Error": "",
	 *           "mean": "",
	 *           "mode": "",
	 *           "95.0": "",
	 *           "99.0": "",
	 *           "99.9": "",
	 *           "99.99": "",
	 *           "99.999": "",
	 *           "99.9999": "",
	 *           "100.0": "",
	 *           "spike": ""
	 *        },
	 *        {
	 *           "name": "obj_fun_var2",
	 *           "query": "obj_fun_var2_query",
	 *           "datasource": "prometheus",
	 *           "score": "",
	 *           "Error": "",
	 *           "mean": "",
	 *           "mode": "",
	 *           "95.0": "",
	 *           "99.0": "",
	 *           "99.9": "",
	 *           "99.99": "",
	 *           "99.999": "",
	 *           "99.9999": "",
	 *           "100.0": "",
	 *           "spike": ""
	 *        },
	 *        {
	 *           "name": "obj_fun_var3",
	 *           "query": "obj_fun_var3_query",
	 *           "datasource": "prometheus",
	 *           "score": "",
	 *           "Error": "",
	 *           "mean": "",
	 *           "mode": "",
	 *           "95.0": "",
	 *           "99.0": "",
	 *           "99.9": "",
	 *           "99.99": "",
	 *           "99.999": "",
	 *           "99.9999": "",
	 *           "100.0": "",
	 *           "spike": ""
	 *        },
	 *        {
	 *           "name": "cpuRequest",
	 *           "query": "cpuRequest_query",
	 *           "datasource": "prometheus",
	 *           "score": "",
	 *           "Error": "",
	 *           "mean": "",
	 *           "mode": "",
	 *           "95.0": "",
	 *           "99.0": "",
	 *           "99.9": "",
	 *           "99.99": "",
	 *           "99.999": "",
	 *           "99.9999": "",
	 *           "100.0": "",
	 *           "spike": ""
	 *        },
	 *        {
	 *           "name": "memRequest",
	 *           "query": "memRequest_query",
	 *           "datasource": "prometheus",
	 *           "score": "",
	 *           "Error": "",
	 *           "mean": "",
	 *           "mode": "",
	 *           "95.0": "",
	 *           "99.0": "",
	 *           "99.9": "",
	 *           "99.99": "",
	 *           "99.999": "",
	 *           "99.9999": "",
	 *           "100.0": "",
	 *           "spike": ""
	 *        }
	 *    ]
	 *  }
	 *}
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		JSONObject outputJson = new JSONObject();
		resp.setContentType("application/json");
		String applicationName = req.getParameter(EMUtils.APPLICATION_NAME);
		ExperimentTrialObject trialResult = ExperimentManager.getExperimentMap(10000);
		outputJson = createJSONOutput(trialResult, applicationName);
		resp.getWriter().println(outputJson.toString());
	}


	private JSONObject createJSONOutput(ExperimentTrialObject trialsObject, String applicationName) {
		
		JSONObject outputJson = new JSONObject();
		JSONObject trials = new JSONObject("trials");
		trials.put("id", trialsObject.getId());
		trials.put("app-version", trialsObject.getAppVersion());
		trials.put("deployment_name", trialsObject.getDeploymentName());
		trials.put("trial_num", trialsObject.getTrialNumber());
		trials.put("trial_run", trialsObject.getTrialRun());
		trials.put("trial_measurement_time", trialsObject.getTrialMeasurementTime());
		trials.put("trial_result", trialsObject.getTrialResult());
		trials.put("trial_result_info", trialsObject.getTrialResultInfo());
		trials.put("trial_result_error", trialsObject.getTrialResultError());
		trials.put("trial_result", trialsObject.getTrialResult());
		JSONArray metricsArray = new JSONArray();
		List<MetricObject> metricsObjects = trialsObject.getMetricsObjects();
		for(MetricObject metricsObj: metricsObjects ) {
			Map<String, String> metrcsMap = new HashMap<String, String>();
			metrcsMap.put("name", metricsObj.getName());
			metrcsMap.put("query", metricsObj.getQuery());
			metrcsMap.put("datasource", metricsObj.getDataSource());
			metrcsMap.put("score", String.valueOf(metricsObj.getScore()));
			metrcsMap.put("Error", String.valueOf(metricsObj.getError()));
			metrcsMap.put("mean", String.valueOf(metricsObj.getMean()));
			metrcsMap.put("mode", String.valueOf(metricsObj.getMode()));
			metrcsMap.put("95.0", String.valueOf(metricsObj.getPercentile95()));
			metrcsMap.put("99.0", String.valueOf(metricsObj.getPercentile99()));
			metrcsMap.put("99.9", String.valueOf(metricsObj.getPercentile99Point9()));
			metrcsMap.put("99.99", String.valueOf(metricsObj.getPercentile99Point99()));
			metrcsMap.put("99.999", String.valueOf(metricsObj.getPercentile99Point999()));
			metrcsMap.put("99.9999", String.valueOf(metricsObj.getPercentile99Point9999()));
			metrcsMap.put("100.0", String.valueOf(metricsObj.getPercentile100()));
			metrcsMap.put("spike", String.valueOf(metricsObj.getSpike()));
			metricsArray.put(metrcsMap);
		}
		
		return outputJson;
	}
}
