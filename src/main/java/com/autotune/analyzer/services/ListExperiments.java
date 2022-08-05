package com.autotune.analyzer.services;

import com.autotune.analyzer.AutotuneExperiment;
import com.autotune.analyzer.RunExperiment;
import com.autotune.common.experiments.ExperimentSummary;
import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.TrialDetails;
import com.autotune.utils.TrialHelpers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.stream.Collectors;

import static com.autotune.analyzer.Experimentator.experimentsMap;
import static com.autotune.analyzer.loop.HPOInterface.postTrialResultToHPO;
import static com.autotune.utils.AnalyzerConstants.ServiceConstants.*;
import static com.autotune.utils.AutotuneConstants.HpoOperations.EXP_TRIAL_GENERATE_SUBSEQUENT;
import static com.autotune.utils.ExperimentMessages.RunExperiment.*;
import static com.autotune.utils.ServerContext.OPTUNA_TRIALS_END_POINT;

public class ListExperiments extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListExperiments.class);
    private static final int TRIAL_RUN_LIMIT = 10;


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);

        JSONArray experimentTrialJSONArray = new JSONArray();
        for (String deploymentName : experimentsMap.keySet()) {
            AutotuneExperiment autotuneExperiment = experimentsMap.get(deploymentName);
            for (int trialNum : autotuneExperiment.getExperimentTrials().keySet()) {
                ExperimentTrial experimentTrial = autotuneExperiment.getExperimentTrials().get(trialNum);
                JSONArray experimentTrialJSON = new JSONArray(TrialHelpers.experimentTrialToJSON(experimentTrial));
                experimentTrialJSONArray.put(experimentTrialJSON.get(0));
            }
        }
        response.getWriter().println(experimentTrialJSONArray.toString(4));
        response.getWriter().close();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		/*
			Dummy values are set here once metric collection implemented will change logic accordingly
		 */
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);

        String trialResultsData = request.getReader().lines().collect(Collectors.joining());
        JSONObject trialResultsJson = new JSONObject(trialResultsData);

        String deployment_name = trialResultsJson.getString("deployment_name");
        JSONObject trialInfoJson = trialResultsJson.getJSONObject("info").getJSONObject("trial_info");
        int trialNumber = trialInfoJson.getInt("trial_num");
        AutotuneExperiment autotuneExperiment = experimentsMap.get(deployment_name);
        ExperimentTrial experimentTrial = autotuneExperiment.getExperimentTrials().get(trialNumber);
        LOGGER.debug("POST Trials to HPO.");
        // POST the result back to HPO
        URL experimentTrialsURL = null;
        try {
            experimentTrialsURL = new URL(OPTUNA_TRIALS_END_POINT);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        postTrialResultToHPO(experimentTrial, experimentTrialsURL);
        try {
            Thread.sleep(1000 * 10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ExperimentSummary es = autotuneExperiment.getExperimentSummary();
        es.setTrialsCompleted(es.getTrialsCompleted() + 1);
        es.setTrialsPassed(es.getTrialsPassed() + 1);
        TrialDetails trialDetails = autotuneExperiment.getExperimentTrials().get(trialNumber).getTrialDetails().get(TRAINING);
        trialDetails.setResult("SUCCESS");
        trialDetails.setResultError("None");
        trialDetails.setEndTime(Timestamp.from(Instant.now()));
        autotuneExperiment.setHPOoperation(EXP_TRIAL_GENERATE_SUBSEQUENT);
        if(es.getTrialsCompleted() < TRIAL_RUN_LIMIT) {
            RunExperiment runExperiment = autotuneExperiment.getExperimentThread();
            runExperiment.run();
        }else{
            autotuneExperiment.setExperimentStatus(STATUS_TRIAL_NUMBER + trialNumber + STATUS_SENT_RESULT_TO_HPO );
            autotuneExperiment.summarizeTrial(trialDetails);//es.setBestTrial(1);
        }
    }

}
