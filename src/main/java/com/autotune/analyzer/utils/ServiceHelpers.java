/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.utils;

import com.autotune.analyzer.application.ApplicationDeployment;
import com.autotune.analyzer.application.ApplicationSearchSpace;
import com.autotune.analyzer.application.ApplicationServiceStack;
import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.kruizeLayer.KruizeLayer;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.performanceProfiles.MetricProfileCollection;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.performanceProfiles.PerformanceProfilesDeployment;
import com.autotune.analyzer.recommendations.term.Terms;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ContainerDeviceList;
import com.autotune.common.data.result.GPUDeviceData;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.exceptions.DataSourceNotExist;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.GenericRestApiClient;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.Utils;
import com.google.gson.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.autotune.analyzer.utils.AnalyzerConstants.AutotuneConfigConstants.CATEGORICAL_TYPE;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.operator.KruizeOperator.deploymentMap;

/**
 * Helper functions used by the REST APIs to create the output JSON object
 */
public class ServiceHelpers {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceHelpers.class);
    private ServiceHelpers() {
    }

    /**
     * Copy over the details of the experiment from the given Autotune Object to the JSON object provided.
     *
     * @param experimentJson
     * @param kruizeObject
     */
    public static void addExperimentDetails(JSONObject experimentJson, KruizeObject kruizeObject) {
        PerformanceProfile performanceProfile = PerformanceProfilesDeployment.performanceProfilesMap
                .get(kruizeObject.getPerformanceProfile());
        experimentJson.put(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME, kruizeObject.getExperimentName());
        experimentJson.put(AnalyzerConstants.AutotuneObjectConstants.DIRECTION, performanceProfile.getSloInfo().getDirection());
        experimentJson.put(AnalyzerConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION, performanceProfile.getSloInfo().getObjectiveFunction());
        experimentJson.put(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS, performanceProfile.getSloInfo().getSloClass());
        experimentJson.put(AnalyzerConstants.AutotuneObjectConstants.EXPERIMENT_ID, kruizeObject.getExperiment_id());
        experimentJson.put(AnalyzerConstants.AutotuneObjectConstants.HPO_ALGO_IMPL, kruizeObject.getHpoAlgoImpl());
        experimentJson.put(AnalyzerConstants.AutotuneObjectConstants.NAMESPACE, kruizeObject.getNamespace());
    }

    /**
     * Copy over the array of deployments and the included stack names for the given
     * Autotune Object to the JSON Object provided
     *
     * @param experimentJson JSON object to be updated
     * @param kruizeObject
     */
    public static void addDeploymentDetails(JSONObject experimentJson, KruizeObject kruizeObject) {
        if (deploymentMap.get(kruizeObject.getExperimentName()).isEmpty()) {
            return;
        }

        JSONArray deploymentArray = new JSONArray();
        for (String deploymentName : deploymentMap.get(kruizeObject.getExperimentName()).keySet()) {
            JSONObject deploymentJson = new JSONObject();
            ApplicationDeployment applicationDeployment = deploymentMap.get(kruizeObject.getExperimentName()).get(deploymentName);
            deploymentJson.put(AnalyzerConstants.ServiceConstants.DEPLOYMENT_NAME, applicationDeployment.getDeploymentName());
            deploymentJson.put(AnalyzerConstants.ServiceConstants.NAMESPACE, applicationDeployment.getNamespace());
            JSONArray stackArray = new JSONArray();
            if (!applicationDeployment.getApplicationServiceStackMap().isEmpty()) {
                for (String stackName : applicationDeployment.getApplicationServiceStackMap().keySet()) {
                    ApplicationServiceStack applicationServiceStack = applicationDeployment.getApplicationServiceStackMap().get(stackName);
                    JSONObject stackJson = new JSONObject();
                    stackJson.put(AnalyzerConstants.ServiceConstants.STACK_NAME, stackName);
                    stackJson.put(AnalyzerConstants.ServiceConstants.CONTAINER_NAME, applicationServiceStack.getContainerName());
                    stackArray.put(stackJson);
                }
            }
            deploymentJson.put(AnalyzerConstants.ServiceConstants.STACKS, stackArray);
            deploymentArray.put(deploymentJson);
        }

        experimentJson.put(AnalyzerConstants.ServiceConstants.DEPLOYMENTS, deploymentArray);
    }

    /**
     * Copy over the details of the LAYER from the given KruizeLayer object to the JSON object provided
     *
     * @param layerJson
     * @param kruizeLayer
     */
    public static void addLayerDetails(JSONObject layerJson, KruizeLayer kruizeLayer) {
        layerJson.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_ID, kruizeLayer.getLayerId());
        layerJson.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_NAME, kruizeLayer.getLayerName());
        layerJson.put(AnalyzerConstants.ServiceConstants.LAYER_DETAILS, kruizeLayer.getDetails());
        layerJson.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_LEVEL, kruizeLayer.getLevel());
    }

    /**
     * Copy over the tunable details of the TUNABLE provided without adding the query details
     *
     * @param tunableJson
     * @param tunable
     */
    private static void addTunable(JSONObject tunableJson, Tunable tunable) {
        tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.NAME, tunable.getName());
        tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.VALUE_TYPE, tunable.getValueType());

        if (tunable.getValueType().equalsIgnoreCase(CATEGORICAL_TYPE)) {
            tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.TUNABLE_CHOICES, tunable.getChoices());
        } else {
            tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.UPPER_BOUND, tunable.getUpperBound());
            tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.LOWER_BOUND, tunable.getLowerBound());
            tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.STEP, tunable.getStep());
        }
    }

    /**
     * Copy over the details of the TUNABLES of a LAYER from the given KruizeLayer object to the JSON object provided
     * If the sloClass is not null then only copy over the TUNABLE if it matches the sloClass.
     *
     * @param tunablesArray
     * @param kruizeLayer
     * @param sloClass
     */
    public static void addLayerTunableDetails(JSONArray tunablesArray, KruizeLayer kruizeLayer, String sloClass) {
        for (Tunable tunable : kruizeLayer.getTunables()) {
            if (sloClass == null || tunable.sloClassList.contains(sloClass)) {
                JSONObject tunableJson = new JSONObject();
                addTunable(tunableJson, tunable);
                String tunableQuery = tunable.getQueries().get(KruizeDeploymentInfo.monitoring_agent);
                String query = AnalyzerConstants.NONE;
                if (tunableQuery != null && !tunableQuery.isEmpty()) {
                    query = tunableQuery;
                }
                tunableJson.put(AnalyzerConstants.ServiceConstants.QUERY_URL, query);
                tunablesArray.put(tunableJson);
            }
        }
    }

    /**
     * Copy over the details of the user specified function variables from the given autotune object to the JSON object provided
     *
     * @param funcVarJson
     * @param kruizeObject
     */
    public static void addFunctionVariablesDetails(JSONObject funcVarJson, KruizeObject kruizeObject) {
        // Add function_variables info
        JSONArray functionVariablesArray = new JSONArray();
        PerformanceProfile performanceProfile = PerformanceProfilesDeployment.performanceProfilesMap
                .get(kruizeObject.getPerformanceProfile());
        for (Metric functionVariable : performanceProfile.getSloInfo().getFunctionVariables()) {
            JSONObject functionVariableJson = new JSONObject();
            functionVariableJson.put(AnalyzerConstants.AutotuneObjectConstants.NAME, functionVariable.getName());
            functionVariableJson.put(AnalyzerConstants.AutotuneObjectConstants.VALUE_TYPE, functionVariable.getValueType());
            functionVariableJson.put(AnalyzerConstants.ServiceConstants.QUERY_URL, functionVariable.getQuery());
            functionVariablesArray.put(functionVariableJson);
        }
        funcVarJson.put(AnalyzerConstants.AutotuneObjectConstants.FUNCTION_VARIABLES, functionVariablesArray);
    }

    /**
     * Copy over the details of the SearchSpace from the given Autotune object to the JSON object provided.
     * The searchSpace will be specific to a pod as provided.
     *
     * @param outputJsonArray
     * @param applicationSearchSpace
     */
    public static void addApplicationToSearchSpace(JSONArray outputJsonArray, ApplicationSearchSpace applicationSearchSpace) {
        if (applicationSearchSpace == null) {
            return;
        }

        JSONObject applicationJson = new JSONObject();
        applicationJson.put(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME, applicationSearchSpace.getExperimentName());
        applicationJson.put(AnalyzerConstants.AutotuneObjectConstants.DIRECTION, applicationSearchSpace.getDirection());
        applicationJson.put(AnalyzerConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION, applicationSearchSpace.getObjectiveFunction());
        applicationJson.put(AnalyzerConstants.AutotuneObjectConstants.EXPERIMENT_ID, applicationSearchSpace.getExperimentId());
        applicationJson.put(AnalyzerConstants.AutotuneObjectConstants.HPO_ALGO_IMPL, applicationSearchSpace.getHpoAlgoImpl());
        applicationJson.put(AnalyzerConstants.AutotuneObjectConstants.VALUE_TYPE, applicationSearchSpace.getValueType());
        applicationJson.put(AnalyzerConstants.AutotuneObjectConstants.TOTAL_TRIALS, applicationSearchSpace.getTotalTrials());
        applicationJson.put(AnalyzerConstants.AutotuneObjectConstants.PARALLEL_TRIALS, applicationSearchSpace.getParallelTrials());

        JSONArray tunablesJsonArray = new JSONArray();
        if (!applicationSearchSpace.getTunablesMap().isEmpty()) {
            for (String applicationTunableName : applicationSearchSpace.getTunablesMap().keySet()) {
                Tunable tunable = applicationSearchSpace.getTunablesMap().get(applicationTunableName);
                JSONObject tunableJson = new JSONObject();
                // Pass the full name here that includes the layer and stack names
                tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.NAME, tunable.getFullName());
                // searchSpace is passing only the tunable value and not a string
                tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.VALUE_TYPE, tunable.getValueType());
                // check the tunable type and if it's categorical then we need to add the list of the values else we'll take the upper, lower bound values
                if (tunable.getValueType().equalsIgnoreCase(CATEGORICAL_TYPE)) {
                    tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.TUNABLE_CHOICES, tunable.getChoices());
                } else {
                    tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.LOWER_BOUND, tunable.getLowerBoundValue());
                    tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.UPPER_BOUND, tunable.getUpperBoundValue());
                    tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.STEP, tunable.getStep());
                }
                tunablesJsonArray.put(tunableJson);
            }
        }

        applicationJson.put(AnalyzerConstants.AutotuneConfigConstants.TUNABLES, tunablesJsonArray);
        outputJsonArray.put(applicationJson);
    }

    public static class KruizeObjectOperations {
        private KruizeObjectOperations() {

        }

        public static boolean checkRecommendationTimestampExists(KruizeObject kruizeObject, String timestamp) {
            boolean timestampExists = false;
            try {
                if (!Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, timestamp)) {
                    return false;
                }
                Date medDate = Utils.DateUtils.getDateFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, timestamp);
                if (null == medDate) {
                    return false;
                }
                Timestamp givenTimestamp = new Timestamp(medDate.getTime());
                for (K8sObject k8sObject : kruizeObject.getKubernetes_objects()) {
                    for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
                        for (Timestamp key : containerData.getContainerRecommendations().getData().keySet()) {
                            if (key.equals(givenTimestamp)) {
                                timestampExists = true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return timestampExists;
        }

        public static void checkAndTagGPUWorkloads(KruizeObject kruizeObject) throws Exception {
            SimpleDateFormat sdf = new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, Locale.ROOT);
            long interval_end_time_epoc = 0;
            long interval_start_time_epoc = 0;
            String metricProfileName = kruizeObject.getPerformanceProfile();
            PerformanceProfile metricProfile = MetricProfileCollection.getInstance().getMetricProfileCollection().get(metricProfileName);
            if (null == metricProfile) {
                LOGGER.error("MetricProfile does not exist or is not valid: {}", metricProfileName);
                return;
            }

            String maxDateQuery = null;
            String gpuDetectionQuery = null;
            Metric metricEntry = null;
            List<Metric> metrics = metricProfile.getSloInfo().getFunctionVariables();
            for (Metric metric: metrics) {
                String name = metric.getName();
                if(name.equals("maxDate")){
                    maxDateQuery = metric.getAggregationFunctionsMap().get("max").getQuery();
                } else if (name.equals("gpuMemoryUsage")) {
                    gpuDetectionQuery = metric.getAggregationFunctionsMap().get("max").getQuery();
                    metricEntry = metric;
                }
            }

            if (null == maxDateQuery || maxDateQuery.isEmpty()) {
                throw new NullPointerException("maxDate query cannot be empty or null");
            }

            if (null == gpuDetectionQuery || gpuDetectionQuery.isEmpty()) {
                throw new NullPointerException("gpuMemoryUsage query cannot be empty or null");
            }

            Double measurementDurationMinutesInDouble = kruizeObject.getTrial_settings().getMeasurement_durationMinutes_inDouble();
            List<K8sObject> kubernetes_objects = kruizeObject.getKubernetes_objects();

            String dataSource = kruizeObject.getDataSource();
            DataSourceInfo dataSourceInfo = new ExperimentDBService().loadDataSourceFromDBByName(dataSource);
            if (dataSourceInfo == null) {
                throw new DataSourceNotExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_INFO);
            }

            for (K8sObject k8sObject : kubernetes_objects) {
                String namespace = k8sObject.getNamespace();
                String workload = k8sObject.getName();
                String workload_type = k8sObject.getType();
                HashMap<String, ContainerData> containerDataMap = k8sObject.getContainerDataMap();
                // Iterate over containers
                for (Map.Entry<String, ContainerData> entry : containerDataMap.entrySet()) {
                    ContainerData containerData = entry.getValue();
                    String containerName = containerData.getContainer_name();

                    String queryToEncode = null;

                    LOGGER.info("maxDateQuery: {}", maxDateQuery);
                    queryToEncode =  maxDateQuery
                            .replace(AnalyzerConstants.NAMESPACE_VARIABLE, namespace)
                            .replace(AnalyzerConstants.CONTAINER_VARIABLE, containerName)
                            .replace(AnalyzerConstants.WORKLOAD_VARIABLE, workload)
                            .replace(AnalyzerConstants.WORKLOAD_TYPE_VARIABLE, workload_type);

                    String dateMetricsUrl = String.format(KruizeConstants.DataSourceConstants.DATE_ENDPOINT_WITH_QUERY,
                            dataSourceInfo.getUrl(),
                            URLEncoder.encode(queryToEncode, CHARACTER_ENCODING)
                    );

                    LOGGER.info(dateMetricsUrl);
                    JSONObject genericJsonObject = new GenericRestApiClient(dateMetricsUrl).fetchMetricsJson(KruizeConstants.APIMessages.GET, "");
                    JsonObject jsonObject = new Gson().fromJson(genericJsonObject.toString(), JsonObject.class);
                    JsonArray resultArray = jsonObject.getAsJsonObject(KruizeConstants.JSONKeys.DATA).getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT);

                    if (null == resultArray || resultArray.isEmpty()) {
                        // Need to alert that container max duration is not detected
                        // Ignoring it here, as we take care of it at generate recommendations
                        return;
                    }

                    resultArray = resultArray.get(0)
                            .getAsJsonObject().getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.VALUE);
                    long epochTime = resultArray.get(0).getAsLong();
                    String timestamp = sdf.format(new Date(epochTime * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC));
                    Date date = sdf.parse(timestamp);
                    Timestamp dateTS = new Timestamp(date.getTime());
                    interval_end_time_epoc = dateTS.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                            - ((long) dateTS.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE);
                    int maxDay = Terms.getMaxDays(kruizeObject.getTerms());
                    LOGGER.info(KruizeConstants.APIMessages.MAX_DAY, maxDay);
                    Timestamp startDateTS = Timestamp.valueOf(Objects.requireNonNull(dateTS).toLocalDateTime().minusDays(maxDay));
                    interval_start_time_epoc = startDateTS.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                            - ((long) startDateTS.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC);

                    gpuDetectionQuery = gpuDetectionQuery.replace(AnalyzerConstants.NAMESPACE_VARIABLE, namespace)
                            .replace(AnalyzerConstants.CONTAINER_VARIABLE, containerName)
                            .replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDurationMinutesInDouble.intValue()))
                            .replace(AnalyzerConstants.WORKLOAD_VARIABLE, workload)
                            .replace(AnalyzerConstants.WORKLOAD_TYPE_VARIABLE, workload_type);

                    String podMetricsUrl;
                    try {
                        podMetricsUrl = String.format(KruizeConstants.DataSourceConstants.DATASOURCE_ENDPOINT_WITH_QUERY,
                                dataSourceInfo.getUrl(),
                                URLEncoder.encode(gpuDetectionQuery, CHARACTER_ENCODING),
                                interval_start_time_epoc,
                                interval_end_time_epoc,
                                measurementDurationMinutesInDouble.intValue() * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE);
                        LOGGER.info(podMetricsUrl);
                        genericJsonObject = new GenericRestApiClient(podMetricsUrl).fetchMetricsJson(KruizeConstants.APIMessages.GET, "");
                        jsonObject = new Gson().fromJson(genericJsonObject.toString(), JsonObject.class);
                        resultArray = jsonObject.getAsJsonObject(KruizeConstants.JSONKeys.DATA).getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT);

                        if (null != resultArray && !resultArray.isEmpty()) {
                            for (JsonElement result : resultArray) {
                                JsonObject resultObject = result.getAsJsonObject();
                                JsonArray valuesArray = resultObject.getAsJsonArray(KruizeConstants.DataSourceConstants
                                        .DataSourceQueryJSONKeys.VALUES);

                                for (JsonElement element : valuesArray) {
                                    JsonArray valueArray = element.getAsJsonArray();
                                    double value = valueArray.get(1).getAsDouble();
                                    // TODO: Check for non-zero values to mark as GPU workload
                                    break;
                                }

                                JsonObject metricObject = resultObject.getAsJsonObject(KruizeConstants.JSONKeys.METRIC);
                                String modelName = metricObject.get(KruizeConstants.JSONKeys.MODEL_NAME).getAsString();
                                if (null == modelName)
                                    continue;

                                boolean isSupportedMig = checkIfModelIsKruizeSupportedMIG(modelName);
                                if (isSupportedMig) {
                                    GPUDeviceData gpuDeviceData = new GPUDeviceData(metricObject.get(KruizeConstants.JSONKeys.MODEL_NAME).getAsString(),
                                            metricObject.get(KruizeConstants.JSONKeys.HOSTNAME).getAsString(),
                                            metricObject.get(KruizeConstants.JSONKeys.UUID).getAsString(),
                                            metricObject.get(KruizeConstants.JSONKeys.DEVICE).getAsString(),
                                            isSupportedMig);


                                    if (null == containerData.getContainerDeviceList()) {
                                        ContainerDeviceList containerDeviceList = new ContainerDeviceList();
                                        containerData.setContainerDeviceList(containerDeviceList);
                                    }
                                    containerData.getContainerDeviceList().addDevice(AnalyzerConstants.DeviceType.GPU, gpuDeviceData);
                                    // TODO: Currently we consider only the first mig supported GPU
                                    return;
                                }
                            }
                        }
                    } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException |
                             JsonSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        public static boolean checkIfModelIsKruizeSupportedMIG(String modelName) {
            if (null == modelName || modelName.isEmpty())
                return false;

            modelName = modelName.toUpperCase();

            boolean A100_CHECK = (modelName.contains("A100") &&
                    (modelName.contains("40GB") || modelName.contains("80GB")));

            boolean H100_CHECK = false;

            if (!A100_CHECK) {
                H100_CHECK = (modelName.contains("H100") && modelName.contains("80GB"));
            }

            return A100_CHECK || H100_CHECK;
        }
    }
}
