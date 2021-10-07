package com.autotune.experimentManager.data.input;

import com.autotune.experimentManager.data.input.deployments.EMConfigDeployments;
import com.autotune.experimentManager.data.input.info.EMConfigInfo;
import com.autotune.experimentManager.data.input.metadata.EMConfigMetaData;
import com.autotune.experimentManager.data.input.settings.EMConfigSettings;
import org.json.JSONObject;

public class EMConfigObject {
    private EMConfigMetaData metadata;
    private EMConfigInfo info;
    private EMConfigSettings settings;
    private EMConfigDeployments deployments;

    EMConfigObject (JSONObject inputJSON) {

    }

    EMConfigObject() {

    }
}
