package com.autotune.experimentManager.services;

import com.autotune.experimentManager.core.ExperimentManager;
import com.autotune.experimentManager.data.*;
import com.autotune.experimentManager.exceptions.EMInvalidInstanceCreation;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.experimentManager.utils.EMUtil;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreateExperiment extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String inputData = req.getReader().lines().collect(Collectors.joining());
        JSONObject json = new JSONObject(inputData);
        try {
            EMTrialConfig config = new EMTrialConfig(json);
            ExperimentTrialData trailData = new ExperimentTrialData(config);
            String runId = UUID.randomUUID().toString();
            EMMapper.getInstance().getMap().put(runId, trailData);
            EMStageTransition transition = new EMStageTransition(runId, EMUtil.EMExpStages.CREATE_CONFIG);
            EMStageProcessQueue.getStageProcessQueueInstance().getQueue().add(transition);
            ExperimentManager.notifyQueueProcessor();
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println(runId);
        } catch (IncompatibleInputJSONException | EMInvalidInstanceCreation e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }
}
