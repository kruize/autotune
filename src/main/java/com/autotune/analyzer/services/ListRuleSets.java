package com.autotune.analyzer.services;

import com.autotune.database.dao.ExperimentDAOImpl;
import com.autotune.database.table.lm.KruizeLMRuleSetEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
            // check name is present
            String ruleSetName = request.getParameter("name");

            // Load all rulesets from DB
            List<KruizeLMRuleSetEntry> ruleSets;

            if (ruleSetName == null || ruleSetName.isEmpty()) {
                // load all
                ruleSets = new ExperimentDAOImpl().loadAllRuleSet();
            } else {
                // load by name
                ruleSets = new ExperimentDAOImpl().loadRuleSetByName(ruleSetName);
            }

            // Convert to clean JSON using ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode arrayNode = objectMapper.createArrayNode();

            for (KruizeLMRuleSetEntry entry : ruleSets) {
                ObjectNode ruleSetNode = objectMapper.createObjectNode();
                ruleSetNode.put("apiVersion", entry.getApi_version());
                ruleSetNode.put("kind", entry.getKind());
                ruleSetNode.set("metadata", entry.getMetadata());

                // Create rulesets wrapper object
                ObjectNode rulesetsWrapper = objectMapper.createObjectNode();
                rulesetsWrapper.set("stack", entry.getStack());
                rulesetsWrapper.set("rules", entry.getRules());
                rulesetsWrapper.set("dependencies", entry.getDependencies());

                ruleSetNode.set("rulesets", rulesetsWrapper);

                arrayNode.add(ruleSetNode);
            }

            String jsonResponse = objectMapper.writeValueAsString(arrayNode);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonResponse);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            // error handling
        }
    }

}
