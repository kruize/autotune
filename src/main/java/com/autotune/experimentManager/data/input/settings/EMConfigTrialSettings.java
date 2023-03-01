package com.autotune.experimentManager.data.input.settings;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.AutotuneConstants;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMConfigTrialSettings implements ConvertToJSON {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMConfigTrialSettings.class);
    private String totalDuration;
    private int warmupCycles;
    private String warmupDuration;
    private int measurementCycles;
    private String measurementDuration;
    private int iterations;

    public int getIterations() {
        return iterations;
    }

    public int getWarmupCycles() {
        return warmupCycles;
    }

    public int getMeasurementCycles() {
        return measurementCycles;
    }

    public String getTotalDuration() {
        return totalDuration;
    }

    public String getWarmupDuration() {
        return warmupDuration;
    }

    public String getMeasurementDuration() {
        return measurementDuration;
    }

    public EMConfigTrialSettings(JSONObject jsonObject) throws IncompatibleInputJSONException {
        LOGGER.info("Creating EMConfigTrialSettings");
        if (!jsonObject.has(AutotuneConstants.JSONKeys.TRIAL_SETTINGS)) {
            throw new IncompatibleInputJSONException();
        }
        JSONObject trialSettingsJSON = jsonObject.getJSONObject(AutotuneConstants.JSONKeys.TRIAL_SETTINGS);
        if(trialSettingsJSON.has(AutotuneConstants.JSONKeys.TOTAL_DURATION)){
            this.totalDuration = trialSettingsJSON.getString(AutotuneConstants.JSONKeys.TOTAL_DURATION);
        }
        this.warmupCycles = trialSettingsJSON.getInt(AutotuneConstants.JSONKeys.WARMUP_CYCLES);
        this.warmupDuration = trialSettingsJSON.getString(AutotuneConstants.JSONKeys.WARMUP_DURATION);
        this.measurementCycles = trialSettingsJSON.getInt(AutotuneConstants.JSONKeys.MEASUREMENT_CYCLES);
        this.measurementDuration = trialSettingsJSON.getString(AutotuneConstants.JSONKeys.MEASUREMENT_DURATION);
        if (!trialSettingsJSON.has(AutotuneConstants.JSONKeys.ITERATIONS)) {
            this.iterations = 3;
        } else {
            this.iterations = trialSettingsJSON.getInt(AutotuneConstants.JSONKeys.ITERATIONS);
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject trialSettingsJSON = new JSONObject();
        trialSettingsJSON.put(AutotuneConstants.JSONKeys.TOTAL_DURATION, this.totalDuration);
        trialSettingsJSON.put(AutotuneConstants.JSONKeys.WARMUP_CYCLES, this.warmupCycles);
        trialSettingsJSON.put(AutotuneConstants.JSONKeys.WARMUP_DURATION, this.warmupDuration);
        trialSettingsJSON.put(AutotuneConstants.JSONKeys.MEASUREMENT_CYCLES, this.measurementCycles);
        trialSettingsJSON.put(AutotuneConstants.JSONKeys.MEASUREMENT_DURATION, this.measurementDuration);
        return trialSettingsJSON;
    }
}
