package com.autotune.experimentManager.data.input.metadata;

import com.autotune.experimentManager.data.input.EMConfigObject;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
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
        if (!jsonObject.has(EMConstants.EMJSONKeys.EXPERIMENT_ID)){
            throw new IncompatibleInputJSONException();
        }

        if (!jsonObject.has(EMConstants.EMJSONKeys.EXPERIMENT_NAME)) {
            throw new IncompatibleInputJSONException();
        }

        setApplicationName(jsonObject.getString(EMConstants.EMJSONKeys.EXPERIMENT_NAME));
        setExpId(jsonObject.getString(EMConstants.EMJSONKeys.EXPERIMENT_ID));
    }

    @Override
    public JSONObject toJSON(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(EMConstants.EMJSONKeys.EXPERIMENT_ID, getExpId());
        jsonObject.put(EMConstants.EMJSONKeys.EXPERIMENT_NAME, getApplicationName());
        return jsonObject;
    }
}
