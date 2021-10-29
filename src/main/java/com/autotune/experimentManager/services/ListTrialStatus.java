package com.autotune.experimentManager.services;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.data.input.deployments.EMConfigDeploymentMetrics;
import com.autotune.experimentManager.data.input.metrics.EMMetricsPercentileInfo;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.data.input.metrics.EMCycleMetrics;
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
        if (null == runIdParam) {
            EMMapper.getInstance().getMap().forEach((key, value) -> {
                runIdList.add((String) key);
            });
        } else {
            runIdList.add(runIdParam);
        }

        JSONArray API_RESPONSE_ARRAY = new JSONArray();

        for (String runId : runIdList) {
            validRunId = true;
            JSONObject API_RESPONSE = new JSONObject();

            String completeStatus = req.getParameter(EMConstants.InputJsonKeys.ListTrialStatusKeys.COMPLETE_STATUS);
            String summary = req.getParameter(EMConstants.InputJsonKeys.ListTrialStatusKeys.SUMMARY);

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

            if(validRunId) {
                if (null != completeStatus && completeStatus.equalsIgnoreCase("true")) {
                    ExperimentTrialData etd = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
                    for (EMConfigDeploymentMetrics mt : etd.getConfig().getEmConfigObject().getDeployments().getTrainingDeployment().getMetrics()) {
                        if (mt.getName().equalsIgnoreCase(EMConstants.StandardDefaults.CPU_QUERY_NAME)) {
                            EMCycleMetrics emMetrics1 = new EMCycleMetrics();
                            emMetrics1.setMax("2.89521234892416");
                            emMetrics1.setMin("1.98086843874675");
                            emMetrics1.setMean("2.48143");
                            emMetrics1.setError("0");
                            EMCycleMetrics emMetrics2 = new EMCycleMetrics();
                            emMetrics2.setMax("3.83409655915519");
                            emMetrics2.setMin("2.89521234892416");
                            emMetrics2.setMean("3.34481");
                            emMetrics2.setError("0");
                            EMCycleMetrics emMetrics3 = new EMCycleMetrics();
                            emMetrics3.setMax("3.9406789954963");
                            emMetrics3.setMin("3.83409655915519");
                            emMetrics3.setMean("3.89973");
                            emMetrics3.setError("0");
                            EMCycleMetrics emMetrics4 = new EMCycleMetrics();
                            emMetrics4.setMax("0.301721905574074");
                            emMetrics4.setMin("0.0024651828");
                            emMetrics4.setMean("0.0571715");
                            emMetrics4.setError("0");
                            EMCycleMetrics emMetrics5 = new EMCycleMetrics();
                            emMetrics5.setMax("1.24245153869416");
                            emMetrics5.setMin("0.301721905574074");
                            emMetrics5.setMean("0.725029");
                            emMetrics5.setError("0");
                            EMCycleMetrics emMetrics6 = new EMCycleMetrics();
                            emMetrics6.setMax("1.98086843874675");
                            emMetrics6.setMin("1.24245153869416");
                            emMetrics6.setMean("1.58245");
                            emMetrics6.setError("0");
                            mt.getResults().getWarmupResults().getCollectiveCyclesMetrics().getCycleMetricsList().add(emMetrics1);
                            mt.getResults().getWarmupResults().getCollectiveCyclesMetrics().getCycleMetricsList().add(emMetrics2);
                            mt.getResults().getWarmupResults().getCollectiveCyclesMetrics().getCycleMetricsList().add(emMetrics3);
                            mt.getResults().getMeasurementResults().getCollectiveCyclesMetrics().getCycleMetricsList().add(emMetrics4);
                            mt.getResults().getMeasurementResults().getCollectiveCyclesMetrics().getCycleMetricsList().add(emMetrics5);
                            mt.getResults().getMeasurementResults().getCollectiveCyclesMetrics().getCycleMetricsList().add(emMetrics6);
                        } else if (mt.getName().equalsIgnoreCase(EMConstants.StandardDefaults.MEM_QUERY_NAME)) {
                            EMCycleMetrics emMetrics1 = new EMCycleMetrics();
                            emMetrics1.setMax("555");
                            emMetrics1.setMin("553");
                            emMetrics1.setMean("554.204");
                            emMetrics1.setError("0");
                            EMCycleMetrics emMetrics2 = new EMCycleMetrics();
                            emMetrics2.setMax("557");
                            emMetrics2.setMin("555");
                            emMetrics2.setMean("556.547");
                            emMetrics2.setError("0");
                            EMCycleMetrics emMetrics3 = new EMCycleMetrics();
                            emMetrics3.setMax("558");
                            emMetrics3.setMin("557");
                            emMetrics3.setMean("557.824");
                            emMetrics3.setError("0");
                            EMCycleMetrics emMetrics4 = new EMCycleMetrics();
                            emMetrics4.setMax("1152");
                            emMetrics4.setMin("368");
                            emMetrics4.setMean("805.328");
                            emMetrics4.setError("0");
                            EMCycleMetrics emMetrics5 = new EMCycleMetrics();
                            emMetrics5.setMax("1161");
                            emMetrics5.setMin("1089");
                            emMetrics5.setMean("1157.18");
                            emMetrics5.setError("0");
                            EMCycleMetrics emMetrics6 = new EMCycleMetrics();
                            emMetrics6.setMax("1166");
                            emMetrics6.setMin("1160");
                            emMetrics6.setMean("1165.12");
                            emMetrics6.setError("0");
                            mt.getResults().getWarmupResults().getCollectiveCyclesMetrics().getCycleMetricsList().add(emMetrics1);
                            mt.getResults().getWarmupResults().getCollectiveCyclesMetrics().getCycleMetricsList().add(emMetrics2);
                            mt.getResults().getWarmupResults().getCollectiveCyclesMetrics().getCycleMetricsList().add(emMetrics3);
                            mt.getResults().getMeasurementResults().getCollectiveCyclesMetrics().getCycleMetricsList().add(emMetrics4);
                            mt.getResults().getMeasurementResults().getCollectiveCyclesMetrics().getCycleMetricsList().add(emMetrics5);
                            mt.getResults().getMeasurementResults().getCollectiveCyclesMetrics().getCycleMetricsList().add(emMetrics6);
                        } else if (mt.getName().equalsIgnoreCase(EMConstants.StandardDefaults.THROUGHPUT)) {
                            EMCycleMetrics emCycleMetrics = new EMCycleMetrics();
                            emCycleMetrics.setMean("26982.7008082658");
                            mt.getResults().getMeasurementResults().getCollectiveCyclesMetrics().getCycleMetricsList().add(emCycleMetrics);
                        } else if (mt.getName().equalsIgnoreCase(EMConstants.StandardDefaults.RESPONSE_TIME)) {
                            EMCycleMetrics emCycleMetrics = new EMCycleMetrics();
                            emCycleMetrics.setMean("2.83657160134552");
                            mt.getResults().getMeasurementResults().getCollectiveCyclesMetrics().getCycleMetricsList().add(emCycleMetrics);
                        }
                    }
                    JSONObject trialConfigJson = null;
                    try {
                        trialConfigJson = ((ExperimentTrialData) EMMapper.getInstance().getMap().get(runId)).getConfig().getEmConfigObject().toJSON();
                    } catch (EMDataObjectIsInEditingException e) {
                        e.printStackTrace();
                    } catch (EMDataObjectIsNotFilledException e) {
                        e.printStackTrace();
                    }
                    API_RESPONSE.put(EMConstants.EMJSONKeys.EXPERIMENT_ID, trialConfigJson.getString(EMConstants.EMJSONKeys.EXPERIMENT_ID));
                    API_RESPONSE.put(EMConstants.EMJSONKeys.APPLICATION_NAME, trialConfigJson.getString(EMConstants.EMJSONKeys.APPLICATION_NAME));
                    API_RESPONSE.put(EMConstants.EMJSONKeys.INFO, trialConfigJson.getJSONObject(EMConstants.EMJSONKeys.INFO));
                    API_RESPONSE.put(EMConstants.EMJSONKeys.SETTINGS,trialConfigJson.getJSONObject(EMConstants.EMJSONKeys.SETTINGS));
                    API_RESPONSE.put(EMConstants.EMJSONKeys.DEPLOYMENTS, trialConfigJson.getJSONObject(EMConstants.EMJSONKeys.DEPLOYMENTS));
                }

                if (null != summary && summary.equalsIgnoreCase("true")) {
                    JSONObject inputJson = null;
                    try {
                        inputJson = ((ExperimentTrialData) EMMapper.getInstance().getMap().get(runId)).getConfig().getInputJSON();
                    } catch (EMDataObjectIsInEditingException e) {
                        e.printStackTrace();
                    } catch (EMDataObjectIsNotFilledException e) {
                        e.printStackTrace();
                    }

                    for (Object obj : inputJson.getJSONArray(EMConstants.EMJSONKeys.DEPLOYMENTS)) {
                        JSONObject json = (JSONObject) obj;
                        if (json.getString(EMConstants.EMJSONKeys.TYPE).equalsIgnoreCase(EMConstants.EMConfigDeployments.DeploymentTypes.TRAINING)) {
                            for (Object metricObj : json.getJSONArray(EMConstants.EMJSONKeys.METRICS)) {
                                JSONObject metricJsonObj = (JSONObject) metricObj;
                                EMCycleMetrics emMetrics = new EMCycleMetrics();
                                EMMetricsPercentileInfo percentileInfo = new EMMetricsPercentileInfo();
                                if (metricJsonObj.getString(EMConstants.EMJSONKeys.NAME).equalsIgnoreCase(EMConstants.StandardDefaults.CPU_QUERY_NAME)){
                                    emMetrics.setMax("4.03508112365185");
                                    emMetrics.setMin("1.71682251536324");
                                    emMetrics.setMean("3.15783");
                                } else if (metricJsonObj.getString(EMConstants.EMJSONKeys.NAME).equalsIgnoreCase(EMConstants.StandardDefaults.MEM_QUERY_NAME)) {
                                    emMetrics.setMax("1948");
                                    emMetrics.setMin("1195");
                                    emMetrics.setMean("1794.09");
                                } else if (metricJsonObj.getString(EMConstants.EMJSONKeys.NAME).equalsIgnoreCase(EMConstants.StandardDefaults.THROUGHPUT)) {
                                    emMetrics.setMean("21106.1");
                                } else if (metricJsonObj.getString(EMConstants.EMJSONKeys.NAME).equalsIgnoreCase(EMConstants.StandardDefaults.RESPONSE_TIME)) {
                                    emMetrics.setMax("2693.354289");
                                    emMetrics.setPercentileInfoAvailable(true);
                                    percentileInfo.setP50("2.3");
                                    percentileInfo.setP95("3.2");
                                    percentileInfo.setP97("3.9");
                                    percentileInfo.setP99("3.9");
                                    percentileInfo.setP99point99("3.9");
                                    percentileInfo.setP99point999("200");
                                    percentileInfo.setP100("2693");
                                    emMetrics.setPercentileInfo(percentileInfo);
                                }
                                metricJsonObj.put(EMConstants.EMJSONKeys.RESULTS, emMetrics.toJSON());
                            }
                        }
                    }
                    API_RESPONSE.put(EMConstants.EMJSONKeys.EXPERIMENT_ID, inputJson.getString(EMConstants.EMJSONKeys.EXPERIMENT_ID));
                    API_RESPONSE.put(EMConstants.EMJSONKeys.APPLICATION_NAME, inputJson.getString(EMConstants.EMJSONKeys.APPLICATION_NAME));
                    API_RESPONSE.put(EMConstants.EMJSONKeys.INFO, inputJson.getJSONObject(EMConstants.EMJSONKeys.INFO));
                    API_RESPONSE.put(EMConstants.EMJSONKeys.SETTINGS, inputJson.getJSONObject(EMConstants.EMJSONKeys.SETTINGS));
                    API_RESPONSE.put(EMConstants.EMJSONKeys.DEPLOYMENTS, inputJson.getJSONArray(EMConstants.EMJSONKeys.DEPLOYMENTS));
                }
            }
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().println(API_RESPONSE_ARRAY.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }
}
