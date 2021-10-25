package com.autotune.experimentManager.data.input.deployments;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class EMConfigProductionDeployment extends EMConfigBaseDeployment implements ConvertToJSON {

    private String type;
    private String deploymentName;
    private String namespace;
    private ArrayList<EMConfigDeploymentMetrics> metrics;
    private ArrayList<EMConfigDeploymentConfig> configs;
    private JSONArray rawConfigs;

    public JSONArray getRawConfigs() {
        return rawConfigs;
    }

    public EMConfigProductionDeployment() {

    }

    public EMConfigProductionDeployment(JSONObject jsonObject) throws IncompatibleInputJSONException {
        if (!jsonObject.has(EMConstants.EMJSONKeys.TYPE)
                || !jsonObject.has(EMConstants.EMJSONKeys.DEPLOYMENT_NAME)
                || !jsonObject.has(EMConstants.EMJSONKeys.NAMESPACE)) {
            throw new IncompatibleInputJSONException();
        }
        this.type = jsonObject.getString(EMConstants.EMJSONKeys.TYPE);
        this.deploymentName = jsonObject.getString(EMConstants.EMJSONKeys.DEPLOYMENT_NAME);
        this.namespace = jsonObject.getString(EMConstants.EMJSONKeys.NAMESPACE);
        this.rawConfigs = jsonObject.getJSONArray(EMConstants.EMJSONKeys.CONFIG);
        this.metrics = new ArrayList<EMConfigDeploymentMetrics>();
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public String getDeploymentName() {
        return this.deploymentName;
    }

    @Override
    public String getNamespace() {
        return this.namespace;
    }

    @Override
    public ArrayList<EMConfigDeploymentMetrics> getMetrics() {
        return this.metrics;
    }

    @Override
    public ArrayList<EMConfigDeploymentConfig> getConfigs() {
        return this.configs;
    }

    @Override
    public JSONObject toJSON() throws EMDataObjectIsInEditingException, EMDataObjectIsNotFilledException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(EMConstants.EMJSONKeys.TYPE, getType());
        jsonObject.put(EMConstants.EMJSONKeys.DEPLOYMENT_NAME, getDeploymentName());
        jsonObject.put(EMConstants.EMJSONKeys.NAMESPACE, getNamespace());
        jsonObject.put(EMConstants.EMJSONKeys.METRICS, getMetrics());
        jsonObject.put(EMConstants.EMJSONKeys.CONFIG, getRawConfigs());
        return jsonObject;
    }
}
