package com.autotune.experimentManager.data.input.info;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.AutotuneConstants;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMDataSourceDefinition implements ConvertToJSON {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMDataSourceDefinition.class);
    private String name;
    private String url;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public EMDataSourceDefinition(JSONObject jsonObject) throws IncompatibleInputJSONException {
        if (!jsonObject.has(AutotuneConstants.JSONKeys.URL) || !jsonObject.has(AutotuneConstants.JSONKeys.NAME)) {
            throw  new IncompatibleInputJSONException();
        }
        this.name = jsonObject.getString(AutotuneConstants.JSONKeys.NAME);
        this.url = jsonObject.getString(AutotuneConstants.JSONKeys.URL);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject datasourceDefJsonObj = new JSONObject();
        datasourceDefJsonObj.put(AutotuneConstants.JSONKeys.NAME, getName());
        datasourceDefJsonObj.put(AutotuneConstants.JSONKeys.URL, getUrl());
        return datasourceDefJsonObj;
    }
}
