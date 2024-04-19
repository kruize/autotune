package com.autotune.analyzer.services;

import com.autotune.analyzer.serviceObjects.DSMetadataAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.data.dataSourceMetadata.DataSourceMetadataInfo;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceManager;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeSupportedTypes;
import com.autotune.utils.MetricsConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

public class DSMetadataService extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(DSMetadataService.class);

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String statusValue = "failure";
        Timer.Sample timerImportDSMetadata = Timer.start(MetricsConfig.meterRegistry());
        //Key = dataSourceName
        HashMap<String, DataSourceMetadataInfo> dataSourceMetadataMap = new HashMap<>();
        String inputData = "";

        try {
            // Set the character encoding of the request to UTF-8
            request.setCharacterEncoding(CHARACTER_ENCODING);

            inputData = request.getReader().lines().collect(Collectors.joining());

            if (null == inputData || inputData.isEmpty()) {
                throw new Exception("Request input data cannot be null or empty");
            }

            DSMetadataAPIObject metadataAPIObject = new Gson().fromJson(inputData, DSMetadataAPIObject.class);

            metadataAPIObject.validateInputFields();

            String dataSourceName = metadataAPIObject.getDataSourceName();

            if (null == dataSourceName || dataSourceName.isEmpty()) {
                sendErrorResponse(
                        inputData,
                        response,
                        null,
                        HttpServletResponse.SC_BAD_REQUEST,
                        AnalyzerErrorConstants.APIErrors.DSMetadataAPI.DATASOURCE_NAME_MANDATORY);
            }

            DataSourceInfo datasource = new ExperimentDBService().loadDataSourceFromDBByName(dataSourceName);
            if(null != datasource) {
                new DataSourceManager().importMetadataFromDataSource(datasource);
                DataSourceMetadataInfo dataSourceMetadata = new ExperimentDBService().loadMetadataFromDBByName(dataSourceName, "false");
                dataSourceMetadataMap.put(dataSourceName,dataSourceMetadata);
            }

            if (dataSourceMetadataMap.isEmpty() || !dataSourceMetadataMap.containsKey(dataSourceName)) {
                sendErrorResponse(
                        inputData,
                        response,
                        new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_DATASOURCE_NAME_METADATA_EXCPTN),
                        HttpServletResponse.SC_BAD_REQUEST,
                        String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.DATASOURCE_METADATA_IMPORT_ERROR_MSG, dataSourceName)
                );
            } else {
                sendSuccessResponse(response, dataSourceMetadataMap.get(dataSourceName));
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Unknown exception caught: " + e.getMessage());
            sendErrorResponse(inputData, response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: " + e.getMessage());
        } finally {
            if (null != timerImportDSMetadata) {
                MetricsConfig.timerImportDSMetadata = MetricsConfig.timerBImportDSMetadata.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerImportDSMetadata.stop(MetricsConfig.timerImportDSMetadata);
            }
        }

    }

    private void sendSuccessResponse(HttpServletResponse response, DataSourceMetadataInfo dataSourceMetadata) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);

        String gsonStr = "";
        if (null != dataSourceMetadata) {
            Gson gsonObj = new GsonBuilder()
                    .disableHtmlEscaping()
                    .setPrettyPrinting()
                    .enableComplexMapKeySerialization()
                    .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                    .create();
            gsonStr = gsonObj.toJson(dataSourceMetadata);
        }
        response.getWriter().println(gsonStr);
        response.getWriter().close();
    }

    public void sendErrorResponse(String inputRequestPayload, HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws
            IOException {
        if (null != e) {
            LOGGER.error(e.toString());
            e.printStackTrace();
            if (null == errorMsg) errorMsg = e.getMessage();
        }
        // check for the input request data to debug issues, if any
        LOGGER.debug(inputRequestPayload);
        response.sendError(httpStatusCode, errorMsg);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        String statusValue = "failure";
        Timer.Sample timerImportDSMetadata = Timer.start(MetricsConfig.meterRegistry());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        String gsonStr;

        String dataSourceName = request.getParameter(AnalyzerConstants.ServiceConstants.DATASOURCE);
        String clusterName = request.getParameter(AnalyzerConstants.ServiceConstants.CLUSTER_NAME);
        String namespace = request.getParameter(AnalyzerConstants.ServiceConstants.NAMESPACE);
        String verbose = request.getParameter(AnalyzerConstants.ServiceConstants.VERBOSE);
        String internalVerbose = "false";
        //Key = dataSource name
        HashMap<String, DataSourceMetadataInfo> dataSourceMetadataMap = new HashMap<>();
        boolean error = false;
        // validate Query params
        Set<String> invalidParams = new HashSet<>();
        for (String param : request.getParameterMap().keySet()) {
            if (!KruizeSupportedTypes.DSMETADATA_QUERY_PARAMS_SUPPORTED.contains(param)) {
                invalidParams.add(param);
            }
        }

        try {
            if (invalidParams.isEmpty()){
                if (null != verbose) {
                    internalVerbose = verbose;
                }

                if (isValidBooleanValue(internalVerbose)) {
                    try {
                        if (null != dataSourceName) {
                            try {
                                DataSourceMetadataInfo dataSourceMetadata = null;
                                if (null == clusterName) {
                                    dataSourceMetadata = new ExperimentDBService().loadMetadataFromDBByName(dataSourceName, internalVerbose);

                                } else if (null != clusterName){
                                    if (null == namespace) {
                                        dataSourceMetadata = new ExperimentDBService().loadMetadataFromDBByClusterName(dataSourceName, clusterName, internalVerbose);
                                    } else {
                                        internalVerbose = "true";
                                        dataSourceMetadata = new ExperimentDBService().loadMetadataFromDBByNamespace(dataSourceName, clusterName, namespace);
                                    }
                                }

                                if (null != dataSourceMetadata) {
                                    dataSourceMetadataMap.put(dataSourceName, dataSourceMetadata);
                                }
                            } catch (Exception e) {
                                LOGGER.error("Loading saved Datasource metadata {} failed: {} ", dataSourceName, e.getMessage());
                            }

                            if (!dataSourceMetadataMap.containsKey(dataSourceName)) {
                                error = true;
                                sendErrorResponse(
                                        response,
                                        new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.MISSING_DATASOURCE_METADATA_EXCPTN),
                                        HttpServletResponse.SC_BAD_REQUEST,
                                        String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.MISSING_DATASOURCE_METADATA_MSG, dataSourceName, clusterName, namespace)
                                );
                            }

                        } else {
                            error = true;
                            sendErrorResponse(
                                    response,
                                    new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_DATASOURCE_NAME_METADATA_EXCPTN),
                                    HttpServletResponse.SC_BAD_REQUEST,
                                    String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_DATASOURCE_NAME_METADATA_MSG
                                            , dataSourceName)
                            );
                        }

                        if (!error) {
                            // create Gson Object
                            Gson gsonObj = createGsonObject();
                            gsonStr = gsonObj.toJson(dataSourceMetadataMap.get(dataSourceName));
                            response.getWriter().println(gsonStr);
                            response.getWriter().close();
                            statusValue = "success";
                        }
                    } catch (Exception e) {
                        LOGGER.error("Exception: " + e.getMessage());
                        e.printStackTrace();
                        sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    }
                } else {
                    sendErrorResponse(
                            response,
                            new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_QUERY_PARAM_VALUE),
                            HttpServletResponse.SC_BAD_REQUEST,
                            String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_QUERY_PARAM_VALUE)
                    );
                }
            } else {
                sendErrorResponse(
                        response,
                        new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_QUERY_PARAM),
                        HttpServletResponse.SC_BAD_REQUEST,
                        String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_QUERY_PARAM, invalidParams)
                );
            }
        } finally {
            if (null != timerImportDSMetadata) {
                MetricsConfig.timerImportDSMetadata = MetricsConfig.timerBImportDSMetadata.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerImportDSMetadata.stop(MetricsConfig.timerImportDSMetadata);
            }
        }
    }

    public void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws
            IOException {
        if (null != e) {
            LOGGER.error(e.toString());
            e.printStackTrace();
            if (null == errorMsg) errorMsg = e.getMessage();
        }
        response.sendError(httpStatusCode, errorMsg);
    }
    private Gson createGsonObject() {
        return new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                .create();
    }
    private boolean isValidBooleanValue(String value) {
        return value != null && (value.equals("true") || value.equals("false"));
    }
}
