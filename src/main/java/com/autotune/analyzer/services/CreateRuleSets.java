package com.autotune.analyzer.services;

import com.autotune.analyzer.exceptions.KruizeResponse;
import com.autotune.analyzer.serviceObjects.BulkJobStatus;
import com.autotune.analyzer.serviceObjects.CreateRuleSetsAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.database.dao.ExperimentDAOImpl;
import com.autotune.database.table.lm.KruizeLMRuleSetEntry;
import com.autotune.utils.KruizeConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Collectors;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

public class CreateRuleSets extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateExperiment.class);

    /**
     * It reads the input data from the request, converts it into a RuleSet object using GSON.
     * It validates, converts to DB entry, and saves to database.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOGGER.info("Received request to create rule set...");

        String inputData = "";
        CreateRuleSetsAPIObject ruleSetObject = null;

        try {
            // read the json payload
            request.setCharacterEncoding(CHARACTER_ENCODING);
            inputData = request.getReader().lines().collect(Collectors.joining());
            LOGGER.debug("Request body: {}", inputData);

            // convert the payload to our java object (CreateRuleSetApiObject)
            Gson gson = new Gson();
            ruleSetObject = gson.fromJson(inputData, CreateRuleSetsAPIObject.class);

            // add validation function
            boolean isValid = validateRuleSet(ruleSetObject);
            if (!isValid) {
                throw new Exception("Invalid rule set object");
            }

            // convert api object to database entry
            KruizeLMRuleSetEntry dbEntry = convertToDBEntry(ruleSetObject);

            // save ruleset to database
            ValidationOutputData validationOutputData = new ExperimentDAOImpl().addRuleSetToDB(dbEntry);
            if (!validationOutputData.isSuccess()) {
                throw new Exception("Failed to save ruleset: " + validationOutputData.getMessage());
            }

            // send success response
            sendSuccessResponse(response, "RuleSet created successfully");

        } catch (Exception e) {
            // send error response
            e.printStackTrace();
            LOGGER.error("Unknown exception caught: " + e.getMessage());
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: " + e.getMessage());
        }

    }

    /**
     * Dummy Validation Function
     *
     * @return
     *
     */
    private boolean validateRuleSet(CreateRuleSetsAPIObject ruleSetObject) {
        LOGGER.debug("Validating rule set object..");
        return true;
    }


    /**
     * Send success response in case of no errors or exceptions.
     *
     * @param response
     * @param message
     * @throws IOException
     */
    private void sendSuccessResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = response.getWriter();
        out.append(
                new Gson().toJson(
                        new KruizeResponse(message + " View Rule Set at /listRuleSets",
                                HttpServletResponse.SC_CREATED, "", "SUCCESS")
                )
        );
        out.flush();
    }

    /**
     * Send response containing corresponding error message in case of failures and exceptions
     *
     * @param response
     * @param e
     * @param httpStatusCode
     * @param errorMsg
     * @throws IOException
     */
    public void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws
            IOException {
        if (null != e) {
            LOGGER.error(e.toString());
            e.printStackTrace();
            if (null == errorMsg)
                errorMsg = e.getMessage();
        }
        response.sendError(httpStatusCode, errorMsg);
    }

    /**
     * Convert API object to database entry
     *
     * @param apiObject The API object from the request
     * @return Database entry ready to persist
     * @throws Exception if conversion fails
     */
    private KruizeLMRuleSetEntry convertToDBEntry(CreateRuleSetsAPIObject apiObject) throws Exception {
        KruizeLMRuleSetEntry entry = new KruizeLMRuleSetEntry();

        // Set simple fields
        entry.setApi_version(apiObject.getApiVersion());
        entry.setKind(apiObject.getKind());
        entry.setName(apiObject.getMetadata().getName());  // Primary key from metadata

        // Convert complex objects to JsonNode
        Gson gson = new Gson();
        ObjectMapper objectMapper = new ObjectMapper();

        // Convert metadata to JsonNode
        String metadataJson = gson.toJson(apiObject.getMetadata());
        entry.setMetadata(objectMapper.readTree(metadataJson));

        // Convert stack to JsonNode
        String stackJson = gson.toJson(apiObject.getRulesets().getStack());
        entry.setStack(objectMapper.readTree(stackJson));

        // Convert rules to JsonNode
        String rulesJson = gson.toJson(apiObject.getRulesets().getRules());
        entry.setRules(objectMapper.readTree(rulesJson));

        // Convert dependencies to JsonNode
        String dependenciesJson = gson.toJson(apiObject.getRulesets().getDependencies());
        entry.setDependencies(objectMapper.readTree(dependenciesJson));

        return entry;
    }
}
