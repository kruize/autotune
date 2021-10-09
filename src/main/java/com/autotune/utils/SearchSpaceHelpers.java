package com.autotune.utils;

import com.autotune.analyzer.AutotuneExperiment;
import com.autotune.analyzer.application.ApplicationSearchSpace;
import com.autotune.analyzer.application.ApplicationServiceStack;
import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.deployment.AutotuneDeployment;
import com.autotune.analyzer.exceptions.InvalidBoundsException;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.HttpUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.autotune.analyzer.Experimentator.experimentsMap;

public class SearchSpaceHelpers {

    public static void getSearchSpaceJSONArray(JSONArray searchSpaceJsonArray, String id) {
        AutotuneExperiment autotuneExperiment = null;
        ApplicationSearchSpace applicationSearchSpace = null;
        if (null != id) {
            autotuneExperiment = experimentsMap.get(id);
            if (null != autotuneExperiment) {
                applicationSearchSpace = autotuneExperiment.getApplicationServiceStack().getApplicationSearchSpace();
                convertSearchSpaceToJson(searchSpaceJsonArray, applicationSearchSpace);
            }
        } else {
            for (String expId : experimentsMap.keySet()) {
                autotuneExperiment = experimentsMap.get(expId);
                applicationSearchSpace = autotuneExperiment.getApplicationServiceStack().getApplicationSearchSpace();
                convertSearchSpaceToJson(searchSpaceJsonArray, applicationSearchSpace);
            }
        }
    }

    public static void convertSearchSpaceToJson(JSONArray searchSpaceJsonArray, ApplicationSearchSpace applicationSearchSpace) {
        if (null != applicationSearchSpace) {

            JSONObject applicationJson = new JSONObject();
            applicationJson.put(AnalyzerConstants.ServiceConstants.APPLICATION_NAME, applicationSearchSpace.getApplicationName());
            applicationJson.put(AnalyzerConstants.AutotuneObjectConstants.ID, applicationSearchSpace.getExperimentId());
            applicationJson.put(AnalyzerConstants.AutotuneConfigConstants.VALUE_TYPE, applicationSearchSpace.getValueType());
            applicationJson.put(AnalyzerConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION, applicationSearchSpace.getObjectiveFunction());
            applicationJson.put(AnalyzerConstants.AutotuneObjectConstants.DIRECTION, applicationSearchSpace.getDirection());
            applicationJson.put(AnalyzerConstants.AutotuneObjectConstants.HPO_ALGO_IMPL, applicationSearchSpace.getHpoAlgoImpl());

            JSONArray tunablesJsonArray = new JSONArray();
            for (String applicationTunableName : applicationSearchSpace.getApplicationTunablesMap().keySet()) {
                Tunable applicationTunable = applicationSearchSpace.getApplicationTunablesMap().get(applicationTunableName);
                JSONObject tunableJson = new JSONObject();
                tunableJson.put("name", applicationTunable.getName());
                Double uBound = applicationTunable.getUpperBoundValue();
                tunableJson.put("upper_bound", uBound);
                Double lBound = applicationTunable.getLowerBoundValue();
                tunableJson.put("lower_bound", lBound);
                tunableJson.put("value_type", applicationTunable.getValueType());
                tunableJson.put("step", applicationTunable.getStep());

                tunablesJsonArray.put(tunableJson);
            }
            applicationJson.put("tunables", tunablesJsonArray);

            searchSpaceJsonArray.put(applicationJson);
        }
    }

    public static void validateAndParse(String iData) {
        JSONObject experiment = new JSONObject(iData);
        try {
            String url = experiment.getString("url");
            URL searchSpace = new URL(url);

            String searchSpaceJson = HttpUtils.getDataFromURL(searchSpace, "");
            String id = parseSearchSpace(searchSpaceJson);

        } catch (Exception e) {
            /* ERROR */
        }
    }

