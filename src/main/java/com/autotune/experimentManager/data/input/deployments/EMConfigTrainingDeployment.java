package com.autotune.experimentManager.data.input.deployments;

import com.autotune.experimentManager.data.input.EMMetricInput;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.AutotuneConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class EMConfigTrainingDeployment extends EMConfigBaseDeployment implements ConvertToJSON {

    private String type;
    private String deploymentName;
    private String namespace;
    private ArrayList<EMMetricInput> metrics;
    private ArrayList<EMConfigDeploymentContainerConfig> containerConfigs;

    public EMConfigTrainingDeployment() {

    }

    public EMConfigTrainingDeployment(JSONObject jsonObject) throws IncompatibleInputJSONException, EMInvalidInstanceCreation {
        if (!jsonObject.has(AutotuneConstants.JSONKeys.TYPE)
                || !jsonObject.has(AutotuneConstants.JSONKeys.DEPLOYMENT_NAME)
                || !jsonObject.has(AutotuneConstants.JSONKeys.NAMESPACE)
                || !jsonObject.has(AutotuneConstants.JSONKeys.POD_METRICS)) {
            throw new IncompatibleInputJSONException();
        }
        this.type = jsonObject.getString(AutotuneConstants.JSONKeys.TYPE);
        this.deploymentName = jsonObject.getString(AutotuneConstants.JSONKeys.DEPLOYMENT_NAME);
        this.namespace = jsonObject.getString(AutotuneConstants.JSONKeys.NAMESPACE);
        this.containerConfigs = new ArrayList<EMConfigDeploymentContainerConfig>();
        this.metrics = new ArrayList<EMMetricInput>();
        JSONArray containers = jsonObject.getJSONArray(AutotuneConstants.JSONKeys.CONTAINERS);
        for (Object raw_container : containers) {
            JSONObject container = (JSONObject) raw_container;
            EMConfigDeploymentContainerConfig containerConfig = new EMConfigDeploymentContainerConfig(container);
            this.containerConfigs.add(containerConfig);
        }
        JSONArray jsonMetrics = jsonObject.getJSONArray(AutotuneConstants.JSONKeys.POD_METRICS);
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
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    @Override
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public ArrayList<EMMetricInput> getMetrics() {
        return this.metrics;
    }

    @Override
    public void setMetrics(ArrayList<EMMetricInput> metrics) {
        this.metrics = metrics;
    }

    @Override
    public ArrayList<EMConfigDeploymentContainerConfig> getContainers() {
        return containerConfigs;
    }

    @Override
    public void setContainers(ArrayList<EMConfigDeploymentContainerConfig> containerConfigs) {
        this.containerConfigs = containerConfigs;
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
        jsonObject.put(AutotuneConstants.JSONKeys.TYPE, getType());
        jsonObject.put(AutotuneConstants.JSONKeys.DEPLOYMENT_NAME, getDeploymentName());
        jsonObject.put(AutotuneConstants.JSONKeys.NAMESPACE, getNamespace());
        JSONArray jsonArray = new JSONArray();
        for (EMMetricInput mtrcs : getMetrics()) {
            jsonArray.put(mtrcs.toJSON());
        }
        jsonObject.put(AutotuneConstants.JSONKeys.POD_METRICS, getMetrics());
        jsonObject.put(AutotuneConstants.JSONKeys.CONTAINERS, getContainersToJSON());
        return jsonObject;
    }
}
