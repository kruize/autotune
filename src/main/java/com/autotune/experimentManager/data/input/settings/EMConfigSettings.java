package com.autotune.experimentManager.data.input.settings;

import com.autotune.experimentManager.data.input.abscls.DataEditor;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;

public class EMConfigSettings extends DataEditor<EMConfigSettings> implements ConvertToJSON {
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
        if(!jsonObject.has(EMConstants.EMJSONKeys.SETTINGS)) {
            throw new IncompatibleInputJSONException();
        }
        JSONObject contentJson = jsonObject.getJSONObject(EMConstants.EMJSONKeys.SETTINGS);
        trialSettings = new EMConfigTrialSettings(contentJson);
        deploymentSettings = new EMConfigDeploymentSettings(contentJson);
    }

    public EMConfigSettings() {

    }

    @Override
    public EMConfigSettings edit() {
        return null;
    }

    @Override
    public EMConfigSettings done() {
        return null;
    }

    @Override
    public JSONObject toJSON() throws EMDataObjectIsInEditingException, EMDataObjectIsNotFilledException {
        if (this.isEditing()) {
            throw new EMDataObjectIsInEditingException();
        }
        JSONObject subObj = new JSONObject();
        subObj.put(EMConstants.EMJSONKeys.TRIAL_SETTINGS, trialSettings.toJSON().getJSONObject(EMConstants.EMJSONKeys.TRIAL_SETTINGS));
        subObj.put(EMConstants.EMJSONKeys.DEPLOYMENT_SETTINGS, deploymentSettings.toJSON().getJSONObject(EMConstants.EMJSONKeys.DEPLOYMENT_SETTINGS));

        return subObj;
    }
}
