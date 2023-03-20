package com.autotune.experimentManager.data.input.deployments;

import com.autotune.experimentManager.data.input.EMMetricInput;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.KruizeConstants;
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
        if (!jsonObject.has(KruizeConstants.JSONKeys.TYPE)
                || !jsonObject.has(KruizeConstants.JSONKeys.DEPLOYMENT_NAME)
                || !jsonObject.has(KruizeConstants.JSONKeys.NAMESPACE)
                || !jsonObject.has(KruizeConstants.JSONKeys.POD_METRICS)) {
            throw new IncompatibleInputJSONException();
        }
        this.type = jsonObject.getString(KruizeConstants.JSONKeys.TYPE);
        this.deploymentName = jsonObject.getString(KruizeConstants.JSONKeys.DEPLOYMENT_NAME);
        this.namespace = jsonObject.getString(KruizeConstants.JSONKeys.NAMESPACE);
        this.containerConfigs = new ArrayList<EMConfigDeploymentContainerConfig>();
        this.metrics = new ArrayList<EMMetricInput>();
        JSONArray containers = jsonObject.getJSONArray(KruizeConstants.JSONKeys.CONTAINERS);
        for (Object raw_container : containers) {
            JSONObject container = (JSONObject) raw_container;
            EMConfigDeploymentContainerConfig containerConfig = new EMConfigDeploymentContainerConfig(container);
            this.containerConfigs.add(containerConfig);
        }
        JSONArray jsonMetrics = jsonObject.getJSONArray(KruizeConstants.JSONKeys.POD_METRICS);
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
        jsonObject.put(KruizeConstants.JSONKeys.TYPE, getType());
        jsonObject.put(KruizeConstants.JSONKeys.DEPLOYMENT_NAME, getDeploymentName());
        jsonObject.put(KruizeConstants.JSONKeys.NAMESPACE, getNamespace());
        JSONArray jsonArray = new JSONArray();
        for (EMMetricInput mtrcs : getMetrics()) {
            jsonArray.put(mtrcs.toJSON());
        }
        jsonObject.put(KruizeConstants.JSONKeys.POD_METRICS, getMetrics());
        jsonObject.put(KruizeConstants.JSONKeys.CONTAINERS, getContainersToJSON());
        return jsonObject;
    }
}
