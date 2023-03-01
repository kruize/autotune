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
package com.autotune.utils;

import com.autotune.analyzer.serviceObjects.ContainerMetricsHelper;
import com.autotune.analyzer.serviceObjects.CreateExperimentSO;
import com.autotune.analyzer.serviceObjects.ListRecommendationsSO;
import com.autotune.analyzer.serviceObjects.UpdateResultsSO;
import com.autotune.common.data.result.*;
import com.autotune.common.k8sObjects.ContainerObject;
import com.autotune.common.k8sObjects.DeploymentObject;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.common.k8sObjects.KruizeObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Contains methods that are of general utility in the codebase
 */
public class Utils
{
	private Utils() { }

	public static String generateID(Object object) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(object.toString().getBytes(StandardCharsets.UTF_8));

			StringBuffer hexString = new StringBuffer();

			for (int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);
				if (hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}

			return hexString.toString();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static AnalyzerConstants.K8S_OBJECT_TYPES getApproriateK8sObjectType(String objectType) {
		if (null == objectType)
			return null;

		if (objectType.isEmpty() || objectType.isBlank())
			return null;

		objectType = objectType.trim();

		if (objectType.equalsIgnoreCase(AnalyzerConstants.K8sObjectConstants.Types.DEPLOYMENT))
			return AnalyzerConstants.K8S_OBJECT_TYPES.DEPLOYMENT;

		if (objectType.equalsIgnoreCase(AnalyzerConstants.K8sObjectConstants.Types.DEPLOYMENT_CONFIG))
			return AnalyzerConstants.K8S_OBJECT_TYPES.DEPLOYMENT_CONFIG;

		if (objectType.equalsIgnoreCase(AnalyzerConstants.K8sObjectConstants.Types.STATEFULSET))
			return AnalyzerConstants.K8S_OBJECT_TYPES.STATEFULSET;

		if (objectType.equalsIgnoreCase(AnalyzerConstants.K8sObjectConstants.Types.REPLICASET))
			return AnalyzerConstants.K8S_OBJECT_TYPES.REPLICASET;

		if (objectType.equalsIgnoreCase(AnalyzerConstants.K8sObjectConstants.Types.REPLICATION_CONTROLLER))
			return AnalyzerConstants.K8S_OBJECT_TYPES.REPLICATION_CONTROLLER;

		if (objectType.equalsIgnoreCase(AnalyzerConstants.K8sObjectConstants.Types.DAEMONSET))
			return AnalyzerConstants.K8S_OBJECT_TYPES.DAEMONSET;

		return null;
	}

	public static String getAppropriateK8sObjectTypeString(AnalyzerConstants.K8S_OBJECT_TYPES objectType) {
		if (null == objectType)
			return null;

		if (objectType == AnalyzerConstants.K8S_OBJECT_TYPES.DEPLOYMENT)
			return AnalyzerConstants.K8sObjectConstants.Types.DEPLOYMENT;

		if (objectType == AnalyzerConstants.K8S_OBJECT_TYPES.DEPLOYMENT_CONFIG)
			return AnalyzerConstants.K8sObjectConstants.Types.DEPLOYMENT_CONFIG;

		if (objectType == AnalyzerConstants.K8S_OBJECT_TYPES.STATEFULSET)
			return AnalyzerConstants.K8sObjectConstants.Types.STATEFULSET;

		if (objectType == AnalyzerConstants.K8S_OBJECT_TYPES.REPLICASET)
			return AnalyzerConstants.K8sObjectConstants.Types.REPLICASET;

		if (objectType == AnalyzerConstants.K8S_OBJECT_TYPES.REPLICATION_CONTROLLER)
			return AnalyzerConstants.K8sObjectConstants.Types.REPLICATION_CONTROLLER;

		if (objectType == AnalyzerConstants.K8S_OBJECT_TYPES.DAEMONSET)
			return AnalyzerConstants.K8sObjectConstants.Types.DAEMONSET;

		return null;
	}

	public static AnalyzerConstants.MetricName getAppropriateMetricName(String metricName) {
		if (null == metricName)
			return null;

		if (metricName.equalsIgnoreCase(AnalyzerConstants.MetricNameConstants.CPU_REQUEST))
			return AnalyzerConstants.MetricName.cpuRequest;

		if (metricName.equalsIgnoreCase(AnalyzerConstants.MetricNameConstants.CPU_LIMIT))
			return AnalyzerConstants.MetricName.cpuLimit;

		if (metricName.equalsIgnoreCase(AnalyzerConstants.MetricNameConstants.CPU_USAGE))
			return AnalyzerConstants.MetricName.cpuUsage;

		if (metricName.equalsIgnoreCase(AnalyzerConstants.MetricNameConstants.CPU_THROTTLE))
			return AnalyzerConstants.MetricName.cpuThrottle;

		if (metricName.equalsIgnoreCase(AnalyzerConstants.MetricNameConstants.MEMORY_REQUEST))
			return AnalyzerConstants.MetricName.memoryRequest;

		if (metricName.equalsIgnoreCase(AnalyzerConstants.MetricNameConstants.MEMORY_LIMIT))
			return AnalyzerConstants.MetricName.memoryLimit;

		if (metricName.equalsIgnoreCase(AnalyzerConstants.MetricNameConstants.MEMORY_USAGE))
			return AnalyzerConstants.MetricName.memoryUsage;

		if (metricName.equalsIgnoreCase(AnalyzerConstants.MetricNameConstants.MEMORY_RSS))
			return AnalyzerConstants.MetricName.memoryRSS;

		return null;
	}

	public static class Converters {
		private Converters() {

		}

