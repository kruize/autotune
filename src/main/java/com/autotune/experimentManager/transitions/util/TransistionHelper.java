package com.autotune.experimentManager.transitions.util;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.data.input.EMMetricInput;
import com.autotune.experimentManager.data.input.metrics.EMMetricResult;
import com.autotune.experimentManager.data.iteration.EMIterationMetricResult;
import com.autotune.experimentManager.exceptions.EMMetricCollectionException;
import com.autotune.experimentManager.utils.EMConstants;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

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

    public static class CollectMetrics {
        public static void startMetricCollection(ExperimentTrialData etd) {
            for (EMMetricInput emMetricInput : etd.getConfig()
                                                .getEmConfigObject()
                                                .getDeployments()
                                                .getTrainingDeployment()
                                                .getMetrics()) {
                String query = expandQuery(etd, emMetricInput.getQuery());
                ArrayList<EMMetricResult> metricResults = getMetricsFromDataSource(emMetricInput.getName(), query, emMetricInput.getDataSource());
                try {
                    int totalCycles = etd.getEmIterationManager().getIterationData(etd.getEmIterationManager().getCurrentIteration()).getTotalCycles();
                    if (metricResults.size() < totalCycles) {
                        throw new EMMetricCollectionException();
                    }
                    int warmupCycles = etd.getEmIterationManager().getIterationData(etd.getEmIterationManager().getCurrentIteration()).getWarmCycles();
                    EMIterationMetricResult emIterationMetricResult = etd.getEmIterationManager().getIterationData(etd.getEmIterationManager().getCurrentIteration()).getEmIterationResult().getIterationMetricResult(emMetricInput.getName());
                    for (int i = 0; i < metricResults.size(); i++) {
                        if (i >= warmupCycles) {
                            emIterationMetricResult.addToMeasurementList(metricResults.get(i));
                        } else {
                            emIterationMetricResult.addToWarmUpList(metricResults.get(i));
                        }
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }

            }
        }

        public static String expandQuery(ExperimentTrialData etd, String query) {
            // Needs to be expanded based on the pod details
            return null;
        }

        public static ArrayList<EMMetricResult> getMetricsFromDataSource(String metricName, String Query, String datasource) {
            // Needs to be replaced with EMRestAPI for datasource querying
            System.out.println("Collecting metrics from datasource");
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
                    "\"info\":{\"trial_info\":{\"trial_id\":\"\"," +
                    "\"trial_num\":0}}}");
            return retJsonObj;
        }
    }

    public static class DataPoster {
        public static void sendData(String URL, JSONObject payload) {
            try {
                System.out.print(URL);
                HttpClient httpClient = HttpClientBuilder.create().build();
                try {
                    HttpPost request = new HttpPost(URL);
                    StringEntity params = new StringEntity(payload.toString());
                    request.addHeader("content-type", "application/json; utf-8");
                    request.setEntity(params);
                    HttpResponse response = httpClient.execute(request);
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    System.out.println("Sending Metrics JSON done.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
