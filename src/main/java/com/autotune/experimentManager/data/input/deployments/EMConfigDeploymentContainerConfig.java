package com.autotune.experimentManager.data.input.deployments;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONArray;
import org.json.JSONObject;

public class EMConfigDeploymentContainerConfig implements ConvertToJSON {
    private String imageName;
    private String containerName;
    private JSONArray config;

    public String getImageName() {
        return imageName;
    }

    public String getContainerName() {
        return containerName;
    }

    public JSONArray getConfig() {
        return config;
    }

    public EMConfigDeploymentContainerConfig(JSONObject container) throws IncompatibleInputJSONException {
        if (!container.has(EMConstants.EMJSONKeys.IMAGE_NAME)
            || !container.has(EMConstants.EMJSONKeys.CONTAINER_NAME)
            || !container.has(EMConstants.EMJSONKeys.CONFIG)) {
            throw new IncompatibleInputJSONException();
        }
        this.config = container.getJSONArray(EMConstants.EMJSONKeys.CONFIG);
        this.imageName = container.getString(EMConstants.EMJSONKeys.IMAGE_NAME);
        this.containerName = container.getString(EMConstants.EMJSONKeys.CONTAINER_NAME);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(EMConstants.EMJSONKeys.IMAGE_NAME, getImageName());
        jsonObject.put(EMConstants.EMJSONKeys.CONTAINER_NAME, getContainerName());
        jsonObject.put(EMConstants.EMJSONKeys.CONFIG, getConfig());
        return jsonObject;
    }
}
