package com.autotune.experimentManager.data.input.metrics;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;

public class EMMetricResultSummary implements ConvertToJSON {
    private String result = null;
    private String resultError = null;
    private boolean isSuccess = false;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResultError() {
        return resultError;
    }

    public void setResultError(String resultError) {
        this.resultError = resultError;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        if (this.isSuccess) {
            jsonObject.put(EMConstants.EMJSONKeys.RESULT_OUTCOME, EMConstants.EMJSONKeys.SUCCESS);
        } else {
            jsonObject.put(EMConstants.EMJSONKeys.RESULT_OUTCOME, EMConstants.EMJSONKeys.SUCCESS);
        }
        if (null != result) {
            jsonObject.put(EMConstants.EMJSONKeys.RESULT, this.result);
        }
        if (null != resultError) {
            jsonObject.put(EMConstants.EMJSONKeys.RESULT_ERROR, this.resultError);
        }
        return jsonObject;
    }
}
