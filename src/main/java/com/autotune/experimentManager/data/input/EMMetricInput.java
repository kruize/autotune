package com.autotune.experimentManager.data.input;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.utils.AutotuneConstants;
import org.json.JSONObject;

public class EMMetricInput implements ConvertToJSON {
    private String name;
    private String query;
    private String dataSource;

    public EMMetricInput(JSONObject jsonObject) throws EMInvalidInstanceCreation {
        if(!jsonObject.has(AutotuneConstants.JSONKeys.NAME)
            || !jsonObject.has(AutotuneConstants.JSONKeys.QUERY)
            || !jsonObject.has(AutotuneConstants.JSONKeys.DATASOURCE)) {
            throw new EMInvalidInstanceCreation();
        }
        this.name = jsonObject.getString(AutotuneConstants.JSONKeys.NAME);
        this.query = jsonObject.getString(AutotuneConstants.JSONKeys.QUERY);
        this.dataSource = jsonObject.getString(AutotuneConstants.JSONKeys.DATASOURCE);
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
        returnJSON.put(AutotuneConstants.JSONKeys.NAME, getName());
        returnJSON.put(AutotuneConstants.JSONKeys.QUERY, getQuery());
        returnJSON.put(AutotuneConstants.JSONKeys.DATASOURCE, getDataSource());
        return returnJSON;
    }
}
