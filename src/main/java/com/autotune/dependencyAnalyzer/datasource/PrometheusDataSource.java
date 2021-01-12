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
package com.autotune.dependencyAnalyzer.datasource;

import com.autotune.dependencyAnalyzer.exceptions.TooManyRecursiveCallsException;
import com.autotune.dependencyAnalyzer.util.DAConstants;
import com.autotune.dependencyAnalyzer.util.HttpUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class PrometheusDataSource implements DataSource
{
	private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusDataSource.class);

	private final String dataSourceURL;
	private final String token;

	public PrometheusDataSource(String monitoringAgentEndpoint, String token) {
		this.dataSourceURL = monitoringAgentEndpoint;
		this.token = token;
	}

	public String getDataSourceURL() {
		return dataSourceURL;
	}

	public String getToken() {
		return token;
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
			LOGGER.info(response.toString());
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
		String response = HttpUtil.getDataFromURL(new URL(dataSourceURL + DAConstants.PROMETHEUS_ENDPOINT + query), token);

		JSONObject responseJson = new JSONObject(response);
		ArrayList<String> valuesList = new ArrayList<>();

		int level = 0;
		try {
			DataSourceFactory.parseJsonForKey(responseJson, key, valuesList, level);
		} catch (TooManyRecursiveCallsException e) {
			e.printStackTrace();
		}
		return valuesList;
	}
}
