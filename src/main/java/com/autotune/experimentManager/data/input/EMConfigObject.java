package com.autotune.experimentManager.data.input;

import com.autotune.experimentManager.data.input.deployments.EMConfigDeployments;
import com.autotune.experimentManager.data.input.info.EMConfigInfo;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.data.input.metadata.EMConfigMetaData;
import com.autotune.experimentManager.data.input.settings.EMConfigSettings;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.AutotuneConstants;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMConfigObject implements ConvertToJSON {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMConfigObject.class);
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

    public EMConfigObject(JSONObject inputJSON) throws IncompatibleInputJSONException, EMInvalidInstanceCreation {
        LOGGER.info("Creating individual data holders for input data json");
        metadata = new EMConfigMetaData(inputJSON);
        info = new EMConfigInfo(inputJSON);
        settings = new EMConfigSettings(inputJSON);
        deployments = new EMConfigDeployments(inputJSON);
    }

    public EMConfigObject() {

    }


    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        JSONObject metadataJsonObject = getMetadata().toJSON();
        JSONObject infoJsonObject = getInfo().toJSON();
        JSONObject settingsJsonObject = getSettings().toJSON();
        JSONObject deploymentsJsonObject = getDeployments().toJSON();

        jsonObject.put(AutotuneConstants.JSONKeys.EXPERIMENT_ID, metadataJsonObject.getString(AutotuneConstants.JSONKeys.EXPERIMENT_ID));
        jsonObject.put(AutotuneConstants.JSONKeys.EXPERIMENT_NAME, metadataJsonObject.getString(AutotuneConstants.JSONKeys.EXPERIMENT_NAME));
        jsonObject.put(AutotuneConstants.JSONKeys.INFO, infoJsonObject);
        jsonObject.put(AutotuneConstants.JSONKeys.SETTINGS,settingsJsonObject);
        jsonObject.put(AutotuneConstants.JSONKeys.DEPLOYMENTS, deploymentsJsonObject.getJSONArray(AutotuneConstants.JSONKeys.DEPLOYMENTS));
        return jsonObject;
    }
}
