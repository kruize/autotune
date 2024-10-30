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

import com.autotune.analyzer.exceptions.FetchMetricsError;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.auth.AuthenticationStrategy;
import com.autotune.common.auth.AuthenticationStrategyFactory;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceOperatorImpl;
import com.autotune.common.utils.CommonUtils;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.GenericRestApiClient;
import com.google.gson.*;
import org.apache.http.conn.HttpHostConnectException;
import org.json.JSONArray;
import org.json.JSONException;
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
        if (KruizeDeploymentInfo.k8s_type.equalsIgnoreCase(KruizeConstants.OPENSHIFT)) {
            return KruizeConstants.DataSourceConstants.OPENSHIFT_MONITORING_PROMETHEUS_DEFAULT_SERVICE_PORT;
        }
        return KruizeConstants.DataSourceConstants.PROMETHEUS_DEFAULT_SERVICE_PORT;
    }

    /**
     * Check if a datasource is reachable, implementation of this function
     * should check and return the reachability status (REACHABLE, NOT_REACHABLE)
     *
     * @param dataSource The DataSourceInfo object containing information about the data source
     * @return DatasourceReachabilityStatus
     */
    @Override
    public CommonUtils.DatasourceReachabilityStatus isServiceable(DataSourceInfo dataSource) {
        String dataSourceStatus;
        Object queryResult;

        String query = KruizeConstants.DataSourceConstants.PROMETHEUS_REACHABILITY_QUERY;
        CommonUtils.DatasourceReachabilityStatus reachabilityStatus;

        queryResult = this.getValueForQuery(dataSource, query);

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
     *
     * @param dataSource The DataSourceInfo object containing information about the data source
     * @param query      String containing the query to be executed
     * @return Object containing the result value for the specified query
     */
    @Override
    public Object getValueForQuery(DataSourceInfo dataSource, String query) {
        try {
            JSONObject jsonObject = getJsonObjectForQuery(dataSource, query);

            if (null == jsonObject) {
                return null;
            } else {
                return "1";   //if it returns HTTP STATUS_OK  200
            }


        } catch (JSONException e) {
            LOGGER.error(e.getMessage());
        } catch (NullPointerException e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    /**
     * executes specified query on datasource and returns the JSON Object
     *
     * @param dataSource The DataSourceInfo object containing information about the data source
     * @param query      String containing the query to be executed
     * @return JSONObject for the specified query
     */
    @Override
    public JSONObject getJsonObjectForQuery(DataSourceInfo dataSource, String query) {
        // Create the client
        GenericRestApiClient apiClient = new GenericRestApiClient(dataSource);
        apiClient.setBaseURL(CommonUtils.getBaseDataSourceUrl(
                dataSource,
                KruizeConstants.SupportedDatasources.PROMETHEUS
        ));

        if (null == apiClient) {
            return null;
        }

        try {
            JSONObject jsonObject = apiClient.fetchMetricsJson(
                    KruizeConstants.HttpConstants.MethodType.GET,
                    query);
            /*  TODO need to separate it out this logic form here
            if (!jsonObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.STATUS))
                return null;
            if (!jsonObject.getString(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.STATUS).equalsIgnoreCase(KruizeConstants.DataSourceConstants.DataSourceQueryStatus.SUCCESS))
                return null;
            if (!jsonObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.DATA))
                return null;
            if (!jsonObject.getJSONObject(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.DATA).has(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT))
                return null;
            if (jsonObject.getJSONObject(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.DATA).getJSONArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT).isEmpty())
                return  null;

             */

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
        } catch (FetchMetricsError e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * returns query endpoint for prometheus datasource
     * @return String containing query endpoint
     */
    @Override
    public String getQueryEndpoint() {
        return AnalyzerConstants.PROMETHEUS_API;
    }

    /**
     * executes specified query on datasource and returns the result array
     *
     * @param dataSource DatasourceInfo object containing the datasource details
     * @param query      String containing the query to be executed
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
    public JsonArray getResultArrayForQuery(DataSourceInfo dataSource, String query) {
        try {
            JSONObject jsonObject = getJsonObjectForQuery(dataSource, query);

            if (null == jsonObject) {
                return null;
            }

            String jsonString = jsonObject.toString();
            JsonObject parsedJsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            JsonObject dataObject = parsedJsonObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.DATA).getAsJsonObject();

            if (dataObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT) && dataObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT).isJsonArray()) {
                JsonArray resultArray = dataObject.getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT);

                if (null != resultArray) {
                    return resultArray;
                }
            }
        } catch (JsonParseException e) {
            LOGGER.error(e.getMessage());
            throw e;
        } catch (NullPointerException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
        return null;
    }

    /**
     * Validates a JSON array to ensure it is not null, not a JSON null, and has at least one element.
     *
     * @param resultArray The JSON array to be validated.
     * @return True if the JSON array is valid (not null, not a JSON null, and has at least one element), otherwise false.
     */
    public boolean validateResultArray(JsonArray resultArray) {

        if (null == resultArray || resultArray.isJsonNull() || resultArray.size() == 0) {
            return false;
        }
        return true;
    }
}
