package com.autotune.experimentManager.services;

import com.autotune.experimentManager.core.ExperimentManager;
import com.autotune.experimentManager.data.*;
import com.autotune.experimentManager.services.util.EMAPIHandler;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

public class CreateExperiment extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateExperiment.class);
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String inputData = req.getReader().lines().collect(Collectors.joining());
        JSONObject json = new JSONObject(inputData);

        ExperimentTrialData trialData = EMAPIHandler.createETD(json);
        String runId = EMAPIHandler.registerTrial(trialData);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().println(runId);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }
}
