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

package com.autotune.experimentmanager.fsm.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.autotune.experimentmanager.fsm.api.EMEventHandler;
import com.autotune.experimentmanager.fsm.events.RecommendedConfigReceiveEvent;
import com.autotune.experimentmanager.fsm.object.ExperimentTrialObject;
import com.autotune.experimentmanager.fsm.object.MetricObject;
import com.autotune.experimentmanager.utils.EMUtils;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * This class will iterate the input JSON objects and create a modified existing
 * app Deploying object set a trial deployment to the ExperimentTrialObject.
 * 
 * @author Bipin Kumar
 *
 */
public class ConfigurationReceiverHandler implements EMEventHandler<RecommendedConfigReceiveEvent> {

	@Override
	public void handleEvent(RecommendedConfigReceiveEvent event) throws Exception {

		ExperimentTrialObject data = event.getData();
		if (data != null) {
			data = getExperimentTrialObject(data);
			System.out.println("Configuration recieved successfully");
		} else {
			System.out.println("Event input data is set to null");
		}

	}

	private ExperimentTrialObject getExperimentTrialObject(ExperimentTrialObject trialInput) throws IOException {

//		JSONObject recJSONObject = AutotuneUtil.retriveDataFromURL(data.getURL());
		//TODO after integration you must remove the below line and uncomment the above line.
		JSONObject recJSONObject = EMUtils.getFileFromResourceAsStream(ConfigurationReceiverHandler.class,"em_input.json");

		JSONObject trialObj = recJSONObject.getJSONObject(EMUtils.TRIALS);
		String deploymentName = trialObj.getString(EMUtils.DEPLOYMENT_NAME_KEY);
		JSONArray updateConfig = trialObj.getJSONArray(EMUtils.UPDATE_CONFIG);

		// create ExperimentTrialsInput object
		trialInput.setId(trialObj.getLong(EMUtils.ID));
		trialInput.setAppVersion(trialObj.getString(EMUtils.APP_VERSION));
		trialInput.setDeploymentName(trialObj.getString(EMUtils.DEPLOYMENT_NAME));
		trialInput.setTrialNumber(trialObj.getInt(EMUtils.TRIAL_NUM));
		trialInput.setTrialRun(trialObj.getString(EMUtils.TRIAL_RUN));
		trialInput.setTrialMeasurementTime(trialObj.getString(EMUtils.TRIAL_MEASUREMENT_TIME));

		List<MetricObject> metricList = new ArrayList<MetricObject>();

		JSONArray metricsArray = trialObj.getJSONArray(EMUtils.METRICS);
		for (int i = 0; i < metricsArray.length(); i++) {
			JSONObject metricJSONOjb = metricsArray.getJSONObject(i);
			MetricObject metricObject = new MetricObject();
			metricObject.setName(metricJSONOjb.getString(EMUtils.NAME));
			metricObject.setQuery(metricJSONOjb.getString(EMUtils.QUERY));
			metricObject.setDataSource(metricJSONOjb.getString(EMUtils.DATASOURCE));

			metricList.add(metricObject);
		}

		trialInput.setMetricsObjects(metricList);
		Deployment deployment = getAppModifiedDeployment(EMUtils.NAMESPACE, deploymentName, updateConfig);
		trialInput.setTrialDeployment(deployment);

		return trialInput;

	}

	private Deployment getAppModifiedDeployment(String nameSpace, String deploymentName, JSONArray updateConfig) {

		// create client and deployment
		KubernetesClient client = new DefaultKubernetesClient();
		Deployment deployment = client.apps().deployments().inNamespace(nameSpace).withName(deploymentName).get();

		Container deployedAppContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
		// Itererating the update_config and

		for (int i = 0; i < updateConfig.length(); i++) {
			JSONObject obj = updateConfig.getJSONObject(i);
			JSONObject containerObj = obj.getJSONObject("spec").getJSONObject("template").getJSONObject("spec")
					.getJSONObject("container");
			ResourceRequirements resourcesRequirement = new ResourceRequirements();
			if (containerObj.has("resources")) {
				resourcesRequirement = deployedAppContainer.getResources();
				JSONObject resourcesRec = containerObj.getJSONObject("resources");
				Map<String, Quantity> propertiesMap = new HashMap<String, Quantity>();
				JSONObject requests = resourcesRec.getJSONObject("requests");
				if (requests != null) {
					Iterator<String> keysItr = requests.keys();
					while (keysItr.hasNext()) {
						String key = keysItr.next();
						String value = requests.get(key).toString();
						propertiesMap.put(key, new Quantity(value));
					}

					resourcesRequirement.setRequests(propertiesMap);

				}
				propertiesMap.clear();
				JSONObject limits = resourcesRec.getJSONObject("limits");
				if (limits != null) {
					Iterator<String> keysItr = limits.keys();
					while (keysItr.hasNext()) {
						String key = keysItr.next();
						String value = requests.get(key).toString();
						propertiesMap.put(key, new Quantity(value));
					}

					resourcesRequirement.setLimits(propertiesMap);
				}
				deployedAppContainer.setResources(resourcesRequirement);
			}
			if (containerObj.has("env")) {
				JSONObject recommendedEnv = containerObj.getJSONObject("env");
				List<EnvVar> envList = new ArrayList<EnvVar>();
				Iterator<String> recIter = recommendedEnv.keys();
				while (recIter.hasNext()) {
					String key = recIter.next();
					String value = recommendedEnv.getString(key);

					// setting env. variables
					EnvVar arg = new EnvVar();
					arg.setName(key);
					arg.setValue(value);
					envList.add(arg);
				}
				deployedAppContainer.setEnv(envList);
			}

		}
		Container originalContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
		originalContainer = deployedAppContainer;

		return deployment;
	}

}
