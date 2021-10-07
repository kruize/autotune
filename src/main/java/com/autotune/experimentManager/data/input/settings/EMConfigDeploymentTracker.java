package com.autotune.experimentManager.data.input.settings;

import com.autotune.experimentManager.data.input.abscls.DataEditor;
import com.autotune.experimentManager.data.input.interfaces.ConvertToJSON;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class EMConfigDeploymentTracker extends DataEditor<EMConfigDeploymentTracker> implements ConvertToJSON {
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

    @Override
    public EMConfigDeploymentTracker edit() {
        return null;
    }

    @Override
    public EMConfigDeploymentTracker done() {
        return null;
    }

    @Override
    public JSONObject toJSON() throws EMDataObjectIsInEditingException, EMDataObjectIsNotFilledException {
        if (this.isEditing()) {
            throw new EMDataObjectIsInEditingException();
        }
        JSONArray trackerArray = new JSONArray(this.trackerList);
        JSONObject trackerJsonObject =  new JSONObject();
        trackerJsonObject.put(EMConstants.EMJSONKeys.TRACKERS, trackerArray);
        JSONObject depTrackObject = new JSONObject();
        depTrackObject.put(EMConstants.EMJSONKeys.DEPLOYMENT_TRACKING, trackerJsonObject);
        return depTrackObject;
    }
}
