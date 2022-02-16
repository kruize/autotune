package com.autotune.experimentManager.transitions.util;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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

    public static class MetricsFormatter {
        public static JSONObject getMetricsJson(String runId) {
            // Need to get data from ETD and process it
            ExperimentTrialData trialData = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
            JSONObject retJsonObj = new JSONObject("{\"experiment_name\":\"galaxies-autotune-min-http-response-time\"," +
                    "\"deployments\":[{\"deployment_name\":\"galaxies-sample\"," +
                    "\"namespace\":\"default\"," +
                    "\"containers\":[{\"image_name\":\"dinogun/galaxies:1.2-jdk-11.0.10_9\"," +
                    "\"container_name\":\"galaxies\"," +
                    "\"container_metrics\":[{\"name\":\"cpuRequest\"," +
                    "\"summary_results\":{\"general_info\":{\"mean\":3.98389," +
                    "\"min\":3.6809621160604," +
                    "\"max\":4.10951920556296}}}," +
                    "{\"name\":\"JvmMemoryUsed\"," +
                    "\"summary_results\":{\"general_info\":{\"max\":1123," +
                    "\"min\":769," +
                    "\"mean\":832.63}}}," +
                    "{\"name\":\"memoryRequest\"," +
                    "\"summary_results\":{\"general_info\":{\"max\":1212," +
                    "\"min\":834," +
                    "\"mean\":976.794}}}]}]," +
                    "\"pod_metrics\":[{\"name\":\"request_sum\"," +
                    "\"summary_results\":{\"general_info\":{\"min\":2.15," +
                    "\"mean\":31.91," +
                    "\"max\":2107.212121}," +
                    "\"percentile_info\":{\"50p\":0.63," +
                    "\"95p\":8.94," +
                    "\"97p\":64.75," +
                    "\"99p\":82.59," +
                    "\"99.9p\":93.48," +
                    "\"99.99p\":111.5," +
                    "\"99.999p\":198.52," +
                    "\"100p\":30000}}}," +
                    "{\"name\":\"request_count\"," +
                    "\"summary_results\":{\"general_info\":{\"max\":21466," +
                    "\"min\":2.11," +
                    "\"mean\":21045}}}]}]," +
                    "\"experiment_id\":\"7671cf10f52b288d48e3f60806af0740ec09a09360ed1da3a6e24df4cfd27256\"," +
                    "\"app-version\":\"v1\"," +
                    "\"info\":{\"trial_info\":{\"trial_id\":\"\"," +
                    "\"trial_num\":0}}}");
            return retJsonObj;
        }
    }

    public static class DataPoster {
        public static void sendData(String URL, JSONObject payload) {
            try {
                URL url = new URL (URL);
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json; utf-8");
                con.setDoOutput(true);
                try(OutputStream os = con.getOutputStream()) {
                    byte[] input = payload.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
