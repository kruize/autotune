/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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

import com.autotune.analyzer.exceptions.FetchMetricsError;
import com.autotune.common.utils.CommonUtils;
import com.google.gson.JsonArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * DataSourceOperator is an abstraction which has a generic and implementation,
 * and it can also be implemented by each data source provider type.
 * <p>
 * Currently Supported Implementations:
 * - Prometheus
 * <p>
 * The Implementation should have helper functions to perform operations related
 * to datasource
 */

public interface DataSourceOperator {
    /**
     * Returns the default service port for provider
     *
     * @return String containing the port number
     */
    String getDefaultServicePortForProvider();

    /**
     * Returns the instance of specific operator class based on provider type
     *
     * @param provider String containing the name of provider
     * @return instance of specific operator
     */
    DataSourceOperatorImpl getOperator(String provider);

    /**
     * Check if a datasource is reachable, implementation of this function
     * should check and return the reachability status (REACHABLE, NOT_REACHABLE)
     *
     * @param dataSource DatasourceInfo object containing the datasource details
     * @return DatasourceReachabilityStatus
     */
    CommonUtils.DatasourceReachabilityStatus isServiceable(DataSourceInfo dataSource) throws FetchMetricsError, Exception;

    /**
     * executes specified query on datasource and returns the result value
     *
     * @param dataSource DatasourceInfo object containing the datasource details
     * @param query      String containing the query to be executed
     * @return Object containing the result value for the specified query
     */
    Object getValueForQuery(DataSourceInfo dataSource, String query) throws FetchMetricsError, Exception;

    /**
     * executes specified query on datasource and returns the JSON Object
     *
     * @param dataSource DatasourceInfo object containing the datasource details
     * @param query      String containing the query to be executed
     * @return JSONObject for the specified query
     */
    JSONObject getJsonObjectForQuery(DataSourceInfo dataSource, String query) throws Exception, FetchMetricsError;

    /**
     * executes specified query on datasource and returns the result array
     *
     * @param dataSource DatasourceInfo object containing the datasource details
     * @param query      String containing the query to be executed
     * @return JsonArray containing the result array for the specified query
     */
    public JsonArray getResultArrayForQuery(DataSourceInfo dataSource, String query) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException;

    /**
     * Validates a JSON array to ensure it is not null, not a JSON null, and has at least one element.
     *
     * @param resultArray The JSON array to be validated.
     * @return True if the JSON array is valid (not null, not a JSON null, and has at least one element), otherwise false.
     */
    boolean validateResultArray(JsonArray resultArray);

    /**
     * returns query endpoint for datasource
     *
     * @return String containing query endpoint
     */
    String getQueryEndpoint();
}
