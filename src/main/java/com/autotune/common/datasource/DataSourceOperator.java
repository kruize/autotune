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

import com.autotune.common.utils.CommonUtils;
import com.google.gson.JsonArray;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * DataSourceOperator is an abstraction which has a generic and implementation,
 * and it can also be implemented by each data source provider type.
 *
 * Currently Supported Implementations:
 *  - Prometheus
 *
 *  The Implementation should have helper functions to perform operations related
 *  to datasource
 */

public interface DataSourceOperator {
    /**
     * Returns the default service port for provider
     * @return String containing the port number
     */
    String getDefaultServicePortForProvider();

    /**
     * Returns the instance of specific operator class based on provider type
     * @param provider String containing the name of provider
     * @return instance of specific operator
     */
    DataSourceOperatorImpl getOperator(String provider);

    /**
     * Check if a datasource is reachable, implementation of this function
     * should check and return the reachability status (REACHABLE, NOT_REACHABLE)
     * @param dataSourceUrl String containing the url for the datasource
     * @return DatasourceReachabilityStatus
     */
    CommonUtils.DatasourceReachabilityStatus isServiceable(String dataSourceUrl);

    /**
     * executes specified query on datasource and returns the result value
     * @param url String containing the url for the datasource
     * @param query String containing the query to be executed
     * @return Object containing the result value for the specified query
     */
    Object getValueForQuery(String url, String query);

    /**
     * executes specified query on datasource and returns the JSON Object
     * @param url String containing the url for the datasource
     * @param query String containing the query to be executed
     * @return JSONObject for the specified query
     */
    JSONObject getJsonObjectForQuery(String url, String query);

    /**
     * executes specified query on datasource and returns the result array
     * @param url String containing the url for the datasource
     * @param query String containing the query to be executed
     * @return JsonArray containing the result array for the specified query
     */
    public JsonArray getResultArrayForQuery(String url, String query);
}