		public static class KruizeObjectConverters {
			private KruizeObjectConverters() {

			}

			public static CreateExperimentSO convertKruizeObjectToCreateExperimentSO(KruizeObject kruizeObject) {
				// Need to be implemented if needed
				return null;
			}

			public static KruizeObject convertCreateExperimentSOToKruizeObject(CreateExperimentSO createExperimentSO) {
				// To be implemented
				KruizeObject kruizeObject =  new KruizeObject();
				HashMap<String, DeploymentObject> deploymentObjectHashMap = new HashMap<String, DeploymentObject>();
				for (K8sObject k8sObject: createExperimentSO.getKubernetesObjects()) {
					if (null != k8sObject.getName() && !k8sObject.getName().isBlank()) {
						DeploymentObject deploymentObject = new DeploymentObject(k8sObject.getName());
						AnalyzerConstants.K8S_OBJECT_TYPES objectType = getApproriateK8sObjectType(k8sObject.getType());
						if (null != objectType)
							deploymentObject.setType(objectType);
						HashMap<String, ContainerObject> containerObjectHashMap = new HashMap<String, ContainerObject>();
						for (ContainerObject containerObject: k8sObject.getContainers()) {
							containerObjectHashMap.put(containerObject.getContainer_name(), containerObject);
						}
						deploymentObject.setContainers(containerObjectHashMap);
						deploymentObject.setNamespace(k8sObject.getNamespace());
						// TODO: Need to be changed as it should not be set at higher level
						kruizeObject.setNamespace(k8sObject.getNamespace());
						deploymentObjectHashMap.put(k8sObject.getName(), deploymentObject);
					}
				}
				kruizeObject.setDeployments(deploymentObjectHashMap);
				kruizeObject.setExperimentName(createExperimentSO.getExperimentName());
				kruizeObject.setApiVersion(createExperimentSO.getApiVersion());
				kruizeObject.setTargetCluster(createExperimentSO.getTargetCluster());
				kruizeObject.setMode(createExperimentSO.getMode());
				kruizeObject.setPerformanceProfile(createExperimentSO.getPerformanceProfile());
				kruizeObject.setTrial_settings(createExperimentSO.getTrialSettings());
				kruizeObject.setRecommendation_settings(createExperimentSO.getRecommendationSettings());
				return kruizeObject;
			}

			public static ListRecommendationsSO convertKruizeObjectToListRecommendationSO(KruizeObject kruizeObject) {
				// Need to be implemented if needed
				ListRecommendationsSO listRecommendationsSO =  new ListRecommendationsSO();
				listRecommendationsSO.setApiVersion(kruizeObject.getApiVersion());
				listRecommendationsSO.setExperimentName(kruizeObject.getExperimentName());
				listRecommendationsSO.setClusterName(kruizeObject.getClusterName());
				List<K8sObject> k8sObjectsList = new ArrayList<K8sObject>();
				for (DeploymentObject deploymentObject : kruizeObject.getDeployments().values()) {
					K8sObject k8sObject = new K8sObject();
					k8sObject.setName(deploymentObject.getName());
					k8sObject.setType(getAppropriateK8sObjectTypeString(deploymentObject.getType()));
					k8sObject.setNamespace(deploymentObject.getNamespace());
					List<ContainerObject> containerObjects = new ArrayList<ContainerObject>();
					for (ContainerObject containerObject: deploymentObject.getContainers().values()) {
						containerObjects.add(containerObject);
					}
					k8sObject.setContainers(containerObjects);
					k8sObjectsList.add(k8sObject);
				}
				listRecommendationsSO.setKubernetesObjects(k8sObjectsList);
				return listRecommendationsSO;
			}

			public static ExperimentResultData convertUpdateResultsSOToExperimentResultData(UpdateResultsSO updateResultsSO) {
				ExperimentResultData experimentResultData = new ExperimentResultData();
				experimentResultData.setStarttimestamp(updateResultsSO.getStartTimestamp());
				experimentResultData.setEndtimestamp(updateResultsSO.getEndTimestamp());
				experimentResultData.setExperiment_name(updateResultsSO.getExperimentName());
				List<DeploymentResultData> deploymentResultDataList = new ArrayList<DeploymentResultData>();
				for (K8sObject k8sObject : updateResultsSO.getKubernetesObjects()) {
					DeploymentResultData deploymentResultData = new DeploymentResultData();
					deploymentResultData.setDeployment_name(k8sObject.getName());
					deploymentResultData.setNamespace(k8sObject.getNamespace());
					List<Containers> containersList =  new ArrayList<Containers>();
					for (ContainerObject containerObject: k8sObject.getContainers()) {
						Containers containers =  new Containers();
						containers.setContainer_name(containerObject.getContainer_name());
						containers.setImage_name(containerObject.getImage());
						HashMap<AnalyzerConstants.MetricName, HashMap<String, Results>> metricsMap =  new HashMap<>();
						for (ContainerMetricsHelper containerMetricsHelper : containerObject.getMetrics()) {
							HashMap<String, Results> resultsHashMap =  new HashMap<>();
							resultsHashMap.put("results", containerMetricsHelper.getResults());
							AnalyzerConstants.MetricName metricName = getAppropriateMetricName(containerMetricsHelper.getName());
							if (null != metricName) {
								metricsMap.put(metricName, resultsHashMap);
							}
						}
						containers.setContainer_metrics(metricsMap);
						containersList.add(containers);
					}
					deploymentResultData.setContainers(containersList);
					deploymentResultDataList.add(deploymentResultData);
				}
				experimentResultData.setDeployments(deploymentResultDataList);
				return experimentResultData;
			}
		}
	}
}
