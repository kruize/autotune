package com.autotune.experimentManager.data.input.settings;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.KruizeConstants;
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
        if (!jsonObject.has(KruizeConstants.JSONKeys.TRIAL_SETTINGS)) {
            throw new IncompatibleInputJSONException();
        }
        JSONObject trialSettingsJSON = jsonObject.getJSONObject(KruizeConstants.JSONKeys.TRIAL_SETTINGS);
        if(trialSettingsJSON.has(KruizeConstants.JSONKeys.TOTAL_DURATION)){
            this.totalDuration = trialSettingsJSON.getString(KruizeConstants.JSONKeys.TOTAL_DURATION);
        }
        this.warmupCycles = trialSettingsJSON.getInt(KruizeConstants.JSONKeys.WARMUP_CYCLES);
        this.warmupDuration = trialSettingsJSON.getString(KruizeConstants.JSONKeys.WARMUP_DURATION);
        this.measurementCycles = trialSettingsJSON.getInt(KruizeConstants.JSONKeys.MEASUREMENT_CYCLES);
        this.measurementDuration = trialSettingsJSON.getString(KruizeConstants.JSONKeys.MEASUREMENT_DURATION);
        if (!trialSettingsJSON.has(KruizeConstants.JSONKeys.ITERATIONS)) {
            this.iterations = 3;
        } else {
            this.iterations = trialSettingsJSON.getInt(KruizeConstants.JSONKeys.ITERATIONS);
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject trialSettingsJSON = new JSONObject();
        trialSettingsJSON.put(KruizeConstants.JSONKeys.TOTAL_DURATION, this.totalDuration);
        trialSettingsJSON.put(KruizeConstants.JSONKeys.WARMUP_CYCLES, this.warmupCycles);
        trialSettingsJSON.put(KruizeConstants.JSONKeys.WARMUP_DURATION, this.warmupDuration);
        trialSettingsJSON.put(KruizeConstants.JSONKeys.MEASUREMENT_CYCLES, this.measurementCycles);
        trialSettingsJSON.put(KruizeConstants.JSONKeys.MEASUREMENT_DURATION, this.measurementDuration);
        return trialSettingsJSON;
    }
}
