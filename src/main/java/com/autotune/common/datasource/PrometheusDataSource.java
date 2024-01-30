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
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.GenericRestApiClient;
import com.autotune.utils.authModels.BearerAccessToken;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

// TODO: This class will be deleted in next cleanup PR and dataSourceInfo class will be used
public class PrometheusDataSource implements DataSource
{
	private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusDataSource.class);

	private final String dataSourceURL;

	public PrometheusDataSource(String monitoringAgentEndpoint) {
		this.dataSourceURL = monitoringAgentEndpoint;
	}

	/**
	 * Returns the datasource endpoint from which queries can be run
	 * @return String containing the URL
	 */
	public String getDataSourceURL() {
		return dataSourceURL;
	}

	@Override
	public String getQueryEndpoint() {
		return AnalyzerConstants.PROMETHEUS_API;
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

}
