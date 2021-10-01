package com.autotune.experimentManager.data.input;

import com.autotune.experimentManager.data.input.interfaces.Editable;
import org.json.JSONObject;

public class EMConfigInfo implements Editable<EMConfigInfo> {
    EMConfigInfo(JSONObject jsonObject) {

    }

    EMConfigInfo() {

    }

    public JSONObject toJSON() {
        return null;
    }

    @Override
    public EMConfigInfo edit() {
        return null;
    }
}
