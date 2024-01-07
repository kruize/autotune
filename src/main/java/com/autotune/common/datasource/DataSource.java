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

import com.autotune.common.utils.CommonUtils;

import java.net.MalformedURLException;
import java.util.List;

public interface DataSource
{
	/**
	 * Returns the name of the data source
	 * @return String containing the name of the data source
	 */
	String getName();

	/**
	 * Returns the name of service for data source
	 * @return String containing the name of service for data source
	 */
	String getServiceName();

	/**
	 * Run the getAppsForLayer and return the list of applications matching the layer.
	 * @param query getAppsForLayer query for the layer
	 * @param key The key to search for in the response
	 * @return List of all applications from the query
	 * @throws MalformedURLException
	 */
	List<String> getAppsForLayer(String query, String key) throws MalformedURLException;

	/**
	 * Returns the datasource endpoint from which queries can be run
	 * @return String containing the URL
	 */
	String getDataSourceURL();

	/**
	 * Returns the query API endpoint for the datasource
	 * @return String containing the API endpoint
	 */
	String getQueryEndpoint();

	/**
	 * Check if a datasource is reachable, implementation of this function
	 * should check and return the reachability status (REACHABLE, NOT_REACHABLE)
	 * @return DatasourceReachabilityStatus
	 */
	CommonUtils.DatasourceReachabilityStatus isReachable();

	/**
	 * Check if a datasource is reliable to use, implementation of this function
	 * should check and return the reachability status (RELIABLE, NOT RELIABLE)
	 * @return DatasourceReliabilityStatus
	 */
	CommonUtils.DatasourceReliabilityStatus isReliable(Object... objects);

	/**
	 * Returns the provider for the datasource
	 * @return String containing the provider type
	 */
	String getProvider();
}
