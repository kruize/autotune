/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.kruizeObject.RecommendationSettings;
import com.autotune.analyzer.serviceObjects.*;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.dataSourceMetadata.*;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceManager;
import com.autotune.common.k8sObjects.TrialSettings;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 *
 */
@WebServlet(asyncSupported = true)
public class CrawlerService extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerService.class);
    private ExecutorService executorService = Executors.newFixedThreadPool(10);


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Generates recommendations
     *
     * @param request  an {@link HttpServletRequest} object that
     *                 contains
     *                 the request the client has made
     *                 of the servlet
     * @param response an {@link HttpServletResponse} object that
     *                 contains the response the servlet sends
     *                 to the client
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String statusValue = "failure";
        List<String> experimentNames = new ArrayList<>();
        try {
            // Set the character encoding of the request to UTF-8
            request.setCharacterEncoding(CHARACTER_ENCODING);
            // Get the values from the request parameters
            String intervalEndTimeStr = request.getParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME);
            String intervalStartTimeStr = request.getParameter(KruizeConstants.JSONKeys.INTERVAL_START_TIME);

            // Initialize StringBuilder for uniqueKey
            StringBuilder uniqueKeyBuilder = new StringBuilder();

            // Get all parameter names from the query string
            Enumeration<String> parameterNames = request.getParameterNames();

            boolean firstParam = true;
            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                if (!"interval_start_time".equals(paramName) && !"interval_end_time".equals(paramName)) {
                    String paramValue = request.getParameter(paramName);
                    LOGGER.info(paramValue);
                    // Check if the value is enclosed with double quotes
                    if (!paramValue.startsWith("\"") || !paramValue.endsWith("\"")) {
                        paramValue = "\"" + paramValue + "\"";
                    }

                    if (!firstParam) {
                        uniqueKeyBuilder.append(" , ");
                    }
                    uniqueKeyBuilder.append(paramName).append("=").append(paramValue);
                    firstParam = false;
                }
            }

            String uniqueKey = uniqueKeyBuilder.toString();
            long interval_end_time_epoc = 0;
            long interval_start_time_epoc = 0;
            LocalDateTime localDateTime = LocalDateTime.parse(intervalEndTimeStr, DateTimeFormatter.ofPattern(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT));
            interval_end_time_epoc = localDateTime.toEpochSecond(ZoneOffset.UTC);
            Timestamp interval_end_time = Timestamp.from(localDateTime.toInstant(ZoneOffset.UTC));
            localDateTime = LocalDateTime.parse(intervalStartTimeStr, DateTimeFormatter.ofPattern(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT));
            interval_start_time_epoc = localDateTime.toEpochSecond(ZoneOffset.UTC);
            Timestamp interval_start_time =  Timestamp.from(localDateTime.toInstant(ZoneOffset.UTC));

     /*       SimpleDateFormat sdf = new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, Locale.ROOT);
            interval_end_time_epoc = interval_end_time.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                    - ((long) interval_end_time.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE);
            interval_start_time_epoc = interval_start_time.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                    - ((long) interval_start_time.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC);*/


            // Start async processing
            AsyncContext asyncContext = request.startAsync();

            int steps = 15 * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE;
            DataSourceManager dataSourceManager = new DataSourceManager();
            DataSourceInfo datasource = dataSourceManager.fetchDataSourceFromDBByName("prometheus-1");
            DataSourceMetadataInfo metadataInfo = dataSourceManager.importMetadataFromDataSource(datasource,uniqueKey,interval_start_time_epoc,interval_end_time_epoc,steps);

            Collection<DataSource> dataSourceCollection = metadataInfo.getDataSourceHashMap().values();
            for(DataSource ds : dataSourceCollection) {
                HashMap<String, DataSourceCluster> clusterHashMap = ds.getDataSourceClusterHashMap();
                for(DataSourceCluster dsc : clusterHashMap.values()) {
                    HashMap<String, DataSourceNamespace> namespaceHashMap = dsc.getDataSourceNamespaceHashMap();
                    for (DataSourceNamespace namespace : namespaceHashMap.values()) {
                        HashMap<String, DataSourceWorkload> dataSourceWorkloadHashMap = namespace.getDataSourceWorkloadHashMap();
                        if(dataSourceWorkloadHashMap != null) {
                            for(DataSourceWorkload dsw : dataSourceWorkloadHashMap.values()) {
                                HashMap<String, DataSourceContainer> dataSourceContainerHashMap = dsw.getDataSourceContainerHashMap();
                                if(dataSourceContainerHashMap != null) {
                                    for(DataSourceContainer dc : dataSourceContainerHashMap.values()) {
                                        CreateExperimentAPIObject createExperimentAPIObject = new CreateExperimentAPIObject();
                                        createExperimentAPIObject.setMode("monitor");
                                        createExperimentAPIObject.setTargetCluster("local");
                                        createExperimentAPIObject.setApiVersion("v2.0");
                                        String experiment_name = "prometheus-1"+"-"+dsc.getDataSourceClusterName()+"-"+namespace.getDataSourceNamespaceName()
                                                +"-"+dsw.getDataSourceWorkloadName()+"("+dsw.getDataSourceWorkloadType()+")"+"-"+dc.getDataSourceContainerName();
                                        createExperimentAPIObject.setExperimentName(experiment_name);
                                        createExperimentAPIObject.setDatasource("prometheus-1");
                                        createExperimentAPIObject.setClusterName(dsc.getDataSourceClusterName());
                                        createExperimentAPIObject.setPerformanceProfile("resource-optimization-openshift");
                                        List<KubernetesAPIObject> kubernetesAPIObjectList = new ArrayList<>();
                                        KubernetesAPIObject kubernetesAPIObject = new KubernetesAPIObject();
                                        ContainerAPIObject cao = new ContainerAPIObject(dc.getDataSourceContainerName(),
                                                dc.getDataSourceContainerImageName(),null,null);
                                        kubernetesAPIObject.setContainerAPIObjects(Arrays.asList(cao));
                                        kubernetesAPIObject.setName(dsw.getDataSourceWorkloadName());
                                        kubernetesAPIObject.setType(dsw.getDataSourceWorkloadType());
                                        kubernetesAPIObject.setNamespace(namespace.getDataSourceNamespaceName());
                                        kubernetesAPIObjectList.add(kubernetesAPIObject);
                                        createExperimentAPIObject.setKubernetesObjects(kubernetesAPIObjectList);
                                        RecommendationSettings rs = new RecommendationSettings();
                                        rs.setThreshold(0.1);
                                        createExperimentAPIObject.setRecommendationSettings(rs);
                                        TrialSettings trialSettings= new TrialSettings();
                                        trialSettings.setMeasurement_durationMinutes("15min");
                                        createExperimentAPIObject.setTrialSettings(trialSettings);
                                        List<KruizeObject> kruizeExpList = new ArrayList<>();

                                        createExperimentAPIObject.setExperiment_id(Utils.generateID(createExperimentAPIObject.toString()));
                                        createExperimentAPIObject.setStatus(AnalyzerConstants.ExperimentStatus.IN_PROGRESS);

                                        try {
                                            new ExperimentDBService().addExperimentToDB(createExperimentAPIObject);
                                            experimentNames.add(experiment_name);
                                        }catch (Exception e){
                                            LOGGER.info(e.getMessage());
                                        }
                                    }
                                }

                            }
                        }


                    }
                }
            }
            executorService.submit(() -> {
                try {
                    // Call another servlet using RequestDispatcher
                    asyncContext.getRequest().getRequestDispatcher("/generateRecommendations?experiment_name="+experimentNames.get(0))
                            .forward(asyncContext.getRequest(), asyncContext.getResponse());

                    // Complete the async processing
                    asyncContext.complete();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            });
            response.sendRedirect("listExperiments");
            // Complete the async context (optional depending on your use case)
            asyncContext.complete();


      /*      DataSourceMetadataInfo dataSourceMetadataInfo = dataSourceManager.DataSourceMetadataClusterView("prometheus-1", metadataInfo);

            List<KruizeDSMetadataEntry> kruizeMetadataList = DBHelpers.Converters.KruizeObjectConverters.convertDataSourceMetadataToMetadataObj(dataSourceMetadataInfo);

            if (null != kruizeMetadataList && !kruizeMetadataList.isEmpty()) {
                List<DataSourceMetadataInfo> dataSourceMetadataInfoList = DBHelpers.Converters.KruizeObjectConverters
                        .convertKruizeMetadataToDataSourceMetadataObject(kruizeMetadataList);
            }*/
/*
            Gson gsonObj = new GsonBuilder()
                    .disableHtmlEscaping()
                    .setPrettyPrinting()
                    .enableComplexMapKeySerialization()
                    .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                    .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                    .create();
            String gsonStr = gsonObj.toJson(metadataInfo);
            // Output the extracted values
            response.setContentType("text/plain");
            response.getWriter().println("interval_start_time: " + intervalStartTimeStr);
            response.getWriter().println("interval_end_time: " + intervalEndTimeStr);
            response.getWriter().println("uniqueKey: " + uniqueKey);
            response.getWriter().println(gsonStr);*/

        } catch (Exception e) {
            LOGGER.error("Exception occurred while processing request: " + e.getMessage());
            e.printStackTrace();
        } finally {

        }
    }

    @Override
    public void destroy() {
        executorService.shutdown();
    }
}
