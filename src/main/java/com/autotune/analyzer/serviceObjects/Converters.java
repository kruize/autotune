package com.autotune.analyzer.serviceObjects;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.k8sObjects.DeploymentObject;
import com.autotune.common.k8sObjects.K8sObject;

import java.util.ArrayList;
import java.util.List;

import static com.autotune.utils.Utils.getAppropriateK8sObjectTypeString;

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

		public static ListRecommendationsSO convertKruizeObjectToListRecommendationSO(KruizeObject kruizeObject) {
			// Need to be implemented if needed
			ListRecommendationsSO listRecommendationsSO =  new ListRecommendationsSO();
			listRecommendationsSO.setApiVersion(kruizeObject.getApiVersion());
			listRecommendationsSO.setExperimentName(kruizeObject.getExperimentName());
			listRecommendationsSO.setClusterName(kruizeObject.getClusterName());
			List<K8sObject> k8sObjectsList = new ArrayList<>();
			for (DeploymentObject deploymentObject : kruizeObject.getDeployments().values()) {
				K8sObject k8sObject = new K8sObject();
				k8sObject.setName(deploymentObject.getName());
				k8sObject.setType(getAppropriateK8sObjectTypeString(deploymentObject.getType()));
				k8sObject.setNamespace(deploymentObject.getNamespace());
				List<ContainerData> containerDataList = new ArrayList<>();
				for (ContainerData containerData : deploymentObject.getContainers().values()) {
					containerDataList.add(containerData);
				}
				k8sObject.setContainerDataList(containerDataList);
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
			List<K8sObject> k8sObjectList = updateResultsSO.getKubernetesObjects();
			experimentResultData.setKubernetes_objects(k8sObjectList);
			return experimentResultData;
		}
	}
}
