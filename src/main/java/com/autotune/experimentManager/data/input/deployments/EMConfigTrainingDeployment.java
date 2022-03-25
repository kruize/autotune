package com.autotune.experimentManager.data.input.deployments;

import com.autotune.experimentManager.data.input.EMMetricInput;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class EMConfigTrainingDeployment extends EMConfigBaseDeployment implements ConvertToJSON {

    private String type;
    private String deploymentName;
    private String namespace;
    private ArrayList<EMMetricInput> podMetrics;
    private ArrayList<EMMetricInput> allMetrics;
    private ArrayList<EMConfigDeploymentContainerConfig> containerConfigs;

    public EMConfigTrainingDeployment() {

    }

    public EMConfigTrainingDeployment(JSONObject jsonObject) throws IncompatibleInputJSONException, EMInvalidInstanceCreation {
        if (!jsonObject.has(EMConstants.EMJSONKeys.TYPE)
                || !jsonObject.has(EMConstants.EMJSONKeys.DEPLOYMENT_NAME)
                || !jsonObject.has(EMConstants.EMJSONKeys.NAMESPACE)
                || !jsonObject.has(EMConstants.EMJSONKeys.POD_METRICS)) {
            throw new IncompatibleInputJSONException();
        }
        this.type = jsonObject.getString(EMConstants.EMJSONKeys.TYPE);
        this.deploymentName = jsonObject.getString(EMConstants.EMJSONKeys.DEPLOYMENT_NAME);
        this.namespace = jsonObject.getString(EMConstants.EMJSONKeys.NAMESPACE);
        this.containerConfigs = new ArrayList<EMConfigDeploymentContainerConfig>();
        this.podMetrics = new ArrayList<EMMetricInput>();
        this.allMetrics = new ArrayList<EMMetricInput>();
        JSONArray containers = jsonObject.getJSONArray(EMConstants.EMJSONKeys.CONTAINERS);
        for (Object raw_container : containers) {
            JSONObject container = (JSONObject) raw_container;
            EMConfigDeploymentContainerConfig containerConfig = new EMConfigDeploymentContainerConfig(container);
            this.containerConfigs.add(containerConfig);
        }
        JSONArray jsonMetrics = jsonObject.getJSONArray(EMConstants.EMJSONKeys.POD_METRICS);
        for (Object raw_metric : jsonMetrics) {
            JSONObject metric = (JSONObject) raw_metric;
            EMMetricInput emMetricInput = new EMMetricInput(metric);
            this.podMetrics.add(emMetricInput);
        }
        for (EMConfigDeploymentContainerConfig config : this.containerConfigs) {
            this.allMetrics.addAll(config.getContainerMetrics());
        }
        this.allMetrics.addAll(podMetrics);
    }

    public ArrayList<EMMetricInput> getAllMetrics() {
        return allMetrics;
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
    public ArrayList<EMMetricInput> getPodMetrics() {
        return this.podMetrics;
    }

    @Override
    public void setPodMetrics(ArrayList<EMMetricInput> podMetrics) {
        this.podMetrics = podMetrics;
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
        jsonObject.put(EMConstants.EMJSONKeys.TYPE, getType());
        jsonObject.put(EMConstants.EMJSONKeys.DEPLOYMENT_NAME, getDeploymentName());
        jsonObject.put(EMConstants.EMJSONKeys.NAMESPACE, getNamespace());
        JSONArray jsonArray = new JSONArray();
        for (EMMetricInput mtrcs : getPodMetrics()) {
            jsonArray.put(mtrcs.toJSON());
        }
        jsonObject.put(EMConstants.EMJSONKeys.POD_METRICS, getPodMetrics());
        jsonObject.put(EMConstants.EMJSONKeys.CONTAINERS, getContainersToJSON());
        return jsonObject;
    }
}
