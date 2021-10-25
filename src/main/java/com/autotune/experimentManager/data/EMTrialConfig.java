package com.autotune.experimentManager.data;

import com.autotune.experimentManager.core.EMExecutorService;
import com.autotune.experimentManager.data.input.EMConfigObject;
import com.autotune.experimentManager.exceptions.EMInvalidTimeDuarationException;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class EMTrialConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMTrialConfig.class);

    private EMConfigObject emConfigObject;
    private int totalCycles;
    private boolean isWarmup;
    private int currentCycle;

    public int getCurrentCycle() {
        return currentCycle;
    }

    public void setCurrentCycle(int currentCycle) {
        this.currentCycle = currentCycle;
    }

    public EMConfigObject getEmConfigObject() {
        return emConfigObject;
    }

    public void setEmConfigObject(EMConfigObject emConfigObject) {
        this.emConfigObject = emConfigObject;
    }

    public int getTotalCycles() {
        return totalCycles;
    }

    public void setTotalCycles(int totalCycles) {
        this.totalCycles = totalCycles;
    }

    public boolean isWarmup() {
        return isWarmup;
    }

    public void setWarmup(boolean warmup) {
        isWarmup = warmup;
    }

    public int getCycleDuration() throws EMInvalidTimeDuarationException {
        int duration = EMConstants.StandardDefaults.NEGATIVE_INT_DEFAULT;
        if (isWarmup() && emConfigObject.getSettings().getTrialSettings().getWarmupCycles() > 0) {
            int warmupDuration = EMUtil.extractTimeQuantity(emConfigObject.getSettings().getTrialSettings().getWarmupDuration());
            TimeUnit warmupTimeunit = EMUtil.extractTimeUnit(emConfigObject.getSettings().getTrialSettings().getWarmupDuration());
            if(null == warmupTimeunit) {
                LOGGER.error("Invalid Timeunits given in the input json");
            } else {
                duration = EMUtil.getSecondsFromTimeunit(warmupDuration, warmupTimeunit);
            }
        } else {
            int measurementDuration = EMUtil.extractTimeQuantity(emConfigObject.getSettings().getTrialSettings().getMeasurementDuration());
            TimeUnit measurementTimeunit = EMUtil.extractTimeUnit(emConfigObject.getSettings().getTrialSettings().getMeasurementDuration());
            if(null == measurementTimeunit) {
                LOGGER.error("Invalid Timeunits given in the input json");
            } else {
                duration = EMUtil.getSecondsFromTimeunit(measurementDuration, measurementTimeunit);
            }
        }
        return duration;
    }



    public EMTrialConfig(JSONObject inputJSON) throws IncompatibleInputJSONException {
        this.emConfigObject = new EMConfigObject(inputJSON);
        if (emConfigObject.getSettings().getTrialSettings().getWarmupCycles() <= 0 && emConfigObject.getSettings().getTrialSettings().getMeasurementCycles() <= 0) {
            throw new IncompatibleInputJSONException();
        }
        if(emConfigObject.getSettings().getTrialSettings().getWarmupCycles() <= 0) {
            this.totalCycles = emConfigObject.getSettings().getTrialSettings().getMeasurementCycles();
        }
        this.totalCycles = emConfigObject.getSettings().getTrialSettings().getWarmupCycles() + emConfigObject.getSettings().getTrialSettings().getMeasurementCycles();
    }

    public JSONArray getTrainingConfigs() {
        return emConfigObject.getDeployments().getTrainingDeployment().getRawConfigs();
    }

    public String getDeploymentName() {
        return emConfigObject.getDeployments().getTrainingDeployment().getDeploymentName();
    }

    public String getDeploymentStrategy() {
        return emConfigObject.getSettings().getDeploymentSettings().getDeploymentPolicy().getType();
    }

    public String getDeploymentNamespace() {
        return "default";
    }
}
