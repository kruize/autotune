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
package com.autotune.common.datasource.prometheus;

import com.autotune.common.datasource.DataSourceOperator;
import com.autotune.common.datasource.DataSourceOperatorImpl;
import com.autotune.common.datasource.KruizeDataSourceOperator;
import com.autotune.common.utils.CommonUtils;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.GenericRestApiClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.conn.HttpHostConnectException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 *  PrometheusDataOperatorImpl extends DataSourceOperatorImpl class
 *  This class provides Prometheus specific implementation for DataSourceOperator functions
 */
public class PrometheusDataOperatorImpl extends DataSourceOperatorImpl {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PrometheusDataOperatorImpl.class);

    private static PrometheusDataOperatorImpl prometheusDataOperator = null;;
    private PrometheusDataOperatorImpl() {
        super();
    }

    /**
     * Returns the instance of PrometheusDataOperatorImpl class
     * @return PrometheusDataOperatorImpl instance
     */
    public static PrometheusDataOperatorImpl getInstance() {
        if (null == prometheusDataOperator) {
            prometheusDataOperator = new PrometheusDataOperatorImpl();
        }
        return prometheusDataOperator;
    }

    /**
     * Returns the default service port for prometheus
     * @return String containing the port number
     */
    @Override
    public String getDefaultServicePortForProvider() {
        return KruizeConstants.DataSourceConstants.PROMETHEUS_DEFAULT_SERVICE_PORT;
    }

    /**
     * Check if a datasource is reachable, implementation of this function
     * should check and return the reachability status (REACHABLE, NOT_REACHABLE)
     * @param dataSourceURL String containing the url for the datasource
     * @return DatasourceReachabilityStatus
     */
    @Override
    public CommonUtils.DatasourceReachabilityStatus isServiceable(String dataSourceURL) {
        String dataSourceStatus;
        Object queryResult;

        String query = KruizeConstants.DataSourceConstants.PROMETHEUS_REACHABILITY_QUERY;
        CommonUtils.DatasourceReachabilityStatus reachabilityStatus;

        queryResult = this.getValueForQuery(dataSourceURL, query);

        if (queryResult != null){
            dataSourceStatus = queryResult.toString();
        } else {
            dataSourceStatus = "0";
        }

        if (dataSourceStatus.equalsIgnoreCase("1")){
            reachabilityStatus = CommonUtils.DatasourceReachabilityStatus.REACHABLE;
        } else {
            reachabilityStatus = CommonUtils.DatasourceReachabilityStatus.NOT_REACHABLE;
        }
        return reachabilityStatus;
    }

    /**
     * executes specified query on datasource and returns the result value
     * @param url String containing the url for the datasource
     * @param query String containing the query to be executed
     * @return Object containing the result value for the specified query
     */
    @Override
    public Object getValueForQuery(String url, String query) {
        JSONObject jsonObject = getJsonObjectForQuery(url, query);

        if (null == jsonObject) {
            return null;
        }

        JSONArray result = jsonObject.getJSONObject("data").getJSONArray("result");
        for (Object result_obj: result) {
            JSONObject result_json = (JSONObject) result_obj;
            if (result_json.has("value")
                    && !result_json.getJSONArray("value").isEmpty()) {
                return result_json.getJSONArray("value").getString(1);
            }
        }
        return null;
    }

    /**
     * executes specified query on datasource and returns the JSON Object
     * @param url String containing the url for the datasource
     * @param query String containing the query to be executed
     * @return JSONObject for the specified query
     */
    @Override
    public JSONObject getJsonObjectForQuery(String url, String query) {
        GenericRestApiClient apiClient = new GenericRestApiClient(
                CommonUtils.getBaseDataSourceUrl(
                        url,
                        KruizeConstants.SupportedDatasources.PROMETHEUS
                )
        );

        if (null == apiClient) {
            return null;
        }

        try {
            JSONObject jsonObject = apiClient.fetchMetricsJson(
                    KruizeConstants.HttpConstants.MethodType.GET,
                    query);
            if (!jsonObject.has("status"))
                return null;
            if (!jsonObject.getString("status").equalsIgnoreCase("success"))
                return null;
            if (!jsonObject.has("data"))
                return null;
            if (!jsonObject.getJSONObject("data").has("result"))
                return null;
            if (jsonObject.getJSONObject("data").getJSONArray("result").isEmpty())
                return  null;

            return jsonObject;

        } catch (HttpHostConnectException e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.DATASOURCE_CONNECTION_FAILED);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * executes specified query on datasource and returns the result array
     * @param url String containing the url for the datasource
     * @param query String containing the query to be executed
     * @return JsonArray containing the result array for the specified query
     *
     * Example output JsonArray -
     * [
     *   {
     *     "metric": {
     *       "__name__": "exampleMetric"
     *     },
     *     "value": [1642612628.987, "1"]
     *   }
     * ]
     */

    @Override
    public JsonArray getResultArrayForQuery(String url, String query) {
        JSONObject jsonObject = getJsonObjectForQuery(url, query);

        if (null == jsonObject) {
            return null;
        }

        String jsonString = jsonObject.toString();
        JsonObject parsedJsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonObject dataObject = parsedJsonObject.get("data").getAsJsonObject();

        if (dataObject.has("result") && dataObject.get("result").isJsonArray()) {
            JsonArray resultArray = dataObject.getAsJsonArray("result");

            if (resultArray != null) {
                return resultArray;
            }
        }
        return null;
    }
}
