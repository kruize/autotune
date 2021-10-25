package com.autotune.experimentManager.data.input.settings;

import com.autotune.experimentManager.data.input.abscls.DataEditor;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;

public class EMConfigTrialSettings extends DataEditor<EMConfigTrialSettings> implements ConvertToJSON {

    private String totalDuration;
    private int warmupCycles;
    private String warmupDuration;
    private int measurementCycles;
    private String measurementDuration;

    public int getWarmupCycles() {
        return warmupCycles;
    }

    public void setWarmupCycles(int warmupCycles) {
        this.warmupCycles = warmupCycles;
    }

    public int getMeasurementCycles() {
        return measurementCycles;
    }

    public void setMeasurementCycles(int measurementCycles) {
        this.measurementCycles = measurementCycles;
    }

    public String getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(String totalDuration) {
        this.totalDuration = totalDuration;
    }



    public String getWarmupDuration() {
        return warmupDuration;
    }

    public void setWarmupDuration(String warmupDuration) {
        this.warmupDuration = warmupDuration;
    }



    public String getMeasurementDuration() {
        return measurementDuration;
    }

    public void setMeasurementDuration(String measurementDuration) {
        this.measurementDuration = measurementDuration;
    }

    public EMConfigTrialSettings() {

    }

    public EMConfigTrialSettings(JSONObject jsonObject) throws IncompatibleInputJSONException {
        if (!jsonObject.has(EMConstants.EMJSONKeys.TRIAL_SETTINGS)) {
            throw new IncompatibleInputJSONException();
        }
        JSONObject trialSettingsJSON = jsonObject.getJSONObject(EMConstants.EMJSONKeys.TRIAL_SETTINGS);
        this.totalDuration = trialSettingsJSON.getString(EMConstants.EMJSONKeys.TOTAL_DURATION);
        this.warmupCycles = trialSettingsJSON.getInt(EMConstants.EMJSONKeys.WARMUP_CYCLES);
        this.warmupDuration = trialSettingsJSON.getString(EMConstants.EMJSONKeys.WARMUP_DURATION);
        this.measurementCycles = trialSettingsJSON.getInt(EMConstants.EMJSONKeys.MEASUREMENT_CYCLES);
        this.measurementDuration = trialSettingsJSON.getString(EMConstants.EMJSONKeys.MEASUREMENT_DURATION);
    }

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
        if (this.isEditing()) {
            throw new EMDataObjectIsInEditingException();
        }
        JSONObject trialSettingsJSON = new JSONObject();
        trialSettingsJSON.put(EMConstants.EMJSONKeys.TOTAL_DURATION, this.totalDuration);
        trialSettingsJSON.put(EMConstants.EMJSONKeys.WARMUP_CYCLES, this.warmupCycles);
        trialSettingsJSON.put(EMConstants.EMJSONKeys.WARMUP_DURATION, this.warmupDuration);
        trialSettingsJSON.put(EMConstants.EMJSONKeys.MEASUREMENT_CYCLES, this.measurementCycles);
        trialSettingsJSON.put(EMConstants.EMJSONKeys.MEASUREMENT_DURATION, this.measurementDuration);
        JSONObject parentJSONObject = new JSONObject();
        parentJSONObject.put(EMConstants.EMJSONKeys.TRIAL_SETTINGS, trialSettingsJSON);
        return parentJSONObject;
    }
}
