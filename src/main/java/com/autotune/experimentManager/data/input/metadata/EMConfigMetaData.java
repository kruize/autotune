package com.autotune.experimentManager.data.input.metadata;

import com.autotune.experimentManager.data.input.abscls.DataEditor;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;

public class EMConfigMetaData extends DataEditor<EMConfigMetaData> implements ConvertToJSON {
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
        if (!jsonObject.has(EMConstants.EMJSONKeys.EXPERIMENT_ID)){
            throw new IncompatibleInputJSONException();
        }

        if (!jsonObject.has(EMConstants.EMJSONKeys.APPLICATION_NAME)) {
            throw new IncompatibleInputJSONException();
        }

        setApplicationName(jsonObject.getString(EMConstants.EMJSONKeys.APPLICATION_NAME));
        setExpId(jsonObject.getString(EMConstants.EMJSONKeys.EXPERIMENT_ID));
    }

    @Override
    public EMConfigMetaData edit() {
        return null;
    }

    @Override
    public EMConfigMetaData done() {
        return null;
    }

    @Override
    public JSONObject toJSON() throws EMDataObjectIsInEditingException, EMDataObjectIsNotFilledException {
        if (this.isEditing()) {
            throw new EMDataObjectIsInEditingException();
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(EMConstants.EMJSONKeys.EXPERIMENT_ID, getExpId());
        jsonObject.put(EMConstants.EMJSONKeys.APPLICATION_NAME, getApplicationName());
        return jsonObject;
    }
}
