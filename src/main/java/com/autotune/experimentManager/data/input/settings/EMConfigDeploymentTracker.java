package com.autotune.experimentManager.data.input.settings;

import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMConstants;
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
        if (!jsonObject.has(EMConstants.EMJSONKeys.DEPLOYMENT_TRACKING)) {
            throw new IncompatibleInputJSONException();
        }
        JSONObject trackingObject = jsonObject.getJSONObject(EMConstants.EMJSONKeys.DEPLOYMENT_TRACKING);
        if(!trackingObject.has(EMConstants.EMJSONKeys.TRACKERS)) {
            throw new IncompatibleInputJSONException();
        }
        JSONArray trackersList = trackingObject.getJSONArray(EMConstants.EMJSONKeys.TRACKERS);
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
        trackerJsonObject.put(EMConstants.EMJSONKeys.TRACKERS, trackerArray);
        JSONObject depTrackObject = new JSONObject();
        depTrackObject.put(EMConstants.EMJSONKeys.DEPLOYMENT_TRACKING, trackerJsonObject);
        return depTrackObject;
    }
}
