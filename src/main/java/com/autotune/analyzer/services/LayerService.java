package com.autotune.analyzer.services;

import com.autotune.analyzer.Layer.Layer;
import com.autotune.analyzer.exceptions.PerformanceProfileResponse;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serial;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

@WebServlet(asyncSupported = true)
public class LayerService extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(LayerService.class);
    private ConcurrentHashMap<String, Layer> layersMap;


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        layersMap = (ConcurrentHashMap<String, Layer>) getServletContext()
                .getAttribute(AnalyzerConstants.LayerConstants.LAYER_MAP);
    }

    /**
     * Validate and create new Layer.
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, Layer> layerMap = new ConcurrentHashMap<>();
            String inputData = request.getReader().lines().collect(Collectors.joining());
            Layer kruizeLayer = Converters.KruizeObjectConverters.convertInputJSONToCreateLayer(inputData);
            //skipping validation for hackathon scope
            ValidationOutputData addedToDB = new ExperimentDBService().addLayerToDB(kruizeLayer);
            if (addedToDB.isSuccess()) {
                layerMap.put(String.valueOf(kruizeLayer.getMetadata().getName()), kruizeLayer);
                getServletContext().setAttribute(AnalyzerConstants.LayerConstants.LAYER_MAP, layerMap);
                sendSuccessResponse(response, String.format(KruizeConstants.LayerAPIMessages.CREATE_LAYER_SUCCESS_MSG, kruizeLayer.getMetadata().getName()));
            } else {
                sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, addedToDB.getMessage());
            }
        } catch (Exception e) {
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Validation failed: " + e.getMessage());
        }
    }


    /**
     * Get List of Layers
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
        String gsonStr = "[]";

        Map<String, Layer> kruizeLayerMap = new ConcurrentHashMap<>();
        String layerName = request.getParameter(AnalyzerConstants.LayerConstants.LAYER_NAME);

        // Fetch specific layer by name or all layers from the DB
        try {
            new ExperimentDBService().loadLayers(kruizeLayerMap, layerName);
            if (!kruizeLayerMap.isEmpty()) {
                // Convert map to list for JSON response
                List<Layer> layers = new ArrayList<>(kruizeLayerMap.values());
                gsonStr = new Gson().toJson(layers);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load layer data: {} ", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            gsonStr = new Gson().toJson("Failed to load layer from DB: " + e.getMessage());
        }
        response.getWriter().println(gsonStr);
        response.getWriter().close();
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
                        new PerformanceProfileResponse(message +
                                KruizeConstants.LayerAPIMessages.VIEW_LAYERS_MSG,
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
            if (null == errorMsg)
                errorMsg = e.getMessage();
        }
        response.sendError(httpStatusCode, errorMsg);
    }

}
