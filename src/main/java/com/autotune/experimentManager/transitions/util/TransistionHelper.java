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
                                                .getPodMetrics()) {
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
            JSONObject retJson = trialData.getConfig().getEmConfigObject().toJSON();
            System.out.println("Input JSON without Metrics :");
            System.out.println(retJson.toString(2));
            retJson.remove("settings");
            retJson.getJSONObject("info").remove("datasource_info");
            JSONObject cpuRequestResult = new JSONObject("" +
                    "{\"general_info\":{\"mean\":3.98389," +
                    "\"min\":3.6809621160604," +
                    "\"max\":4.10951920556296}}"
            );
            JSONObject jvmMemUsedResult = new JSONObject("" +
                    "{\"general_info\":{\"max\":1123," +
                    "\"min\":769," +
                    "\"mean\":832.63}}");
            JSONObject memoryRequestResult = new JSONObject("" +
                    "{\"general_info\":{\"max\":1212," +
                    "\"min\":834," +
                    "\"mean\":976.794}}");
            JSONObject requestSumResult = new JSONObject("" +
                    "{\"general_info\":{\"min\":2.15," +
                    "\"mean\":31.91," +
                    "\"max\":2107.212121}," +
                    "\"percentile_info\":{\"50p\":0.63," +
                    "\"95p\":8.94," +
                    "\"97p\":64.75," +
                    "\"99p\":82.59," +
                    "\"99.9p\":93.48," +
                    "\"99.99p\":111.5," +
                    "\"99.999p\":198.52," +
                    "\"100p\":30000}}");
            JSONObject requestCountResult = new JSONObject("" +
                    "{\"general_info\":{\"max\":21466," +
                    "\"min\":2.11," +
                    "\"mean\":21045}}");
            JSONArray containersArr = ((JSONObject) retJson.getJSONArray("deployments").get(0)).getJSONArray("containers");
            for (Object container: containersArr){
                JSONObject containerJson = (JSONObject) container;
                containerJson.remove("config");
                JSONArray containerMetrics = containerJson.getJSONArray("container_metrics");
                for (Object contMetObj : containerMetrics) {
                    JSONObject contMetJson = (JSONObject) contMetObj;
                    contMetJson.remove("datasource");
                    contMetJson.remove("query");
                    if (contMetJson.getString("name").equalsIgnoreCase("cpuRequest")) {
                        contMetJson.put("summary_results", cpuRequestResult);
                    } else if (contMetJson.getString("name").equalsIgnoreCase("memoryRequest")) {
                        contMetJson.put("summary_results", memoryRequestResult);
                    } else if (contMetJson.getString("name").equalsIgnoreCase("request_sum")) {
                        contMetJson.put("summary_results", requestSumResult);
                    } else if (contMetJson.getString("name").equalsIgnoreCase("request_count")) {
                        contMetJson.put("summary_results", requestCountResult);
                    } else if (contMetJson.getString("name").equalsIgnoreCase("JvmMemoryUsed")) {
                        contMetJson.put("summary_results", jvmMemUsedResult);
                    }
                }
            }
            JSONArray podMetrics = ((JSONObject) retJson.getJSONArray("deployments").get(0)).getJSONArray("pod_metrics");
            for (Object podmetobj : podMetrics) {
                JSONObject podmetJson = (JSONObject) podmetobj;
                podmetJson.remove("datasource");
                podmetJson.remove("query");
                if (podmetJson.getString("name").equalsIgnoreCase("cpuRequest")) {
                    podmetJson.put("summary_results", cpuRequestResult);
                } else if (podmetJson.getString("name").equalsIgnoreCase("memoryRequest")) {
                    podmetJson.put("summary_results", memoryRequestResult);
                } else if (podmetJson.getString("name").equalsIgnoreCase("request_sum")) {
                    podmetJson.put("summary_results", requestSumResult);
                } else if (podmetJson.getString("name").equalsIgnoreCase("request_count")) {
                    podmetJson.put("summary_results", requestCountResult);
                } else if (podmetJson.getString("name").equalsIgnoreCase("JvmMemoryUsed")) {
                    podmetJson.put("summary_results", jvmMemUsedResult);
                }
            }
//            JSONObject retJsonObj = new JSONObject("{\"experiment_name\":\""+ trialData.getConfig().getEmConfigObject().getMetadata().getApplicationName() +"\"," +
//                    "\"deployments\":[{\"deployment_name\":\""+ trialData.getConfig().getEmConfigObject().getDeployments().getTrainingDeployment().getDeploymentName() +"\"," +
//                    "\"namespace\":\"default\"," +
//                    "\"containers\":[{\"image_name\":\"dinogun/galaxies:1.2-jdk-11.0.10_9\"," +
//                    "\"container_name\":\"galaxies\"," +
//                    "\"container_metrics\":[{\"name\":\"cpuRequest\"," +
//                    "\"summary_results\":{\"general_info\":{\"mean\":3.98389," +
//                    "\"min\":3.6809621160604," +
//                    "\"max\":4.10951920556296}}}," +
//                    "{\"name\":\"JvmMemoryUsed\"," +
//                    "\"summary_results\":{\"general_info\":{\"max\":1123," +
//                    "\"min\":769," +
//                    "\"mean\":832.63}}}," +
//                    "{\"name\":\"memoryRequest\"," +
//                    "\"summary_results\":{\"general_info\":{\"max\":1212," +
//                    "\"min\":834," +
//                    "\"mean\":976.794}}}]}]," +
//                    "\"pod_metrics\":[{\"name\":\"request_sum\"," +
//                    "\"summary_results\":{\"general_info\":{\"min\":2.15," +
//                    "\"mean\":31.91," +
//                    "\"max\":2107.212121}," +
//                    "\"percentile_info\":{\"50p\":0.63," +
//                    "\"95p\":8.94," +
//                    "\"97p\":64.75," +
//                    "\"99p\":82.59," +
//                    "\"99.9p\":93.48," +
//                    "\"99.99p\":111.5," +
//                    "\"99.999p\":198.52," +
//                    "\"100p\":30000}}}," +
//                    "{\"name\":\"request_count\"," +
//                    "\"summary_results\":{\"general_info\":{\"max\":21466," +
//                    "\"min\":2.11," +
//                    "\"mean\":21045}}}]}]," +
//                    "\"experiment_id\":\"" + trialData.getConfig().getEmConfigObject().getMetadata().getExpId() + "\"," +
//                    "\"info\":{\"trial_info\":{\"trial_id\":\"\"," +
//                    "\"trial_num\":0}}}");
            System.out.println("Input JSON with Metrics :");
            System.out.println(retJson.toString(2));
            return retJson;
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
