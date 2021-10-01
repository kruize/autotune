package com.autotune.experimentManager.data.input;

import com.autotune.experimentManager.data.input.interfaces.Editable;
import org.json.JSONObject;

public class EMConfigSettings implements Editable<EMConfigSettings> {
    EMConfigSettings(JSONObject jsonObject) {

    }

    EMConfigSettings() {

    }

    @Override
    public EMConfigSettings edit() {
        return null;
    }
}
