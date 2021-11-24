package com.autotune.analyzer.utils;

import com.autotune.analyzer.application.ApplicationServiceStack;
import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.datasource.DataSource;
import com.autotune.analyzer.datasource.DataSourceFactory;
import com.autotune.analyzer.deployment.DeploymentInfo;
import com.autotune.analyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.analyzer.k8sObjects.AutotuneConfig;
import com.autotune.analyzer.k8sObjects.AutotuneObject;
import com.autotune.analyzer.k8sObjects.FunctionVariable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;

public class ServiceHelpers {
	private ServiceHelpers() { }

	public static void addExperimentDetails(JSONObject jsonObject, AutotuneObject autotuneObject) {
		jsonObject.put(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME, autotuneObject.getExperimentName());
		jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.DIRECTION, autotuneObject.getSloInfo().getDirection());
		jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION, autotuneObject.getSloInfo().getObjectiveFunction());
		jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS, autotuneObject.getSloInfo().getSloClass());
		jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.ID, autotuneObject.getExperimentId());
		jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.HPO_ALGO_IMPL, autotuneObject.getSloInfo().getHpoAlgoImpl());
	}

	public static void addLayerDetails(JSONObject layerJson, AutotuneConfig autotuneConfig) {
		layerJson.put(AnalyzerConstants.AutotuneConfigConstants.ID, autotuneConfig.getLayerId());
		layerJson.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_NAME, autotuneConfig.getLayerName());
		layerJson.put(AnalyzerConstants.ServiceConstants.LAYER_DETAILS, autotuneConfig.getDetails());
		layerJson.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_LEVEL, autotuneConfig.getLevel());
	}

	public static void addLayerTunableDetails(JSONArray tunablesArray, AutotuneConfig autotuneConfig, String sloClass) {
		for (Tunable tunable : autotuneConfig.getTunables()) {
			if (sloClass == null || tunable.sloClassList.contains(sloClass)) {
				JSONObject tunableJson = new JSONObject();
				String tunableQuery = tunable.getQueries().get(DeploymentInfo.getMonitoringAgent());
				tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.NAME, tunable.getName());
				tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.UPPER_BOUND, tunable.getUpperBound());
				tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.LOWER_BOUND, tunable.getLowerBound());
				tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.VALUE_TYPE, tunable.getValueType());
				tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.STEP, tunable.getStep());
				try {
					String query = AnalyzerConstants.NONE;
					final DataSource dataSource = DataSourceFactory.getDataSource(DeploymentInfo.getMonitoringAgent());
					// If tunable has a query specified
					if (tunableQuery != null && !tunableQuery.isEmpty()) {
						query = Objects.requireNonNull(dataSource).getDataSourceURL() +
								dataSource.getQueryEndpoint() + tunableQuery;
					}
					tunableJson.put(AnalyzerConstants.ServiceConstants.QUERY_URL, query);
				} catch (MonitoringAgentNotFoundException e) {
					tunableJson.put(AnalyzerConstants.ServiceConstants.QUERY_URL, tunableQuery);
				}
				tunablesArray.put(tunableJson);
			}
		}
	}

	public static void addFunctionVariablesDetails(JSONObject jsonObject, AutotuneObject autotuneObject) {
		// Add function_variables info
		JSONArray functionVariablesArray = new JSONArray();
		for (FunctionVariable functionVariable : autotuneObject.getSloInfo().getFunctionVariables()) {
			JSONObject functionVariableJson = new JSONObject();
			functionVariableJson.put(AnalyzerConstants.AutotuneObjectConstants.NAME, functionVariable.getName());
			functionVariableJson.put(AnalyzerConstants.AutotuneObjectConstants.VALUE_TYPE, functionVariable.getValueType());
			try {
				final DataSource dataSource = DataSourceFactory.getDataSource(DeploymentInfo.getMonitoringAgent());
				functionVariableJson.put(AnalyzerConstants.ServiceConstants.QUERY_URL, Objects.requireNonNull(dataSource).getDataSourceURL() +
						dataSource.getQueryEndpoint() + functionVariable.getQuery());
			} catch (MonitoringAgentNotFoundException e) {
				functionVariableJson.put(AnalyzerConstants.ServiceConstants.QUERY_URL, functionVariable.getQuery());
			}
			functionVariablesArray.put(functionVariableJson);
		}
		jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLES, functionVariablesArray);
	}
}
