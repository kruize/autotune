package com.autotune.experimentManager.data.input.settings;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.utils.AutotuneConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class EMConfigDeploymentTracker implements ConvertToJSON {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMConfigDeploymentTracker.class);
    private ArrayList<String> trackerList;

    public ArrayList<String> getTrackerList() {
        return trackerList;
    }

    public void setTrackerList(ArrayList<String> trackerList) {
        this.trackerList = trackerList;
    }

    public EMConfigDeploymentTracker() {
        trackerList = new ArrayList<String>();
    }

    public EMConfigDeploymentTracker(JSONObject jsonObject) throws IncompatibleInputJSONException {
        this.trackerList = new ArrayList<String>();
        if (!jsonObject.has(AutotuneConstants.JSONKeys.DEPLOYMENT_TRACKING)) {
            throw new IncompatibleInputJSONException();
        }
        JSONObject trackingObject = jsonObject.getJSONObject(AutotuneConstants.JSONKeys.DEPLOYMENT_TRACKING);
        if(!trackingObject.has(AutotuneConstants.JSONKeys.TRACKERS)) {
            throw new IncompatibleInputJSONException();
        }
        JSONArray trackersList = trackingObject.getJSONArray(AutotuneConstants.JSONKeys.TRACKERS);
        for (Object o: trackersList.toList()) {
            LOGGER.info("In the loop");
            if (null != o) {
                this.trackerList.add(o.toString());
            }
        }
        LOGGER.info("Creating EMConfigDeploymentTracker");
    }

    @Override
    public JSONObject toJSON() {
        JSONArray trackerArray = new JSONArray(this.trackerList);
        JSONObject trackerJsonObject =  new JSONObject();
        trackerJsonObject.put(AutotuneConstants.JSONKeys.TRACKERS, trackerArray);
        return trackerJsonObject;
    }
}
