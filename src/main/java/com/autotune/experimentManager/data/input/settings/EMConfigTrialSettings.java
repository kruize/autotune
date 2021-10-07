package com.autotune.experimentManager.data.input.settings;

import com.autotune.experimentManager.data.input.abscls.DataEditor;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import org.json.JSONObject;

public class EMConfigTrialSettings extends DataEditor<EMConfigTrialSettings> implements ConvertToJSON {

    @Override
    public EMConfigTrialSettings edit() {
        return null;
    }

    @Override
    public EMConfigTrialSettings done() {
        return null;
    }

    @Override
    public JSONObject toJSON() throws EMDataObjectIsInEditingException, EMDataObjectIsNotFilledException {
        return null;
    }
}
