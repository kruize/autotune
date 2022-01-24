package com.autotune.experimentManager.data.input.settings;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMConfigDeploymentSettings implements ConvertToJSON {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMConfigDeploymentSettings.class);
    private EMConfigDeploymentTracker deploymentTracker;
    private EMConfigDeploymentPolicy deploymentPolicy;

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
        LOGGER.info("Creating EMConfigDeploymentSettings");
        if(!jsonObject.has(EMConstants.EMJSONKeys.DEPLOYMENT_SETTINGS)) {
            throw new IncompatibleInputJSONException();
        }
        JSONObject contentObject = jsonObject.getJSONObject(EMConstants.EMJSONKeys.DEPLOYMENT_SETTINGS);
        deploymentTracker = new EMConfigDeploymentTracker(contentObject);
        deploymentPolicy = new EMConfigDeploymentPolicy(contentObject);
    }



    @Override
    public JSONObject toJSON() {
        JSONObject subObject = new JSONObject();
        subObject.put(EMConstants.EMJSONKeys.DEPLOYMENT_POLICY, getDeploymentPolicy().toJSON());
        subObject.put(EMConstants.EMJSONKeys.DEPLOYMENT_TRACKING, getDeploymentTracker().toJSON().getJSONArray(EMConstants.EMJSONKeys.DEPLOYMENT_TRACKING));
        return  subObject;
    }
}
