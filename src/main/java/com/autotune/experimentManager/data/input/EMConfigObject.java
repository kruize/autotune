package com.autotune.experimentManager.data.input;

import com.autotune.experimentManager.data.input.deployments.EMConfigDeployments;
import com.autotune.experimentManager.data.input.info.EMConfigInfo;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.data.input.metadata.EMConfigMetaData;
import com.autotune.experimentManager.data.input.settings.EMConfigSettings;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;

public class EMConfigObject implements ConvertToJSON {
    private EMConfigMetaData metadata;
    private EMConfigInfo info;
    private EMConfigSettings settings;
    private EMConfigDeployments deployments;

    public EMConfigMetaData getMetadata() {
        return metadata;
    }

    public EMConfigInfo getInfo() {
        return info;
    }

    public EMConfigSettings getSettings() {
        return settings;
    }

    public EMConfigDeployments getDeployments() {
        return deployments;
    }

    public EMConfigObject (JSONObject inputJSON) throws IncompatibleInputJSONException {
        metadata = new EMConfigMetaData(inputJSON);
        info = new EMConfigInfo(inputJSON);
        settings = new EMConfigSettings(inputJSON);
        deployments = new EMConfigDeployments(inputJSON);
    }

    public EMConfigObject() {

    }


    @Override
    public JSONObject toJSON() throws EMDataObjectIsInEditingException, EMDataObjectIsNotFilledException {
        JSONObject jsonObject = new JSONObject();
        JSONObject metadataJsonObject = getMetadata().toJSON();
        JSONObject infoJsonObject = getInfo().toJSON();
        JSONObject settingsJsonObject = getSettings().toJSON();
        JSONObject deploymentsJsonObject = getDeployments().toJSON();

        jsonObject.put(EMConstants.EMJSONKeys.EXPERIMENT_ID, metadataJsonObject.getString(EMConstants.EMJSONKeys.EXPERIMENT_ID));
        jsonObject.put(EMConstants.EMJSONKeys.APPLICATION_NAME, metadataJsonObject.getString(EMConstants.EMJSONKeys.APPLICATION_NAME));
        jsonObject.put(EMConstants.EMJSONKeys.INFO, infoJsonObject);
        jsonObject.put(EMConstants.EMJSONKeys.SETTINGS,settingsJsonObject);
        jsonObject.put(EMConstants.EMJSONKeys.DEPLOYMENTS, deploymentsJsonObject);
        return jsonObject;
    }
}
