package com.autotune.experimentManager.data.input.deployments;

import com.autotune.experimentManager.data.input.EMMetricInput;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.KruizeConstants;
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
        if (!container.has(KruizeConstants.JSONKeys.IMAGE_NAME)
            || !container.has(KruizeConstants.JSONKeys.CONTAINER_NAME)
            || !container.has(KruizeConstants.JSONKeys.CONFIG)) {
            throw new IncompatibleInputJSONException();
        }
        this.config = container.getJSONArray(KruizeConstants.JSONKeys.CONFIG);
        this.imageName = container.getString(KruizeConstants.JSONKeys.IMAGE_NAME);
        this.containerName = container.getString(KruizeConstants.JSONKeys.CONTAINER_NAME);
        JSONArray metrics = container.getJSONArray(KruizeConstants.JSONKeys.CONTAINER_METRICS);
        this.containerMetrics = new ArrayList<EMMetricInput>();
        for (Object metric : metrics) {
            JSONObject metricObj = (JSONObject) metric;
            containerMetrics.add(new EMMetricInput(metricObj));
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KruizeConstants.JSONKeys.IMAGE_NAME, getImageName());
        jsonObject.put(KruizeConstants.JSONKeys.CONTAINER_NAME, getContainerName());
        jsonObject.put(KruizeConstants.JSONKeys.CONFIG, getConfig());
        jsonObject.put(KruizeConstants.JSONKeys.CONTAINER_METRICS, getContainerMetricsJSON());
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
