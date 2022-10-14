package com.autotune.analyzer.services;

import com.autotune.analyzer.AutotuneExperiment;
import com.autotune.analyzer.RunExperiment;
import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.experimentManager.exceptions.IncompatibleInputJSONException;
import com.autotune.utils.AnalyzerConstants;
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
import java.util.stream.Collectors;

import static com.autotune.analyzer.Experimentator.experimentsMap;
import static com.autotune.utils.AnalyzerConstants.ServiceConstants.*;
import static com.autotune.utils.TrialHelpers.updateExperimentTrial;

public class ListExperiments extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListExperiments.class);


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
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);

        LOGGER.info("Processing trial result...");
        try {
            String experimentName = request.getParameter(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME);
            // String deploymentName = request.getParameter(AnalyzerConstants.ServiceConstants.DEPLOYMENT_NAME);

            String trialResultsData = request.getReader().lines().collect(Collectors.joining());
            JSONObject trialResultsJson = new JSONObject(trialResultsData);

            // Read in the experiment name and the deployment name in the received JSON from EM
            String experimentNameJson = trialResultsJson.getString(EXPERIMENT_NAME);
            String trialNumber = trialResultsJson.getString("trialNumber");

            JSONArray deploymentsJsonArray = trialResultsJson.getJSONArray("deployments");
            for (Object deploymentObject : deploymentsJsonArray) {
                JSONObject deploymentJsonObject = (JSONObject) deploymentObject;
                String deploymentNameJson = deploymentJsonObject.getString(DEPLOYMENT_NAME);
                AutotuneExperiment autotuneExperiment = experimentsMap.get(deploymentNameJson);

                // Check if the passed in JSON has the same info as in the URL
                if (!experimentName.equals(experimentNameJson) || autotuneExperiment == null) {
                    LOGGER.error("Bad results JSON passed: {}", experimentNameJson);
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    break;
                }

                try {
                    LOGGER.info("going into of updateExperiment");
                    updateExperimentTrial(trialNumber, autotuneExperiment, trialResultsJson);
                    LOGGER.info("Came out of updateExperiment");
                } catch (InvalidValueException | IncompatibleInputJSONException e) {
                    e.printStackTrace();
                }
                RunExperiment runExperiment = autotuneExperiment.getExperimentThread();
                // Received a metrics JSON from EM after a trial, let the waiting thread know
                LOGGER.info("Received trial result for experiment: " + experimentNameJson + "; Deployment name: " + deploymentNameJson);
                runExperiment.send();
            }
            response.getWriter().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}