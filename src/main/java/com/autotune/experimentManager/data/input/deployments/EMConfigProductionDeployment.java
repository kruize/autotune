package com.autotune.experimentManager.data.input.deployments;

import com.autotune.experimentManager.data.input.EMMetricInput;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class EMConfigProductionDeployment extends EMConfigBaseDeployment implements ConvertToJSON {
    private String type;
    private String deploymentName;
    private String namespace;
    private ArrayList<EMMetricInput> metrics;
    private ArrayList<EMConfigDeploymentContainerConfig> containerConfigs;

    public EMConfigProductionDeployment(JSONObject jsonObject) throws IncompatibleInputJSONException, EMInvalidInstanceCreation {
        if (!jsonObject.has(EMConstants.EMJSONKeys.TYPE)
                || !jsonObject.has(EMConstants.EMJSONKeys.DEPLOYMENT_NAME)
                || !jsonObject.has(EMConstants.EMJSONKeys.NAMESPACE)
                || !jsonObject.has(EMConstants.EMJSONKeys.METRICS)) {
            throw new IncompatibleInputJSONException();
        }
        this.type = jsonObject.getString(EMConstants.EMJSONKeys.TYPE);
        this.deploymentName = jsonObject.getString(EMConstants.EMJSONKeys.DEPLOYMENT_NAME);
        this.namespace = jsonObject.getString(EMConstants.EMJSONKeys.NAMESPACE);
        this.containerConfigs = new ArrayList<EMConfigDeploymentContainerConfig>();
        this.metrics = new ArrayList<EMMetricInput>();
        JSONArray containers = jsonObject.getJSONArray(EMConstants.EMJSONKeys.CONTAINERS);
        for (Object raw_container : containers) {
            JSONObject container = (JSONObject) raw_container;
            EMConfigDeploymentContainerConfig containerConfig = new EMConfigDeploymentContainerConfig(container);
            this.containerConfigs.add(containerConfig);
        }
        JSONArray jsonMetrics = jsonObject.getJSONArray(EMConstants.EMJSONKeys.METRICS);
        for (Object raw_metric : jsonMetrics) {
            JSONObject metric = (JSONObject) raw_metric;
            EMMetricInput emMetricInput = new EMMetricInput(metric);
            this.metrics.add(emMetricInput);
        }
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
    public ArrayList<EMMetricInput> getMetrics() {
        return this.metrics;
    }

    @Override
    public ArrayList<EMConfigDeploymentContainerConfig> getContainers() {
        return containerConfigs;
    }

    public JSONArray getContainersToJSON() {
        JSONArray jsonArray = new JSONArray();
        for (EMConfigDeploymentContainerConfig containerConfig: getContainers()) {
            jsonArray.put(containerConfig.toJSON());
        }
        return jsonArray;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(EMConstants.EMJSONKeys.TYPE, getType());
        jsonObject.put(EMConstants.EMJSONKeys.DEPLOYMENT_NAME, getDeploymentName());
        jsonObject.put(EMConstants.EMJSONKeys.NAMESPACE, getNamespace());
        JSONArray jsonArray = new JSONArray();
        for (EMMetricInput mtrcs : getMetrics()) {
            jsonArray.put(mtrcs.toJSON());
        }
        jsonObject.put(EMConstants.EMJSONKeys.METRICS, getMetrics());
        jsonObject.put(EMConstants.EMJSONKeys.CONTAINERS, getContainersToJSON());
        return jsonObject;
    }
}
