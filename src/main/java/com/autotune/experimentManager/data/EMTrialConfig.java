package com.autotune.experimentManager.data;

import com.autotune.experimentManager.data.input.EMConfigObject;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.services.util.EMAPIHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMTrialConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMTrialConfig.class);
    final EMConfigObject emConfigObject;

    public EMTrialConfig(JSONObject jsonObject) throws EMInvalidInstanceCreation, IncompatibleInputJSONException {
        LOGGER.info("Creating EMConfigObject");
        emConfigObject = new EMConfigObject(jsonObject);
    }

    public EMConfigObject getEmConfigObject() {
        return emConfigObject;
    }

    public String getDeploymentName() {
        return emConfigObject.getDeployments().getTrainingDeployment().getDeploymentName();
    }

    public String getDeploymentStrategy() {
        return emConfigObject.getSettings().getDeploymentSettings().getDeploymentPolicy().getType();
    }

    public String getDeploymentNamespace() {
        return emConfigObject.getDeployments().getTrainingDeployment().getNamespace();
    }

    public JSONArray getTrainingContainers() {
        return emConfigObject.getDeployments().getTrainingDeployment().getContainersToJSON();
    }
}
