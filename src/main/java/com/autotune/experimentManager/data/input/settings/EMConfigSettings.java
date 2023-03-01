package com.autotune.experimentManager.data.input.settings;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.data.input.metadata.EMConfigMetaData;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.utils.AutotuneConstants;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMConfigSettings implements ConvertToJSON {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMConfigSettings.class);
    private EMConfigTrialSettings trialSettings;
    private EMConfigDeploymentSettings deploymentSettings;

    public EMConfigTrialSettings getTrialSettings() {
        return trialSettings;
    }

    public void setTrialSettings(EMConfigTrialSettings trialSettings) {
        this.trialSettings = trialSettings;
    }

    public EMConfigDeploymentSettings getDeploymentSettings() {
        return deploymentSettings;
    }

    public void setDeploymentSettings(EMConfigDeploymentSettings deploymentSettings) {
        this.deploymentSettings = deploymentSettings;
    }

    public EMConfigSettings(JSONObject jsonObject) throws IncompatibleInputJSONException {
        LOGGER.info("Creating EMConfigSettings");
        if(!jsonObject.has(AutotuneConstants.JSONKeys.SETTINGS)) {
            throw new IncompatibleInputJSONException();
        }
        JSONObject contentJson = jsonObject.getJSONObject(AutotuneConstants.JSONKeys.SETTINGS);
        trialSettings = new EMConfigTrialSettings(contentJson);
        deploymentSettings = new EMConfigDeploymentSettings(contentJson);
    }

    public EMConfigSettings() {

    }

    @Override
    public JSONObject toJSON() {
        JSONObject subObj = new JSONObject();
        subObj.put(AutotuneConstants.JSONKeys.TRIAL_SETTINGS, trialSettings.toJSON());
        subObj.put(AutotuneConstants.JSONKeys.DEPLOYMENT_SETTINGS, deploymentSettings.toJSON());
        return subObj;
    }
}
