package com.autotune.experimentManager.data.input.settings;

import com.autotune.experimentManager.data.input.abscls.DataEditor;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;

public class EMConfigDeploymentSettings extends DataEditor<EMConfigDeploymentSettings> implements ConvertToJSON {

    private EMConfigDeploymentInfo deploymentInfo;
    private EMConfigDeploymentTracker deploymentTracker;
    private EMConfigDeploymentPolicy deploymentPolicy;

    public EMConfigDeploymentInfo getDeploymentInfo() {
        return deploymentInfo;
    }

    public void setDeploymentInfo(EMConfigDeploymentInfo deploymentInfo) {
        this.deploymentInfo = deploymentInfo;
    }

    public EMConfigDeploymentTracker getDeploymentTracker() {
        return deploymentTracker;
    }

    public void setDeploymentTracker(EMConfigDeploymentTracker deploymentTracker) {
        this.deploymentTracker = deploymentTracker;
    }

    public EMConfigDeploymentPolicy getDeploymentPolicy() {
        return deploymentPolicy;
    }

    public void setDeploymentPolicy(EMConfigDeploymentPolicy deploymentPolicy) {
        this.deploymentPolicy = deploymentPolicy;
    }

    public EMConfigDeploymentSettings() {

    }

    public EMConfigDeploymentSettings(JSONObject jsonObject) throws IncompatibleInputJSONException {
        if(!jsonObject.has(EMConstants.EMJSONKeys.DEPLOYMENT_SETTINGS)) {
            throw new IncompatibleInputJSONException();
        }
        JSONObject contentObject = jsonObject.getJSONObject(EMConstants.EMJSONKeys.DEPLOYMENT_SETTINGS);
        deploymentInfo = new EMConfigDeploymentInfo(contentObject);
        deploymentTracker = new EMConfigDeploymentTracker(contentObject);
        deploymentPolicy = new EMConfigDeploymentPolicy(contentObject);
    }

    @Override
    public EMConfigDeploymentSettings edit() {
        return null;
    }

    @Override
    public EMConfigDeploymentSettings done() {
        return null;
    }

    @Override
    public JSONObject toJSON() throws EMDataObjectIsInEditingException, EMDataObjectIsNotFilledException {
        if(this.isEditing()) {
            throw new EMDataObjectIsInEditingException();
        }
        JSONObject subObject = new JSONObject();
        subObject.put(EMConstants.EMJSONKeys.DEPLOYMENT_INFO, getDeploymentInfo().toJSON().getJSONObject(EMConstants.EMJSONKeys.DEPLOYMENT_INFO));
        subObject.put(EMConstants.EMJSONKeys.DEPLOYMENT_POLICY, getDeploymentPolicy().toJSON().getJSONObject(EMConstants.EMJSONKeys.DEPLOYMENT_POLICY));
        subObject.put(EMConstants.EMJSONKeys.DEPLOYMENT_TRACKING, getDeploymentTracker().toJSON().getJSONObject(EMConstants.EMJSONKeys.DEPLOYMENT_TRACKING));
        JSONObject returnJson = new JSONObject();
        returnJson.put(EMConstants.EMJSONKeys.DEPLOYMENT_SETTINGS, subObject);
        return  returnJson;
    }
}
