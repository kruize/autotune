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
package com.autotune.dependencyAnalyzer.deployment;

import com.autotune.dependencyAnalyzer.application.ApplicationServiceStack;
import com.autotune.dependencyAnalyzer.application.Tunable;
import com.autotune.dependencyAnalyzer.datasource.DataSource;
import com.autotune.dependencyAnalyzer.datasource.DataSourceFactory;
import com.autotune.dependencyAnalyzer.env.EnvInfo;
import com.autotune.dependencyAnalyzer.exceptions.InvalidBoundsException;
import com.autotune.dependencyAnalyzer.exceptions.InvalidValueException;
import com.autotune.dependencyAnalyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.dependencyAnalyzer.k8sObjects.*;
import com.autotune.dependencyAnalyzer.util.DAConstants;
import com.autotune.dependencyAnalyzer.variables.Variables;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Maintains information about the Autotune resources deployed in the cluster
 */
public class AutotuneDeployment
{
	/**
	 * Key: Name of autotuneObject
	 * Value: AutotuneObject instance matching the name
	 */
	public static Map<String, AutotuneObject> autotuneObjectMap = new HashMap<>();
	public static Map<String, AutotuneConfig> autotuneConfigMap = new HashMap<>();

	/**
	 * Outer map:
	 * Key: Name of autotune Object
	 *
	 * Inner map:
	 * Key: Name of application
	 * Value: ApplicationServiceStack instance for the application.
	 */
	public static Map<String, Map<String, ApplicationServiceStack>> applicationServiceStackMap = new HashMap<>();

	private static final Logger LOGGER = LoggerFactory.getLogger(AutotuneDeployment.class);

	/**
	 * Get Autotune objects from kubernetes, and watch for any additions, modifications or deletions.
	 * Add obtained autotune objects to map and match autotune object with pods.
	 *
	 * @throws IOException if unable to get Kubernetes config
	 * @param autotuneDeployment
	 */
	public static void getAutotuneObjects(final AutotuneDeployment autotuneDeployment) throws IOException {
		KubernetesClient client = new DefaultKubernetesClient();

		/* Watch for events (additions, modifications or deletions) of autotune objects */
		Watcher<String> autotuneObjectWatcher = new Watcher<>() {
			@Override
			public void eventReceived(Action action, String resource) {
				switch (action.toString().toUpperCase()) {
					case "ADDED":
						try {
							autotuneDeployment.addAutotuneObjectToMap(resource);
							autotuneDeployment.matchPodsToAutotuneObject(client);
						} catch (InvalidValueException e) {
							System.out.println(e.getMessage());
						}
						for (String autotuneConfig : autotuneConfigMap.keySet()) {
							addLayerInfo(autotuneConfigMap.get(autotuneConfig));
						}
						break;
					default:
						break;
				}
			}

			@Override
			public void onClose(KubernetesClientException e) { }
		};

		Watcher<String> autotuneConfigWatcher = new Watcher<>() {
			@Override
			public void eventReceived(Action action, String resource) {
				switch (action.toString().toUpperCase()) {
					case "ADDED":
						AutotuneConfig autotuneConfig = null;
						try {
							autotuneConfig = getAutotuneConfig(resource, client, KubernetesContexts.getAutotuneVariableContext());
						} catch (InvalidValueException e) {
							e.printStackTrace();
						}
						if (autotuneConfig != null)
							addLayerInfo(autotuneConfig);
						break;
					default:
						break;
				}
			}

			@Override
			public void onClose(KubernetesClientException e) { }
		};

		/* Register custom watcher for autotune object and autotuneconfig object*/
		client.customResource(KubernetesContexts.getAutotuneCrdContext()).watch(autotuneObjectWatcher);
		client.customResource(KubernetesContexts.getAutotuneConfigContext()).watch(autotuneConfigWatcher);
	}

