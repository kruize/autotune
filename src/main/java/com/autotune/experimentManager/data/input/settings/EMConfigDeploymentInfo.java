package com.autotune.experimentManager.data.input.settings;

import com.autotune.experimentManager.data.input.abscls.DataEditor;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;

public class EMConfigDeploymentInfo extends DataEditor<EMConfigDeploymentInfo> implements ConvertToJSON {
    private String deploymentName;
    private String targetEnv;

    public EMConfigDeploymentInfo() {

    }

    public EMConfigDeploymentInfo(JSONObject jsonObject) throws IncompatibleInputJSONException {
        if (!jsonObject.has(EMConstants.EMJSONKeys.DEPLOYMENT_INFO)) {
            throw new IncompatibleInputJSONException();
        }
        JSONObject depInfoJSON = jsonObject.getJSONObject(EMConstants.EMJSONKeys.DEPLOYMENT_INFO);
        if (!depInfoJSON.has(EMConstants.EMJSONKeys.DEPLOYMENT_NAME)) {
            throw new IncompatibleInputJSONException();
        }
        this.deploymentName = depInfoJSON.getString(EMConstants.EMJSONKeys.DEPLOYMENT_NAME);
        this.targetEnv = depInfoJSON.getString(EMConstants.EMJSONKeys.TARGET_ENV);
    }

    @Override
    public EMConfigDeploymentInfo edit() {
        return null;
    }

    @Override
    public EMConfigDeploymentInfo done() {
        return null;
    }

    @Override
    public JSONObject toJSON() throws EMDataObjectIsInEditingException, EMDataObjectIsNotFilledException {
        if (this.isEditing()) {
            throw new EMDataObjectIsInEditingException();
        }

        JSONObject depInfoJSON =  new JSONObject();
        depInfoJSON.put(EMConstants.EMJSONKeys.DEPLOYMENT_NAME, this.deploymentName);
        depInfoJSON.put(EMConstants.EMJSONKeys.TARGET_ENV, this.targetEnv);
        JSONObject parentJSONObject = new JSONObject();
        parentJSONObject.put(EMConstants.EMJSONKeys.DEPLOYMENT_INFO, depInfoJSON);
        return parentJSONObject;
    }
}
