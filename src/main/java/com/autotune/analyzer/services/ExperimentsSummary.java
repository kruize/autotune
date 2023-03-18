package com.autotune.analyzer.services;

import com.autotune.analyzer.KruizeExperiment;
import com.autotune.analyzer.Experimentator;
import com.autotune.analyzer.experiment.ExperimentHelpers;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.autotune.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

public class ExperimentsSummary extends HttpServlet
{
	@Override
	protected void doGet(HttpServletRequest request,
						 HttpServletResponse response) throws ServletException, IOException {

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(JSON_CONTENT_TYPE);
		response.setCharacterEncoding(CHARACTER_ENCODING);

		JSONArray experimentsJSONArray = new JSONArray();
		for (String experimentID : Experimentator.experimentsMap.keySet()) {
			KruizeExperiment kruizeExperiment = Experimentator.experimentsMap.get(experimentID);
			JSONObject experimentJSON = ExperimentHelpers.experimentToJSON(kruizeExperiment);
			experimentsJSONArray.put(experimentJSON);
		}
		response.getWriter().println(experimentsJSONArray.toString(4));
		response.getWriter().close();

	}
}
