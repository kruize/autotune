package com.autotune.experimentManager.data.input.settings;

import com.autotune.experimentManager.data.EMTrialConfig;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
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
        if (!jsonObject.has(EMConstants.EMJSONKeys.TRIAL_SETTINGS)) {
            throw new IncompatibleInputJSONException();
        }
        JSONObject trialSettingsJSON = jsonObject.getJSONObject(EMConstants.EMJSONKeys.TRIAL_SETTINGS);
        if(trialSettingsJSON.has(EMConstants.EMJSONKeys.TOTAL_DURATION)){
            this.totalDuration = trialSettingsJSON.getString(EMConstants.EMJSONKeys.TOTAL_DURATION);
        }
        this.warmupCycles = Integer.parseInt(trialSettingsJSON.getString(EMConstants.EMJSONKeys.WARMUP_CYCLES));
        this.warmupDuration = trialSettingsJSON.getString(EMConstants.EMJSONKeys.WARMUP_DURATION);
        this.measurementCycles = Integer.parseInt(trialSettingsJSON.getString(EMConstants.EMJSONKeys.MEASUREMENT_CYCLES));
        this.measurementDuration = trialSettingsJSON.getString(EMConstants.EMJSONKeys.MEASUREMENT_DURATION);
        if (!trialSettingsJSON.has(EMConstants.EMJSONKeys.ITERATIONS)) {
            this.iterations = 3;
        } else {
            this.iterations = trialSettingsJSON.getInt(EMConstants.EMJSONKeys.ITERATIONS);
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject trialSettingsJSON = new JSONObject();
        trialSettingsJSON.put(EMConstants.EMJSONKeys.TOTAL_DURATION, this.totalDuration);
        trialSettingsJSON.put(EMConstants.EMJSONKeys.WARMUP_CYCLES, this.warmupCycles);
        trialSettingsJSON.put(EMConstants.EMJSONKeys.WARMUP_DURATION, this.warmupDuration);
        trialSettingsJSON.put(EMConstants.EMJSONKeys.MEASUREMENT_CYCLES, this.measurementCycles);
        trialSettingsJSON.put(EMConstants.EMJSONKeys.MEASUREMENT_DURATION, this.measurementDuration);
        return trialSettingsJSON;
    }
}
