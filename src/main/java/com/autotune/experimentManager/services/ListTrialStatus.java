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
import com.autotune.experimentManager.services.util.EMAPIHandler;
import com.autotune.experimentManager.transitions.util.TransistionHelper;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This class is the handler for the endpoint `listTrialStatus`
 *
 * Returns the status for the requested trial id
 */
public class ListTrialStatus extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String experiment_name = null;
        String trial_num = null;
        // Setting off verbose to false by default
        boolean verbose = false;

        // Extract Experiment Name if exist
        experiment_name = req.getParameter(EMConstants.InputJsonKeys.ListTrialStatusKeys.EXPERIMENT_NAME);
        // Extract Trial Number if exist
        trial_num = req.getParameter(EMConstants.InputJsonKeys.ListTrialStatusKeys.TRIAL_NUM);
        // Extract Verbose option if exist
        String checkVerbose = req.getParameter(EMConstants.InputJsonKeys.ListTrialStatusKeys.VERBOSE);
        if (null != checkVerbose && checkVerbose.equalsIgnoreCase("true")) {
            verbose = true;
        }

        // get the status JSON based on the type of requirement sent by the user
        JSONObject API_RESPONSE = EMAPIHandler.getStatusJson(experiment_name, trial_num, verbose);


       /* if (null != API_RESPONSE.getString("Error")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            resp.setStatus(HttpServletResponse.SC_OK);
        }*/
        resp.getWriter().println(API_RESPONSE.toString(4));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }
}
