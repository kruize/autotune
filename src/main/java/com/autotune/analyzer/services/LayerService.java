/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.autotune.analyzer.services;

import com.autotune.analyzer.exceptions.MonitoringAgentNotSupportedException;
import com.autotune.analyzer.kruizeLayer.KruizeLayer;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.database.dao.ExperimentDAOImpl;
import com.autotune.database.helper.DBHelpers;
import com.autotune.database.table.lm.KruizeLMLayerEntry;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.KruizeSupportedTypes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * REST API service for Layer management (create and list operations)
 */
@WebServlet(asyncSupported = true)
public class LayerService extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(LayerService.class);

    /**
     * Create a new Layer
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String inputData = request.getReader().lines().collect(Collectors.joining());
            KruizeLayer kruizeLayer = Converters.KruizeObjectConverters.convertInputJSONToCreateLayer(inputData);

            // Validate that layer doesn't already exist
            ExperimentDAOImpl experimentDAO = new ExperimentDAOImpl();
            List<KruizeLMLayerEntry> existingLayers = experimentDAO.loadLayerByName(kruizeLayer.getLayerName());

            if (existingLayers != null && !existingLayers.isEmpty()) {
                sendErrorResponse(response, null, HttpServletResponse.SC_CONFLICT,
                        String.format(AnalyzerErrorConstants.APIErrors.CreateLayerAPI.LAYER_ALREADY_EXISTS, kruizeLayer.getLayerName()));
                return;
            }

            // Convert KruizeLayer to KruizeLMLayerEntry
            KruizeLMLayerEntry layerEntry = convertToLayerEntry(kruizeLayer);

            // Add to database
            ValidationOutputData addedToDB = experimentDAO.addLayerToDB(layerEntry);

            if (addedToDB.isSuccess()) {
                LOGGER.debug(KruizeConstants.LayerAPIMessages.ADD_LAYER_TO_DB, kruizeLayer.getLayerName());
                sendSuccessResponse(response, String.format(KruizeConstants.LayerAPIMessages.CREATE_LAYER_SUCCESS_MSG, kruizeLayer.getLayerName()));
            } else {
                sendErrorResponse(response, null, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        String.format(AnalyzerErrorConstants.APIErrors.CreateLayerAPI.ADD_LAYER_TO_DB_FAILURE, addedToDB.getMessage()));
            }
        } catch (MonitoringAgentNotSupportedException e) {
            LOGGER.error("Failed to create layer: {}", e.getMessage());
            sendErrorResponse(response, new Exception(e), HttpServletResponse.SC_BAD_REQUEST, "Validation failed: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Failed to create layer: {}", e.getMessage());
            sendErrorResponse(response, e, HttpServletResponse.SC_BAD_REQUEST, "Validation failed: " + e.getMessage());
        }
    }

    /**
     * Get list of Layers
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_OK);

        String layerName = request.getParameter("name");
        boolean error = false;

        // Validate query params
        Set<String> invalidParams = new HashSet<>();
        for (String param : request.getParameterMap().keySet()) {
            if (!KruizeSupportedTypes.LIST_LAYERS_QUERY_PARAMS_SUPPORTED.contains(param)) {
                invalidParams.add(param);
            }
        }

        try {
            if (invalidParams.isEmpty()) {
                List<KruizeLMLayerEntry> layerEntries = new ArrayList<>();
                ExperimentDAOImpl experimentDAO = new ExperimentDAOImpl();

                try {
                    if (null != layerName && !layerName.isEmpty()) {
                        layerEntries = experimentDAO.loadLayerByName(layerName);
                    } else {
                        layerEntries = experimentDAO.loadAllLayers();
                    }

                    // Check if layer exists
                    if (layerName != null && layerEntries.isEmpty()) {
                        error = true;
                        sendErrorResponse(
                                response,
                                new Exception(AnalyzerErrorConstants.APIErrors.ListLayerAPI.INVALID_LAYER_NAME_EXCPTN),
                                HttpServletResponse.SC_BAD_REQUEST,
                                String.format(AnalyzerErrorConstants.APIErrors.ListLayerAPI.INVALID_LAYER_NAME_MSG, layerName)
                        );
                    } else if (null == layerName && layerEntries.isEmpty()) {
                        error = true;
                        sendErrorResponse(
                                response,
                                new Exception(AnalyzerErrorConstants.APIErrors.ListLayerAPI.NO_LAYERS_EXCPTN),
                                HttpServletResponse.SC_BAD_REQUEST,
                                AnalyzerErrorConstants.APIErrors.ListLayerAPI.NO_LAYERS
                        );
                    }

                    if (!error) {
                        // Convert database objects to domain objects for clean JSON serialization
                        List<KruizeLayer> kruizeLayers = new ArrayList<>();
                        for (KruizeLMLayerEntry entry : layerEntries) {
                            KruizeLayer layer = DBHelpers.Converters.KruizeObjectConverters.convertLayerDBObjToLayerObject(entry);
                            if (layer != null) {
                                kruizeLayers.add(layer);
                            }
                        }

                        Gson gsonObj = createGsonObject();
                        String gsonStr = gsonObj.toJson(kruizeLayers);
                        response.getWriter().println(gsonStr);
                        response.getWriter().close();
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception while loading layers: {}", e.getMessage());
                    e.printStackTrace();
                    sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            String.format(AnalyzerErrorConstants.APIErrors.ListLayerAPI.LOAD_ALL_LAYERS_ERROR, e.getMessage()));
                }
            } else {
                sendErrorResponse(
                        response,
                        new Exception(AnalyzerErrorConstants.APIErrors.ListLayerAPI.INVALID_QUERY_PARAM),
                        HttpServletResponse.SC_BAD_REQUEST,
                        String.format(AnalyzerErrorConstants.APIErrors.ListLayerAPI.INVALID_QUERY_PARAM, invalidParams)
                );
            }
        } catch (Exception e) {
            LOGGER.error("Failed to list layers: {}", e.getMessage());
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Convert KruizeLayer to KruizeLMLayerEntry for database storage
     *
     * @param kruizeLayer
     * @return
     * @throws Exception
     */
    private KruizeLMLayerEntry convertToLayerEntry(KruizeLayer kruizeLayer) throws Exception {
        KruizeLMLayerEntry entry = new KruizeLMLayerEntry();
        Gson gson = new Gson();
        ObjectMapper objectMapper = new ObjectMapper();

        entry.setApi_version(kruizeLayer.getApiVersion());
        entry.setKind(kruizeLayer.getKind());

        // Convert metadata to JsonNode
        if (kruizeLayer.getMetadata() != null) {
            String metadataJson = gson.toJson(kruizeLayer.getMetadata());
            JsonNode metadataNode = objectMapper.readTree(metadataJson);
            entry.setMetadata(metadataNode);
        }

        entry.setLayer_name(kruizeLayer.getLayerName());
        entry.setLayer_level(kruizeLayer.getLayerLevel());
        entry.setDetails(kruizeLayer.getDetails());

        // Convert layer_presence to JsonNode
        if (kruizeLayer.getLayerPresence() != null) {
            String layerPresenceJson = gson.toJson(kruizeLayer.getLayerPresence());
            JsonNode layerPresenceNode = objectMapper.readTree(layerPresenceJson);
            entry.setLayer_presence(layerPresenceNode);
        }

        // Convert tunables to JsonNode
        if (kruizeLayer.getTunables() != null) {
            String tunablesJson = gson.toJson(kruizeLayer.getTunables());
            JsonNode tunablesNode = objectMapper.readTree(tunablesJson);
            entry.setTunables(tunablesNode);
        }

        return entry;
    }

    /**
     * Send success response
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

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("message", message + KruizeConstants.LayerAPIMessages.VIEW_LAYERS_MSG);
        responseMap.put("httpcode", HttpServletResponse.SC_CREATED);
        responseMap.put("documentationLink", "");
        responseMap.put("status", "SUCCESS");

        out.append(new Gson().toJson(responseMap));
        out.flush();
    }

    /**
     * Send error response
     *
     * @param response
     * @param e
     * @param httpStatusCode
     * @param errorMsg
     * @throws IOException
     */
    private void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws IOException {
        if (null != e) {
            LOGGER.error(e.toString());
            if (null == errorMsg)
                errorMsg = e.getMessage();
        }
        response.sendError(httpStatusCode, errorMsg);
    }

    /**
     * Create Gson object for JSON serialization
     *
     * @return
     */
    private Gson createGsonObject() {
        return new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();
    }
}
