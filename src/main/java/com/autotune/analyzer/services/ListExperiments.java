package com.autotune.analyzer.services;

import com.autotune.analyzer.AutotuneExperiment;
import com.autotune.analyzer.Experimentator;
import com.autotune.analyzer.experiments.ExperimentTrial;
import com.autotune.utils.TrialHelpers;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

public class ListExperiments extends HttpServlet
{
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(JSON_CONTENT_TYPE);
			response.setCharacterEncoding(CHARACTER_ENCODING);

			JSONArray experimentTrialJSONArray = new JSONArray();
			for (String experimentId : Experimentator.experimentsMap.keySet()) {
				AutotuneExperiment autotuneExperiment = Experimentator.experimentsMap.get(experimentId);
				for (ExperimentTrial experimentTrial : autotuneExperiment.getExperimentTrials()) {
					JSONObject experimentTrialJSON = TrialHelpers.experimentTrialToJSON(experimentTrial);
					experimentTrialJSONArray.put(experimentTrialJSON);
				}
			}
			response.getWriter().println(experimentTrialJSONArray.toString(4));
			response.getWriter().close();
		} catch (Exception ex) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
