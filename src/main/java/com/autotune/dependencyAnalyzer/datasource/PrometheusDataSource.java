package com.autotune.dependencyAnalyzer.datasource;

import com.autotune.util.HttpUtil;
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

    String dataSource = "prometheus";
    String dataSourceURL;
    String token;
    String endpoint = "/api/v1/query?query=";

    public PrometheusDataSource(String monitoringAgentEndpoint, String token)
    {
        this.dataSourceURL = monitoringAgentEndpoint;
        this.token = token;
    }



    private JSONArray getAsJsonArray(String response) throws IndexOutOfBoundsException
    {
        JSONObject jsonObject = new JSONObject(response);

        return jsonObject
                .getJSONObject("data")
                .getJSONArray("result")
                .getJSONObject(0)
                .getJSONArray("value");
    }

    private String getValueForQuery(String response) throws IndexOutOfBoundsException
    {
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
     * Run the getAllAppsQuery and return the list of applications matching the layer.
     * @param query GetAllApps query for the layer
     * @param key The key to search for in the response
     * @return ArrayList of all applications from the query
     * @throws MalformedURLException
     */
    public ArrayList<String> getAppsForLayer(String query, String key) throws MalformedURLException
    {
        String response = HttpUtil.getDataFromURL(new URL(dataSourceURL + endpoint + query), "");

        JSONObject responseJson = new JSONObject(response);
        ArrayList<String> valuesList = new ArrayList<>();

        getValuesForKey(responseJson, key, valuesList);

        return valuesList;
    }

    private static void getValuesForKey(JSONObject jsonObj, String key, ArrayList<String> values)
    {
        jsonObj.keySet().forEach(keyStr ->
        {
            Object keyvalue = jsonObj.get(keyStr);

            if (keyStr.equals(key))
                values.add(keyvalue.toString());

            //for nested objects
            if (keyvalue instanceof JSONObject)
                getValuesForKey((JSONObject)keyvalue, key, values);

            //for json array, iterate and recursively get values
            if (keyvalue instanceof JSONArray)
            {
                JSONArray jsonArray = (JSONArray) keyvalue;
                for (int index = 0; index < jsonArray.length(); index++)
                {
                    Object jsonObject = jsonArray.get(index);
                    if (jsonObject instanceof JSONObject) {
                        getValuesForKey((JSONObject) jsonObject, key, values);
                    }
                }
            }
        });
    }
}
