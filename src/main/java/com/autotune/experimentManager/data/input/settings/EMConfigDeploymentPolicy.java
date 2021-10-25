package com.autotune.experimentManager.data.input.settings;

import com.autotune.experimentManager.data.input.abscls.DataEditor;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;

public class EMConfigDeploymentPolicy extends DataEditor<EMConfigDeploymentPolicy> implements ConvertToJSON {

    private String type;

    public EMConfigDeploymentPolicy() {
        this.type = EMConstants.EMJSONValueDefaults.DEPLOYMENT_TYPE_DEFAULT;
    }

    public EMConfigDeploymentPolicy(JSONObject jsonObject) throws IncompatibleInputJSONException {
        if (!jsonObject.has(EMConstants.EMJSONKeys.DEPLOYMENT_POLICY)) {
            throw new IncompatibleInputJSONException();
        }
        JSONObject depPolicyObject = jsonObject.getJSONObject(EMConstants.EMJSONKeys.DEPLOYMENT_POLICY);
        if (!depPolicyObject.has(EMConstants.EMJSONKeys.TYPE)) {
            this.type = EMConstants.EMJSONValueDefaults.DEPLOYMENT_TYPE_DEFAULT;
        } else {
            this.type = depPolicyObject.getString(EMConstants.EMJSONKeys.TYPE);
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public EMConfigDeploymentPolicy edit() {
        return null;
    }

    @Override
    public EMConfigDeploymentPolicy done() {
        return null;
    }

    @Override
    public JSONObject toJSON() throws EMDataObjectIsInEditingException, EMDataObjectIsNotFilledException {
        JSONObject typeObject = new JSONObject();
        typeObject.put(EMConstants.EMJSONKeys.TYPE, this.type);
        JSONObject parentJSONObject = new JSONObject();
        parentJSONObject.put(EMConstants.EMJSONKeys.DEPLOYMENT_POLICY, typeObject);
        return parentJSONObject;
    }
}
