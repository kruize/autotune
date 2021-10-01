package com.autotune.experimentManager.data.input;

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