	/**
	 * Get map of pods matching the autotune object using the labels.
	 *
	 * @param client KubernetesClient to get pods in cluster
	 */
	private void matchPodsToAutotuneObject(KubernetesClient client) {
		for (String autotuneObjectKey : autotuneObjectMap.keySet()) {
			AutotuneObject autotuneObject = autotuneObjectMap.get(autotuneObjectKey);
			String labelKey = autotuneObject.getSelectorInfo().getMatchLabel();
			String labelValue = autotuneObject.getSelectorInfo().getMatchLabelValue();

			PodList podList = client.pods().inAnyNamespace().withLabel(labelKey).list();

			for (Pod pod : podList.getItems()) {
				ObjectMeta podMetadata = pod.getMetadata();
				String matchingLabelValue = podMetadata.getLabels().get(labelKey);
				String status = pod.getStatus().getPhase();

				if (matchingLabelValue != null && matchingLabelValue.equals(labelValue)) {
					ApplicationServiceStack applicationServiceStack = new ApplicationServiceStack(podMetadata.getName(),
							podMetadata.getNamespace(), status);
					// If autotuneobject is already in map
					if (applicationServiceStackMap.containsKey(autotuneObject.getName())) {
						//If applicationservicestack is not already in list
						if (!applicationServiceStackMap.get(autotuneObject.getName()).containsKey(podMetadata.getName())) {
							applicationServiceStackMap.get(autotuneObject.getName()).put(applicationServiceStack.getApplicationServiceName(),
									applicationServiceStack);
						}
					}else {
						Map<String, ApplicationServiceStack> innerMap = new HashMap<>();
						innerMap.put(applicationServiceStack.getApplicationServiceName(), applicationServiceStack);
						applicationServiceStackMap.put(autotuneObject.getName(), innerMap);
					}
				}
			}
		}
	}

	/**
	 * Add Autotune object to map of monitored objects.
	 *
	 * @param autotuneObject JSON string of the autotune object
	 *
	 * @throws InvalidValueException if autotune object contains values not supported.
	 */
	private void addAutotuneObjectToMap(String autotuneObject) throws InvalidValueException {
		//TODO Make changes here after new autotune CRD gets merged
		JSONObject autotuneObjectJson = new JSONObject(autotuneObject);
		JSONObject specJson = autotuneObjectJson.getJSONObject("spec");

		JSONObject slaJson = specJson.getJSONObject("sla");
		String sla_class = slaJson.optString("sla_class");
		String direction = slaJson.optString("direction");
		String objectiveFunction = slaJson.optString("objective_function");

		JSONArray functionVariables = slaJson.optJSONArray("function_variables");
		ArrayList<FunctionVariable> functionVariableArrayList = new ArrayList<>();

		for (Object functionVariableObj : functionVariables) {
			JSONObject functionVariableJson = (JSONObject) functionVariableObj;
			String variableName = functionVariableJson.optString("name");
			String query = functionVariableJson.optString("query");
			String datasource = functionVariableJson.optString("datasource");
			String valueType = functionVariableJson.optString("value_type");

			FunctionVariable functionVariable = new FunctionVariable(variableName,
					query,
					datasource,
					valueType);
			functionVariableArrayList.add(functionVariable);
		}

		SlaInfo slaInfo = new SlaInfo(sla_class,
				objectiveFunction,
				direction,
				functionVariableArrayList);

		JSONObject selectorJson = specJson.getJSONObject("selector");

		String matchLabel = selectorJson.optString("matchLabel");
		String matchLabelValue = selectorJson.optString("matchLabelValue");
		String matchRoute = selectorJson.optString("matchRoute");
		String matchURI = selectorJson.optString("matchURI");
		String matchService = selectorJson.optString("matchService");

		SelectorInfo selectorInfo = new SelectorInfo(matchLabel,
				matchLabelValue,
				matchRoute,
				matchURI,
				matchService);

		String mode = slaJson.getString("mode");

		String name = autotuneObjectJson.getJSONObject("metadata").getString("name");
		String namespace = autotuneObjectJson.getJSONObject("metadata").getString("namespace");

		AutotuneObject autotuneObjectInfo = new AutotuneObject(name,
				namespace,
				mode,
				slaInfo,
				selectorInfo
		);
		autotuneObjectMap.put(name, autotuneObjectInfo);
	}

