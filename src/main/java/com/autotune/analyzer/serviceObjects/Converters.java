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
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.Utils;
import com.google.gson.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Converters {
	private Converters() {

	}

	public static class KruizeObjectConverters {
		private static final Logger LOGGER = LoggerFactory.getLogger(KruizeObjectConverters.class);
		private KruizeObjectConverters() {

		}

		public static CreateExperimentAPIObject convertKruizeObjectToCreateExperimentSO(KruizeObject kruizeObject) {
			// Need to be implemented if needed
			return null;
		}

		public static KruizeObject convertCreateExperimentAPIObjToKruizeObject(CreateExperimentAPIObject createExperimentAPIObject) {
			KruizeObject kruizeObject = new KruizeObject();
			List<K8sObject> k8sObjectList = new ArrayList<>();
			List<KubernetesAPIObject> kubernetesAPIObjectsList = createExperimentAPIObject.getKubernetesObjects();
			for (KubernetesAPIObject kubernetesAPIObject : kubernetesAPIObjectsList) {
				K8sObject k8sObject = new K8sObject(kubernetesAPIObject.getName(), kubernetesAPIObject.getType(), kubernetesAPIObject.getNamespace());
				List<ContainerAPIObject> containerAPIObjects = kubernetesAPIObject.getContainerAPIObjects();
				HashMap<String, ContainerData> containerDataHashMap = new HashMap<>();
				for (ContainerAPIObject containerAPIObject : containerAPIObjects) {
					ContainerData containerData = new ContainerData(containerAPIObject.getContainer_name(),
							containerAPIObject.getContainer_image_name(), new ContainerRecommendations(), null);
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
				List<KubernetesAPIObject> kubernetesAPIObjects = new ArrayList<>();
				KubernetesAPIObject kubernetesAPIObject;
				for (K8sObject k8sObject : kruizeObject.getKubernetes_objects()) {
					kubernetesAPIObject = new KubernetesAPIObject(k8sObject.getName(), k8sObject.getType(), k8sObject.getNamespace());
					HashMap<String, ContainerData> containerDataMap = new HashMap<>();
					List<ContainerAPIObject> containerAPIObjects = new ArrayList<>();
					for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
						ContainerAPIObject containerAPIObject;
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
									clonedContainerData.getContainerRecommendations().setData(recommendations);
									containerAPIObject = new ContainerAPIObject(clonedContainerData.getContainer_name(),
											clonedContainerData.getContainer_image_name(),
											clonedContainerData.getContainerRecommendations(),
											new ArrayList<>(clonedContainerData.getMetrics().values()));
									containerAPIObjects.add(containerAPIObject);
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
								clonedContainerData.getContainerRecommendations().setData(recommendations);
								containerAPIObject = new ContainerAPIObject(clonedContainerData.getContainer_name(),
										clonedContainerData.getContainer_image_name(),
										clonedContainerData.getContainerRecommendations(),
										new ArrayList<>(clonedContainerData.getMetrics().values()));
								containerAPIObjects.add(containerAPIObject);
							}
						} else {
							containerAPIObject = new ContainerAPIObject(containerData.getContainer_name(),
									containerData.getContainer_image_name(),
									containerData.getContainerRecommendations(),
									new ArrayList<>(containerData.getMetrics().values()));
							containerAPIObjects.add(containerAPIObject);
							containerDataMap.put(containerData.getContainer_name(), containerData);
						}
					}
					kubernetesAPIObject.setContainerAPIObjects(containerAPIObjects);
					kubernetesAPIObjects.add(kubernetesAPIObject);
				}
				listRecommendationsAPIObject.setKubernetesObjects(kubernetesAPIObjects);
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
			List<KubernetesAPIObject> kubernetesAPIObjectList = updateResultsAPIObject.getKubernetesObjects();
			List<K8sObject> k8sObjectList = new ArrayList<>();
			for (KubernetesAPIObject kubernetesAPIObject : kubernetesAPIObjectList) {
				K8sObject k8sObject = new K8sObject(kubernetesAPIObject.getName(), kubernetesAPIObject.getType(), kubernetesAPIObject.getNamespace());
				List<ContainerAPIObject> containersList = kubernetesAPIObject.getContainerAPIObjects();
				HashMap<String, ContainerData> containerDataHashMap = new HashMap<>();
				for (ContainerAPIObject containerAPIObject : containersList) {
					HashMap<AnalyzerConstants.MetricName, Metric> metricsMap = new HashMap<>();
					HashMap<Timestamp, IntervalResults> resultsMap = new HashMap<>();
					ContainerData containerData = new ContainerData(containerAPIObject.getContainer_name(), containerAPIObject.getContainer_image_name(), containerAPIObject.getContainerRecommendations(), metricsMap);
					HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsHashMap = new HashMap<>();
					for (Metric metric : containerAPIObject.getMetrics()) {
						metricsMap.put(AnalyzerConstants.MetricName.valueOf(metric.getName()), metric);
						MetricResults metricResults = metric.getMetricResult();
						metricResults.setName(metric.getName());
						IntervalResults intervalResults = new IntervalResults(updateResultsAPIObject.startTimestamp,
								updateResultsAPIObject.endTimestamp);
						metricResultsHashMap.put(AnalyzerConstants.MetricName.valueOf(metric.getName()), metricResults);
						intervalResults.setMetricResultsMap(metricResultsHashMap);
						resultsMap.put(updateResultsAPIObject.getEndTimestamp(), intervalResults);
					}
					containerData.setResults(resultsMap);
					containerDataHashMap.put(containerData.getContainer_name(), containerData);
				}
				k8sObject.setContainerDataMap(containerDataHashMap);
				LOGGER.debug("k8sObject = {}", new Gson().toJson(k8sObject));
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
				Double profileVersion = jsonObject.has(AnalyzerConstants.PROFILE_VERSION) ? jsonObject.getDouble(AnalyzerConstants.PROFILE_VERSION) : null;
				String k8sType = jsonObject.has(AnalyzerConstants.PerformanceProfileConstants.K8S_TYPE) ? jsonObject.getString(AnalyzerConstants.PerformanceProfileConstants.K8S_TYPE) : null;
				JSONObject sloJsonObject = jsonObject.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.SLO);
				JSONArray functionVariableArray = sloJsonObject.getJSONArray(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLES);
				ArrayList<Metric> functionVariablesList = new ArrayList<>();
				for (Object object : functionVariableArray) {
					JSONObject functionVarObj = (JSONObject) object;
					String name = functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.NAME);
					String datasource = functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.DATASOURCE);
					String query = functionVarObj.has(AnalyzerConstants.AutotuneObjectConstants.QUERY) ? functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.QUERY):null;
					String valueType = functionVarObj.getString(AnalyzerConstants.AutotuneObjectConstants.VALUE_TYPE);
					String kubeObject = functionVarObj.has(AnalyzerConstants.KUBERNETES_OBJECT) ? functionVarObj.getString(AnalyzerConstants.KUBERNETES_OBJECT) : null;
					Metric metric = new Metric(name, query, datasource, valueType, kubeObject);
					JSONArray aggrFunctionArray = functionVarObj.has(AnalyzerConstants.AGGREGATION_FUNCTIONS) ? functionVarObj.getJSONArray(AnalyzerConstants.AGGREGATION_FUNCTIONS):null;
					for (Object innerObject : aggrFunctionArray) {
						JSONObject aggrFuncJsonObject = (JSONObject) innerObject;
						HashMap<String, AggregationFunctions> aggregationFunctionsMap = new HashMap<>();
						String function = aggrFuncJsonObject.getString(AnalyzerConstants.FUNCTION);
						String aggrFuncQuery = aggrFuncJsonObject.getString(KruizeConstants.JSONKeys.QUERY);
						String version = aggrFuncJsonObject.has(KruizeConstants.JSONKeys.VERSION) ? aggrFuncJsonObject.getString(KruizeConstants.JSONKeys.VERSION) : null;
						AggregationFunctions aggregationFunctions = new AggregationFunctions(function, aggrFuncQuery, version );
						aggregationFunctionsMap.put(function, aggregationFunctions);
						metric.setAggregationFunctionsMap(aggregationFunctionsMap);
					}
					functionVariablesList.add(metric);
				}
				String sloClass = sloJsonObject.has(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS) ? sloJsonObject.get(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS).toString() : null;
				String direction = sloJsonObject.has(AnalyzerConstants.AutotuneObjectConstants.DIRECTION) ? sloJsonObject.get(AnalyzerConstants.AutotuneObjectConstants.DIRECTION).toString() :null;
				ObjectiveFunction objectiveFunction = new Gson().fromJson(sloJsonObject.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION).toString(), ObjectiveFunction.class);
				SloInfo sloInfo = new SloInfo(sloClass, objectiveFunction, direction, functionVariablesList);
				performanceProfile = new PerformanceProfile(perfProfileName, profileVersion, k8sType, sloInfo);
			}
			return performanceProfile;
		}

		public static ConcurrentHashMap<String, KruizeObject> ConvertUpdateResultDataToAPIResponse(ConcurrentHashMap<String, KruizeObject> mainKruizeExperimentMap) {
			return null;
		}

		public static ConcurrentHashMap<String, KruizeObject> ConvertRecommendationDataToAPIResponse(ConcurrentHashMap<String, KruizeObject> mainKruizeExperimentMap) {
			return null;
		}

	}
}
