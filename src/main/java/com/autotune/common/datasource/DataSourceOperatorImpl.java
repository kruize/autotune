package com.autotune.common.datasource;

import com.autotune.common.datasource.prometheus.PrometheusDataOperatorImpl;
import com.autotune.common.utils.CommonUtils;
import com.autotune.utils.KruizeConstants;
import com.google.gson.JsonArray;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

public class DataSourceOperatorImpl implements DataSourceOperator {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DataSourceOperatorImpl.class);
    private static DataSourceOperatorImpl dataSourceOperator = null;
    protected DataSourceOperatorImpl() {
    }

    /**
     * Returns the instance of DataSourceOperatorImpl class
     * @return DataSourceOperatorImpl instance
     */
    public static DataSourceOperatorImpl getInstance() {
        if (null == dataSourceOperator) {
            dataSourceOperator = new DataSourceOperatorImpl();
        }
        return dataSourceOperator;
    }

    /**
     * Returns the instance of specific operator class based on provider type
     * @param provider String containg the name of provider
     * @return instance of specific operator
     */
    @Override
    public DataSourceOperatorImpl getOperator(String provider) {
        if (provider.equalsIgnoreCase(KruizeConstants.SupportedDatasources.PROMETHEUS)) {
            return PrometheusDataOperatorImpl.getInstance();
        }
        return null;
    }

    /**
     * Returns the default service port for prometheus
     * @return String containing the port number
     */
    @Override
    public String getDefaultServicePortForProvider(){
        return "";
    }

    /**
     * Check if a datasource is reachable, implementation of this function
     * should check and return the reachability status (REACHABLE, NOT_REACHABLE)
     * @param dataSourceUrl String containing the url for the datasource
     * @return DatasourceReachabilityStatus
     */
    @Override
    public CommonUtils.DatasourceReachabilityStatus isServiceable(String dataSourceUrl){
        return null;
    }

    /**
     * executes specified query on datasource and returns the result value
     * @param url String containing the url for the datasource
     * @param query String containing the query to be executed
     * @return Object containing the result value for the specified query
     */
    @Override
    public Object getValueForQuery(String url, String query){
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
        return null;
    }

    /**
     * executes specified query on datasource and returns the result array
     * @param url String containing the url for the datasource
     * @param query String containing the query to be executed
     * @return JsonArray containing the result array for the specified query
     */
    @Override
    public JsonArray getResultArrayForQuery(String url, String query) {
        return null;
    }

    /**
     * Validates a JSON array to ensure it is not null, not a JSON null, and has at least one element.
     *
     * @param resultArray The JSON array to be validated.
     * @return True if the JSON array is valid (not null, not a JSON null, and has at least one element), otherwise false.
     */
    @Override
    public boolean validateResultArray(JsonArray resultArray) { return false;}
}
