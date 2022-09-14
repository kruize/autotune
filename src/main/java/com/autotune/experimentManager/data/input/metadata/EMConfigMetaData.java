package com.autotune.experimentManager.data.input.metadata;

import com.autotune.experimentManager.data.input.EMConfigObject;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.utils.AutotuneConstants;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMConfigMetaData implements ConvertToJSON {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMConfigMetaData.class);

    private String expId;
    private String applicationName;

    public String getExpId() {
        return expId;
    }

    public void setExpId(String expId) {
        this.expId = expId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public EMConfigMetaData() {

    }

    public EMConfigMetaData(JSONObject jsonObject) throws IncompatibleInputJSONException {
        LOGGER.info("Creating EMConfigMetaData");
        if (!jsonObject.has(AutotuneConstants.JSONKeys.EXPERIMENT_ID)){
            throw new IncompatibleInputJSONException();
        }

        if (!jsonObject.has(AutotuneConstants.JSONKeys.EXPERIMENT_NAME)) {
            throw new IncompatibleInputJSONException();
        }

        setApplicationName(jsonObject.getString(AutotuneConstants.JSONKeys.EXPERIMENT_NAME));
        setExpId(jsonObject.getString(AutotuneConstants.JSONKeys.EXPERIMENT_ID));
    }

    @Override
    public JSONObject toJSON(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(AutotuneConstants.JSONKeys.EXPERIMENT_ID, getExpId());
        jsonObject.put(AutotuneConstants.JSONKeys.EXPERIMENT_NAME, getApplicationName());
        return jsonObject;
    }
}
