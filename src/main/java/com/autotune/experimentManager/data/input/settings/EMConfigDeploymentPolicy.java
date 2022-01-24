package com.autotune.experimentManager.data.input.settings;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMConfigDeploymentPolicy implements ConvertToJSON {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMConfigDeploymentPolicy.class);
    private String type;

    public EMConfigDeploymentPolicy() {
        this.type = EMConstants.EMJSONValueDefaults.DEPLOYMENT_TYPE_DEFAULT;
    }

    public EMConfigDeploymentPolicy(JSONObject jsonObject) throws IncompatibleInputJSONException {
        LOGGER.info("Creating EMConfigDeploymentPolicy");
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
    public JSONObject toJSON() {
        JSONObject typeObject = new JSONObject();
        typeObject.put(EMConstants.EMJSONKeys.TYPE, this.type);
        return typeObject;
    }
}
