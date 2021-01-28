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
import com.autotune.dependencyAnalyzer.exceptions.MonitoringAgentNotSupportedException;
import com.autotune.dependencyAnalyzer.k8sObjects.*;
import com.autotune.dependencyAnalyzer.util.AutotuneSupportedTypes;
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
import org.json.JSONException;
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
						} catch (InvalidValueException | MonitoringAgentNotSupportedException e) {
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
	private void addAutotuneObjectToMap(String autotuneObject) throws InvalidValueException, MonitoringAgentNotSupportedException {
		//TODO Make changes here after new autotune CRD gets merged
		JSONObject autotuneObjectJson = new JSONObject(autotuneObject);

		String mode;
		String name;
		SlaInfo slaInfo;
		String namespace;
		SelectorInfo selectorInfo;
		try {
			JSONObject specJson = autotuneObjectJson.getJSONObject(DAConstants.AutotuneObjectConstants.SPEC);

			JSONObject slaJson = specJson.getJSONObject(DAConstants.AutotuneObjectConstants.SLA);
			String sla_class = slaJson.getString(DAConstants.AutotuneObjectConstants.SLA_CLASS);
			String direction = slaJson.getString(DAConstants.AutotuneObjectConstants.DIRECTION);
			String objectiveFunction = slaJson.getString(DAConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION);

			JSONArray functionVariables = slaJson.getJSONArray(DAConstants.AutotuneObjectConstants.FUNCTION_VARIABLES);
			ArrayList<FunctionVariable> functionVariableArrayList = new ArrayList<>();

			for (Object functionVariableObj : functionVariables) {
				JSONObject functionVariableJson = (JSONObject) functionVariableObj;
				String variableName = functionVariableJson.optString(DAConstants.AutotuneObjectConstants.NAME);
				String query = functionVariableJson.optString(DAConstants.AutotuneObjectConstants.QUERY);
				String datasource = functionVariableJson.optString(DAConstants.AutotuneObjectConstants.DATASOURCE);
				String valueType = functionVariableJson.optString(DAConstants.AutotuneObjectConstants.VALUE_TYPE);

				//Check if datasource is supported
				if (!AutotuneSupportedTypes.MONITORING_AGENTS_SUPPORTED.contains(datasource.toLowerCase()))
					throw new MonitoringAgentNotSupportedException();

				if (!AutotuneSupportedTypes.VALUE_TYPES_SUPPORTED.contains(valueType.toLowerCase()))
					throw new InvalidValueException("Invalid value_type for function variable");

				FunctionVariable functionVariable = new FunctionVariable(variableName,
						query,
						datasource,
						valueType);

				for (FunctionVariable variable : functionVariableArrayList) {
					if (variable.getName().equals(variableName))
						throw new InvalidValueException("Multiple function varibles with same name");
				}
				functionVariableArrayList.add(functionVariable);
			}

			slaInfo = new SlaInfo(sla_class,
					objectiveFunction,
					direction,
					functionVariableArrayList);

			JSONObject selectorJson = specJson.getJSONObject(DAConstants.AutotuneObjectConstants.SELECTOR);

			String matchLabel = selectorJson.getString(DAConstants.AutotuneObjectConstants.MATCH_LABEL);
			String matchLabelValue = selectorJson.getString(DAConstants.AutotuneObjectConstants.MATCH_LABEL_VALUE);
			String matchRoute = selectorJson.optString(DAConstants.AutotuneObjectConstants.MATCH_ROUTE);
			String matchURI = selectorJson.optString(DAConstants.AutotuneObjectConstants.MATCH_URI);
			String matchService = selectorJson.optString(DAConstants.AutotuneObjectConstants.MATCH_SERVICE);

			selectorInfo = new SelectorInfo(matchLabel,
					matchLabelValue,
					matchRoute,
					matchURI,
					matchService);

			mode = slaJson.getString(DAConstants.AutotuneObjectConstants.MODE);

			name = autotuneObjectJson.getJSONObject(DAConstants.AutotuneObjectConstants.METADATA)
					.getString(DAConstants.AutotuneObjectConstants.NAME);
			namespace = autotuneObjectJson.getJSONObject(DAConstants.AutotuneObjectConstants.METADATA)
					.getString(DAConstants.AutotuneObjectConstants.NAMESPACE);

			AutotuneObject autotuneObjectInfo = new AutotuneObject(name,
					namespace,
					mode,
					slaInfo,
					selectorInfo
			);
			autotuneObjectMap.put(name, autotuneObjectInfo);
			LOGGER.info("Added autotune object " + name);
		} catch (JSONException e) {
			LOGGER.error("Invalid Autotune yaml");
		} catch (MonitoringAgentNotSupportedException | InvalidValueException e) {
			LOGGER.error(e.getMessage());
		}
	}

	/**
	 * Parse AutotuneConfig JSON and create matching AutotuneConfig object
	 *  @param autotuneConfigResource The JSON file for the autotuneconfig resource in the cluster.
	 * @param client
	 * @param autotuneVariableContext
	 */
	@SuppressWarnings("unchecked")
	private static AutotuneConfig getAutotuneConfig(String autotuneConfigResource, KubernetesClient client, CustomResourceDefinitionContext autotuneVariableContext) throws InvalidValueException {
		try {
			JSONObject autotuneConfigJson = new JSONObject(autotuneConfigResource);
			JSONObject presenceJson = autotuneConfigJson.getJSONObject(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE);
			JSONArray layerPresenceQueryJson = presenceJson.optJSONArray(DAConstants.AutotuneConfigConstants.DATASOURCE);
			JSONArray layerPresenceLabelJson = presenceJson.optJSONArray(DAConstants.AutotuneConfigConstants.LABEL);

			String namespace = autotuneConfigJson.getJSONObject(DAConstants.AutotuneConfigConstants.METADATA).optString(DAConstants.AutotuneConfigConstants.NAMESPACE);

			Map<String, Object> envVariblesMap = client.customResource(autotuneVariableContext).get(namespace, EnvInfo.getK8s_type());

			ArrayList<Map<String, String>> arrayList = (ArrayList<Map<String, String>>) envVariblesMap.get(DAConstants.AutotuneConfigConstants.QUERY_VARIABLES);

			String layerPresenceQuery = null;
			String layerPresenceKey = null;

			String layerPresenceLabel = null;
			String layerPresenceLabelValue = null;

			String presence = presenceJson.optString(DAConstants.AutotuneConfigConstants.PRESENCE);

			//If neither layerPresenceQuery, layerPresenceLabels or the presence field is specified throw exception

			if (presence == null && layerPresenceQueryJson == null && layerPresenceLabelJson == null)
				throw new InvalidValueException("Invalid AutotuneConfig yaml");

			if (layerPresenceQueryJson != null) {
				for (Object datasource : layerPresenceQueryJson) {
					JSONObject datasourceJson = (JSONObject) datasource;
					if (datasourceJson.getString("name").equals(EnvInfo.getDataSource())) {
						layerPresenceQuery = datasourceJson.getString(DAConstants.AutotuneConfigConstants.QUERY);
						layerPresenceKey = datasourceJson.getString(DAConstants.AutotuneConfigConstants.KEY);
						break;
					}
				}
			}

			if (layerPresenceLabelJson != null) {
				for (Object label : layerPresenceLabelJson) {
					JSONObject labelJson = (JSONObject) label;
					layerPresenceLabel = labelJson.optString(DAConstants.AutotuneConfigConstants.NAME);
					layerPresenceLabelValue = labelJson.optString(DAConstants.AutotuneConfigConstants.VALUE);
				}
			}

			String configName = autotuneConfigJson.getString(DAConstants.AutotuneConfigConstants.LAYER_NAME);
			String details = autotuneConfigJson.optString(DAConstants.AutotuneConfigConstants.DETAILS);
			int level = autotuneConfigJson.getInt(DAConstants.AutotuneConfigConstants.LEVEL);

			try {
				layerPresenceQuery = Variables.updateQueryWithVariables(null, null,
						layerPresenceQuery, arrayList);
			} catch (IOException ignored) { }

			JSONArray tunablesJsonArray = autotuneConfigJson.getJSONArray(DAConstants.AutotuneConfigConstants.TUNABLES);
			ArrayList<Tunable> tunableArrayList = new ArrayList<>();

			for (Object tunablesObject : tunablesJsonArray) {
				JSONObject tunableJson = (JSONObject) tunablesObject;

				JSONArray dataSourceArray = tunableJson.getJSONObject(DAConstants.AutotuneConfigConstants.QUERIES)
						.getJSONArray(DAConstants.AutotuneConfigConstants.DATASOURCE);

				// Store the datasource and query from the JSON in a map
				Map<String, String> queriesMap = new HashMap<>();
				for (Object dataSourceObject : dataSourceArray) {
					JSONObject dataSourceJson = (JSONObject) dataSourceObject;
					String datasource = dataSourceJson.optString(DAConstants.AutotuneConfigConstants.NAME);
					String datasourceQuery = dataSourceJson.optString(DAConstants.AutotuneConfigConstants.QUERY);

					try {
						datasourceQuery = Variables.updateQueryWithVariables(null, null,
								datasourceQuery, arrayList);
					} catch (IOException ignored) { }

					queriesMap.put(datasource, datasourceQuery);
				}

				String name = tunableJson.getString(DAConstants.AutotuneConfigConstants.NAME);
				String tunableValueType = tunableJson.getString(DAConstants.AutotuneConfigConstants.VALUE_TYPE);
				String upperBound = tunableJson.getString(DAConstants.AutotuneConfigConstants.UPPER_BOUND);
				String lowerBound = tunableJson.getString(DAConstants.AutotuneConfigConstants.LOWER_BOUND);

				ArrayList<String> slaClassList = new ArrayList<>();

				JSONArray slaClassJson = tunableJson.getJSONArray(DAConstants.AutotuneConfigConstants.SLA_CLASS);
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
		} catch (JSONException e) {
			LOGGER.error("Invalid Autotune yaml");
		} catch (InvalidValueException e) {
			LOGGER.error(e.getMessage());
		}
		return null;
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
