package com.autotune.experimentManager.services.util;

import com.autotune.experimentManager.data.EMTrialConfig;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import org.json.JSONObject;

public class EMAPIHandler {
    public static ExperimentTrialData createETD(JSONObject json) {
        try {
            EMTrialConfig config = new EMTrialConfig(json);
            ExperimentTrialData trailData = new ExperimentTrialData(config);
            return trailData;
        } catch (IncompatibleInputJSONException | EMInvalidInstanceCreation e) {
            e.printStackTrace();
        }
        return null;
    }
}
