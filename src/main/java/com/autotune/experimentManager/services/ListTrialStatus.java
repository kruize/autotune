package com.autotune.experimentManager.services;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.exceptions.EMDataObjectIsInEditingException;
import com.autotune.experimentManager.exceptions.EMDataObjectIsNotFilledException;
import com.autotune.experimentManager.utils.EMConstants;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ListTrialStatus extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
//        String inputData = req.getReader().lines().collect(Collectors.joining());
//        JSONObject json = new JSONObject(inputData);
//        String runId = json.getString(EMConstants.InputJsonKeys.GetTrailStatusInputKeys.RUN_ID);
        JSONObject API_RESPONSE = new JSONObject();
        String runId = req.getParameter(EMConstants.InputJsonKeys.ListTrialStatusKeys.RUN_ID);
        String completeStatus = req.getParameter(EMConstants.InputJsonKeys.ListTrialStatusKeys.COMPLETE_STATUS);

        if (null == runId) {
            API_RESPONSE.put(EMConstants.InputJsonKeys.ListTrialStatusKeys.ERROR, "Run ID cannot be null");
        } else {
            if (EMMapper.getInstance().getMap().containsKey(runId)) {
                API_RESPONSE.put(EMConstants.InputJsonKeys.ListTrialStatusKeys.STATUS, ((ExperimentTrialData) EMMapper.getInstance().getMap().get(runId)).getStatus().toString());
            } else {
                API_RESPONSE.put(EMConstants.InputJsonKeys.ListTrialStatusKeys.ERROR, "Invalid Run ID");
            }
        }
        if (completeStatus.equalsIgnoreCase("true")) {
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

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().println(API_RESPONSE.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }


}
