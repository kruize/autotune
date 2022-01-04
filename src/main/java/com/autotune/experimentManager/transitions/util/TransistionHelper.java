package com.autotune.experimentManager.transitions.util;

import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONArray;
import org.json.JSONObject;

public class TransistionHelper {
    public static class LoadAnalyser {
        public static boolean isLoadApplied() {
            return false;
        }

        public static boolean isReadyToLoad() {
            return true;
        }
    }

    public static class ConfigHelper {
        public static JSONArray getContainerConfig(String containerName, JSONArray containersConfig) {
            for (Object obj : containersConfig) {
                JSONObject containerObj = (JSONObject) obj;
                if (containerObj.getString(EMConstants.EMJSONKeys.CONTAINER_NAME).equalsIgnoreCase(containerName)) {
                    return containerObj.getJSONArray(EMConstants.EMJSONKeys.CONFIG);
                }
            }
            return null;
        }
    }
}
