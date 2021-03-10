package com.autotune.recommendation_manager.service;

import org.json.JSONArray;

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
		JSONArray jsonArray = new JSONArray();
		SearchSpace searchSpace = new SearchSpace();
		searchSpace.getSearchSpace(jsonArray, id);

		output = pythonInterpreter.call(API(id, jsonArray));

		/*
		experiment object : trials and data
		
		 */
	}
}
