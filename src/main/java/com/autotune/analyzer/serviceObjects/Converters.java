package com.autotune.analyzer.serviceObjects;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.Utils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Converters {
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
			List<K8sObject> k8sObjectList = createExperimentSO.getKubernetesObjects();
			for (K8sObject k8sObject : k8sObjectList) {
				for (ContainerData containerData : k8sObject.getContainerDataList()) {
					ContainerRecommendations containerRecommendations = new ContainerRecommendations();
					containerData.setContainerRecommendations(containerRecommendations);
				}
			}
			kruizeObject.setKubernetesObjects(k8sObjectList);
			kruizeObject.setExperimentName(createExperimentSO.getExperimentName());
			kruizeObject.setApiVersion(createExperimentSO.getApiVersion());
			kruizeObject.setTarget_cluster(createExperimentSO.getTargetCluster());
			kruizeObject.setClusterName(createExperimentSO.getClusterName());
			kruizeObject.setMode(createExperimentSO.getMode());
			kruizeObject.setPerformanceProfile(createExperimentSO.getPerformanceProfile());
			kruizeObject.setTrial_settings(createExperimentSO.getTrialSettings());
			kruizeObject.setRecommendation_settings(createExperimentSO.getRecommendationSettings());
			return kruizeObject;
		}
//		public static KruizeObject convertCreateExperimentSOToKruizeObject(CreateExperimentSO createExperimentSO) {
//			// To be implemented
//			KruizeObject kruizeObject =  new KruizeObject();
//			HashMap<String, DeploymentObject> deploymentObjectHashMap = new HashMap<>();
//			for (K8sObject k8sObject: createExperimentSO.getKubernetesObjects()) {
//				if (null != k8sObject.getName() && !k8sObject.getName().isBlank()) {
//					DeploymentObject deploymentObject = new DeploymentObject(k8sObject.getName());
//					AnalyzerConstants.K8S_OBJECT_TYPES objectType = Utils.getApproriateK8sObjectType(k8sObject.getType());
//					if (null != objectType)
//						deploymentObject.setType(objectType);
//					HashMap<String, ContainerData> containerObjectHashMap = new HashMap<>();
//					for (ContainerData containerData: k8sObject.getContainerDataList()) {
//						ContainerRecommendations containerRecommendations =  new ContainerRecommendations();
//						containerData.setContainerRecommendations(containerRecommendations);
//						containerObjectHashMap.put(containerData.getContainer_name(), containerData);
//					}
//					deploymentObject.setContainers(containerObjectHashMap);
//					deploymentObject.setNamespace(k8sObject.getNamespace());
//					// TODO: Need to be changed as it should not be set at higher level
//					kruizeObject.setNamespace(k8sObject.getNamespace());
//					deploymentObjectHashMap.put(k8sObject.getName(), deploymentObject);
//				}
//			}
//			kruizeObject.setDeployments(deploymentObjectHashMap);
//			kruizeObject.setExperimentName(createExperimentSO.getExperimentName());
//			kruizeObject.setApiVersion(createExperimentSO.getApiVersion());
//			kruizeObject.setTarget_cluster(createExperimentSO.getTargetCluster());
//			kruizeObject.setClusterName(createExperimentSO.getClusterName());
//			kruizeObject.setMode(createExperimentSO.getMode());
//			kruizeObject.setPerformanceProfile(createExperimentSO.getPerformanceProfile());
//			kruizeObject.setTrial_settings(createExperimentSO.getTrialSettings());
//			kruizeObject.setRecommendation_settings(createExperimentSO.getRecommendationSettings());
//			return kruizeObject;
//		}

		public static ListRecommendationsSO convertKruizeObjectToListRecommendationSO(
				KruizeObject kruizeObject,
				boolean getLatest,
				boolean checkForTimestamp,
				String monitoringEndTimestamp) {
			ListRecommendationsSO listRecommendationsSO =  new ListRecommendationsSO();
			try {
				listRecommendationsSO.setApiVersion(kruizeObject.getApiVersion());
				listRecommendationsSO.setExperimentName(kruizeObject.getExperimentName());
				listRecommendationsSO.setClusterName(kruizeObject.getClusterName());
				List<K8sObject> k8sObjectsList = new ArrayList<>();
				for (K8sObject k8sObject : kruizeObject.getKubernetesObjects()) {
					k8sObject.setName(k8sObject.getName());
					k8sObject.setType(k8sObject.getType()); // TODO: Need to check for proper type here
					k8sObject.setNamespace(k8sObject.getNamespace());
					List<ContainerData> containerDataList = new ArrayList<>();
					for (ContainerData containerData: k8sObject.getContainerDataList()) {
						// if a Time stamp is passed it holds the priority than latest
						if (checkForTimestamp) {
							// This step causes a performance degradation, need to be replaced with a better flow of creating SO's
							ContainerData clonedContainerData = Utils.getClone(containerData, ContainerData.class);
							if (null != clonedContainerData) {
								HashMap<Timestamp, HashMap<String, HashMap<String, Recommendation>>> recommendations = clonedContainerData.getContainerRecommendations().getData();
								Date medDate = Utils.DateUtils.getDateFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT,monitoringEndTimestamp);
								Timestamp givenTimestamp = new Timestamp(medDate.getTime());
								if (recommendations.containsKey(givenTimestamp)) {
									List<Timestamp> tempList = new ArrayList<>();
									for (Timestamp timestamp : recommendations.keySet()) {
										if (!timestamp.equals(givenTimestamp))
											tempList.add(timestamp);
									}
									for (Timestamp timestamp : tempList) {
										recommendations.remove(timestamp);
									}
									containerDataList.add(clonedContainerData);
								}
							}
						} else if (getLatest) {
							// This step causes a performance degradation, need to be replaced with a better flow of creating SO's
							ContainerData clonedContainerData = Utils.getClone(containerData, ContainerData.class);
							if (null != clonedContainerData) {
								HashMap<Timestamp, HashMap<String,HashMap<String, Recommendation>>> recommendations = clonedContainerData.getContainerRecommendations().getData();
								Timestamp latestTimestamp = null;
								List<Timestamp> tempList = new ArrayList<>();
								for (Timestamp timestamp : recommendations.keySet()) {
									if (null == latestTimestamp) {
										latestTimestamp = timestamp;
									} else {
										if (timestamp.after(latestTimestamp)) {
											tempList.add(latestTimestamp);
											latestTimestamp = timestamp;
										} else {
											tempList.add(timestamp);
										}
									}
								}
								for (Timestamp timestamp : tempList) {
									recommendations.remove(timestamp);
								}
								containerDataList.add(clonedContainerData);
							}
						} else {
							containerDataList.add(containerData);
						}
					}
					k8sObject.setContainerDataList(containerDataList);
					k8sObjectsList.add(k8sObject);
				}
				listRecommendationsSO.setKubernetesObjects(k8sObjectsList);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return listRecommendationsSO;
		}

		public static ExperimentResultData convertUpdateResultsSOToExperimentResultData(UpdateResultsSO updateResultsSO) {
			ExperimentResultData experimentResultData = new ExperimentResultData();
			experimentResultData.setStarttimestamp(updateResultsSO.getStartTimestamp());
			experimentResultData.setEndtimestamp(updateResultsSO.getEndTimestamp());
			experimentResultData.setExperiment_name(updateResultsSO.getExperimentName());
			List<K8sObject> k8sObjectList = updateResultsSO.getKubernetesObjects();
			experimentResultData.setKubernetes_objects(k8sObjectList);
			return experimentResultData;
		}
	}
}
