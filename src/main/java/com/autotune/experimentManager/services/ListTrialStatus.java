/*******************************************************************************
 * Copyright (c) 2021, 2021 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.autotune.experimentManager.services;

import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.ExperimentTrialData;
import com.autotune.experimentManager.transitions.util.TransistionHelper;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class is the handler for the endpoint `listTrialStatus`
 *
 * Returns the status for the requested trial id
 */
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
            JSONObject API_RESPONSE = null;

            if (null == runId) {
                API_RESPONSE = new JSONObject();
                API_RESPONSE.put(EMConstants.InputJsonKeys.ListTrialStatusKeys.ERROR, "Run ID cannot be null");
                validRunId = false;
            } else if (!EMMapper.getInstance().getMap().containsKey(runId)) {
                API_RESPONSE = new JSONObject();
                API_RESPONSE.put(EMConstants.InputJsonKeys.ListTrialStatusKeys.ERROR, "Invalid Run ID");
                validRunId = false;
            }

            String completeStatus = req.getParameter("completeStatus");
            String summary = req.getParameter(EMConstants.InputJsonKeys.ListTrialStatusKeys.SUMMARY);

            if(validRunId) {
                if (null != summary && summary.equalsIgnoreCase("true")) {
                    ExperimentTrialData etd = (ExperimentTrialData) EMMapper.getInstance().getMap().get(runId);
                    if (etd.getStatus() == EMUtil.EMExpStatus.COMPLETED) {
                        API_RESPONSE = TransistionHelper.MetricsFormatter.getMetricsJson(runId);
                        API_RESPONSE.put(EMConstants.InputJsonKeys.ListTrialStatusKeys.STATUS, ((ExperimentTrialData) EMMapper.getInstance().getMap().get(runId)).getStatus().toString());
                    }
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
