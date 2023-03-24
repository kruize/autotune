package com.autotune.experimentManager.data.input.info;

import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.utils.KruizeConstants;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMTrialInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMTrialInfo.class);
    private String trialId;
    private int trialNum;
    private String trialResultUrl;

    public String getTrialResultUrl() {
        return trialResultUrl;
    }

    public void setTrialResultUrl(String trialResultUrl) {
        this.trialResultUrl = trialResultUrl;
    }

    public String getTrialId() {
        return trialId;
    }

    public void setTrialId(String trialId) {
        this.trialId = trialId;
    }

    public int getTrialNum() {
        return trialNum;
    }

    public void setTrialNum(int trialNum) {
        this.trialNum = trialNum;
    }

    public EMTrialInfo(JSONObject jsonObject) throws IncompatibleInputJSONException {
        LOGGER.info("Creating EMConfigInfo");
        if (!jsonObject.has(KruizeConstants.JSONKeys.TRIAL_INFO)) {
            throw  new IncompatibleInputJSONException();
        }
        JSONObject subObj = jsonObject.getJSONObject(KruizeConstants.JSONKeys.TRIAL_INFO);
        if (null != subObj) {
            if (!subObj.has(KruizeConstants.JSONKeys.TRIAL_ID) || !subObj.has(KruizeConstants.JSONKeys.TRIAL_NUM)) {
                throw new IncompatibleInputJSONException();
            }
            this.trialId = subObj.getString(KruizeConstants.JSONKeys.TRIAL_ID);
            this.trialNum = subObj.getInt(KruizeConstants.JSONKeys.TRIAL_NUM);
            if(subObj.has(KruizeConstants.JSONKeys.TRIAL_RESULT_URL)){
                this.trialResultUrl = subObj.getString(KruizeConstants.JSONKeys.TRIAL_RESULT_URL);
            }
        } else {
            throw new IncompatibleInputJSONException();
        }
    }

    public EMTrialInfo() {
        this.trialNum = EMConstants.EMJSONValueDefaults.TRIAL_NUM_DEFAULT;
        this.trialId = EMConstants.EMJSONValueDefaults.TRIAL_ID_DEFAULT;
        this.trialResultUrl = EMConstants.EMJSONValueDefaults.TRIAL_RESULT_URL_DEFAULT;
    }

    public EMTrialInfo(String trialId, int trialNum, String url) {
        this.trialId = trialId;
        this.trialNum = trialNum;
        this.trialResultUrl = url;
    }

    public JSONObject toJSON() {

        JSONObject infoJsonObject = new JSONObject();
        infoJsonObject.put(KruizeConstants.JSONKeys.TRIAL_ID, this.trialId);
        infoJsonObject.put(KruizeConstants.JSONKeys.TRIAL_NUM, this.trialNum);
        infoJsonObject.put(KruizeConstants.JSONKeys.TRIAL_RESULT_URL, this.trialResultUrl);


        return infoJsonObject;
    }
}
