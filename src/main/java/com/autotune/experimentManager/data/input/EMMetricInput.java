package com.autotune.experimentManager.data.input;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.utils.KruizeConstants;
import org.json.JSONObject;

public class EMMetricInput implements ConvertToJSON {
    private String name;
    private String query;
    private String dataSource;

    public EMMetricInput(JSONObject jsonObject) throws EMInvalidInstanceCreation {
        if(!jsonObject.has(KruizeConstants.JSONKeys.NAME)
            || !jsonObject.has(KruizeConstants.JSONKeys.QUERY)
            || !jsonObject.has(KruizeConstants.JSONKeys.DATASOURCE)) {
            throw new EMInvalidInstanceCreation();
        }
        this.name = jsonObject.getString(KruizeConstants.JSONKeys.NAME);
        this.query = jsonObject.getString(KruizeConstants.JSONKeys.QUERY);
        this.dataSource = jsonObject.getString(KruizeConstants.JSONKeys.DATASOURCE);
    }

    public EMMetricInput(String name, String query, String dataSource) {
        this.name = name;
        this.query = query;
        this.dataSource = dataSource;
    }

    public String getName() {
        return name;
    }

    public String getQuery() {
        return query;
    }

    public String getDataSource() {
        return dataSource;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject returnJSON = new JSONObject();
        returnJSON.put(KruizeConstants.JSONKeys.NAME, getName());
        returnJSON.put(KruizeConstants.JSONKeys.QUERY, getQuery());
        returnJSON.put(KruizeConstants.JSONKeys.DATASOURCE, getDataSource());
        return returnJSON;
    }
}
