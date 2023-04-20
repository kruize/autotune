package com.autotune.experimentManager.data.input.settings;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.utils.KruizeConstants;
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
        if (!jsonObject.has(KruizeConstants.JSONKeys.DEPLOYMENT_POLICY)) {
            throw new IncompatibleInputJSONException();
        }
        JSONObject depPolicyObject = jsonObject.getJSONObject(KruizeConstants.JSONKeys.DEPLOYMENT_POLICY);
        if (!depPolicyObject.has(KruizeConstants.JSONKeys.TYPE)) {
            this.type = EMConstants.EMJSONValueDefaults.DEPLOYMENT_TYPE_DEFAULT;
        } else {
            this.type = depPolicyObject.getString(KruizeConstants.JSONKeys.TYPE);
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
        typeObject.put(KruizeConstants.JSONKeys.TYPE, this.type);
        return typeObject;
    }
}
