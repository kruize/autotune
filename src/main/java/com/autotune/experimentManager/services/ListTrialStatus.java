package com.autotune.experimentManager.services;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

public class ListTrialStatus extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
//        String inputData = req.getReader().lines().collect(Collectors.joining());
//        JSONObject json = new JSONObject(inputData);
//        String runId = json.getString(EMConstants.InputJsonKeys.GetTrailStatusInputKeys.RUN_ID);
        ArrayList<String> runIdList = new ArrayList<String>();
        boolean validRunId = true;
        String runIdParam = req.getParameter(EMConstants.InputJsonKeys.ListTrialStatusKeys.RUN_ID);
        if (null != runIdParam) {
            runIdList.add(runIdParam);
        } else {
            EMMapper.getInstance().getMap().forEach((key, value) -> {
                runIdList.add((String) key);
            });
        }

        JSONArray API_RESPONSE_ARRAY = new JSONArray();

        for (String runId : runIdList) {
            validRunId = true;
            JSONObject API_RESPONSE = new JSONObject();
            if (null == runId) {
                API_RESPONSE.put(EMConstants.InputJsonKeys.ListTrialStatusKeys.ERROR, "Run ID cannot be null");
                validRunId = false;
            } else {
                if (EMMapper.getInstance().getMap().containsKey(runId)) {
                    API_RESPONSE.put(EMConstants.InputJsonKeys.ListTrialStatusKeys.STATUS, ((ExperimentTrialData) EMMapper.getInstance().getMap().get(runId)).getStatus().toString());
                } else {
                    API_RESPONSE.put(EMConstants.InputJsonKeys.ListTrialStatusKeys.ERROR, "Invalid Run ID");
                    validRunId = false;
                }
            }

            String completeStatus = req.getParameter("completeStatus");
            String summary = req.getParameter(EMConstants.InputJsonKeys.ListTrialStatusKeys.SUMMARY);

            if(validRunId) {
                if (null != summary && summary.equalsIgnoreCase("true")) {
                    ExperimentTrialData etd = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
                    JSONObject inputJSONConfig = etd.getConfig().getInputJSON();
                    JSONArray deploymentsArr = inputJSONConfig.getJSONArray("deployments");
                    for (Object deployRawObj : deploymentsArr) {
                        JSONObject deployObj = (JSONObject) deployRawObj;
                        if (deployObj.getString("type").equalsIgnoreCase("training")) {
                            JSONArray metricsArr = deployObj.getJSONArray("metrics");
                            for (Object metricsRawObj : metricsArr) {
                                JSONObject metricObj = (JSONObject) metricsRawObj;
                                JSONObject results = new JSONObject();
                                if (metricObj.getString("name").equalsIgnoreCase("cpuRequest")) {
                                    results.put("max", "4.03508112365185");
                                    results.put("min", "1.71682251536324");
                                    results.put("mean", "3.15783");
                                    results.put("error", "0");
                                } else if (metricObj.getString("name").equalsIgnoreCase("memoryRequest")) {
                                    results.put("max", "1948");
                                    results.put("min", "1195");
                                    results.put("mean", "1794.09");
                                    results.put("error","0");
                                } else if (metricObj.getString("name").equalsIgnoreCase("request_count")) {
                                    results.put("mean", "21106.01");
                                    results.put("error","0");
                                } else if (metricObj.getString("name").equalsIgnoreCase("response_time")) {
                                    results.put("max","2693.354289");
                                    results.put("error", "0");
                                    JSONObject percentileInfo = new JSONObject();
                                    percentileInfo.put("P50", "2.3");
                                    percentileInfo.put("P95", "3.2");
                                    percentileInfo.put("P97", "3.9");
                                    percentileInfo.put("P99", "3.9");
                                    percentileInfo.put("P99.99", "3.9");
                                    percentileInfo.put("P99.999", "200");
                                    percentileInfo.put("P100", "2693");
                                    results.put("percentile_info", percentileInfo);
                                }
                                metricObj.put("results", results);
                            }
                        }
                    }
                    API_RESPONSE.put(EMConstants.EMJSONKeys.EXPERIMENT_ID, inputJSONConfig.getString(EMConstants.EMJSONKeys.EXPERIMENT_ID));
                    API_RESPONSE.put(EMConstants.EMJSONKeys.EXPERIMENT_NAME, inputJSONConfig.getString(EMConstants.EMJSONKeys.EXPERIMENT_NAME));
                    API_RESPONSE.put(EMConstants.EMJSONKeys.INFO, inputJSONConfig.getJSONObject(EMConstants.EMJSONKeys.INFO));
                    API_RESPONSE.put(EMConstants.EMJSONKeys.SETTINGS, inputJSONConfig.getJSONObject(EMConstants.EMJSONKeys.SETTINGS));
                    API_RESPONSE.put(EMConstants.EMJSONKeys.DEPLOYMENTS, inputJSONConfig.getJSONArray(EMConstants.EMJSONKeys.DEPLOYMENTS));
                }
            }
            API_RESPONSE_ARRAY.put(API_RESPONSE);
        }



        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().println(API_RESPONSE_ARRAY.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }
}
