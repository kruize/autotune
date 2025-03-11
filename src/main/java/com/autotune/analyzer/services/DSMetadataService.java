/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
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

import com.autotune.analyzer.adapters.DeviceDetailsAdapter;
import com.autotune.analyzer.adapters.RecommendationItemAdapter;
import com.autotune.analyzer.exceptions.KruizeResponse;
import com.autotune.analyzer.metadataProfiles.MetadataProfile;
import com.autotune.analyzer.metadataProfiles.MetadataProfileCollection;
import com.autotune.analyzer.serviceObjects.DSMetadataAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.dataSourceMetadata.DataSourceMetadataInfo;
import com.autotune.common.data.system.info.device.DeviceDetails;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceManager;
import com.autotune.common.datasource.DataSourceMetadataValidation;
import com.autotune.common.utils.CommonUtils;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

public class DSMetadataService extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(DSMetadataService.class);

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Handles the POST request for importing metadata - POST /dsmetadata
     * @param request  the HttpServletRequest containing the client request
     * @param response the HttpServletResponse containing the response to be sent to the client
     *
     * The input request body should be a JSON object with the following structure:
     * Example:
     * {
     *     "version": "v1.0",
     *     "datasource_name": "exampleDataSourceName",
     *     "metadata_profile": "cluster-metadata-local-monitoring",
     *     "measurement_duration": "15min"
     * }
     * where:
     * The `version` field is the version of the API, and the `datasource_name` field is the datasource name
     *
     * Example API response:
     * {
     *     "datasources": {
     *         "exampleDataSourceName": {
     *             "datasource_name": "exampleDataSourceName",
     *             "clusters": {
     *                 "exampleClusterName": {
     *                     "cluster_name": "exampleClusterName"
     *                 }
     *             }
     *         }
     *     }
     * }
     * API response displays cluster-level metadata
     * NOTE - POST /dsmetadata API also supports multiple import metadata actions
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String statusValue = "failure";
        Timer.Sample timerImportDSMetadata = Timer.start(MetricsConfig.meterRegistry());
        //Key = dataSourceName
        HashMap<String, DataSourceMetadataInfo> dataSourceMetadataMap = new HashMap<>();
        String inputData = "";
        DataSourceManager dataSourceManager = new DataSourceManager();

        try {
            // Set the character encoding of the request to UTF-8
            request.setCharacterEncoding(CHARACTER_ENCODING);

            inputData = request.getReader().lines().collect(Collectors.joining());

            if (null == inputData || inputData.isEmpty()) {
                throw new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.DATASOURCE_METADATA_MISSING_REQUEST_INPUT_EXCPTN);
            }

            DSMetadataAPIObject metadataAPIObject = new Gson().fromJson(inputData, DSMetadataAPIObject.class);

            ValidationOutputData validationOutputData = validateMandatoryFields(metadataAPIObject);
            if (validationOutputData.isSuccess()) {

                String dataSourceName = metadataAPIObject.getDataSourceName();

                // fetch the DatasourceInfo object based on datasource name
                DataSourceInfo datasource;
                try {
                    datasource = CommonUtils.getDataSourceInfo(dataSourceName);
                } catch (Exception e) {
                    sendErrorResponse(
                            inputData,
                            response,
                            new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_DATASOURCE_NAME_METADATA_EXCPTN),
                            HttpServletResponse.SC_BAD_REQUEST,
                            String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.DATASOURCE_METADATA_IMPORT_ERROR_MSG, dataSourceName)
                    );
                    return;
                }

                String metadataProfileName = metadataAPIObject.getMetadataProfile();
                if (null == metadataAPIObject.getMeasurementDurationMinutes()) {
                    metadataAPIObject.setMeasurementDurationMinutes(AnalyzerConstants.MetadataProfileConstants.DEFAULT_MEASUREMENT_DURATION);
                }
                Integer measurementDuration = metadataAPIObject.getMeasurement_duration_inInteger();

                // Verify if metadata_profile specified is valid
                if (null == MetadataProfileCollection.getInstance().getMetadataProfileCollection().get(metadataProfileName)) {
                    sendErrorResponse(
                            inputData,
                            response,
                            new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_METADATA_PROFILE_NAME_EXCPTN),
                            HttpServletResponse.SC_BAD_REQUEST,
                            String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_METADATA_PROFILE_NAME_MSG, metadataProfileName)
                    );
                    return;
                }

                DataSourceMetadataInfo metadataInfo = dataSourceManager.importMetadataFromDataSource(metadataProfileName,
                        datasource,"",0, 0, 0, measurementDuration, new HashMap<>(), new HashMap<>());

                // Validate imported metadataInfo object
                DataSourceMetadataValidation validationObject = new DataSourceMetadataValidation();
                validationObject.validate(metadataInfo);

                if (!validationObject.isSuccess()) {
                    sendErrorResponse(
                            response,
                            new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.DATASOURCE_METADATA_VALIDATION_FAILURE_EXCPTN),
                            HttpServletResponse.SC_BAD_REQUEST,
                            validationObject.getErrorMessage()
                    );
                    return;
                }

                try {
                    // fetch and delete metadata from database
                    dataSourceManager.deleteMetadataFromDBByDataSource(datasource);
                    // add imported metadata to database
                    dataSourceManager.addMetadataToDB(metadataInfo);
                } catch (Exception e) {
                    sendErrorResponse(inputData, response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    return;
                }

                dataSourceMetadataMap.put(dataSourceName, metadataInfo);

                DataSourceMetadataInfo dataSourceMetadataInfo = dataSourceManager.DataSourceMetadataClusterView(dataSourceName, metadataInfo);

                if (dataSourceMetadataMap.isEmpty() || !dataSourceMetadataMap.containsKey(dataSourceName)) {
                    sendErrorResponse(
                            inputData,
                            response,
                            new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_DATASOURCE_NAME_METADATA_EXCPTN),
                            HttpServletResponse.SC_BAD_REQUEST,
                            String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.DATASOURCE_METADATA_IMPORT_ERROR_MSG, dataSourceName)
                    );
                    return;
                }

                sendSuccessResponse(response, dataSourceMetadataInfo);
            } else {
                sendErrorResponse(
                        response,
                        new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.MISSING_QUERY_PARAM_EXCPTN),
                        validationOutputData.getErrorCode(),
                        validationOutputData.getMessage());
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

    private List<String> mandatoryFields = new ArrayList<>(Arrays.asList(
            AnalyzerConstants.VERSION,
            AnalyzerConstants.DATASOURCE_NAME,
            AnalyzerConstants.METADATA_PROFILE
    ));

    public ValidationOutputData validateMandatoryFields(DSMetadataAPIObject metadataAPIObject) {
        List<String> missingMandatoryFields = new ArrayList<>();
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);

        String errorMsg = "";
        mandatoryFields.forEach(
                mField -> {
                    String methodName = "get" + mField.substring(0, 1).toUpperCase() + mField.substring(1);
                    try {
                        Method getNameMethod = metadataAPIObject.getClass().getMethod(methodName);
                        if (getNameMethod.invoke(metadataAPIObject) == null) {
                            missingMandatoryFields.add(mField);
                        }
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        LOGGER.error("Method name for {} does not exist and the error is {}", mField, e.getMessage());
                    }
                }
        );

        if(!missingMandatoryFields.isEmpty()) {
            errorMsg = errorMsg.concat(String.format("Mandatory parameters missing %s ", missingMandatoryFields));
            validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
            validationOutputData.setSuccess(false);
            validationOutputData.setMessage(errorMsg);
        } else {
            validationOutputData.setSuccess(true);
        }

        return validationOutputData;
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
                    .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
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

    /**
     * Handles the GET request for listing metadata - GET /dsmetadata
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     *
     * Supported Query Parameters -
     * datasource	name of the datasource(required)
     * cluster_name	name of the cluster(optional)
     * namespace	The namespace(optional)
     * verbose	    Flag to retrieve container-level metadata(optional)
     *
     * When the verbose parameter is set to true, the API response includes granular container-level details in the metadata,
     * offering a more comprehensive view of the clusters, namespaces, workloads and containers associated with the
     * specified datasource. When the verbose parameter is not provided or set to false, the API response provides basic
     * information like list of clusters, namespaces associated with the specified datasource
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        String statusValue = "failure";
        Timer.Sample timerListDSMetadata = Timer.start(MetricsConfig.meterRegistry());
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
                        if (null == dataSourceName || dataSourceName.isEmpty()) {
                            error = true;
                            sendErrorResponse(
                                    response,
                                    new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_DATASOURCE_NAME_METADATA_EXCPTN),
                                    HttpServletResponse.SC_BAD_REQUEST,
                                    String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.DATASOURCE_NAME_MANDATORY)
                            );
                        } else {
                            try {
                                DataSourceMetadataInfo dataSourceMetadata = null;
                                if (null == clusterName) {
                                    dataSourceMetadata = new ExperimentDBService().loadMetadataFromDBByName(dataSourceName, internalVerbose);
                                } else if (null != clusterName && null == namespace) {
                                    dataSourceMetadata = new ExperimentDBService().loadMetadataFromDBByClusterName(dataSourceName, clusterName, internalVerbose);
                                } else if (null != clusterName && null != namespace) {
                                    internalVerbose = "true";
                                    dataSourceMetadata = new ExperimentDBService().loadMetadataFromDBByNamespace(dataSourceName, clusterName, namespace);
                                }

                                if (null == dataSourceMetadata) {
                                    error = true;
                                    String errorMessage;
                                    Exception exception;

                                    if (null == clusterName) {
                                        exception = new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_DATASOURCE_NAME_METADATA_EXCPTN);
                                        errorMessage = String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_DATASOURCE_NAME_METADATA_MSG, dataSourceName);
                                    } else if (null == namespace) {
                                        exception = new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_DATASOURCE_NAME_CLUSTER_NAME_METADATA_EXCPTN);
                                        errorMessage = String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_DATASOURCE_NAME_CLUSTER_NAME_METADATA_MSG, dataSourceName, clusterName);
                                    } else {
                                        exception = new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.MISSING_DATASOURCE_METADATA_EXCPTN);
                                        errorMessage = String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.MISSING_DATASOURCE_METADATA_MSG, dataSourceName, clusterName, namespace);
                                    }

                                    sendErrorResponse(
                                            response,
                                            exception,
                                            HttpServletResponse.SC_BAD_REQUEST,
                                            errorMessage
                                    );
                                }

                                dataSourceMetadataMap.put(dataSourceName, dataSourceMetadata);
                            } catch (Exception e) {
                                LOGGER.error("Loading saved Datasource metadata {} failed: {} ", dataSourceName, e.getMessage());
                            }
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
                            new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_QUERY_PARAM_EXCPTN),
                            HttpServletResponse.SC_BAD_REQUEST,
                            String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_QUERY_PARAM_VALUE, AnalyzerConstants.ServiceConstants.VERBOSE)
                    );
                }
            } else {
                sendErrorResponse(
                        response,
                        new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_QUERY_PARAM_EXCPTN),
                        HttpServletResponse.SC_BAD_REQUEST,
                        String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_QUERY_PARAM, invalidParams)
                );
            }
        } catch (Exception e) {
            LOGGER.error("Exception: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            if (null != timerListDSMetadata) {
                MetricsConfig.timerListDSMetadata = MetricsConfig.timerBListDSMetadata.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerListDSMetadata.stop(MetricsConfig.timerListDSMetadata);
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
                .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
                .create();
    }
    private boolean isValidBooleanValue(String value) {
        return value != null && (value.equals("true") || value.equals("false"));
    }

    /**
     * TODO temp solution to delete metadata, Need to evaluate use cases
     * Handles the DELETE request for deleting metadata - DELETE /dsmetadata
     *
     * The input request body should be a JSON object with the following structure:
     * {
     *     "version": "exampleVersion",
     *     "datasource_name": "exampleDataSourceName"
     * }
     *
     * The `version` field is the version of the API, and the `datasource_name` field is the data source name.
     *
     * The expected output is a response indicating the success or failure of the datasource metadata deletion.
     *
     * @param request   the HttpServletRequest containing the client request
     * @param response  the HttpServletResponse containing the response to be sent to the client
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HashMap<String, DataSourceMetadataInfo> dataSourceMetadataMap = new HashMap<>();
        String inputData = "";
        DataSourceManager dataSourceManager = new DataSourceManager();
        try {
            // Set the character encoding of the request to UTF-8
            request.setCharacterEncoding(CHARACTER_ENCODING);

            inputData = request.getReader().lines().collect(Collectors.joining());
            if (null == inputData || inputData.isEmpty()) {
                throw new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.DATASOURCE_METADATA_MISSING_REQUEST_INPUT_EXCPTN);
            }
            DSMetadataAPIObject metadataAPIObject = new Gson().fromJson(inputData, DSMetadataAPIObject.class);
            ValidationOutputData validationOutputData = validateMandatoryFields(metadataAPIObject);
            if (!validationOutputData.isSuccess()) {
                sendErrorResponse(
                        response,
                        new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.MISSING_QUERY_PARAM_EXCPTN),
                        validationOutputData.getErrorCode(),
                        validationOutputData.getMessage());
            }

            String dataSourceName = metadataAPIObject.getDataSourceName();

            DataSourceInfo datasource = dataSourceManager.fetchDataSourceFromDBByName(dataSourceName);

            if (null == datasource) {
                sendErrorResponse(
                        inputData,
                        response,
                        new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.INVALID_DATASOURCE_NAME_METADATA_EXCPTN),
                        HttpServletResponse.SC_BAD_REQUEST,
                        String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.DATASOURCE_METADATA_DELETE_ERROR_MSG, dataSourceName)
                );
            }

            DataSourceMetadataInfo dataSourceMetadata = dataSourceManager.fetchDataSourceMetadataFromDBByName(dataSourceName, "false");
            if (null == dataSourceMetadata) {
                sendErrorResponse(
                        inputData,
                        response,
                        new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.MISSING_DATASOURCE_METADATA_EXCPTN),
                        HttpServletResponse.SC_BAD_REQUEST,
                        String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.DATASOURCE_METADATA_DELETE_ERROR_MSG, dataSourceName)
                );
            }
            dataSourceMetadataMap.put(dataSourceName, dataSourceMetadata);

            if (!dataSourceMetadataMap.isEmpty() && dataSourceMetadataMap.containsKey(dataSourceName)) {
                try {
                    // fetch and delete metadata from database
                    dataSourceManager.deleteMetadataFromDB(dataSourceName);

                    //deletes in-memory metadata object fetched from the cluster of the specified datasource
                    dataSourceManager.deleteMetadataFromDataSource(datasource);
                    dataSourceMetadataMap.remove(dataSourceName);
                } catch (Exception e) {
                    sendErrorResponse(
                            inputData,
                            response,
                            e,
                            HttpServletResponse.SC_BAD_REQUEST,
                            e.getMessage());
                }
            } else {
                sendErrorResponse(
                        inputData,
                        response,
                        new Exception(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.DATASOURCE_METADATA_DELETE_EXCPTN),
                        HttpServletResponse.SC_BAD_REQUEST,
                        String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.DATASOURCE_METADATA_DELETE_ERROR_MSG, dataSourceName)
                );
            }
            sendSuccessResponse(response, KruizeConstants.DataSourceConstants.DataSourceMetadataInfoSuccessMsgs.DATASOURCE_METADATA_DELETED);

        } catch (Exception e) {
            sendErrorResponse(inputData, response, e, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void sendSuccessResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = response.getWriter();
        out.append(
                new Gson().toJson(
                        new KruizeResponse(message + " View imported metadata at GET /dsmetadata", HttpServletResponse.SC_CREATED, "", "SUCCESS")
                )
        );
        out.flush();
    }
}
