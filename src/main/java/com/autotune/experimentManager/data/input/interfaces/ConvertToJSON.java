package com.autotune.experimentManager.data.input.interfaces;

import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import org.json.JSONObject;

public interface ConvertToJSON {
    public JSONObject toJSON() throws EMDataObjectIsInEditingException, EMDataObjectIsNotFilledException;
}
