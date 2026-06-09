package com.autotune.common.datasource.cryostat;

import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceOperatorImpl;
import com.autotune.common.utils.CommonUtils;
import com.autotune.utils.GenericRestApiClient;
import com.google.gson.JsonArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class CryostatDataOperatorImpl extends DataSourceOperatorImpl {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CryostatDataOperatorImpl.class);

    private static CryostatDataOperatorImpl instance;

    private CryostatDataOperatorImpl() {
        super();
    }

    public static CryostatDataOperatorImpl getInstance() {
        if (instance == null) {
            instance = new CryostatDataOperatorImpl();
        }
        return instance;
    }

    @Override
    public String getDefaultServicePortForProvider() {
        return "4180";
    }

    @Override
    public String getQueryEndpoint() {
        return "/api/v4/graphql";
    }

    @Override
    public CommonUtils.DatasourceReachabilityStatus isServiceable(
            DataSourceInfo dataSource)
            throws IOException, NoSuchAlgorithmException,
            KeyStoreException, KeyManagementException {

        String reachabilityQuery =
                "{\"query\":\"{ environmentNodes { name } }\"}";

        JSONObject response =
                getJsonObjectForQuery(dataSource, reachabilityQuery);

        return response != null
                ? CommonUtils.DatasourceReachabilityStatus.REACHABLE
                : CommonUtils.DatasourceReachabilityStatus.NOT_REACHABLE;
    }

    @Override
    public Object getValueForQuery(DataSourceInfo dataSource, String query)
            throws IOException, NoSuchAlgorithmException,
            KeyStoreException, KeyManagementException {

        JSONObject jsonObject =
                getJsonObjectForQuery(dataSource, query);

        return jsonObject != null ? "1" : null;
    }

    @Override
    public JSONObject getJsonObjectForQuery(
            DataSourceInfo dataSource,
            String query)
            throws IOException, NoSuchAlgorithmException,
            KeyStoreException, KeyManagementException {

        GenericRestApiClient apiClient =
                new GenericRestApiClient(dataSource);

        String baseURL = dataSource.getUrl().toString();

        apiClient.setBaseURL(baseURL + getQueryEndpoint());

        try {
            return apiClient.fetchMetricsJson(
                    "POST",
                    query);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    @Override
    public JsonArray getResultArrayForQuery(
            DataSourceInfo dataSource,
            String query)
            throws IOException, NoSuchAlgorithmException,
            KeyStoreException, KeyManagementException {

        JSONObject response =
                getJsonObjectForQuery(dataSource, query);

        if (response == null) {
            return null;
        }

        // TODO: Parse GraphQL response here

        return null;
    }

    @Override
    public boolean validateResultArray(JsonArray resultArray) {
        return resultArray != null
                && !resultArray.isJsonNull()
                && !resultArray.isEmpty();
    }
}