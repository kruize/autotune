package com.autotune.experimentManager.data.input.deployments;

import com.autotune.experimentManager.data.input.abscls.DataEditor;
import com.autotune.experimentManager.data.input.interfaces.ChangesDone;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.data.input.interfaces.Editable;
import org.json.JSONArray;
import org.json.JSONObject;

public class EMConfigDeployments extends DataEditor<EMConfigDeployments> implements ConvertToJSON {

    EMConfigDeployments(JSONArray jsonArray) {

    }

    EMConfigDeployments() {

    }

    public JSONObject toJSON() {
        return null;
    }

    @Override
    public EMConfigDeployments edit() {
        return this;
    }

    @Override
    public EMConfigDeployments done() {
        return this;
    }
}
