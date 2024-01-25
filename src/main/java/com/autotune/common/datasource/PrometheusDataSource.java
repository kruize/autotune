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
package com.autotune.common.datasource;

import com.autotune.analyzer.exceptions.TooManyRecursiveCallsException;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.utils.CommonUtils;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.GenericRestApiClient;
import com.autotune.utils.authModels.BearerAccessToken;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class PrometheusDataSource extends DataSourceInfo
{
	private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusDataSource.class);

	private final String dataSourceURL;

	public PrometheusDataSource(String name, String provider, String serviceName, String namespace) {
		super(name, provider, serviceName, namespace);
		String url = "";
		if (KruizeDeploymentInfo.k8s_type.equalsIgnoreCase(KruizeConstants.MINIKUBE)) {
			url  = AnalyzerConstants.HTTP_PROTOCOL + "://" + serviceName + "." + namespace + KruizeConstants.DataSourceConstants.PROMETHEUS_DATASOURCE_DNS + ":" + KruizeConstants.DataSourceConstants.PROMETHEUS_PORT;
		} else if (KruizeDeploymentInfo.k8s_type.equalsIgnoreCase(KruizeConstants.OPENSHIFT)) {
			url = AnalyzerConstants.HTTPS_PROTOCOL + "://" + serviceName + "." + namespace + KruizeConstants.DataSourceConstants.PROMETHEUS_DATASOURCE_DNS + ":" + KruizeConstants.DataSourceConstants.PROMETHEUS_PORT;;
		}
		this.dataSourceURL = url;
	}

	public PrometheusDataSource(String name, String provider, URL url) {
		super(name, provider, url);
		this.dataSourceURL = url.toString();
	}

	public String getQueryEndpoint() {
		return AnalyzerConstants.PROMETHEUS_API;
	}

	/**
	 * Returns the datasource endpoint from which queries can be run
	 * @return String containing the URL
	 */
	public String getDataSourceURL() {
		return dataSourceURL;
	}

	public String getToken() throws IOException {
		String fileName = KruizeConstants.AUTH_MOUNT_PATH+"token";
		String authToken = new String(Files.readAllBytes(Paths.get(fileName)));
		return authToken;
	}

	private JSONArray getAsJsonArray(String response) throws IndexOutOfBoundsException {
		JSONObject jsonObject = new JSONObject(response);

		return jsonObject
				.getJSONObject("data")
				.getJSONArray("result")
				.getJSONObject(0)
				.getJSONArray("value");
	}

	private String getValueForQuery(String response) throws IndexOutOfBoundsException {
		try {
			return getAsJsonArray(response)
					.getString(1);

		} catch (Exception e) {
			LOGGER.error(response);
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * Run the getAppsForLayer and return the list of applications matching the layer.
	 * @param query getAppsForLayer query for the layer
	 * @param key The key to search for in the response
	 * @return ArrayList of all applications from the query
	 * @throws MalformedURLException
	 */
	public ArrayList<String> getAppsForLayer(String query, String key) throws MalformedURLException {
		String response = null;
		ArrayList<String> valuesList = new ArrayList<>();
		String queryURL = dataSourceURL + getQueryEndpoint() + query;
		LOGGER.debug("Query URL is: {}", queryURL);
		try {
			GenericRestApiClient genericRestApiClient = new GenericRestApiClient(
					dataSourceURL + getQueryEndpoint(),
					new BearerAccessToken(this.getToken())
			);
			JSONObject responseJson = genericRestApiClient.fetchMetricsJson("GET", query);
			int level = 0;
			try {
				DataSourceFactory.parseJsonForKey(responseJson, key, valuesList, level);
				LOGGER.debug("Applications for the query: {}", valuesList.toString());
			} catch (TooManyRecursiveCallsException e) {
				e.printStackTrace();
			}
		} catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
			LOGGER.error("Unable to proceed due to invalid connection to URL: "+ queryURL);
		}
		return valuesList;
	}

	/**
	 * Check if a datasource is reachable, implementation of this function
	 * should check and return the reachability status (REACHABLE, NOT_REACHABLE)
	 * @return DatasourceReachabilityStatus
	 */
	public CommonUtils.DatasourceReachabilityStatus isReachable() {
		String status;
		Object result;
		String query = KruizeConstants.DataSourceConstants.Reachability_Query;
		CommonUtils.DatasourceReachabilityStatus reachabilityStatus;

		KruizeDataSourceOperator ado = DataSourceOperator.getOperator(KruizeConstants.SupportedDatasources.PROMETHEUS);
		result = ado.getPrometheusDataValue(this.dataSourceURL, query);
		if (result != null){
			status = result.toString();
		} else {
			status = "0";
		}

		if (status.equalsIgnoreCase("1")){
			reachabilityStatus = CommonUtils.DatasourceReachabilityStatus.REACHABLE;
		} else {
			reachabilityStatus = CommonUtils.DatasourceReachabilityStatus.NOT_REACHABLE;
		}
		return reachabilityStatus;
	}
}