    /**
     *
     * @param searchSpaceJson JSON containing the Search Space
     * @return application id string
     */
    public static String parseSearchSpace(String searchSpaceJson) {
        String app_id = null;
        JSONArray sSpaceArray = new JSONArray(searchSpaceJson);
        for (Object sSpaceObject : sSpaceArray) {
            JSONObject sSpace = (JSONObject) sSpaceObject;
            System.out.println("App Name: " + sSpace.getString("application_name"));
            app_id = sSpace.getString("id");
            System.out.println("App ID: " + app_id);
            JSONArray tunables = new JSONArray(sSpace.getJSONArray("tunables").toString());
            for (Object tunablesObject : tunables) {
                JSONObject tunable = (JSONObject) tunablesObject;
                System.out.println("Tunable Name: " + tunable.getString("name"));
                System.out.println("Tunable Value Type: " + tunable.getString("value_type"));
                System.out.println("Tunable step: " + tunable.getDouble("step"));
            }
        }

        return app_id;
    }

    public static void convertJsonToApplicationSearchSpaceArray(ArrayList<ApplicationSearchSpace> applicationSearchSpaceArray,
                                                                String searchSpaceJson) {

        JSONArray searchSpaceArray = new JSONArray(searchSpaceJson);
        // Now create a new ApplicationSearchSpace object from the JSON passed in
        for (Object searchSpaceObject : searchSpaceArray) {

            JSONObject searchSpaceJsonObject = (JSONObject) searchSpaceObject;
            String experimentName = searchSpaceJsonObject.getString("experiment_name");
            String experimentId = searchSpaceJsonObject.getString("experiment_id");
            String podName = searchSpaceJsonObject.getString("pod_name");
            String objectiveFunction = searchSpaceJsonObject.getString("objective_function");
            String hpoAlgoImpl = searchSpaceJsonObject.getString("hpo_algo_impl");
            String direction = searchSpaceJsonObject.getString("direction");
            String valueType = searchSpaceJsonObject.getString("value_type");
            JSONArray tunablesJA = new JSONArray(searchSpaceJsonObject.getJSONArray("tunables").toString());
            ArrayList<Tunable> tunables = new ArrayList<>();

            for (Object tunablesJAObject : tunablesJA) {
                JSONObject tunableJO = (JSONObject) tunablesJAObject;
                String name = tunableJO.getString("name");
                String tValueType = tunableJO.getString("value_type");
                String lowerBound = tunableJO.optString("lower_bound");
                String upperBound = tunableJO.optString("upper_bound");
                double step = tunableJO.optDouble("step");

                ArrayList<String> slaClassList = new ArrayList<>();
                JSONArray slaClassJson = tunableJO.optJSONArray("slaClassList");
                if (slaClassJson != null) {
                    for (Object slaClassObject : slaClassJson) {
                        String slaClass = (String) slaClassObject;
                        slaClassList.add(slaClass);
                    }
                }

                JSONObject tunableQueriesJO = tunableJO.optJSONObject("queries");
                JSONArray dataSourceArrayJA = null;
                if (tunableQueriesJO != null) {
                    dataSourceArrayJA = tunableQueriesJO.optJSONArray("datasource");
                }

                // Store the datasource and query from the JSON in a map
                Map<String, String> queriesMap = new HashMap<>();
                if (dataSourceArrayJA != null) {
                    for (Object dataSourceObject : dataSourceArrayJA) {
                        JSONObject dataSourceJson = (JSONObject) dataSourceObject;
                        String datasource = dataSourceJson.optString("name");
                        String datasourceQuery = dataSourceJson.optString("query");

                        queriesMap.put(datasource, datasourceQuery);
                    }
                }

                Tunable applicationTunable = null;
                try {
                    applicationTunable = new Tunable(name,
                            step,
                            upperBound,
                            lowerBound,
                            tValueType,
                            queriesMap,
                            slaClassList);
                    tunables.add(applicationTunable);
                } catch (InvalidBoundsException e) {
                    e.printStackTrace();
                }
            }
            ApplicationSearchSpace appSearchSpace = new ApplicationSearchSpace(experimentName,
                    experimentId,
                    podName,
                    objectiveFunction,
                    hpoAlgoImpl,
                    direction,
                    valueType);
            applicationSearchSpaceArray.add(appSearchSpace);
        }
    }
}