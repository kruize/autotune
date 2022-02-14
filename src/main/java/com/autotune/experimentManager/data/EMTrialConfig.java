package com.autotune.experimentManager.data;

import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import org.json.JSONArray;
import org.json.JSONObject;

public class EMTrialConfig {
    private final JSONObject inputJSON;
    private final JSONObject configInfo;
    private final JSONObject configSettings;
    private final JSONArray configDeployments;

    private JSONObject parseConfigInfo() throws IncompatibleInputJSONException {
        if (inputJSON.has("info"))
            return inputJSON.getJSONObject("info");
        throw new IncompatibleInputJSONException();
    }

    private JSONObject parseConfigSettings() throws IncompatibleInputJSONException {
        if (inputJSON.has("settings"))
            return inputJSON.getJSONObject("settings");
        throw new IncompatibleInputJSONException();
    }

    private JSONArray parseConfigDeployments() throws IncompatibleInputJSONException {
        if (inputJSON.has("deployments"))
            return inputJSON.getJSONArray("deployments");
        throw new IncompatibleInputJSONException();
    }

    public EMTrialConfig(JSONObject inputJSON) throws IncompatibleInputJSONException {
        this.inputJSON = inputJSON;
        this.configInfo = parseConfigInfo();
        this.configSettings = parseConfigSettings();
        this.configDeployments = parseConfigDeployments();
    }

    public JSONObject getConfigSettings() {
        return this.configSettings;
    }

    public JSONArray getConfigDeployments() {
        return this.configDeployments;
    }

    public JSONArray getTrainingContainers() {
        for (Object deploymentConfig : configDeployments) {
            JSONObject castedDeploymentConfig = (JSONObject) deploymentConfig;
            if(castedDeploymentConfig.getString("type").equalsIgnoreCase("training")) {
                return castedDeploymentConfig.getJSONArray("containers");
            }
        }
        return null;
    }

    public String getDeploymentName() {
        for (Object deploymentConfig : configDeployments) {
            JSONObject castedDeploymentConfig = (JSONObject) deploymentConfig;
            if(castedDeploymentConfig.getString("type").equalsIgnoreCase("training")) {
                return castedDeploymentConfig.getString("deployment_name");
            }
        }
        return null;
    }

    public String getDeploymentStrategy() {
        return configSettings.getJSONObject("deployment_settings").getJSONObject("deployment_policy").getString("type");
    }

    public String getDeploymentNamespace() {
        for (Object deploymentConfig : configDeployments) {
            JSONObject castedDeploymentConfig = (JSONObject) deploymentConfig;
            if(castedDeploymentConfig.getString("type").equalsIgnoreCase("training")) {
                return castedDeploymentConfig.getString("namespace");
            }
        }
        return "default";
    }
}
