package com.autotune.analyzer.services;

import com.autotune.database.dao.ExperimentDAOImpl;
import com.autotune.database.table.lm.KruizeLMRuleSetEntry;
import com.google.gson.Gson;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

public class ListRuleSets extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);

        try {
            // Load all rulesets from DB
            List<KruizeLMRuleSetEntry> ruleSets = new ExperimentDAOImpl().loadAllRuleSet();

            // Convert to JSON and send
            Gson gson = new Gson();
            String jsonResponse = gson.toJson(ruleSets);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonResponse);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            // error handling
        }
    }

}
