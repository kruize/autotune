package com.autotune.experimentManager.data.input.info;

import com.autotune.experimentManager.data.input.abscls.DataEditor;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;

public class EMConfigInfo extends DataEditor<EMConfigInfo> implements ConvertToJSON {

    private String trialId;
    private int trialNum;

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

    public EMConfigInfo(JSONObject jsonObject) throws IncompatibleInputJSONException {
        if (!jsonObject.has("info")) {
            throw  new IncompatibleInputJSONException();
        }
        JSONObject subObj = jsonObject.getJSONObject("info");
        if (null != subObj) {
            if (!subObj.has("trial_id") || !subObj.has("trial_num")) {
                throw new IncompatibleInputJSONException();
            }
            this.trialId = subObj.getString("trial_id");
            this.trialNum = subObj.getInt("trial_num");
        } else {
            throw new IncompatibleInputJSONException();
        }
    }

    public EMConfigInfo() {
        this.trialNum = EMConstants.EMJSONValueDefaults.TRIAL_NUM_DEFAULT;
        this.trialId = EMConstants.EMJSONValueDefaults.TRIAL_ID_DEFAULT;
    }

    public EMConfigInfo(String trialId, int trialNum) {
        this.trialId = trialId;
        this.trialNum = trialNum;
    }

    public EMConfigInfo(int trialNum, String trialId) {
        this.trialNum = trialNum;
        this.trialId = trialId;
    }

    public JSONObject toJSON() throws EMDataObjectIsInEditingException, EMDataObjectIsNotFilledException {
        if (this.isEditing()) {
            throw new EMDataObjectIsInEditingException();
        }

        if (this.trialId.equalsIgnoreCase(EMConstants.EMJSONValueDefaults.TRIAL_ID_DEFAULT) || this.trialNum == EMConstants.EMJSONValueDefaults.TRIAL_NUM_DEFAULT) {
            throw new EMDataObjectIsNotFilledException();
        }

        JSONObject infoJsonObject = new JSONObject();
        infoJsonObject.put(EMConstants.EMJSONKeys.TRIAL_ID, this.trialId);
        infoJsonObject.put(EMConstants.EMJSONKeys.TRIAL_NUM, this.trialNum);

        JSONObject parentJsonObject = new JSONObject();
        parentJsonObject.put(EMConstants.EMJSONKeys.INFO, infoJsonObject);
        return parentJsonObject;
    }

    @Override
    public EMConfigInfo edit() {
        return this;
    }

    @Override
    public EMConfigInfo done() {
        return this;
    }
}
