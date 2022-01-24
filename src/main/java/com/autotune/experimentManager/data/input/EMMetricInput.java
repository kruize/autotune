package com.autotune.experimentManager.data.input;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;

public class EMMetricInput implements ConvertToJSON {
    private String name;
    private String query;
    private String dataSource;

    public EMMetricInput(JSONObject jsonObject) throws EMInvalidInstanceCreation {
        if(!jsonObject.has(EMConstants.EMJSONKeys.NAME)
            || !jsonObject.has(EMConstants.EMJSONKeys.QUERY)
            || !jsonObject.has(EMConstants.EMJSONKeys.DATASOURCE)) {
            throw new EMInvalidInstanceCreation();
        }
        this.name = jsonObject.getString(EMConstants.EMJSONKeys.NAME);
        this.query = jsonObject.getString(EMConstants.EMJSONKeys.QUERY);
        this.dataSource = jsonObject.getString(EMConstants.EMJSONKeys.DATASOURCE);
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
        returnJSON.put(EMConstants.EMJSONKeys.NAME, getName());
        returnJSON.put(EMConstants.EMJSONKeys.QUERY, getQuery());
        returnJSON.put(EMConstants.EMJSONKeys.DATASOURCE, getDataSource());
        return returnJSON;
    }
}
