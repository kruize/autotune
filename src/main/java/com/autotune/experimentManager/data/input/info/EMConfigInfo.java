package com.autotune.experimentManager.data.input.info;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.KruizeConstants;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMConfigInfo implements ConvertToJSON {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMConfigInfo.class);
    private EMTrialInfo trialInfo;
    private EMDataSourceInfo dataSourceInfo;

    public EMTrialInfo getTrialInfo() {
        return trialInfo;
    }

    public void setTrialInfo(EMTrialInfo trialInfo) {
        this.trialInfo = trialInfo;
    }

    public EMDataSourceInfo getDataSourceInfo() {
        return dataSourceInfo;
    }

    public void setDataSourceInfo(EMDataSourceInfo dataSourceInfo) {
        this.dataSourceInfo = dataSourceInfo;
    }

    public EMConfigInfo(JSONObject jsonObject) throws IncompatibleInputJSONException {
        LOGGER.info("Creating EMConfigInfo");
        if (!jsonObject.has(KruizeConstants.JSONKeys.INFO)) {
            throw  new IncompatibleInputJSONException();
        }
        JSONObject subObj = jsonObject.getJSONObject(KruizeConstants.JSONKeys.INFO);
        if (null != subObj) {
            if (!subObj.has(KruizeConstants.JSONKeys.TRIAL_INFO) || !subObj.has(KruizeConstants.JSONKeys.DATASOURCE_INFO)) {
                throw new IncompatibleInputJSONException();
            }
            trialInfo = new EMTrialInfo(subObj);
            dataSourceInfo = new EMDataSourceInfo(subObj);
        } else {
            throw new IncompatibleInputJSONException();
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KruizeConstants.JSONKeys.TRIAL_INFO, trialInfo.toJSON());
        jsonObject.put(KruizeConstants.JSONKeys.DATASOURCE_INFO, dataSourceInfo.toJSON());
        return jsonObject;
    }
}
