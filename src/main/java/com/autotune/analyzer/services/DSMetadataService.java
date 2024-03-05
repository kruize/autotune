package com.autotune.analyzer.services;

import com.autotune.analyzer.serviceObjects.DSMetadataAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.data.dataSourceDetails.DataSourceDetailsInfo;
import com.autotune.common.datasource.DataSourceCollection;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceManager;
import com.autotune.utils.MetricsConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.micrometer.core.instrument.Timer;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

@WebServlet(asyncSupported = true)
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
        HashMap<String, DataSourceDetailsInfo> dataSourceMetadataMap = new HashMap<>();
        String inputData = "";

        try {
            // Set the character encoding of the request to UTF-8
            request.setCharacterEncoding(CHARACTER_ENCODING);

            inputData = request.getReader().lines().collect(Collectors.joining());

            if (null == inputData || inputData.isEmpty()) {
                throw new Exception("Request input data cannot be null or empty");
            }

            DSMetadataAPIObject DSMetadataAPIObject = new Gson().fromJson(inputData, DSMetadataAPIObject.class);

            DSMetadataAPIObject.validateInputFields();

            String dataSourceName = DSMetadataAPIObject.getDataSourceName();

            if (null == dataSourceName || dataSourceName.isEmpty()) {
                sendErrorResponse(
                        inputData,
                        response,
                        null,
                        HttpServletResponse.SC_BAD_REQUEST,
                        AnalyzerErrorConstants.APIErrors.ImportDataSourceMetadataAPI.DATASOURCE_NAME_MANDATORY);
            }
            // kruize_dsmetadata -> addDSMetadataToDB()
            addDSMetadataToDataBase(dataSourceMetadataMap, dataSourceName);

            if (dataSourceMetadataMap.isEmpty() || !dataSourceMetadataMap.containsKey(dataSourceName)) {
                sendErrorResponse(
                        inputData,
                        response,
                        new Exception(AnalyzerErrorConstants.APIErrors.ImportDataSourceMetadataAPI.INVALID_DATASOURCE_NAME_METADATA_EXCPTN),
                        HttpServletResponse.SC_BAD_REQUEST,
                        String.format(AnalyzerErrorConstants.APIErrors.ImportDataSourceMetadataAPI.DATASOURCE_METADATA_IMPORT_ERROR_MSG, dataSourceName)
                );
            } else {
                sendSuccessResponse(response, dataSourceMetadataMap.get(dataSourceName));
            }
            /*
            List<ImportDSMetadataAPIObject> importDSMetadataAPIObjects = Arrays.asList(new Gson().fromJson(inputData, ImportDSMetadataAPIObject[].class));
            if (importDSMetadataAPIObjects.size() > 1) {
                LOGGER.error(AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_DATASOURCE);
                sendErrorResponse(inputData, response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_DATASOURCE);
            } else {
                for (ImportDSMetadataAPIObject importDSMetadataAPIObject: importDSMetadataAPIObjects) {
                    String dataSourceName = importDSMetadataAPIObject.getDataSourceName();

                    if (null == dataSourceName || dataSourceName.isEmpty()) {
                        sendErrorResponse(
                                inputData,
                                response,
                                null,
                                HttpServletResponse.SC_BAD_REQUEST,
                                AnalyzerErrorConstants.APIErrors.ImportDataSourceMetadataAPI.DATASOURCE_NAME_MANDATORY);
                    }
                    // kruize_dsmetadata -> addDSMetadataToDB()
                    addDataSourceMetadataToDataBase(dataSourceMetadataMap, dataSourceName);

                    if (dataSourceMetadataMap.isEmpty() || !dataSourceMetadataMap.containsKey(dataSourceName)) {
                        sendErrorResponse(
                                inputData,
                                response,
                                new Exception(AnalyzerErrorConstants.APIErrors.ListDataSourcesAPI.INVALID_DATASOURCE_NAME_EXCPTN),
                                HttpServletResponse.SC_BAD_REQUEST,
                                String.format(AnalyzerErrorConstants.APIErrors.ListDataSourcesAPI.INVALID_DATASOURCE_NAME_MSG, dataSourceName)
                        );
                    } else {
                        sendSuccessResponse(response, dataSourceMetadataMap.get(dataSourceName));
                    }
                }
            }
             */
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

    private void addDSMetadataToDataBase(HashMap<String, DataSourceDetailsInfo> dataSourceMetadataMap, String dataSourceName) {
        try {
            if (null == dataSourceName || dataSourceName.isEmpty()) {
                return;
            }
            if (DataSourceCollection.getInstance().getDataSourcesCollection().isEmpty() || !DataSourceCollection.getInstance().getDataSourcesCollection().containsKey(dataSourceName)) {
                return;
            }

            DataSourceInfo dataSource = DataSourceCollection.getInstance().getDataSourcesCollection().get(dataSourceName);
            new DataSourceManager().importDataFromDataSource(dataSource);
            dataSourceMetadataMap.put(dataSourceName, new DataSourceManager().getDataFromDataSource(dataSource));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void sendSuccessResponse(HttpServletResponse response, DataSourceDetailsInfo dataSourceMetadata) throws IOException {
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
        //Key = dataSource name
        HashMap<String, DataSourceDetailsInfo> dataSourceDetailsMap = new HashMap<>();
        boolean error = false;

        try {
            if (null != dataSourceName) {
                try {
                    //kruize_dsmetadata -> loadDSMetadataByName()
                    loadDSMetadataByName(dataSourceDetailsMap, dataSourceName);
                } catch (Exception e) {
                    LOGGER.error("Loading saved Datasource metadata {} failed: {} ", dataSourceName, e.getMessage());
                }

                if (!dataSourceDetailsMap.containsKey(dataSourceName)) {
                    error = true;
                    sendErrorResponse(
                            response,
                            new Exception(AnalyzerErrorConstants.APIErrors.ImportDataSourceMetadataAPI.MISSING_DATASOURCE_METADATA_EXCPTN),
                            HttpServletResponse.SC_BAD_REQUEST,
                            String.format(AnalyzerErrorConstants.APIErrors.ImportDataSourceMetadataAPI.MISSING_DATASOURCE_METADATA_MSG, dataSourceName, clusterName, namespace)
                    );
                }

            } else {
                error = true;
                sendErrorResponse(
                        response,
                        new Exception(AnalyzerErrorConstants.APIErrors.ImportDataSourceMetadataAPI.INVALID_DATASOURCE_NAME_METADATA_EXCPTN),
                        HttpServletResponse.SC_BAD_REQUEST,
                        String.format(AnalyzerErrorConstants.APIErrors.ImportDataSourceMetadataAPI.INVALID_DATASOURCE_NAME_METADATA_MSG
                                , dataSourceName)
                );
            }

            if (!error) {
                // create Gson Object
                Gson gsonObj = createGsonObject();
                gsonStr = gsonObj.toJson(dataSourceDetailsMap.get(dataSourceName));
                response.getWriter().println(gsonStr);
                response.getWriter().close();
                statusValue = "success";
            }

        } catch (Exception e) {
            LOGGER.error("Exception: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            if (null != timerImportDSMetadata) {
                MetricsConfig.timerImportDSMetadata = MetricsConfig.timerBImportDSMetadata.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerImportDSMetadata.stop(MetricsConfig.timerImportDSMetadata);
            }
        }
    }

    public void loadDSMetadataByName(HashMap<String, DataSourceDetailsInfo> dataSourceMetadataMap, String dataSourceName){
        try {
            if (null == dataSourceName || dataSourceName.isEmpty()) {
                return;
            }
            if (DataSourceCollection.getInstance().getDataSourcesCollection().isEmpty() || !DataSourceCollection.getInstance().getDataSourcesCollection().containsKey(dataSourceName)) {
                return;
            }

            DataSourceInfo dataSource = DataSourceCollection.getInstance().getDataSourcesCollection().get(dataSourceName);
            DataSourceDetailsInfo dataSourceMetadata = new DataSourceManager().getDataFromDataSource(dataSource);
            if (null != dataSourceMetadata) {
                dataSourceMetadataMap.put(dataSourceName, dataSourceMetadata);
            }

        } catch(Exception e) {
            LOGGER.error(e.getMessage());
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

}
