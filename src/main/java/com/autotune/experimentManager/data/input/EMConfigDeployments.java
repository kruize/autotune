package com.autotune.experimentManager.data.input;

import com.autotune.experimentManager.data.input.interfaces.Editable;
import org.json.JSONArray;

public class EMConfigDeployments implements Editable<EMConfigDeployments> {
    EMConfigDeployments(JSONArray jsonArray) {

    }

    EMConfigDeployments() {

    }

    @Override
    public EMConfigDeployments edit() {
        return null;
    }
}
