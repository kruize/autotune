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
package com.autotune.analyzer.deployment;

import com.autotune.analyzer.application.ApplicationServiceStack;
import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.datasource.DataSource;
import com.autotune.analyzer.datasource.DataSourceFactory;
import com.autotune.analyzer.exceptions.InvalidBoundsException;
import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.analyzer.k8sObjects.*;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.Utils;
import com.autotune.analyzer.variables.Variables;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetList;
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
import java.util.Set;

import static com.autotune.analyzer.Experimentator.startExperiment;
import static com.autotune.analyzer.utils.AnalyzerConstants.POD_TEMPLATE_HASH;

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
	 * @param autotuneDeployment
	 * @throws IOException if unable to get Kubernetes config
	 */
	public static void getAutotuneObjects(final AutotuneDeployment autotuneDeployment) throws IOException {
		KubernetesClient client = new DefaultKubernetesClient();

		/* Watch for events (additions, modifications or deletions) of autotune objects */
		Watcher<String> autotuneObjectWatcher = new Watcher<>() {
			@Override
			public void eventReceived(Action action, String resource) {
				AutotuneObject autotuneObject = null;

				switch (action.toString().toUpperCase()) {
					case "ADDED":
						autotuneObject = autotuneDeployment.getAutotuneObject(resource);
						if (autotuneObject != null) {
							addAutotuneObject(autotuneObject, autotuneDeployment, client);
							String autotuneObjectStr = autotuneObject.getExperimentName();
							// Each AutotuneObject can affect multiple applicationServiceStacks (micro services)
							// For each of these applicationServiceStacks, we need to start the experiments

							if (applicationServiceStackMap.get(autotuneObjectStr) != null) {
								for (String applicationServiceStackName : applicationServiceStackMap.get(autotuneObjectStr).keySet()) {
									ApplicationServiceStack applicationServiceStack = applicationServiceStackMap.get(autotuneObjectStr).get(applicationServiceStackName);
									startExperiment(autotuneObject, applicationServiceStack);
								}
								LOGGER.info("Added autotune object " + autotuneObject.getExperimentName());
							} else {
								LOGGER.info("autotune object " + autotuneObject.getExperimentName() + " not added as no related deployments found!");
							}
						}
						break;
					case "MODIFIED":
						autotuneObject = autotuneDeployment.getAutotuneObject(resource);
						if (autotuneObject != null) {
							// Check if any of the values have changed from the existing object in the map
							if (autotuneObjectMap.get(autotuneObject.getExperimentName()).getExperimentId() != autotuneObject.getExperimentId()) {
								deleteExistingAutotuneObject(resource);
								addAutotuneObject(autotuneObject, autotuneDeployment, client);

								String autotuneObjectStr = autotuneObject.getExperimentId();
								// Each AutotuneObject can affect multiple applicationServiceStacks (micro services)
								// For each of these applicationServiceStacks, we need to restart the experiments
								for (String applicationServiceStackName : applicationServiceStackMap.get(autotuneObjectStr).keySet()) {
									ApplicationServiceStack applicationServiceStack = applicationServiceStackMap.get(autotuneObjectStr).get(applicationServiceStackName);
								}

								LOGGER.info("Modified autotune object {}", autotuneObject.getExperimentName());
							}
						}
						break;
					case "DELETED":
						deleteExistingAutotuneObject(resource);
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
				AutotuneConfig autotuneConfig = null;

				switch (action.toString().toUpperCase()) {
					case "ADDED":
						autotuneConfig = getAutotuneConfig(resource, client, KubernetesContexts.getAutotuneVariableContext());
						if (autotuneConfig != null) {
							autotuneConfigMap.put(autotuneConfig.getName(), autotuneConfig);
							LOGGER.info("Added autotuneconfig " + autotuneConfig.getName());
							addLayerInfo(autotuneConfig);
						}
						break;
					case "MODIFIED":
						autotuneConfig = getAutotuneConfig(resource, client, KubernetesContexts.getAutotuneVariableContext());
						if (autotuneConfig != null) {
							deleteExistingConfig(resource);
							autotuneConfigMap.put(autotuneConfig.getName(), autotuneConfig);
							LOGGER.info("Added modified autotuneconfig " + autotuneConfig.getName());
							addLayerInfo(autotuneConfig);
						}
						break;
					case "DELETED":
						deleteExistingConfig(resource);
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
	 * Add autotuneobject to monitoring map and match pods and autotuneconfigs
	 * @param autotuneObject
	 * @param autotuneDeployment
	 * @param client
	 */
	private static void addAutotuneObject(AutotuneObject autotuneObject, AutotuneDeployment autotuneDeployment, KubernetesClient client) {
		autotuneObjectMap.put(autotuneObject.getExperimentName(), autotuneObject);
		System.out.println("Autotune Object: " + autotuneObject.getExperimentName() + ": Finding Layers");
		autotuneDeployment.matchPodsToAutotuneObject(client);

		for (String autotuneConfig : autotuneConfigMap.keySet()) {
			addLayerInfo(autotuneConfigMap.get(autotuneConfig));
		}
	}

	/**
	 * Delete autotuneobject that's currently monitored
	 * @param autotuneObject
	 */
	private static void deleteExistingAutotuneObject(String autotuneObject) {
		JSONObject autotuneObjectJson = new JSONObject(autotuneObject);
		String name = autotuneObjectJson.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.METADATA)
				.optString(AnalyzerConstants.AutotuneObjectConstants.NAME);

		autotuneObjectMap.remove(name);
		applicationServiceStackMap.remove(name);
		LOGGER.info("Deleted autotune object {}", name);
	}

	/**
	 * Delete existing autotuneconfig in applications monitored by autotune
	 * @param resource JSON string of the autotuneconfig object
	 */
	private static void deleteExistingConfig(String resource) {
		JSONObject autotuneConfigJson = new JSONObject(resource);
		String configName = autotuneConfigJson.optString(AnalyzerConstants.AutotuneConfigConstants.LAYER_NAME);

		LOGGER.info("AutotuneConfig " + configName + " removed from autotune monitoring");
		// Remove from collection of autotuneconfigs in map
		autotuneConfigMap.remove(configName);

		// Remove autotuneconfig for all applications monitored
		for (String autotuneObject : applicationServiceStackMap.keySet()) {
			for (String applicationServiceStackName : applicationServiceStackMap.get(autotuneObject).keySet()) {
				ApplicationServiceStack applicationServiceStack = applicationServiceStackMap.get(autotuneObject).get(applicationServiceStackName);
				applicationServiceStack.getApplicationServiceStackLayers().remove(configName);
			}
		}
	}

	/**
	 * Get map of pods matching the autotune object using the labels.
	 *
	 * @param client KubernetesClient to get pods in cluster
	 */
	private void matchPodsToAutotuneObject(KubernetesClient client) {

		for (String autotuneObjectKey : autotuneObjectMap.keySet()) {

			AutotuneObject autotuneObject = autotuneObjectMap.get(autotuneObjectKey);
			String namespace = autotuneObject.getNamespace();
			String labelKey = autotuneObject.getSelectorInfo().getMatchLabel();
			String labelValue = autotuneObject.getSelectorInfo().getMatchLabelValue();

			DeploymentList deploymentList = client.apps().deployments().inNamespace(namespace).list();
			ReplicaSetList replicaSetList = client.apps().replicaSets().inNamespace(namespace).list();
			PodList podList = client.pods().inNamespace(namespace).withLabel(labelKey, labelValue).list();

			if (deploymentList.getItems().isEmpty()) {
				LOGGER.warn("No deployments in namespace: " + namespace);
				continue;
			}
			if (replicaSetList.getItems().isEmpty()) {
				LOGGER.warn("No replicaset in namespace: " + namespace);
				continue;
			}
			if (podList.getItems().isEmpty()) {
				LOGGER.warn("No podList in namespace: "
						+ namespace + " with label: " + labelKey + " and label value: " + labelValue);
				continue;
			}

			for (Deployment deployment : deploymentList.getItems()) {

				ObjectMeta deploymentMetadata = deployment.getMetadata();
				String deploymentName = deploymentMetadata.getName();
				String deploymentLabelValue = deployment.getSpec().getTemplate().getMetadata().getLabels().get(labelKey);
				LOGGER.debug("Deployment Name: " + deploymentName
						+ " has LabelValue: " + deploymentLabelValue + " for labelKey: " + labelKey);

				if (deploymentLabelValue == null || !deploymentLabelValue.equals(labelValue)) {
					continue;
				}

				for (ReplicaSet replicaSet : replicaSetList.getItems()) {
					ObjectMeta replicasetMetadata = replicaSet.getMetadata();
					String replicasetLabelValue = replicasetMetadata.getLabels().get(labelKey);
					LOGGER.debug("Replicaset Name: " + replicasetMetadata.getName()
							+ " has LabelValue: " + replicasetLabelValue + " for labelKey: " + labelKey);
					if (replicasetLabelValue == null || !replicasetLabelValue.equals(labelValue)) {
						continue;
					}
					String replicasetPodTemplateHash = replicasetMetadata.getLabels().get(POD_TEMPLATE_HASH);

					for (Pod pod : podList.getItems()) {
						ObjectMeta podMetadata = pod.getMetadata();
						String podLabelValue = podMetadata.getLabels().get(labelKey);
						String podTemplateHash = podMetadata.getLabels().get(POD_TEMPLATE_HASH);
						String status = pod.getStatus().getPhase();
						LOGGER.debug("Pod: " + podMetadata.getName()
								+ " Pod Label Value: " + podLabelValue
								+ " Label Key: " + labelKey + " Label Value: " + labelValue
								+ " replicasetPodTemplateHash: " + replicasetPodTemplateHash
								+ " podTemplateHash: " + podTemplateHash);

						if (podLabelValue == null
								|| !podLabelValue.equals(labelValue)
								|| !podTemplateHash.equals(replicasetPodTemplateHash)
						) {
							continue;
						}
						ApplicationServiceStack applicationServiceStack = new ApplicationServiceStack(podMetadata.getName(),
								podMetadata.getNamespace(),
								deploymentName,
								status);
						// If autotuneobject is already in map
						if (applicationServiceStackMap.containsKey(autotuneObject.getExperimentName())) {
							//If applicationservicestack is not already in list
							if (!applicationServiceStackMap.get(autotuneObject.getExperimentName()).containsKey(podMetadata.getName())) {
								applicationServiceStackMap.get(autotuneObject.getExperimentName()).put(applicationServiceStack.getApplicationServiceName(),
										applicationServiceStack);
							}
						} else {
							Map<String, ApplicationServiceStack> innerMap = new HashMap<>();
							innerMap.put(applicationServiceStack.getApplicationServiceName(), applicationServiceStack);
							applicationServiceStackMap.put(autotuneObject.getExperimentName(), innerMap);
						}
					}
				}
			}
		}
	}

	/**
	 * Add Autotune object to map of monitored objects.
	 *
	 * @param autotuneObject JSON string of the autotune object
	 */
	private AutotuneObject getAutotuneObject(String autotuneObject) {
		try {
			JSONObject autotuneObjectJson = new JSONObject(autotuneObject);

			String name;
			String mode;
			SloInfo sloInfo;
			String namespace;
			SelectorInfo selectorInfo;

			JSONObject specJson = autotuneObjectJson.optJSONObject(AnalyzerConstants.AutotuneObjectConstants.SPEC);

			JSONObject sloJson = null;
			String slo_class = null;
			String direction = null;
			String objectiveFunction = null;
			String hpoAlgoImpl = null;
			if (specJson != null) {
				sloJson = specJson.optJSONObject(DAConstants.AutotuneObjectConstants.SLO);
				slo_class = sloJson.optString(DAConstants.AutotuneObjectConstants.SLO_CLASS);
				direction = sloJson.optString(DAConstants.AutotuneObjectConstants.DIRECTION);
				hpoAlgoImpl = sloJson.optString(DAConstants.AutotuneObjectConstants.HPO_ALGO_IMPL);
				objectiveFunction = sloJson.optString(DAConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION);
			}

			JSONArray functionVariables = new JSONArray();
			if (sloJson != null) {
				functionVariables = sloJson.getJSONArray(DAConstants.AutotuneObjectConstants.FUNCTION_VARIABLES);
			}
			ArrayList<Metric> metricArrayList = new ArrayList<>();

			for (Object functionVariableObj : functionVariables) {
				JSONObject functionVariableJson = (JSONObject) functionVariableObj;
				String variableName = functionVariableJson.optString(AnalyzerConstants.AutotuneObjectConstants.NAME);
				String query = functionVariableJson.optString(AnalyzerConstants.AutotuneObjectConstants.QUERY);
				String datasource = functionVariableJson.optString(AnalyzerConstants.AutotuneObjectConstants.DATASOURCE);
				String valueType = functionVariableJson.optString(AnalyzerConstants.AutotuneObjectConstants.VALUE_TYPE);

				Metric metric = new Metric(variableName,
						query,
						datasource,
						valueType);

				metricArrayList.add(metric);
			}

			// If the user has not specified hpoAlgoImpl, we use the default one.
			if (hpoAlgoImpl == null || hpoAlgoImpl.isEmpty()) {
				hpoAlgoImpl = AnalyzerConstants.AutotuneObjectConstants.DEFAULT_HPO_ALGO_IMPL;
			}

			sloInfo = new SloInfo(slo_class,
					objectiveFunction,
					direction,
					hpoAlgoImpl,
					metricArrayList);

			JSONObject selectorJson = null;
			if (specJson != null) {
				selectorJson = specJson.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.SELECTOR);
			}

			assert selectorJson != null;
			String matchLabel = selectorJson.optString(AnalyzerConstants.AutotuneObjectConstants.MATCH_LABEL);
			String matchLabelValue = selectorJson.optString(AnalyzerConstants.AutotuneObjectConstants.MATCH_LABEL_VALUE);
			String matchRoute = selectorJson.optString(AnalyzerConstants.AutotuneObjectConstants.MATCH_ROUTE);
			String matchURI = selectorJson.optString(AnalyzerConstants.AutotuneObjectConstants.MATCH_URI);
			String matchService = selectorJson.optString(AnalyzerConstants.AutotuneObjectConstants.MATCH_SERVICE);

			selectorInfo = new SelectorInfo(matchLabel,
					matchLabelValue,
					matchRoute,
					matchURI,
					matchService);

			mode = specJson.optString(AnalyzerConstants.AutotuneObjectConstants.MODE);
			name = autotuneObjectJson.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.METADATA)
					.optString(AnalyzerConstants.AutotuneObjectConstants.NAME);
			namespace = autotuneObjectJson.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.METADATA)
					.optString(AnalyzerConstants.AutotuneObjectConstants.NAMESPACE);

			// Generate string of all fields we want to use for ID. This is the same as toString() for later comparison
			String idString = "AutotuneObject{" +
					"name='" + name + '\'' +
					", namespace='" + namespace + '\'' +
					", mode='" + mode + '\'' +
					", sloInfo=" + sloInfo +
					", selectorInfo=" + selectorInfo +
					'}';
			String experimentId = Utils.generateID(idString);

			return new AutotuneObject(experimentId,
					name,
					namespace,
					mode,
					sloInfo,
					selectorInfo
			);

		} catch (InvalidValueException | NullPointerException | JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Parse AutotuneConfig JSON and create matching AutotuneConfig object
	 *
	 * @param autotuneConfigResource  The JSON file for the autotuneconfig resource in the cluster.
	 * @param client
	 * @param autotuneVariableContext
	 */
	@SuppressWarnings("unchecked")
	private static AutotuneConfig getAutotuneConfig(String autotuneConfigResource, KubernetesClient client, CustomResourceDefinitionContext autotuneVariableContext) {
		try {
			JSONObject autotuneConfigJson = new JSONObject(autotuneConfigResource);
			JSONObject presenceJson = autotuneConfigJson.optJSONObject(AnalyzerConstants.AutotuneConfigConstants.LAYER_PRESENCE);

			String presence = null;
			JSONObject layerPresenceQueryJson = null;
			JSONArray layerPresenceLabelJson = null;
			if (presenceJson != null) {
				presence = presenceJson.optString(AnalyzerConstants.AutotuneConfigConstants.PRESENCE);
				layerPresenceQueryJson = presenceJson.optJSONObject(AnalyzerConstants.AutotuneConfigConstants.QUERY);
				layerPresenceLabelJson = presenceJson.optJSONArray(AnalyzerConstants.AutotuneConfigConstants.LABEL);
			}

			String name = autotuneConfigJson.getJSONObject(AnalyzerConstants.AutotuneConfigConstants.METADATA).optString(AnalyzerConstants.AutotuneConfigConstants.NAME);
			String namespace = autotuneConfigJson.getJSONObject(AnalyzerConstants.AutotuneConfigConstants.METADATA).optString(AnalyzerConstants.AutotuneConfigConstants.NAMESPACE);

			// Get the autotunequeryvariables for the current kubernetes environment
			ArrayList<Map<String, String>> arrayList = null;
			try {
				Map<String, Object> envVariblesMap = client.customResource(autotuneVariableContext).get(namespace, DeploymentInfo.getKubernetesType());
				arrayList = (ArrayList<Map<String, String>>) envVariblesMap.get(AnalyzerConstants.AutotuneConfigConstants.QUERY_VARIABLES);
			} catch (Exception e) {
				LOGGER.error("Autotunequeryvariable and autotuneconfig {} not in the same namespace", name);
				return null;
			}

			String layerPresenceQuery = null;
			String layerPresenceKey = null;

			String layerPresenceLabel = null;
			String layerPresenceLabelValue = null;

			if (layerPresenceQueryJson != null) {
				JSONArray datasourceArray = layerPresenceQueryJson.getJSONArray(AnalyzerConstants.AutotuneConfigConstants.DATASOURCE);
				for (Object datasource : datasourceArray) {
					JSONObject datasourceJson = (JSONObject) datasource;
					if (datasourceJson.getString(AnalyzerConstants.AutotuneConfigConstants.NAME).equals(DeploymentInfo.getMonitoringAgent())) {
						layerPresenceQuery = datasourceJson.getString(AnalyzerConstants.AutotuneConfigConstants.QUERY);
						layerPresenceKey = datasourceJson.getString(AnalyzerConstants.AutotuneConfigConstants.KEY);
						break;
					}
				}
			}

			if (layerPresenceLabelJson != null) {
				for (Object label : layerPresenceLabelJson) {
					JSONObject labelJson = (JSONObject) label;
					layerPresenceLabel = labelJson.optString(AnalyzerConstants.AutotuneConfigConstants.NAME);
					layerPresenceLabelValue = labelJson.optString(AnalyzerConstants.AutotuneConfigConstants.VALUE);
				}
			}

			String layerName = autotuneConfigJson.optString(AnalyzerConstants.AutotuneConfigConstants.LAYER_NAME);
			String details = autotuneConfigJson.optString(AnalyzerConstants.AutotuneConfigConstants.DETAILS);
			int level = autotuneConfigJson.optInt(AnalyzerConstants.AutotuneConfigConstants.LAYER_LEVEL);

			// Replace the queryvariables in the query
			try {
				layerPresenceQuery = Variables.updateQueryWithVariables(null, null,
						layerPresenceQuery, arrayList);
			} catch (IOException ignored) { }

			JSONArray tunablesJsonArray = autotuneConfigJson.optJSONArray(AnalyzerConstants.AutotuneConfigConstants.TUNABLES);
			ArrayList<Tunable> tunableArrayList = new ArrayList<>();

			for (Object tunablesObject : tunablesJsonArray) {
				JSONObject tunableJson = (JSONObject) tunablesObject;

				JSONObject tunableQueries = tunableJson.optJSONObject(AnalyzerConstants.AutotuneConfigConstants.QUERIES);
				JSONArray dataSourceArray = null;
				if (tunableQueries != null) {
					dataSourceArray = tunableQueries.optJSONArray(AnalyzerConstants.AutotuneConfigConstants.DATASOURCE);
				}

				// Store the datasource and query from the JSON in a map
				Map<String, String> queriesMap = new HashMap<>();
				if (dataSourceArray != null) {
					for (Object dataSourceObject : dataSourceArray) {
						JSONObject dataSourceJson = (JSONObject) dataSourceObject;
						String datasource = dataSourceJson.optString(AnalyzerConstants.AutotuneConfigConstants.NAME);
						String datasourceQuery = dataSourceJson.optString(AnalyzerConstants.AutotuneConfigConstants.QUERY);

						try {
							datasourceQuery = Variables.updateQueryWithVariables(null, null,
									datasourceQuery, arrayList);
						} catch (IOException ignored) { }

						queriesMap.put(datasource, datasourceQuery);
					}
				}

				String tunableName = tunableJson.optString(AnalyzerConstants.AutotuneConfigConstants.NAME);
				String tunableValueType = tunableJson.optString(AnalyzerConstants.AutotuneConfigConstants.VALUE_TYPE);
				String upperBound = tunableJson.optString(AnalyzerConstants.AutotuneConfigConstants.UPPER_BOUND);
				String lowerBound = tunableJson.optString(AnalyzerConstants.AutotuneConfigConstants.LOWER_BOUND);
				// Read in step from the tunable, set it to '1' if not specified.
				double step = tunableJson.optDouble(AnalyzerConstants.AutotuneConfigConstants.STEP, 1);

				ArrayList<String> sloClassList = new ArrayList<>();

				JSONArray sloClassJson = tunableJson.getJSONArray(DAConstants.AutotuneConfigConstants.SLO_CLASS);
				for (Object sloClassObject : sloClassJson) {
					String sloClass = (String) sloClassObject;
					sloClassList.add(sloClass);
				}

				Tunable tunable;
				try {
					tunable = new Tunable(tunableName, step, upperBound, lowerBound, tunableValueType, queriesMap, sloClassList);
					tunableArrayList.add(tunable);
				} catch (InvalidBoundsException e) {
					e.printStackTrace();
				}
			}

			// Generate string of all fields we want to use for ID. This is the same as toString() for later comparison
			String idString = "AutotuneConfig{" +
					"level=" + level +
					", name='" + name + '\'' +
					", layerName='" + layerName + '\'' +
					", presence='" + presence + '\'' +
					", layerPresenceKey='" + layerPresenceKey + '\'' +
					", layerPresenceQuery='" + layerPresenceQuery + '\'' +
					", layerPresenceLabel='" + layerPresenceLabel + '\'' +
					", layerPresenceLabelValue='" + layerPresenceLabelValue + '\'' +
					", tunables=" + tunableArrayList +
					'}';

			String layerId = Utils.generateID(idString);
			return new AutotuneConfig(layerId, name,
					layerName,
					level,
					details,
					presence,
					layerPresenceQuery,
					layerPresenceKey,
					layerPresenceLabel,
					layerPresenceLabelValue,
					tunableArrayList);
		} catch (JSONException | InvalidValueException | NullPointerException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static void addLayerInfo(AutotuneConfig autotuneConfig) {
		String layerPresenceQuery = autotuneConfig.getLayerPresenceQuery();
		String layerPresenceKey = autotuneConfig.getLayerPresenceKey();

		String layerPresenceLabel = autotuneConfig.getLayerPresenceLabel();
		String layerPresenceLabelValue = autotuneConfig.getLayerPresenceLabelValue();

		String presence = autotuneConfig.getPresence();

		//Add to all monitored applications in the cluster
		if (presence.equals(AnalyzerConstants.PRESENCE_ALWAYS)) {
			for (String autotuneObject : applicationServiceStackMap.keySet()) {
				for (String application : applicationServiceStackMap.get(autotuneObject).keySet()) {
					ApplicationServiceStack applicationServiceStack = applicationServiceStackMap.get(autotuneObject).get(application);
					addLayerInfoToApplication(applicationServiceStack, autotuneConfig);
				}
			}
			return;
		}

		if (layerPresenceQuery != null && !layerPresenceQuery.isEmpty()) {
			DataSource dataSource = null;
			try {
				dataSource = DataSourceFactory.getDataSource(DeploymentInfo.getMonitoringAgent());
			} catch (MonitoringAgentNotFoundException e) {
				e.printStackTrace();
			}

			ArrayList<String> apps = null;
			try {
				apps = (ArrayList<String>) dataSource.getAppsForLayer(layerPresenceQuery, layerPresenceKey);
			} catch (MalformedURLException | NullPointerException e) {
				LOGGER.info(AnalyzerErrorConstants.AutotuneConfigErrors.COULD_NOT_GET_LIST_OF_APPLICATIONS + autotuneConfig.getName());
			}

			if (apps != null) {
				for (String application : apps) {
					for (String autotuneObject : applicationServiceStackMap.keySet()) {
						if (applicationServiceStackMap.get(autotuneObject).containsKey(application)) {
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

			// We should now have all layers added to the applicationServiceStackMap

		}
	}

	/**
	 * Add layer, queries and tunables info to the autotuneObject
	 *
	 * @param applicationServiceStack ApplicationServiceStack instance that contains the layer
	 * @param autotuneConfig          AutotuneConfig object for the layer
	 */
	private static void addLayerInfoToApplication(ApplicationServiceStack applicationServiceStack, AutotuneConfig autotuneConfig) {
		//Check if layer already exists
		if (applicationServiceStack.getApplicationServiceStackLayers().containsKey(autotuneConfig.getName())) {
			return;
		}

		ArrayList<Tunable> tunables = new ArrayList<>();
		for (Tunable tunable : autotuneConfig.getTunables()) {
			try {
				Map<String, String> queries = new HashMap<>(tunable.getQueries());

				//Replace the query variables for all queries in the tunable and add the updated tunable copy to the tunables arraylist
				for (String datasource : queries.keySet()) {
					String query = queries.get(datasource);
					query = Variables.updateQueryWithVariables(applicationServiceStack.getApplicationServiceName(),
							applicationServiceStack.getNamespace(), query, null);
					queries.replace(datasource, query);
				}
				Tunable tunableCopy = new Tunable(tunable.getName(),
						tunable.getStep(),
						tunable.getUpperBound(),
						tunable.getLowerBound(),
						tunable.getValueType(),
						queries,
						tunable.getSloClassList());
				tunables.add(tunableCopy);
			} catch (IOException | InvalidBoundsException ignored) { }
		}

		//Create autotuneconfigcopy with updated tunables arraylist
		AutotuneConfig autotuneConfigCopy = null;
		try {
			autotuneConfigCopy = new AutotuneConfig(autotuneConfig.getLayerId(),
					autotuneConfig.getName(),
					autotuneConfig.getLayerName(),
					autotuneConfig.getLevel(),
					autotuneConfig.getDetails(),
					autotuneConfig.getPresence(),
					autotuneConfig.getLayerPresenceQuery(),
					autotuneConfig.getLayerPresenceKey(),
					autotuneConfig.getLayerPresenceLabel(),
					autotuneConfig.getLayerPresenceLabelValue(),
					tunables);
		} catch (InvalidValueException ignored) { }

		LOGGER.info("Added layer " + autotuneConfig.getName() + " to application " + applicationServiceStack.getApplicationServiceName());
		applicationServiceStack.getApplicationServiceStackLayers().put(autotuneConfig.getName(), autotuneConfigCopy);
	}
}
