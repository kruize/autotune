package com.autotune.recommendation_manager.service;

import com.autotune.recommendation_manager.RecommendationManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class GetExperimentJson extends HttpServlet
{
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String id = req.getParameter("id");
		resp.getWriter().print(RecommendationManager.trialMap.get(id));
	}
}