	/**
	 * Parse AutotuneConfig JSON and create matching AutotuneConfig object
	 *  @param autotuneConfigResource The JSON file for the autotuneconfig resource in the cluster.
	 * @param client
	 * @param autotuneVariableContext
	 */
	@SuppressWarnings("unchecked")
	private static AutotuneConfig getAutotuneConfig(String autotuneConfigResource, KubernetesClient client, CustomResourceDefinitionContext autotuneVariableContext) throws InvalidValueException {
		JSONObject autotuneConfigJson = new JSONObject(autotuneConfigResource);
		JSONArray layerPresenceQueryJson = autotuneConfigJson.optJSONObject(DAConstants.LAYER_PRESENCE).optJSONArray("datasource");
		JSONArray layerPresenceLabelJson = autotuneConfigJson.optJSONObject(DAConstants.LAYER_PRESENCE).optJSONArray("label");

		String namespace = autotuneConfigJson.getJSONObject("metadata").optString("namespace");

		Map<String, Object> envVariblesMap = client.customResource(autotuneVariableContext).get(namespace, EnvInfo.getK8s_type());

		ArrayList<Map<String, String>> arrayList = (ArrayList<Map<String, String>>) envVariblesMap.get("query_variables");

		String layerPresenceQuery = null;
		String layerPresenceKey = null;

		String layerPresenceLabel = null;
		String layerPresenceLabelValue = null;

		if (layerPresenceQueryJson != null) {
			for (Object datasource : layerPresenceQueryJson) {
				JSONObject datasourceJson = (JSONObject) datasource;
				if (datasourceJson.getString("name").equals(EnvInfo.getDataSource())) {
					layerPresenceQuery = datasourceJson.getString("query");
					layerPresenceKey = datasourceJson.getString("key");
					break;
				}
			}
		}

		if (layerPresenceLabelJson != null) {
			for (Object label : layerPresenceLabelJson) {
				JSONObject labelJson = (JSONObject) label;
				layerPresenceLabel = labelJson.optString("name");
				layerPresenceLabelValue = labelJson.optString("value");
			}
		}

		String configName = autotuneConfigJson.getString("layer_name");
		String details = autotuneConfigJson.getString("details");
		int level = autotuneConfigJson.getInt("layer_level");
		String presence = autotuneConfigJson.optJSONObject(DAConstants.LAYER_PRESENCE).optString("presence");

		try {
			layerPresenceQuery = Variables.updateQueryWithVariables(null, null,
					layerPresenceQuery, arrayList);
		} catch (IOException ignored) { }

		JSONArray tunablesJsonArray = autotuneConfigJson.getJSONArray("tunables");
		ArrayList<Tunable> tunableArrayList = new ArrayList<>();

		for (Object tunablesObject : tunablesJsonArray) {
			JSONObject tunableJson = (JSONObject) tunablesObject;

			JSONArray dataSourceArray = tunableJson.getJSONObject("queries").getJSONArray("datasource");

			// Store the datasource and query from the JSON in a map
			Map<String, String> queriesMap = new HashMap<>();
			for (Object dataSourceObject : dataSourceArray) {
				JSONObject dataSourceJson = (JSONObject) dataSourceObject;
				String datasource = dataSourceJson.optString("name");
				String datasourceQuery = dataSourceJson.optString("query");

				try {
					datasourceQuery = Variables.updateQueryWithVariables(null, null,
							datasourceQuery, arrayList);
				} catch (IOException ignored) { }

				queriesMap.put(datasource, datasourceQuery);
			}

			String name = tunableJson.getString("name");
			String tunableValueType = tunableJson.getString("value_type");
			String upperBound = tunableJson.optString("upper_bound");
			String lowerBound = tunableJson.optString("lower_bound");

			ArrayList<String> slaClassList = new ArrayList<>();

			JSONArray slaClassJson = tunableJson.getJSONArray("sla_class");
			for (Object slaClassObject : slaClassJson) {
				String slaClass = (String) slaClassObject;
				slaClassList.add(slaClass);
			}

			Tunable tunable;
			try {
				tunable = new Tunable(name, upperBound, lowerBound, tunableValueType, queriesMap, slaClassList);
				tunableArrayList.add(tunable);
			} catch (InvalidBoundsException e) {
				e.printStackTrace();
			}
		}
		AutotuneConfig autotuneConfig = new AutotuneConfig(configName,
				level,
				details,
				presence,
				layerPresenceQuery,
				layerPresenceKey,
				layerPresenceLabel,
				layerPresenceLabelValue,
				tunableArrayList);

		autotuneConfigMap.put(configName, autotuneConfig);
		return autotuneConfig;
	}

