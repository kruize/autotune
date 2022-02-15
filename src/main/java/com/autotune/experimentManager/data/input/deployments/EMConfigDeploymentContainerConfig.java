package com.autotune.experimentManager.data.input.deployments;

import com.autotune.experimentManager.data.input.EMMetricInput;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class EMConfigDeploymentContainerConfig implements ConvertToJSON {
    private String imageName;
    private String containerName;
    private JSONArray config;
    private ArrayList<EMMetricInput> containerMetrics;


    public EMConfigDeploymentContainerConfig() {
    }

    public EMConfigDeploymentContainerConfig(String imageName, String containerName, JSONArray config, ArrayList<EMMetricInput> containerMetrics) {
        this.imageName = imageName;
        this.containerName = containerName;
        this.config = config;
        this.containerMetrics = containerMetrics;
    }

    public ArrayList<EMMetricInput> getContainerMetrics() {
        return containerMetrics;
    }

    public void setContainerMetrics(ArrayList<EMMetricInput> containerMetrics) {
        this.containerMetrics = containerMetrics;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public void setConfig(JSONArray config) {
        this.config = config;
    }

    public String getImageName() {
        return imageName;
    }

    public String getContainerName() {
        return containerName;
    }

    public JSONArray getConfig() {
        return config;
    }

    public EMConfigDeploymentContainerConfig(JSONObject container) throws IncompatibleInputJSONException, EMInvalidInstanceCreation {
        if (!container.has(EMConstants.EMJSONKeys.IMAGE_NAME)
            || !container.has(EMConstants.EMJSONKeys.CONTAINER_NAME)
            || !container.has(EMConstants.EMJSONKeys.CONFIG)) {
            throw new IncompatibleInputJSONException();
        }
        this.config = container.getJSONArray(EMConstants.EMJSONKeys.CONFIG);
        this.imageName = container.getString(EMConstants.EMJSONKeys.IMAGE_NAME);
        this.containerName = container.getString(EMConstants.EMJSONKeys.CONTAINER_NAME);
        JSONArray metrics = container.getJSONArray(EMConstants.EMJSONKeys.CONTAINER_METRICS);
        this.containerMetrics = new ArrayList<EMMetricInput>();
        for (Object metric : metrics) {
            JSONObject metricObj = (JSONObject) metric;
            containerMetrics.add(new EMMetricInput(metricObj));
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(EMConstants.EMJSONKeys.IMAGE_NAME, getImageName());
        jsonObject.put(EMConstants.EMJSONKeys.CONTAINER_NAME, getContainerName());
        jsonObject.put(EMConstants.EMJSONKeys.CONFIG, getConfig());
        jsonObject.put(EMConstants.EMJSONKeys.CONTAINER_METRICS, getContainerMetricsJSON());
        return jsonObject;
    }

    private JSONArray getContainerMetricsJSON() {
        JSONArray jsonArray = new JSONArray();
        for (EMMetricInput emMetricInput : containerMetrics) {
            jsonArray.put(emMetricInput.toJSON());
        }
        return jsonArray;
    }
}
