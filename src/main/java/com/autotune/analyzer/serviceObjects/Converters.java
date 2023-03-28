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
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Converters {
	private Converters() {

	}

	public static class KruizeObjectConverters {
		private KruizeObjectConverters() {

		}

		public static CreateExperimentAPIObject convertKruizeObjectToCreateExperimentSO(KruizeObject kruizeObject) {
			// Need to be implemented if needed
			return null;
		}

		public static KruizeObject convertCreateExperimentAPIObjToKruizeObject(CreateExperimentAPIObject createExperimentAPIObject) {
			KruizeObject kruizeObject = new KruizeObject();
			List<K8sObject> k8sObjectList = new ArrayList<>();
			List<KubernetesObject> kubernetesObjectsList = createExperimentAPIObject.getKubernetesObjects();
			for (KubernetesObject kubernetesObject : kubernetesObjectsList) {
				K8sObject k8sObject = new K8sObject(kubernetesObject.getName(), kubernetesObject.getType(), kubernetesObject.getNamespace());
				List<Container> containersList = kubernetesObject.getContainers();
				HashMap<String, ContainerData> containerDataHashMap = new HashMap<>();
				for (Container container : containersList) {
					ContainerData containerData = new ContainerData(container.getContainer_name(),
							container.getContainer_image_name(), new ContainerRecommendations(), null);
					containerDataHashMap.put(containerData.getContainer_name(), containerData);
				}
				k8sObject.setContainerDataMap(containerDataHashMap);
				k8sObjectList.add(k8sObject);
			}
			kruizeObject.setKubernetes_objects(k8sObjectList);
			kruizeObject.setExperimentName(createExperimentAPIObject.getExperimentName());
			kruizeObject.setApiVersion(createExperimentAPIObject.getApiVersion());
			kruizeObject.setTarget_cluster(createExperimentAPIObject.getTargetCluster());
			kruizeObject.setClusterName(createExperimentAPIObject.getClusterName());
			kruizeObject.setMode(createExperimentAPIObject.getMode());
			kruizeObject.setPerformanceProfile(createExperimentAPIObject.getPerformanceProfile());
			kruizeObject.setSloInfo(createExperimentAPIObject.getSloInfo());
			kruizeObject.setTrial_settings(createExperimentAPIObject.getTrialSettings());
			kruizeObject.setRecommendation_settings(createExperimentAPIObject.getRecommendationSettings());
			return kruizeObject;
		}

		public static ListRecommendationsAPIObject convertKruizeObjectToListRecommendationSO(
				KruizeObject kruizeObject,
				boolean getLatest,
				boolean checkForTimestamp,
				String monitoringEndTimestamp) {
			ListRecommendationsAPIObject listRecommendationsAPIObject = new ListRecommendationsAPIObject();
			try {
				listRecommendationsAPIObject.setApiVersion(kruizeObject.getApiVersion());
				listRecommendationsAPIObject.setExperimentName(kruizeObject.getExperimentName());
				listRecommendationsAPIObject.setClusterName(kruizeObject.getClusterName());
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
				listRecommendationsAPIObject.setKubernetesObjects(k8sObjectsList);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return listRecommendationsAPIObject;
		}

		public static ExperimentResultData convertUpdateResultsAPIObjToExperimentResultData(UpdateResultsAPIObject updateResultsAPIObject) {
			ExperimentResultData experimentResultData = new ExperimentResultData();
			experimentResultData.setStarttimestamp(updateResultsAPIObject.getStartTimestamp());
			experimentResultData.setEndtimestamp(updateResultsAPIObject.getEndTimestamp());
			experimentResultData.setExperiment_name(updateResultsAPIObject.getExperimentName());
			List<KubernetesObject> kubernetesObjectList = updateResultsAPIObject.getKubernetesObjects();
			List<K8sObject> k8sObjectList = new ArrayList<>();
			for (KubernetesObject kubernetesObject : kubernetesObjectList) {
				K8sObject k8sObject = new K8sObject(kubernetesObject.getName(), kubernetesObject.getType(), kubernetesObject.getNamespace());
				List<Container> containersList = kubernetesObject.getContainers();
				HashMap<String, ContainerData> containerDataHashMap = new HashMap<>();
				for (Container container : containersList) {
					HashMap<AnalyzerConstants.MetricName, Metric> metricsMap = new HashMap<>();
					System.out.println("******* container = "+container.toString());
					for (Metric metric : container.getMetrics())
						metricsMap.put(AnalyzerConstants.MetricName.valueOf(metric.getName()), metric);
					ContainerData containerData = new ContainerData(container.getContainer_name(), container.getContainer_image_name(), container.getContainerRecommendations(), metricsMap);
					containerDataHashMap.put(containerData.getContainer_name(), containerData);
				}
				k8sObject.setContainerDataMap(containerDataHashMap);
				k8sObjectList.add(k8sObject);
			}
			experimentResultData.setKubernetes_objects(k8sObjectList);
			return experimentResultData;
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

		public static ConcurrentHashMap<String, CreateExperimentAPIObject> ConvertExperimentDataToAPIResponse(ConcurrentHashMap<String, KruizeObject> mainKruizeExperimentMap) {
			ConcurrentHashMap<String, CreateExperimentAPIObject> createExperimentMap = new ConcurrentHashMap<>();

			for (Map.Entry<String, KruizeObject> entry : mainKruizeExperimentMap.entrySet()) {
				String experimentName = entry.getKey();
				KruizeObject kruizeObject = entry.getValue();
				CreateExperimentAPIObject createExperimentObject = new CreateExperimentAPIObject();

				// set fields in createExperimentObject using kruizeObject
				createExperimentObject.setClusterName(kruizeObject.getClusterName());
				createExperimentObject.setPerformanceProfile(kruizeObject.getPerformanceProfile());
				createExperimentObject.setMode(kruizeObject.getMode());
				createExperimentObject.setTargetCluster(kruizeObject.getTarget_cluster());
				List<KubernetesObject> kubernetesObjects = new ArrayList<>();
				for (K8sObject k8sObject : kruizeObject.getKubernetes_objects()) {
					KubernetesObject kubernetesObject = new KubernetesObject(k8sObject.getName(), k8sObject.getType(),
							k8sObject.getNamespace());
					List<Container> containerList = new ArrayList<>();
					for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
						Container container = new Container(containerData.getContainer_name(),
								containerData.getContainer_image_name(), containerData.getContainerRecommendations(), (List<Metric>) containerData.getMetrics().values());
						containerList.add(container);
					}
					kubernetesObject.setContainers(containerList);
					kubernetesObjects.add(kubernetesObject);
				}
				createExperimentObject.setKubernetesObjects(kubernetesObjects);
				createExperimentObject.setTrialSettings(kruizeObject.getTrial_settings());
				createExperimentObject.setRecommendationSettings(kruizeObject.getRecommendation_settings());
				createExperimentObject.setSloInfo(kruizeObject.getSloInfo());

				createExperimentMap.put(experimentName, createExperimentObject);
			}
			return createExperimentMap;
		}

		public static ConcurrentHashMap<String, KruizeObject> ConvertUpdateResultDataToAPIResponse(ConcurrentHashMap<String, KruizeObject> mainKruizeExperimentMap) {
			return null;
		}

		public static ConcurrentHashMap<String, KruizeObject> ConvertRecommendationDataToAPIResponse(ConcurrentHashMap<String, KruizeObject> mainKruizeExperimentMap) {
			return null;
		}

	}
}