	private static void addLayerInfo(AutotuneConfig autotuneConfig) {
		String layerPresenceQuery = autotuneConfig.getLayerPresenceQuery();
		String layerPresenceKey = autotuneConfig.getLayerPresenceKey();

		String layerPresenceLabel = autotuneConfig.getLayerPresenceLabel();
		String layerPresenceLabelValue = autotuneConfig.getLayerPresenceLabelValue();

		String presence = autotuneConfig.getPresence();

		//Add to all monitored applications in the cluster
		if (presence.equals(DAConstants.PRESENCE_ALWAYS)) {
			for (String autotuneObject : applicationServiceStackMap.keySet()) {
				for (String application : applicationServiceStackMap.get(autotuneObject).keySet()) {
					ApplicationServiceStack applicationServiceStack = applicationServiceStackMap.get(autotuneObject).get(application);
					addLayerInfoToApplication(applicationServiceStack, autotuneConfig);
				}
			}
			return;
		}

		if (layerPresenceQuery != null && !layerPresenceQuery.equals("")) {
			DataSource dataSource = null;
			try {
				dataSource = DataSourceFactory.getDataSource(EnvInfo.getDataSource());
			} catch (MonitoringAgentNotFoundException e) {
				e.printStackTrace();
			}

			ArrayList<String> apps = null;
			try {
				apps = (ArrayList<String>) dataSource.getAppsForLayer(layerPresenceQuery, layerPresenceKey);
			} catch (MalformedURLException | NullPointerException e) {
				LOGGER.info("Could not get the applications for the layer " + autotuneConfig.getName());
			}

			if (apps != null) {
				for (String application : apps) {
					for (String autotuneObject : applicationServiceStackMap.keySet()) {
						if (applicationServiceStackMap.containsKey(application)) {
							addLayerInfoToApplication(applicationServiceStackMap.get(autotuneObject).get(application), autotuneConfig);
						}
					}
				}
			}
		}

		if (layerPresenceLabel != null) {
			KubernetesClient client = new DefaultKubernetesClient();

			PodList podList = client.pods().inAnyNamespace().withLabel(layerPresenceLabel).list();

			for (Pod pod : podList.getItems()) {
				if (pod.getMetadata().getLabels().get(layerPresenceLabel).equals(layerPresenceLabelValue)) {
					for (String autotuneObject : applicationServiceStackMap.keySet()) {
						String podName = pod.getMetadata().getName();
						if (applicationServiceStackMap.get(autotuneObject).containsKey(podName)) {
							addLayerInfoToApplication(applicationServiceStackMap.get(autotuneObject).get(podName), autotuneConfig);
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * Add layer, queries and tunables info to the autotuneObject
	 * @param applicationServiceStack ApplicationServiceStack instance that contains the layer
	 * @param autotuneConfig AutotuneConfig object for the layer
	 */
	private static void addLayerInfoToApplication(ApplicationServiceStack applicationServiceStack, AutotuneConfig autotuneConfig) {
		//Check if layer already exists
		for (AutotuneConfig layer : applicationServiceStack.getStackLayers()) {
			if (layer.getName().equals(autotuneConfig.getName())) {
				return;
			}
		}

		ArrayList<Tunable> tunables = new ArrayList<>();
		for (Tunable tunable : autotuneConfig.getTunables()) {
			try {
				Map<String, String> queries = tunable.getQueries();

				//Replace the query variables for all queries in the tunable and add the updated tunable copy to the tunables arraylist
				for (String datasource : queries.keySet()) {
					String query = queries.get(datasource);
					query = Variables.updateQueryWithVariables(applicationServiceStack.getApplicationServiceName(),
							applicationServiceStack.getNamespace(), query, null);
					queries.replace(datasource, query);
				}
				Tunable tunableCopy = new Tunable(tunable.getName(),
						tunable.getUpperBound(),
						tunable.getLowerBound(),
						tunable.getValueType(),
						queries,
						tunable.getSlaClassList());
				tunables.add(tunableCopy);
			} catch (IOException | InvalidBoundsException ignored) { }
		}

		//Create autotuneconfigcopy with updated tunables arraylist
		AutotuneConfig autotuneConfigCopy = null;
		try {
			autotuneConfigCopy = new AutotuneConfig(autotuneConfig.getName(),
					autotuneConfig.getLevel(),
					autotuneConfig.getDetails(),
					autotuneConfig.getPresence(),
					autotuneConfig.getLayerPresenceQuery(),
					autotuneConfig.getLayerPresenceKey(),
					autotuneConfig.getLayerPresenceLabel(),
					autotuneConfig.getLayerPresenceLabelValue(),
					tunables);
		} catch (InvalidValueException ignored) { }

		LOGGER.info("Added layer " + autotuneConfig.getName() +  " to application " + applicationServiceStack.getApplicationServiceName());
		applicationServiceStack.getStackLayers().add(autotuneConfigCopy);
	}
}
