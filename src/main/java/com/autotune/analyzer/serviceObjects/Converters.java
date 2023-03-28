package com.autotune.analyzer.serviceObjects;

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.kruizeObject.ObjectiveFunction;
import com.autotune.analyzer.kruizeObject.SloInfo;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.AggregationFunctions;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.Utils;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.*;

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
			KruizeObject kruizeObject = new KruizeObject();
			List<K8sObject> k8sObjectList = createExperimentSO.getKubernetesObjects();
			for (K8sObject k8sObject : k8sObjectList) {
				for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
					ContainerRecommendations containerRecommendations = new ContainerRecommendations();
					containerData.setContainerRecommendations(containerRecommendations);
				}
			}
			kruizeObject.setKubernetes_objects(k8sObjectList);
			kruizeObject.setExperimentName(createExperimentSO.getExperimentName());
			kruizeObject.setApiVersion(createExperimentSO.getApiVersion());
			kruizeObject.setTarget_cluster(createExperimentSO.getTargetCluster());
			kruizeObject.setClusterName(createExperimentSO.getClusterName());
			kruizeObject.setMode(createExperimentSO.getMode());
			kruizeObject.setPerformanceProfile(createExperimentSO.getPerformanceProfile());
			kruizeObject.setSloInfo(createExperimentSO.getSloInfo());
			kruizeObject.setTrial_settings(createExperimentSO.getTrialSettings());
			kruizeObject.setRecommendation_settings(createExperimentSO.getRecommendationSettings());
			return kruizeObject;
		}

		public static ListRecommendationsSO convertKruizeObjectToListRecommendationSO(
				KruizeObject kruizeObject,
				boolean getLatest,
				boolean checkForTimestamp,
				String monitoringEndTimestamp) {
			ListRecommendationsSO listRecommendationsSO = new ListRecommendationsSO();
			try {
				listRecommendationsSO.setApiVersion(kruizeObject.getApiVersion());
				listRecommendationsSO.setExperimentName(kruizeObject.getExperimentName());
				listRecommendationsSO.setClusterName(kruizeObject.getClusterName());
				List<K8sObject> k8sObjectsList = new ArrayList<>();
				for (K8sObject k8sObject : kruizeObject.getKubernetes_objects()) {
					k8sObject.setName(k8sObject.getName());
					k8sObject.setType(k8sObject.getType()); // TODO: Need to check for proper type here
					k8sObject.setNamespace(k8sObject.getNamespace());
					HashMap<String, ContainerData> containerDataMap = new HashMap<>();
					for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
						// if a Time stamp is passed it holds the priority than latest
						if (checkForTimestamp) {
							// This step causes a performance degradation, need to be replaced with a better flow of creating SO's
							ContainerData clonedContainerData = Utils.getClone(containerData, ContainerData.class);
							if (null != clonedContainerData) {
								HashMap<Timestamp, HashMap<String, HashMap<String, Recommendation>>> recommendations = clonedContainerData.getContainerRecommendations().getData();
								Date medDate = Utils.DateUtils.getDateFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, monitoringEndTimestamp);
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
									containerDataMap.put(clonedContainerData.getContainer_name(), clonedContainerData);
								}
							}
						} else if (getLatest) {
							// This step causes a performance degradation, need to be replaced with a better flow of creating SO's
							ContainerData clonedContainerData = Utils.getClone(containerData, ContainerData.class);
							if (null != clonedContainerData) {
								HashMap<Timestamp, HashMap<String, HashMap<String, Recommendation>>> recommendations = clonedContainerData.getContainerRecommendations().getData();
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
								containerDataMap.put(clonedContainerData.getContainer_name(), clonedContainerData);
							}
						} else {
							containerDataMap.put(containerData.getContainer_name(), containerData);
						}
					}
					k8sObject.setContainerDataMap(containerDataMap);
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
		public static List<CreateExperimentSO> convertInputJSONToCreateExperimentSO(String inputData) {
			List<CreateExperimentSO> createExperimentSOList = null;
			if (inputData != null) {
				List<JsonObject> jsonObjects = Arrays.asList(new Gson().fromJson(inputData, JsonObject[].class));
				// stream through the jsonObjectList and remove the 'kubernetes_objects' object
				jsonObjects.forEach(jsonObject -> jsonObject.remove("kubernetes_objects"));
				//cast the modified jsonObjects list to corresponding classes
				createExperimentSOList = new Gson().fromJson(jsonObjects.toString(), new TypeToken<List<CreateExperimentSO>>() {}.getType());

				// Now get the K8S object from the original JSON array and modify it to match the K8SObject class
				JSONArray jsonArray = new JSONArray(inputData);
				int count = -1;
				for (Object object : jsonArray) {
					count++;
					JSONObject kruizeObjJson = (JSONObject) object;
					JSONArray k8sJsonArray = kruizeObjJson.getJSONArray(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS);
					List<K8sObject> k8sObjectList = new ArrayList<>();
					K8sObject k8sObject = null;
					for (Object innerObject : k8sJsonArray) {
						JSONObject k8sJsonObject = (JSONObject) innerObject;
						String type = k8sJsonObject.getString(KruizeConstants.JSONKeys.TYPE);
						String name = k8sJsonObject.getString(KruizeConstants.JSONKeys.NAME);
						String namespace = k8sJsonObject.getString(KruizeConstants.JSONKeys.NAME);
						JSONArray containerDataArr = k8sJsonObject.getJSONArray(KruizeConstants.JSONKeys.CONTAINERS);
						HashMap<String, ContainerData> containerDataMap = new HashMap<>();
						for (Object containerDataObj : containerDataArr) {
							JSONObject containerObjectJson = (JSONObject) containerDataObj;
							String containerName = containerObjectJson.getString(KruizeConstants.JSONKeys.CONTAINER_NAME);
							String containerImgName = containerObjectJson.getString(KruizeConstants.JSONKeys.CONTAINER_IMAGE_NAME);
							ContainerData containerData = new ContainerData(containerName, containerImgName, null);
							containerDataMap.put(containerName, containerData);
						}
						k8sObject = new K8sObject(name, type, namespace);
						k8sObject.setContainerDataMap(containerDataMap);
					}
					k8sObjectList.add(k8sObject);
					createExperimentSOList.get(count).setKubernetesObjects(k8sObjectList);
				}
			}
			return createExperimentSOList;
		}

		public static PerformanceProfile convertInputJSONToCreatePerfProfile(String inputData) throws InvalidValueException {
			PerformanceProfile performanceProfile = null;
			if (inputData != null) {
				JSONObject jsonObject = new JSONObject(inputData);
				String perfProfileName = jsonObject.getString(AnalyzerConstants.AutotuneObjectConstants.NAME);
				double profileVersion = jsonObject.getDouble(AnalyzerConstants.PROFILE_VERSION);
				String k8sType = jsonObject.getString(AnalyzerConstants.PerformanceProfileConstants.K8S_TYPE);
				JSONObject sloJsonObject = jsonObject.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.SLO);
				JSONArray functionVariableArray = sloJsonObject.getJSONArray(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLES);
				List<Metric> functionVariablesList = new ArrayList<>();
				for (Object object : functionVariableArray) {
					JSONObject functionVarObj = (JSONObject) object;
					String name = functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.NAME);
					String datasource = functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.DATASOURCE);
					String query = functionVarObj.has(AnalyzerConstants.AutotuneObjectConstants.QUERY) ? functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.QUERY):null;
					String valueType = functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.VALUE_TYPE);
					String kubeObject = functionVarObj.getString(AnalyzerConstants.KUBERNETES_OBJECT);
					Metric metric = new Metric(name, query, datasource, valueType, kubeObject);
					JSONArray aggrFunctionArray = functionVarObj.has(AnalyzerConstants.AGGREGATION_FUNCTIONS) ? functionVarObj.getJSONArray(AnalyzerConstants.AGGREGATION_FUNCTIONS):null;
					for (Object innerObject : aggrFunctionArray) {
						JSONObject aggrFuncJsonObject = (JSONObject) innerObject;
						HashMap<String, AggregationFunctions> aggregationFunctionsMap = new HashMap<>();
						String function = aggrFuncJsonObject.getString(AnalyzerConstants.FUNCTION);
						String aggrFuncQuery = aggrFuncJsonObject.getString(KruizeConstants.JSONKeys.QUERY);
						AggregationFunctions aggregationFunctions = new AggregationFunctions(function, aggrFuncQuery, null );
						aggregationFunctionsMap.put(function, aggregationFunctions);
						metric.setAggregationFunctionsMap(aggregationFunctionsMap);
					}
					functionVariablesList.add(metric);
				}
				String sloClass = sloJsonObject.get(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS).toString();
				String direction = sloJsonObject.get(AnalyzerConstants.AutotuneObjectConstants.DIRECTION).toString();
				ObjectiveFunction objectiveFunction = new Gson().fromJson(sloJsonObject.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION).toString(), ObjectiveFunction.class);
				SloInfo sloInfo = new SloInfo(sloClass, objectiveFunction, direction, (ArrayList<Metric>) functionVariablesList);
				performanceProfile = new PerformanceProfile(perfProfileName, profileVersion, k8sType, sloInfo);
			}
			return performanceProfile;
		}

	}
}
