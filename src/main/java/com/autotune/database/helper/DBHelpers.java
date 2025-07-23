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

package com.autotune.database.helper;

import com.autotune.analyzer.adapters.DeviceDetailsAdapter;
import com.autotune.analyzer.adapters.MetricMetadataAdapter;
import com.autotune.analyzer.adapters.RecommendationItemAdapter;
import com.autotune.analyzer.exceptions.InvalidConversionOfRecommendationEntryException;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.kruizeObject.SloInfo;
import com.autotune.analyzer.metadataProfiles.MetadataProfile;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.recommendations.NamespaceRecommendations;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForTimestamp;
import com.autotune.analyzer.serviceObjects.*;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.ExperimentTypeUtil;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.auth.AuthenticationConfig;
import com.autotune.common.data.dataSourceMetadata.*;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.metrics.MetricMetadata;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.data.result.NamespaceData;
import com.autotune.common.data.system.info.device.DeviceDetails;
import com.autotune.common.datasource.DataSourceCollection;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceMetadataOperator;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.database.table.*;
import com.autotune.database.table.lm.KruizeLMExperimentEntry;
import com.autotune.database.table.lm.KruizeLMMetadataProfileEntry;
import com.autotune.database.table.lm.KruizeLMRecommendationEntry;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.autotune.analyzer.experiment.ExperimentInitiator.getErrorMap;
import static com.autotune.utils.KruizeConstants.KRUIZE_BULK_API.JOB_ID;

/**
 * Helper functions used by the DB to create entity objects.
 */
public class DBHelpers {
    private static final Logger LOGGER = LoggerFactory.getLogger(DBHelpers.class);


    private DBHelpers() {
    }

    // Sets the recommendations in recommendation entry to kruize object
    // The caller should call the function in a try / catch block to catch and act on the
    // InvalidConversionOfRecommendationEntryException
    public static void setRecommendationsToKruizeObject(List<ListRecommendationsAPIObject> listRecommendationsAPIObjectList,
                                                        KruizeObject kruizeObject)
            throws InvalidConversionOfRecommendationEntryException {

        if (null == listRecommendationsAPIObjectList) {
            // Throw an exception stating it cannot be null
            throw new InvalidConversionOfRecommendationEntryException(
                    String.format(
                            AnalyzerErrorConstants.ConversionErrors.KruizeRecommendationError.NOT_NULL,
                            "List Recommendation API objects "
                    )
            );
        }

        if (listRecommendationsAPIObjectList.isEmpty()) {
            // Throw an exception stating it cannot be null
            throw new InvalidConversionOfRecommendationEntryException(
                    String.format(
                            AnalyzerErrorConstants.ConversionErrors.KruizeRecommendationError.NOT_EMPTY,
                            "List Recommendation API objects "
                    )
            );
        }

        // Check if instance of KruizeObject is null
        if (null == kruizeObject) {
            // Throw an exception stating it cannot be null
            throw new InvalidConversionOfRecommendationEntryException(
                    String.format(
                            AnalyzerErrorConstants.ConversionErrors.KruizeRecommendationError.NOT_NULL,
                            KruizeObject.class.getSimpleName()
                    )
            );
        }

        // Check if the kubernetes objects is null
        if (null == kruizeObject.getKubernetes_objects()) {
            // Throw an exception stating it cannot be null
            throw new InvalidConversionOfRecommendationEntryException(
                    String.format(
                            AnalyzerErrorConstants.ConversionErrors.KruizeRecommendationError.NOT_NULL,
                            "Kubernetes Objects List"
                    )
            );
        }

        // Check if the kubernetes objects is empty
        if (kruizeObject.getKubernetes_objects().isEmpty()) {
            // Throw an exception stating it cannot be null
            throw new InvalidConversionOfRecommendationEntryException(
                    String.format(
                            AnalyzerErrorConstants.ConversionErrors.KruizeRecommendationError.NOT_EMPTY,
                            "Kubernetes Objects List"
                    )
            );
        }

        // Iterate over existing kubernetes objects in kruize object
        for (K8sObject k8sObject : kruizeObject.getKubernetes_objects()) {
            HashMap<String, ContainerData> containerDataMap = k8sObject.getContainerDataMap();
            // Check if container data map is not null
            if (null == containerDataMap) {
                throw new InvalidConversionOfRecommendationEntryException(
                        String.format(
                                AnalyzerErrorConstants.ConversionErrors.KruizeRecommendationError.NOT_NULL,
                                "Container data map in Kruize Object"
                        )
                );
            }

            // Check if Container data map and namespace data both are not empty
            if (containerDataMap.isEmpty() && (!k8sObject.getNamespaceDataMap().isEmpty() &&  null == k8sObject.getNamespaceDataMap())) {
                throw new InvalidConversionOfRecommendationEntryException(
                        String.format(
                                AnalyzerErrorConstants.ConversionErrors.KruizeRecommendationError.NOT_EMPTY,
                                "Container data map in Kruize Object"
                        )
                );
            }
        }

        for (ListRecommendationsAPIObject listRecommendationsAPIObject : listRecommendationsAPIObjectList) {
            if (null == listRecommendationsAPIObject) {
                // Throw an exception stating that List Recommendation object cannot be null
                throw new InvalidConversionOfRecommendationEntryException(
                        String.format(
                                AnalyzerErrorConstants.ConversionErrors.KruizeRecommendationError.NOT_NULL,
                                ListRecommendationsAPIObject.class.getSimpleName()
                        )
                );
            }

            // Check Kubernetes API Object
            if (null == listRecommendationsAPIObject.getKubernetesObjects()) {
                // Throw an exception stating that List of k8s objects cannot be null
                throw new InvalidConversionOfRecommendationEntryException(
                        String.format(
                                AnalyzerErrorConstants.ConversionErrors.KruizeRecommendationError.NOT_NULL,
                                KubernetesAPIObject.class.getSimpleName()
                        )
                );
            }

            // Check if the list size is greater than 0
            if (listRecommendationsAPIObject.getKubernetesObjects().isEmpty()) {
                // Throw an exception stating that List of k8s objects cannot be null
                throw new InvalidConversionOfRecommendationEntryException(
                        String.format(
                                AnalyzerErrorConstants.ConversionErrors.KruizeRecommendationError.NOT_EMPTY,
                                KubernetesAPIObject.class.getSimpleName()
                        )
                );
            }

            // Store the obtained list of kubernetes API Objects in a local list
            List<KubernetesAPIObject> kubernetesAPIObjectList = listRecommendationsAPIObject.getKubernetesObjects();

            for (KubernetesAPIObject kubernetesAPIObject : kubernetesAPIObjectList) {
                // Check for null
                if (null == kubernetesAPIObject.getContainerAPIObjects() && null == kubernetesAPIObject.getNamespaceAPIObject()) {
                    throw new InvalidConversionOfRecommendationEntryException(
                            String.format(
                                    AnalyzerErrorConstants.ConversionErrors.KruizeRecommendationError.NOT_NULL,
                                    "List of " + ContainerAPIObject.class.getSimpleName()
                            )
                    );
                }
                // Check for empty list
                if (kubernetesAPIObject.getContainerAPIObjects().isEmpty() && (kubernetesAPIObject.getNamespaceAPIObject() == null ||
                        kubernetesAPIObject.getNamespaceAPIObject().getNamespace() == null ||
                        kubernetesAPIObject.getNamespaceAPIObject().getNamespace().isEmpty())) {
                    throw new InvalidConversionOfRecommendationEntryException(
                            String.format(
                                    AnalyzerErrorConstants.ConversionErrors.KruizeRecommendationError.NOT_EMPTY,
                                    "List of " + ContainerAPIObject.class.getSimpleName()
                            )
                    );
                }
                for (K8sObject k8sObject : kruizeObject.getKubernetes_objects()) {
                    if (null == kubernetesAPIObject.getName()) {
                        // namespace recommendations experiment type
                        NamespaceAPIObject namespaceAPIObject =kubernetesAPIObject.getNamespaceAPIObject();
                        if (null == namespaceAPIObject)
                            continue;

                        String namespaceName = namespaceAPIObject.getNamespace();
                        if (!k8sObject.getNamespaceDataMap().containsKey(namespaceName))
                            continue;

                        NamespaceData namespaceData = k8sObject.getNamespaceDataMap().get(namespaceName);

                        // Set namespace recommendations
                        if (null == namespaceAPIObject.getNamespaceRecommendations())
                            continue;
                        if (null == namespaceAPIObject.getNamespaceRecommendations().getData())
                            continue;
                        if (null == namespaceData.getNamespaceRecommendations()) {
                            namespaceData.setNamespaceRecommendations(Utils.getClone(namespaceAPIObject.getNamespaceRecommendations(), NamespaceRecommendations.class));
                        } else {
                            NamespaceRecommendations namespaceRecommendations = namespaceData.getNamespaceRecommendations();
                            namespaceRecommendations.setVersion(listRecommendationsAPIObject.getApiVersion());
                            if (null == namespaceRecommendations.getData()) {
                                namespaceData.setNamespaceRecommendations(Utils.getClone(namespaceAPIObject.getNamespaceRecommendations(), NamespaceRecommendations.class));
                            } else {
                                namespaceRecommendations.getNotificationMap().clear();
                                namespaceRecommendations.getNotificationMap().putAll(namespaceAPIObject.getNamespaceRecommendations().getNotificationMap());
                                HashMap<Timestamp, MappedRecommendationForTimestamp> data = namespaceRecommendations.getData();
                                data.putAll(namespaceAPIObject.getNamespaceRecommendations().getData());
                            }
                        }
                    } else {
                        // container recommendations experiment type
                        if (kubernetesAPIObject.getName().equalsIgnoreCase(k8sObject.getName())
                                && kubernetesAPIObject.getType().equalsIgnoreCase(k8sObject.getType())) {
                            for (ContainerAPIObject containerAPIObject : kubernetesAPIObject.getContainerAPIObjects()) {
                                String containerName = containerAPIObject.getContainer_name();
                                // Skip the record if the data map doesn't have the container
                                if (!k8sObject.getContainerDataMap().containsKey(containerName))
                                    continue;

                                ContainerData containerData = k8sObject.getContainerDataMap().get(containerName);

                                // Set container recommendations
                                if (null == containerAPIObject.getContainerRecommendations())
                                    continue;
                                if (null == containerAPIObject.getContainerRecommendations().getData())
                                    continue;
                                if (null == containerData.getContainerRecommendations()) {
                                    containerData.setContainerRecommendations(Utils.getClone(containerAPIObject.getContainerRecommendations(), ContainerRecommendations.class));
                                } else {
                                    ContainerRecommendations containerRecommendations = containerData.getContainerRecommendations();
                                    containerRecommendations.setVersion(listRecommendationsAPIObject.getApiVersion());
                                    if (null == containerRecommendations.getData()) {
                                        containerData.setContainerRecommendations(Utils.getClone(containerAPIObject.getContainerRecommendations(), ContainerRecommendations.class));
                                    } else {
                                        containerRecommendations.getNotificationMap().clear();
                                        containerRecommendations.getNotificationMap().putAll(containerAPIObject.getContainerRecommendations().getNotificationMap());
                                        HashMap<Timestamp, MappedRecommendationForTimestamp> data = containerRecommendations.getData();
                                        data.putAll(containerAPIObject.getContainerRecommendations().getData());
                                    }
                                }

                            }
                        }
                    }

                }
            }
        }
    }

    /**
     * Retrieves an existing DataSource from the DB entry or creates a new one if not found.
     *
     * @param kruizeMetadata         KruizeDSMetadataEntry object
     * @param dataSourceMetadataInfo DataSourceMetadataInfo object
     * @return The DataSource instance associated with the DB entry.
     */
    private static DataSource getOrCreateDataSourceFromDB(KruizeDSMetadataEntry kruizeMetadata, DataSourceMetadataInfo dataSourceMetadataInfo) {
        String dataSourceName = kruizeMetadata.getDataSourceName();

        // Check if the data source already exists
        if (dataSourceMetadataInfo.getDatasources().containsKey(dataSourceName)) {
            return dataSourceMetadataInfo.getDatasources().get(dataSourceName);
        }

        DataSource dataSource = new DataSource(dataSourceName, new HashMap<>());
        dataSourceMetadataInfo.getDatasources().put(dataSourceName, dataSource);

        return dataSource;
    }

    /**
     * Retrieves an existing DataSourceCluster from the DB entry or creates a new one if not found.
     *
     * @param kruizeMetadata KruizeDSMetadataEntry object
     * @param dataSource     DataSource object
     * @return The DataSourceCluster instance associated with the DB entry
     */
    private static DataSourceCluster getOrCreateDataSourceClusterFromDB(KruizeDSMetadataEntry kruizeMetadata, DataSource dataSource) {
        String clusterName = kruizeMetadata.getClusterName();

        // Check if the cluster already exists in the DataSource
        if (dataSource.getClusters().containsKey(clusterName)) {
            return dataSource.getClusters().get(clusterName);
        }

        DataSourceCluster dataSourceCluster = new DataSourceCluster(clusterName, new HashMap<>());
        dataSource.getClusters().put(clusterName, dataSourceCluster);

        return dataSourceCluster;
    }

    /**
     * Retrieves an existing DataSourceNamespace from the DB entry or creates a new one if not found.
     *
     * @param kruizeMetadata    KruizeDSMetadataEntry object
     * @param dataSourceCluster DataSourceCluster object
     * @return The DataSourceNamespace instance associated with the DB entry
     */
    private static DataSourceNamespace getOrCreateDataSourceNamespaceFromDB(KruizeDSMetadataEntry kruizeMetadata, DataSourceCluster dataSourceCluster) {
        String namespaceName = kruizeMetadata.getNamespace();

        // Check if the namespace already exists in the DataSourceCluster
        if (dataSourceCluster.getNamespaces().containsKey(namespaceName)) {
            return dataSourceCluster.getNamespaces().get(namespaceName);
        }

        DataSourceNamespace dataSourceNamespace = new DataSourceNamespace(namespaceName, new HashMap<>());
        dataSourceCluster.getNamespaces().put(namespaceName, dataSourceNamespace);

        return dataSourceNamespace;
    }

    /**
     * Retrieves an existing DataSourceWorkload from the DB entry or creates a new one if not found.
     *
     * @param kruizeMetadata      KruizeDSMetadataEntry object
     * @param dataSourceNamespace DataSourceNamespace object
     * @return The DataSourceWorkload instance associated with the DB entry
     */
    private static DataSourceWorkload getOrCreateDataSourceWorkloadFromDB(KruizeDSMetadataEntry kruizeMetadata, DataSourceNamespace dataSourceNamespace) {
        String workloadName = kruizeMetadata.getWorkloadName();

        if (null == workloadName) {
            return null;
        }

        // Check if the workload already exists in the DataSourceNamespace
        if (dataSourceNamespace.getWorkloads().containsKey(workloadName)) {
            return dataSourceNamespace.getWorkloads().get(workloadName);
        }

        DataSourceWorkload dataSourceWorkload = new DataSourceWorkload(workloadName, kruizeMetadata.getWorkloadType(), new HashMap<>());
        dataSourceNamespace.getWorkloads().put(workloadName, dataSourceWorkload);

        return dataSourceWorkload;
    }

    /**
     * Retrieves an existing DataSourceContainer from the DB entry or creates a new one if not found.
     *
     * @param kruizeMetadata     KruizeDSMetadataEntry object
     * @param dataSourceWorkload DataSourceWorkload object
     * @return The DataSourceContainer instance associated with the DB entry
     */
    private static DataSourceContainer getOrCreateDataSourceContainerFromDB(KruizeDSMetadataEntry kruizeMetadata, DataSourceWorkload dataSourceWorkload) {
        String containerName = kruizeMetadata.getContainerName();

        if (null == containerName) {
            return null;
        }

        // Check if the container already exists in the DataSourceWorkload
        if (dataSourceWorkload.getContainers().containsKey(containerName)) {
            return dataSourceWorkload.getContainers().get(containerName);
        }

        DataSourceContainer dataSourceContainer = new DataSourceContainer(containerName, kruizeMetadata.getContainerImageName());
        dataSourceWorkload.getContainers().put(containerName, dataSourceContainer);

        return dataSourceContainer;
    }

    private static KruizeDSMetadataEntry getMetadata(String datasource) {
        DataSourceMetadataOperator dataSourceMetadataOperator = DataSourceMetadataOperator.getInstance();
        HashMap<String, DataSourceInfo> dataSources = DataSourceCollection.getInstance().getDataSourcesCollection();
        DataSourceMetadataInfo dataSourceMetadataInfo = dataSourceMetadataOperator.getDataSourceMetadataInfo(dataSources.get(datasource));
        List<KruizeDSMetadataEntry> kruizeMetadataList = Converters.KruizeObjectConverters.convertDataSourceMetadataToMetadataObj(dataSourceMetadataInfo);
        if (kruizeMetadataList.isEmpty())
            return null;
        else
            return kruizeMetadataList.get(0);
    }

    public static class Converters {
        private Converters() {

        }


        public static class KruizeObjectConverters {
            private KruizeObjectConverters() {

            }

            /**
             * @param apiObject
             * @return KruizeExperimentEntry
             * This methode facilitate to store data into db by accumulating required data from KruizeObject.
             */
            public static KruizeLMExperimentEntry convertCreateAPIObjToExperimentDBObj(CreateExperimentAPIObject apiObject) {
                KruizeLMExperimentEntry kruizeLMExperimentEntry = null;
                try {
                    kruizeLMExperimentEntry = new KruizeLMExperimentEntry();
                    kruizeLMExperimentEntry.setExperiment_name(apiObject.getExperimentName());
                    kruizeLMExperimentEntry.setExperiment_id(Utils.generateID(apiObject));
                    kruizeLMExperimentEntry.setCluster_name(apiObject.getClusterName());
                    kruizeLMExperimentEntry.setMode(apiObject.getMode());
                    kruizeLMExperimentEntry.setPerformance_profile(apiObject.getPerformanceProfile());
                    kruizeLMExperimentEntry.setMetadata_profile(apiObject.getMetadataProfile());
                    kruizeLMExperimentEntry.setVersion(apiObject.getApiVersion());
                    kruizeLMExperimentEntry.setTarget_cluster(apiObject.getTargetCluster());
                    kruizeLMExperimentEntry.setStatus(AnalyzerConstants.ExperimentStatus.IN_PROGRESS);
                    kruizeLMExperimentEntry.setMeta_data(null);
                    kruizeLMExperimentEntry.setDatasource(null);
                    kruizeLMExperimentEntry.setExperiment_type(apiObject.getExperimentType());

                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        kruizeLMExperimentEntry.setExtended_data(
                                objectMapper.readTree(
                                        new Gson().toJson(apiObject)
                                )
                        );
                    } catch (JsonProcessingException e) {
                        throw new Exception("Error while creating Extended data due to : " + e.getMessage());
                    }
                } catch (Exception e) {
                    kruizeLMExperimentEntry = null;
                    LOGGER.error("Error while converting Kruize Object to experimentDetailTable due to {}", e.getMessage());
                    e.printStackTrace();
                }
                return kruizeLMExperimentEntry;
            }

            /**
             * @param experimentResultData
             * @return KruizeResultsEntry
             * This methode facilitate to store data into db by accumulating required data from ExperimentResultData.
             */
            public static KruizeResultsEntry convertExperimentResultToExperimentResultsTable(ExperimentResultData experimentResultData) {
                KruizeResultsEntry kruizeResultsEntry = null;
                Gson gson = new GsonBuilder()
                        .disableHtmlEscaping()
                        .setPrettyPrinting()
                        .enableComplexMapKeySerialization()
                        .setDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT)
                        .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                        .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                        .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
                        .registerTypeAdapter(MetricMetadata.class, new MetricMetadataAdapter())
                        .create();
                try {
                    kruizeResultsEntry = new KruizeResultsEntry();
                    kruizeResultsEntry.setVersion(experimentResultData.getVersion());
                    kruizeResultsEntry.setExperiment_name(experimentResultData.getExperiment_name());
                    kruizeResultsEntry.setCluster_name(experimentResultData.getCluster_name());
                    kruizeResultsEntry.setInterval_start_time(experimentResultData.getIntervalStartTime());
                    kruizeResultsEntry.setInterval_end_time(experimentResultData.getIntervalEndTime());
                    kruizeResultsEntry.setDuration_minutes(
                            Double.valueOf((experimentResultData.getIntervalEndTime().getTime() -
                                    experimentResultData.getIntervalStartTime().getTime()) / (60 * 1000))
                    );
                    Map<String, List<K8sObject>> k8sObjectsMap = Map.of(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS, experimentResultData.getKubernetes_objects());
                    String k8sObjectString = gson.toJson(k8sObjectsMap);
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        kruizeResultsEntry.setExtended_data(
                                objectMapper.readTree(
                                        k8sObjectString
                                )
                        );
                    } catch (JsonProcessingException e) {
                        throw new Exception("Error while creating Extended data due to : " + e.getMessage());
                    }

                } catch (Exception e) {
                    kruizeResultsEntry = null;
                    LOGGER.error("Error while converting ExperimentResultData to ExperimentResultsTable due to {}", e.getMessage());
                    e.printStackTrace();
                }
                return kruizeResultsEntry;
            }

            public static KruizeRecommendationEntry convertKruizeObjectTORecommendation(KruizeObject kruizeObject, Timestamp monitoringEndTime) {
                KruizeRecommendationEntry kruizeRecommendationEntry = null;
                Boolean checkForTimestamp = false;
                Boolean getLatest = true;
                Gson gson = new GsonBuilder()
                        .disableHtmlEscaping()
                        .setPrettyPrinting()
                        .enableComplexMapKeySerialization()
                        .setDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT)
                        .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                        .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                        .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
                        .registerTypeAdapter(MetricMetadata.class, new MetricMetadataAdapter())
                        .create();
                try {
                    ListRecommendationsAPIObject listRecommendationsAPIObject = getListRecommendationAPIObjectForDB(
                            kruizeObject, monitoringEndTime);
                    if (null == listRecommendationsAPIObject) {
                        return null;
                    }
                    LOGGER.debug(new GsonBuilder()
                            .setPrettyPrinting()
                            .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                            .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
                            .registerTypeAdapter(MetricMetadata.class, new MetricMetadataAdapter())
                            .create()
                            .toJson(listRecommendationsAPIObject));
                    kruizeRecommendationEntry = new KruizeRecommendationEntry();
                    kruizeRecommendationEntry.setVersion(KruizeConstants.KRUIZE_RECOMMENDATION_API_VERSION.LATEST.getVersionNumber());
                    kruizeRecommendationEntry.setExperiment_name(listRecommendationsAPIObject.getExperimentName());
                    kruizeRecommendationEntry.setCluster_name(listRecommendationsAPIObject.getClusterName());
                    //kruizeRecommendationEntry.setExperimentType(listRecommendationsAPIObject.getExperimentType());

                    Timestamp endInterval = null;
                    // todo : what happens if two k8 objects or Containers with different timestamp
                    for (KubernetesAPIObject k8sObject : listRecommendationsAPIObject.getKubernetesObjects()) {
                        if (listRecommendationsAPIObject.isNamespaceExperiment()) {
                            endInterval = k8sObject.getNamespaceAPIObject().getNamespaceRecommendations().getData().keySet().stream().max(Timestamp::compareTo).get();
                        } else {
                            for (ContainerAPIObject containerAPIObject : k8sObject.getContainerAPIObjects()) {
                                endInterval = containerAPIObject.getContainerRecommendations().getData().keySet().stream().max(Timestamp::compareTo).get();
                                break;
                            }
                        }
                    }
                    kruizeRecommendationEntry.setInterval_end_time(endInterval);
                    Map k8sObjectsMap = Map.of(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS, listRecommendationsAPIObject.getKubernetesObjects());
                    String k8sObjectString = gson.toJson(k8sObjectsMap);
                    ObjectMapper objectMapper = new ObjectMapper();
                    DateFormat df = new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT);
                    objectMapper.setDateFormat(df);
                    try {
                        kruizeRecommendationEntry.setExtended_data(
                                objectMapper.readTree(
                                        k8sObjectString
                                )
                        );
                    } catch (JsonProcessingException e) {
                        throw new Exception("Error while creating Extended data due to : " + e.getMessage());
                    }
                } catch (Exception e) {
                    kruizeRecommendationEntry = null;
                    LOGGER.error("Error while converting KruizeObject to KruizeRecommendationEntry due to {}", e.getMessage());
                    e.printStackTrace();
                }
                return kruizeRecommendationEntry;
            }


            public static KruizeLMRecommendationEntry convertKruizeObjectTOLMRecommendation(KruizeObject kruizeObject, Timestamp monitoringEndTime) {
                KruizeLMRecommendationEntry kruizeRecommendationEntry = null;
                Boolean checkForTimestamp = false;
                Boolean getLatest = true;
                Gson gson = new GsonBuilder()
                        .disableHtmlEscaping()
                        .setPrettyPrinting()
                        .enableComplexMapKeySerialization()
                        .setDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT)
                        .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                        .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                        .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
                        .registerTypeAdapter(MetricMetadata.class, new MetricMetadataAdapter())
                        .create();
                try {
                    ListRecommendationsAPIObject listRecommendationsAPIObject = getListRecommendationAPIObjectForDB(
                            kruizeObject, monitoringEndTime);
                    if (null == listRecommendationsAPIObject) {
                        return null;
                    }
                    LOGGER.debug(new GsonBuilder()
                            .setPrettyPrinting()
                            .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                            .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
                            .registerTypeAdapter(MetricMetadata.class, new MetricMetadataAdapter())
                            .create()
                            .toJson(listRecommendationsAPIObject));
                    kruizeRecommendationEntry = new KruizeLMRecommendationEntry();
                    kruizeRecommendationEntry.setVersion(KruizeConstants.KRUIZE_RECOMMENDATION_API_VERSION.LATEST.getVersionNumber());
                    kruizeRecommendationEntry.setExperiment_name(listRecommendationsAPIObject.getExperimentName());
                    kruizeRecommendationEntry.setCluster_name(listRecommendationsAPIObject.getClusterName());
                    kruizeRecommendationEntry.setExperimentType(ExperimentTypeUtil.getExperimentTypeFromBitMask(kruizeObject.getExperimentType()).name());

                    Timestamp endInterval = null;
                    // todo : what happens if two k8 objects or Containers with different timestamp
                    for (KubernetesAPIObject k8sObject : listRecommendationsAPIObject.getKubernetesObjects()) {
                        if (listRecommendationsAPIObject.isNamespaceExperiment()) {
                            endInterval = k8sObject.getNamespaceAPIObject().getNamespaceRecommendations().getData().keySet().stream().max(Timestamp::compareTo).get();
                        } else {
                            for (ContainerAPIObject containerAPIObject : k8sObject.getContainerAPIObjects()) {
                                endInterval = containerAPIObject.getContainerRecommendations().getData().keySet().stream().max(Timestamp::compareTo).get();
                                break;
                            }
                        }
                    }
                    kruizeRecommendationEntry.setInterval_end_time(endInterval);
                    Map<String, Object> k8sObjectsMap = new HashMap<>();
                    k8sObjectsMap.put(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS, listRecommendationsAPIObject.getKubernetesObjects());
                    if (null != kruizeObject.getBulkJobId())
                        k8sObjectsMap.put(JOB_ID, kruizeObject.getBulkJobId());
                    String k8sObjectString = gson.toJson(k8sObjectsMap);
                    ObjectMapper objectMapper = new ObjectMapper();
                    DateFormat df = new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT);
                    objectMapper.setDateFormat(df);
                    try {
                        kruizeRecommendationEntry.setExtended_data(
                                objectMapper.readTree(
                                        k8sObjectString
                                )
                        );
                    } catch (JsonProcessingException e) {
                        throw new Exception("Error while creating Extended data due to : " + e.getMessage());
                    }
                } catch (Exception e) {
                    kruizeRecommendationEntry = null;
                    LOGGER.error("Error while converting KruizeObject to KruizeRecommendationEntry due to {}", e.getMessage());
                    e.printStackTrace();
                }
                return kruizeRecommendationEntry;
            }

            public static ListRecommendationsAPIObject getListRecommendationAPIObjectForDB(KruizeObject kruizeObject, Timestamp monitoringEndTime) {
                if (null == kruizeObject)
                    return null;
                if (null == monitoringEndTime)
                    return null;
                if (null == kruizeObject.getKubernetes_objects())
                    return null;
                if (kruizeObject.getKubernetes_objects().isEmpty())
                    return null;
                List<KubernetesAPIObject> kubernetesAPIObjectList = new ArrayList<>();
                for (K8sObject k8sObject : kruizeObject.getKubernetes_objects()) {
                    if (null == k8sObject)
                        continue;
                    if (null == k8sObject.getContainerDataMap() && kruizeObject.isContainerExperiment())
                        continue;
                    if (k8sObject.getContainerDataMap().isEmpty() && kruizeObject.isContainerExperiment())
                        continue;
                    KubernetesAPIObject kubernetesAPIObject = new KubernetesAPIObject(k8sObject.getName(), k8sObject.getType(), k8sObject.getNamespace());
                    boolean matchFound = false;
                    if (kruizeObject.isNamespaceExperiment()) {
                        // saving namespace recommendations
                        NamespaceData clonedNamespaceData = Utils.getClone(k8sObject.getNamespaceDataMap().get(k8sObject.getNamespace()), NamespaceData.class);
                        if (null == clonedNamespaceData)
                            continue;
                        if (null == clonedNamespaceData.getNamespaceRecommendations())
                            continue;
                        if (null == clonedNamespaceData.getNamespaceRecommendations().getData())
                            continue;
                        if (clonedNamespaceData.getNamespaceRecommendations().getData().isEmpty())
                            continue;
                        HashMap<Timestamp, MappedRecommendationForTimestamp> namespaceRecommendations = clonedNamespaceData.getNamespaceRecommendations().getData();

                        if (null != monitoringEndTime && namespaceRecommendations.containsKey(monitoringEndTime)) {
                            matchFound = true;
                            NamespaceAPIObject namespaceAPIObject = null;
                            List<Timestamp> tempList = new ArrayList<>();
                            for (Timestamp timestamp : namespaceRecommendations.keySet()) {
                                if (!timestamp.equals(monitoringEndTime))
                                    tempList.add(timestamp);
                            }
                            for (Timestamp timestamp : tempList) {
                                namespaceRecommendations.remove(timestamp);
                            }
                            clonedNamespaceData.getNamespaceRecommendations().setData(namespaceRecommendations);
                            namespaceAPIObject = new NamespaceAPIObject(clonedNamespaceData.getNamespace_name(),
                                    clonedNamespaceData.getNamespaceRecommendations(),
                                    null);
                            kubernetesAPIObject.setNamespaceAPIObject(namespaceAPIObject);
                        }
                    }

                    List<ContainerAPIObject> containerAPIObjectList = new ArrayList<>();
                    for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
                        ContainerData clonedContainerData = Utils.getClone(containerData, ContainerData.class);
                        if (null == clonedContainerData.getContainerRecommendations())
                            continue;
                        if (null == clonedContainerData.getContainerRecommendations().getData())
                            continue;
                        if (clonedContainerData.getContainerRecommendations().getData().isEmpty())
                            continue;
                        HashMap<Timestamp, MappedRecommendationForTimestamp> recommendations
                                = clonedContainerData.getContainerRecommendations().getData();
                        if (null != monitoringEndTime && !recommendations.containsKey(monitoringEndTime)) {
                            try {
                                Timestamp endInterval = containerData.getContainerRecommendations().getData().keySet().stream().max(Timestamp::compareTo).get();
                                monitoringEndTime = endInterval;
                            } catch (Exception e) {
                                LOGGER.error("Error while converting ContainerData to Timestamp due to and not able to save date into recommendation table: " + e.getMessage());
                            }
                        }
                        if (null != monitoringEndTime && recommendations.containsKey(monitoringEndTime)) {
                            matchFound = true;
                            ContainerAPIObject containerAPIObject = null;
                            List<Timestamp> tempList = new ArrayList<>();
                            for (Timestamp timestamp : recommendations.keySet()) {
                                if (!timestamp.equals(monitoringEndTime))
                                    tempList.add(timestamp);
                            }
                            for (Timestamp timestamp : tempList) {
                                recommendations.remove(timestamp);
                            }
                            clonedContainerData.getContainerRecommendations().setData(recommendations);
                            containerAPIObject = new ContainerAPIObject(clonedContainerData.getContainer_name(),
                                    clonedContainerData.getContainer_image_name(),
                                    clonedContainerData.getContainerRecommendations(),
                                    null);
                            containerAPIObjectList.add(containerAPIObject);
                        }
                    }
                    kubernetesAPIObject.setContainerAPIObjects(containerAPIObjectList);
                    if (matchFound) {
                        kubernetesAPIObjectList.add(kubernetesAPIObject);
                    }
                }
                ListRecommendationsAPIObject listRecommendationsAPIObject = null;
                if (!kubernetesAPIObjectList.isEmpty()) {
                    listRecommendationsAPIObject = new ListRecommendationsAPIObject();
                    listRecommendationsAPIObject.setClusterName(kruizeObject.getClusterName());
                    listRecommendationsAPIObject.setExperimentName(kruizeObject.getExperimentName());
                    listRecommendationsAPIObject.setKubernetesObjects(kubernetesAPIObjectList);
                    listRecommendationsAPIObject.setExperimentType(ExperimentTypeUtil.getExperimentTypeFromBitMask(kruizeObject.getExperimentType()));
                }
                return listRecommendationsAPIObject;
            }


            public static List<CreateExperimentAPIObject> convertLMExperimentEntryToCreateExperimentAPIObject(List<KruizeLMExperimentEntry> entries) throws Exception {
                List<CreateExperimentAPIObject> createExperimentAPIObjects = new ArrayList<>();
                int failureThreshHold = entries.size();
                int failureCount = 0;
                for (KruizeLMExperimentEntry entry : entries) {
                    try {
                        JsonNode extended_data = entry.getExtended_data();
                        String extended_data_rawJson = extended_data.toString();
                        CreateExperimentAPIObject apiObj = new Gson().fromJson(extended_data_rawJson, CreateExperimentAPIObject.class);
                        apiObj.setExperiment_id(entry.getExperiment_id());
                        apiObj.setStatus(entry.getStatus());
                        apiObj.setTargetCluster(entry.getTarget_cluster());
                        apiObj.setMode(entry.getMode());
                        apiObj.setExperimentType(entry.getExperiment_type());
                        createExperimentAPIObjects.add(apiObj);
                    } catch (Exception e) {
                        LOGGER.error("Error in converting to apiObj from db object due to : {}", e.getMessage());
                        LOGGER.error(entry.toString());
                        failureCount++;
                    }
                }
                if (failureThreshHold > 0 && failureCount == failureThreshHold)
                    throw new Exception("None of the experiments are able to load from DB.");

                return createExperimentAPIObjects;
            }

            public static List<CreateExperimentAPIObject> convertExperimentEntryToCreateExperimentAPIObject(List<KruizeExperimentEntry> entries) throws Exception {
                List<CreateExperimentAPIObject> createExperimentAPIObjects = new ArrayList<>();
                int failureThreshHold = entries.size();
                int failureCount = 0;
                for (KruizeExperimentEntry entry : entries) {
                    try {
                        JsonNode extended_data = entry.getExtended_data();
                        String extended_data_rawJson = extended_data.toString();
                        CreateExperimentAPIObject apiObj = new Gson().fromJson(extended_data_rawJson, CreateExperimentAPIObject.class);
                        apiObj.setExperiment_id(entry.getExperiment_id());
                        apiObj.setStatus(entry.getStatus());
                        apiObj.setExperimentType(ExperimentTypeUtil.getExperimentTypeFromBitMask(entry.getExperiment_type()));
                        createExperimentAPIObjects.add(apiObj);
                    } catch (Exception e) {
                        LOGGER.error("Error in converting to apiObj from db object due to : {}", e.getMessage());
                        LOGGER.error(entry.toString());
                        failureCount++;
                    }
                }
                if (failureThreshHold > 0 && failureCount == failureThreshHold)
                    throw new Exception("None of the experiments are able to load from DB.");

                return createExperimentAPIObjects;
            }


            public static List<UpdateResultsAPIObject> convertResultEntryToUpdateResultsAPIObject(List<KruizeResultsEntry> kruizeResultsEntries) {
                ObjectMapper mapper = new ObjectMapper();
                DateFormat df = new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT);
                mapper.setDateFormat(df);
                Gson gson = new GsonBuilder()
                        .disableHtmlEscaping()
                        .setPrettyPrinting()
                        .enableComplexMapKeySerialization()
                        .setDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT)
                        .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                        .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                        .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
                        .registerTypeAdapter(MetricMetadata.class, new MetricMetadataAdapter())
                        .create();
                List<UpdateResultsAPIObject> updateResultsAPIObjects = new ArrayList<>();
                for (KruizeResultsEntry kruizeResultsEntry : kruizeResultsEntries) {
                    try {
                        UpdateResultsAPIObject updateResultsAPIObject = new UpdateResultsAPIObject();
                        updateResultsAPIObject.setApiVersion(kruizeResultsEntry.getVersion());
                        updateResultsAPIObject.setExperimentName(kruizeResultsEntry.getExperiment_name());
                        updateResultsAPIObject.setStartTimestamp(kruizeResultsEntry.getInterval_start_time());
                        updateResultsAPIObject.setEndTimestamp(kruizeResultsEntry.getInterval_end_time());
                        updateResultsAPIObject.setErrors(getErrorMap(kruizeResultsEntry.getErrorReasons()));
                        JsonNode extendedDataNode = kruizeResultsEntry.getExtended_data();
                        JsonNode k8sObjectsNode = extendedDataNode.get(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS);
                        List<K8sObject> k8sObjectList = new ArrayList<>();
                        if (k8sObjectsNode.isArray()) {
                            for (JsonNode node : k8sObjectsNode) {
                                K8sObject k8sObject = gson.fromJson(mapper.writeValueAsString(node), K8sObject.class);
                                if (null != k8sObject) {
                                    k8sObjectList.add(k8sObject);
                                } else {
                                    LOGGER.debug("GSON failed to convert the DB Json object in convertResultEntryToUpdateResultsAPIObject");
                                }
                            }
                        }
                        List<KubernetesAPIObject> kubernetesAPIObjectList = convertK8sObjectListToKubernetesAPIObjectList(k8sObjectList);
                        updateResultsAPIObject.setKubernetesObjects(kubernetesAPIObjectList);
                        updateResultsAPIObjects.add(updateResultsAPIObject);
                    } catch (Exception e) {
                        LOGGER.error("Exception occurred while updating local storage: {}", e.getMessage());
                        e.printStackTrace();
                    }
                }
                return updateResultsAPIObjects;
            }

            private static List<KubernetesAPIObject> convertK8sObjectListToKubernetesAPIObjectList(List<K8sObject> k8sObjectList) throws JsonProcessingException {
                List<KubernetesAPIObject> kubernetesAPIObjects = new ArrayList<>();
                for (K8sObject k8sObject : k8sObjectList) {
                    KubernetesAPIObject kubernetesAPIObject = new KubernetesAPIObject(
                            k8sObject.getName(),
                            k8sObject.getType(),
                            k8sObject.getNamespace()
                    );
                    if (k8sObject.getContainerDataMap() != null && !k8sObject.getContainerDataMap().isEmpty()) {
                        List<ContainerAPIObject> containerAPIObjects = new ArrayList<>();
                        for (Map.Entry<String, ContainerData> entry : k8sObject.getContainerDataMap().entrySet()) {
                            containerAPIObjects.add(new ContainerAPIObject(
                                    entry.getKey(),
                                    entry.getValue().getContainer_image_name(),
                                    entry.getValue().getContainerRecommendations(),
                                    new ArrayList<>(entry.getValue().getMetrics().values())
                            ));
                        }
                        kubernetesAPIObject.setContainerAPIObjects(containerAPIObjects);
                    } else if (k8sObject.getNamespaceDataMap() != null && !k8sObject.getNamespaceDataMap().isEmpty()) {
                        kubernetesAPIObject.setNamespaceAPIObject(getNamespaceAPIObject(k8sObject));
                    }
                    kubernetesAPIObjects.add(kubernetesAPIObject);
                }
                return kubernetesAPIObjects;
            }

            /***
             * Extract the namespaceAPIObject object from the K8sObject object and return
             *
             * @param k8sObject Kubernetes Object from which the namespace data is extracted
             * @return Namespace object containing the namespace details
             */
            private static NamespaceAPIObject getNamespaceAPIObject(K8sObject k8sObject) {
                NamespaceAPIObject namespaceAPIObject = null;
                for (Map.Entry<String, NamespaceData> entry : k8sObject.getNamespaceDataMap().entrySet()) {
                    namespaceAPIObject = new NamespaceAPIObject(
                            entry.getKey(),
                            entry.getValue().getNamespaceRecommendations(),
                            new ArrayList<>(entry.getValue().getMetrics().values())
                    );
                }
                return namespaceAPIObject;
            }

            public static List<ListRecommendationsAPIObject> convertRecommendationEntryToRecommendationAPIObject(
                    List<KruizeRecommendationEntry> kruizeRecommendationEntryList) throws InvalidConversionOfRecommendationEntryException {
                if (null == kruizeRecommendationEntryList)
                    return null;
                if (kruizeRecommendationEntryList.size() == 0)
                    return null;
                Gson gson = new GsonBuilder()
                        .disableHtmlEscaping()
                        .setPrettyPrinting()
                        .enableComplexMapKeySerialization()
                        .setDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT)
                        .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                        .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                        .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
                        .registerTypeAdapter(MetricMetadata.class, new MetricMetadataAdapter())
                        .create();
                List<ListRecommendationsAPIObject> listRecommendationsAPIObjectList = new ArrayList<>();
                for (KruizeRecommendationEntry kruizeRecommendationEntry : kruizeRecommendationEntryList) {
                    // Check if instance of KruizeRecommendationEntry is null
                    if (null == kruizeRecommendationEntry) {
                        // Throw an exception stating it cannot be null
                        throw new InvalidConversionOfRecommendationEntryException(
                                String.format(
                                        AnalyzerErrorConstants.ConversionErrors.KruizeRecommendationError.NOT_NULL,
                                        KruizeRecommendationEntry.class.getSimpleName()
                                )
                        );
                    }
                    // Create an Object Mapper to extract value from JSON Node
                    ObjectMapper objectMapper = new ObjectMapper();
                    DateFormat df = new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT);
                    objectMapper.setDateFormat(df);
                    // Create a holder for recommendation object to save the result from object mapper
                    ListRecommendationsAPIObject listRecommendationsAPIObject = null;
                    JsonNode extendedData = kruizeRecommendationEntry.getExtended_data().get(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS);
                    if (null == extendedData)
                        continue;
                    try {
                        // If successful, the object mapper returns the list recommendation API Object
                        List<KubernetesAPIObject> kubernetesAPIObjectList = new ArrayList<>();
                        if (extendedData.isArray()) {
                            for (JsonNode node : extendedData) {
                                KubernetesAPIObject kubernetesAPIObject = gson.fromJson(objectMapper.writeValueAsString(node), KubernetesAPIObject.class);
                                if (null != kubernetesAPIObject) {
                                    kubernetesAPIObjectList.add(kubernetesAPIObject);
                                } else {
                                    LOGGER.debug("GSON failed to convert the DB Json object in convertRecommendationEntryToRecommendationAPIObject");
                                }
                            }
                        }
                        if (null != kubernetesAPIObjectList) {
                            listRecommendationsAPIObject = new ListRecommendationsAPIObject();
                            listRecommendationsAPIObject.setApiVersion(kruizeRecommendationEntry.getVersion());
                            listRecommendationsAPIObject.setKubernetesObjects(kubernetesAPIObjectList);
                            listRecommendationsAPIObject.setExperimentName(kruizeRecommendationEntry.getExperiment_name());
                            listRecommendationsAPIObject.setClusterName(kruizeRecommendationEntry.getCluster_name());
                        }
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        LOGGER.debug(e.getMessage());
                    }
                    if (null != listRecommendationsAPIObject)
                        listRecommendationsAPIObjectList.add(listRecommendationsAPIObject);
                }
                if (listRecommendationsAPIObjectList.isEmpty())
                    return null;
                return listRecommendationsAPIObjectList;
            }

            public static List<ListRecommendationsAPIObject> convertLMRecommendationEntryToRecommendationAPIObject(
                    List<KruizeLMRecommendationEntry> kruizeRecommendationEntryList) throws InvalidConversionOfRecommendationEntryException {
                if (null == kruizeRecommendationEntryList)
                    return null;
                if (kruizeRecommendationEntryList.size() == 0)
                    return null;
                Gson gson = new GsonBuilder()
                        .disableHtmlEscaping()
                        .setPrettyPrinting()
                        .enableComplexMapKeySerialization()
                        .setDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT)
                        .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                        .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                        .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
                        .registerTypeAdapter(MetricMetadata.class, new MetricMetadataAdapter())
                        .create();
                List<ListRecommendationsAPIObject> listRecommendationsAPIObjectList = new ArrayList<>();
                for (KruizeLMRecommendationEntry kruizeRecommendationEntry : kruizeRecommendationEntryList) {
                    // Check if instance of KruizeRecommendationEntry is null
                    if (null == kruizeRecommendationEntry) {
                        // Throw an exception stating it cannot be null
                        throw new InvalidConversionOfRecommendationEntryException(
                                String.format(
                                        AnalyzerErrorConstants.ConversionErrors.KruizeRecommendationError.NOT_NULL,
                                        KruizeRecommendationEntry.class.getSimpleName()
                                )
                        );
                    }
                    // Create an Object Mapper to extract value from JSON Node
                    ObjectMapper objectMapper = new ObjectMapper();
                    DateFormat df = new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT);
                    objectMapper.setDateFormat(df);
                    // Create a holder for recommendation object to save the result from object mapper
                    ListRecommendationsAPIObject listRecommendationsAPIObject = null;
                    JsonNode extendedData = kruizeRecommendationEntry.getExtended_data().get(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS);
                    if (null == extendedData)
                        continue;
                    try {
                        // If successful, the object mapper returns the list recommendation API Object
                        List<KubernetesAPIObject> kubernetesAPIObjectList = new ArrayList<>();
                        if (extendedData.isArray()) {
                            for (JsonNode node : extendedData) {
                                KubernetesAPIObject kubernetesAPIObject = gson.fromJson(objectMapper.writeValueAsString(node), KubernetesAPIObject.class);
                                if (null != kubernetesAPIObject) {
                                    kubernetesAPIObjectList.add(kubernetesAPIObject);
                                } else {
                                    LOGGER.debug("GSON failed to convert the DB Json object in convertRecommendationEntryToRecommendationAPIObject");
                                }
                            }
                        }
                        if (null != kubernetesAPIObjectList) {
                            listRecommendationsAPIObject = new ListRecommendationsAPIObject();
                            listRecommendationsAPIObject.setApiVersion(kruizeRecommendationEntry.getVersion());
                            listRecommendationsAPIObject.setKubernetesObjects(kubernetesAPIObjectList);
                            listRecommendationsAPIObject.setExperimentName(kruizeRecommendationEntry.getExperiment_name());
                            listRecommendationsAPIObject.setClusterName(kruizeRecommendationEntry.getCluster_name());
                        }
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        LOGGER.debug(e.getMessage());
                    }
                    if (null != listRecommendationsAPIObject)
                        listRecommendationsAPIObjectList.add(listRecommendationsAPIObject);
                }
                if (listRecommendationsAPIObjectList.isEmpty())
                    return null;
                return listRecommendationsAPIObjectList;
            }

            public static KruizePerformanceProfileEntry convertPerfProfileObjToPerfProfileDBObj(PerformanceProfile performanceProfile) {
                KruizePerformanceProfileEntry kruizePerformanceProfileEntry = null;
                try {
                    kruizePerformanceProfileEntry = new KruizePerformanceProfileEntry();
                    kruizePerformanceProfileEntry.setName(performanceProfile.getName());
                    kruizePerformanceProfileEntry.setProfile_version(performanceProfile.getProfile_version());
                    kruizePerformanceProfileEntry.setK8s_type(performanceProfile.getK8S_TYPE());

                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        kruizePerformanceProfileEntry.setSlo(
                                objectMapper.readTree(new Gson().toJson(performanceProfile.getSloInfo())));
                    } catch (JsonProcessingException e) {
                        throw new Exception("Error while creating SLO data due to : " + e.getMessage());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error occurred while converting Performance Profile Object to PerformanceProfile table due to {}", e.getMessage());
                    e.printStackTrace();
                }
                return kruizePerformanceProfileEntry;
            }

            public static List<PerformanceProfile> convertPerformanceProfileEntryToPerformanceProfileObject(List<KruizePerformanceProfileEntry> entries) throws Exception {
                List<PerformanceProfile> performanceProfiles = new ArrayList<>();
                int failureThreshHold = entries.size();
                int failureCount = 0;
                for (KruizePerformanceProfileEntry entry : entries) {
                    try {
                        JsonNode sloData = entry.getSlo();
                        String slo_rawJson = sloData.toString();
                        SloInfo sloInfo = new Gson().fromJson(slo_rawJson, SloInfo.class);
                        PerformanceProfile performanceProfile = new PerformanceProfile(
                                entry.getName(), entry.getProfile_version(), entry.getK8s_type(), sloInfo);
                        performanceProfiles.add(performanceProfile);
                    } catch (Exception e) {
                        LOGGER.error("Error occurred while reading from Performance Profile DB object due to : {}", e.getMessage());
                        LOGGER.error(entry.toString());
                        failureCount++;
                    }
                }
                if (failureThreshHold > 0 && failureCount == failureThreshHold)
                    throw new Exception("None of the Performance Profiles loaded from DB.");

                return performanceProfiles;
            }

            /**
             * converts MetricProfile object to KruizeMetricProfileEntry table object
             *
             * @param metricProfile metricProfile object to be converted
             * @return KruizeMetricProfileEntry table object
             */
            public static KruizeMetricProfileEntry convertMetricProfileObjToMetricProfileDBObj(PerformanceProfile metricProfile) {
                KruizeMetricProfileEntry kruizeMetricProfileEntry = null;
                try {
                    kruizeMetricProfileEntry = new KruizeMetricProfileEntry();
                    kruizeMetricProfileEntry.setApi_version(metricProfile.getApiVersion());
                    kruizeMetricProfileEntry.setKind(metricProfile.getKind());
                    kruizeMetricProfileEntry.setProfile_version(metricProfile.getProfile_version());
                    kruizeMetricProfileEntry.setK8s_type(metricProfile.getK8S_TYPE());

                    ObjectMapper objectMapper = new ObjectMapper();

                    try {
                        JsonNode metadataNode = objectMapper.readTree(metricProfile.getMetadata().toString());
                        kruizeMetricProfileEntry.setMetadata(metadataNode);
                    } catch (JsonProcessingException e) {
                        throw new Exception("Error while creating metadata due to : " + e.getMessage());
                    }
                    kruizeMetricProfileEntry.setName(metricProfile.getMetadata().get("name").asText());

                    try {
                        kruizeMetricProfileEntry.setSlo(
                                objectMapper.readTree(new Gson().toJson(metricProfile.getSloInfo())));
                    } catch (JsonProcessingException e) {
                        throw new Exception("Error while creating SLO data due to : " + e.getMessage());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error occurred while converting MetricProfile Object to MetricProfile table due to {}", e.getMessage());
                    e.printStackTrace();
                }
                return kruizeMetricProfileEntry;
            }

            /**
             * converts KruizeMetricProfileEntry table objects to MetricProfile objects
             *
             * @param kruizeMetricProfileEntryList List of KruizeMetricProfileEntry table objects to be converted
             * @return List containing the MetricProfile objects
             * @throws Exception
             */
            public static List<PerformanceProfile> convertMetricProfileEntryToMetricProfileObject(List<KruizeMetricProfileEntry> kruizeMetricProfileEntryList) throws Exception {
                List<PerformanceProfile> metricProfiles = new ArrayList<>();
                int failureThreshHold = kruizeMetricProfileEntryList.size();
                int failureCount = 0;
                for (KruizeMetricProfileEntry entry : kruizeMetricProfileEntryList) {
                    try {
                        JsonNode metadata = entry.getMetadata();
                        JsonNode sloData = entry.getSlo();
                        String slo_rawJson = sloData.toString();
                        SloInfo sloInfo = new Gson().fromJson(slo_rawJson, SloInfo.class);
                        PerformanceProfile performanceProfile = new PerformanceProfile(
                                entry.getApi_version(), entry.getKind(), metadata, entry.getProfile_version(), entry.getK8s_type(), sloInfo);
                        metricProfiles.add(performanceProfile);
                    } catch (Exception e) {
                        LOGGER.error("Error occurred while reading from MetricProfile DB object due to : {}", e.getMessage());
                        LOGGER.error(entry.toString());
                        failureCount++;
                    }
                }
                if (failureThreshHold > 0 && failureCount == failureThreshHold)
                    throw new Exception("None of the Metric Profiles loaded from DB.");

                return metricProfiles;
            }


            /**
             * converts KruizeDataSourceEntry table objects to DataSourceInfo objects
             *
             * @param kruizeDataSourceList List containing the KruizeDataSourceEntry table objects
             * @return List containing the DataSourceInfo objects
             */
            public static List<DataSourceInfo> convertKruizeDataSourceToDataSourceObject(List<KruizeDataSourceEntry> kruizeDataSourceList) throws Exception {
                List<DataSourceInfo> dataSourceInfoList = new ArrayList<>();
                int failureThreshHold = kruizeDataSourceList.size();
                int failureCount = 0;
                for (KruizeDataSourceEntry kruizeDataSource : kruizeDataSourceList) {
                    try {
                        DataSourceInfo dataSourceInfo;
                        AuthenticationConfig authConfig = null;
                        JSONObject authJson = null;
                        if (kruizeDataSource.getKruizeAuthenticationEntry() != null) {
                            try {
                                JsonNode credentialsNode = kruizeDataSource.getKruizeAuthenticationEntry().getCredentials();
                                String authType = kruizeDataSource.getKruizeAuthenticationEntry().getAuthenticationType();
                                // Parse the JsonNode credentials into a JSONObject
                                if (!credentialsNode.toString().equalsIgnoreCase(AnalyzerConstants.NULL)) {
                                    JSONObject credentialsJson = new JSONObject(credentialsNode.toString());
                                    authJson = new JSONObject()
                                            .put(KruizeConstants.AuthenticationConstants.AUTHENTICATION_TYPE, authType)
                                            .put(KruizeConstants.AuthenticationConstants.AUTHENTICATION_CREDENTIALS, credentialsJson);
                                }
                                authConfig = AuthenticationConfig.createAuthenticationConfigObject(authJson);
                            } catch (Exception e) {
                                e.printStackTrace();
                                LOGGER.error("GSON failed to convert the DB Json object in convertKruizeDataSourceToDataSourceObject");
                            }
                        }
                        if (kruizeDataSource.getServiceName().isEmpty() && null != kruizeDataSource.getUrl()) {
                            dataSourceInfo = new DataSourceInfo(kruizeDataSource.getName(), kruizeDataSource
                                    .getProvider(), null, null, new URL(kruizeDataSource.getUrl()), authConfig);
                        } else {
                            dataSourceInfo = new DataSourceInfo(kruizeDataSource.getName(), kruizeDataSource
                                    .getProvider(), kruizeDataSource.getServiceName(), kruizeDataSource.getNamespace(), null, authConfig);
                        }
                        dataSourceInfoList.add(dataSourceInfo);
                    } catch (Exception e) {
                        LOGGER.error("Error occurred while converting to dataSourceInfo from DB object : {}", e.getMessage());
                        LOGGER.error(e.getMessage());
                        failureCount++;
                    }
                }
                if (failureThreshHold > 0 && failureCount == failureThreshHold)
                    throw new Exception("None of the Datasource loaded from DB.");

                return dataSourceInfoList;
            }

            /**
             * converts DataSourceInfo objects to KruizeDataSourceEntry table objects
             *
             * @param dataSourceInfo DataSourceInfo objects
             * @return KruizeDataSourceEntry table object
             */
            public static KruizeDataSourceEntry convertDataSourceToDataSourceDBObj(DataSourceInfo dataSourceInfo) {
                KruizeDataSourceEntry kruizeDataSource;
                KruizeAuthenticationEntry kruizeAuthenticationEntry;
                try {
                    kruizeDataSource = new KruizeDataSourceEntry();
                    kruizeDataSource.setVersion(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoConstants.version);
                    kruizeDataSource.setName(dataSourceInfo.getName());
                    kruizeDataSource.setProvider(dataSourceInfo.getProvider());
                    kruizeDataSource.setServiceName(dataSourceInfo.getServiceName());
                    kruizeDataSource.setNamespace(dataSourceInfo.getNamespace());
                    kruizeDataSource.setUrl(dataSourceInfo.getUrl().toString());
                } catch (Exception e) {
                    kruizeDataSource = null;
                    LOGGER.error("Error while converting DataSource Object to KruizeDataSource table due to {}", e.getMessage());
                    e.printStackTrace();
                }
                return kruizeDataSource;
            }

            /**
             * converts DataSourceMetadataInfo objects to KruizeDSMetadataEntry table objects
             *
             * @param kruizeMetadataList List of KruizeDSMetadataEntry objects
             * @return DataSourceMetadataInfo object
             * @throws Exception
             */
            public static List<DataSourceMetadataInfo> convertKruizeMetadataToDataSourceMetadataObject(List<KruizeDSMetadataEntry> kruizeMetadataList) throws Exception {
                List<DataSourceMetadataInfo> dataSourceMetadataInfoList = new ArrayList<>();
                int failureThreshHold = kruizeMetadataList.size();
                int failureCount = 0;
                DataSourceMetadataInfo dataSourceMetadataInfo = new DataSourceMetadataInfo(new HashMap<>());

                for (KruizeDSMetadataEntry kruizeMetadata : kruizeMetadataList) {
                    try {
                        DataSource dataSource = getOrCreateDataSourceFromDB(kruizeMetadata, dataSourceMetadataInfo);
                        DataSourceCluster dataSourceCluster = getOrCreateDataSourceClusterFromDB(kruizeMetadata, dataSource);
                        DataSourceNamespace dataSourceNamespace = getOrCreateDataSourceNamespaceFromDB(kruizeMetadata, dataSourceCluster);
                        DataSourceWorkload dataSourceWorkload = getOrCreateDataSourceWorkloadFromDB(kruizeMetadata, dataSourceNamespace);
                        DataSourceContainer dataSourceContainer = getOrCreateDataSourceContainerFromDB(kruizeMetadata, dataSourceWorkload);

                        // Update DataSourceMetadataInfo with the DataSource, DataSourceCluster, DataSourceNamespace, DataSourceWorkload, and DataSourceContainer
                        dataSourceMetadataInfo.getDatasources().put(kruizeMetadata.getDataSourceName(), dataSource);
                        dataSource.getClusters().put(kruizeMetadata.getClusterName(), dataSourceCluster);
                        dataSourceCluster.getNamespaces().put(kruizeMetadata.getNamespace(), dataSourceNamespace);
                        if (null == dataSourceWorkload) {
                            dataSourceNamespace.setWorkloads(null);
                            continue;
                        }
                        dataSourceNamespace.getWorkloads().put(kruizeMetadata.getWorkloadName(), dataSourceWorkload);

                        if (null == dataSourceContainer) {
                            dataSourceWorkload.setContainers(null);
                            continue;
                        }
                        dataSourceWorkload.getContainers().put(kruizeMetadata.getContainerName(), dataSourceContainer);
                    } catch (Exception e) {
                        LOGGER.error("Error occurred while converting to dataSourceMetadataInfo from DB object : {}", e.getMessage());
                        LOGGER.error(e.getMessage());
                        failureCount++;
                    }
                }
                if (failureThreshHold > 0 && failureCount == failureThreshHold)
                    throw new Exception("None of the Metadata loaded from DB.");

                dataSourceMetadataInfoList.add(dataSourceMetadataInfo);
                return dataSourceMetadataInfoList;
            }

            /**
             * Converts KruizeDSMetadataEntry table objects to DataSourceMetadataInfo with only cluster-level metadata
             *
             * @param kruizeMetadataList KruizeDSMetadataEntry objects
             * @return DataSourceMetadataInfo object with only cluster-level metadata
             * @throws Exception
             */
            public static List<DataSourceMetadataInfo> convertKruizeMetadataToClusterLevelDataSourceMetadata(List<KruizeDSMetadataEntry> kruizeMetadataList) throws Exception {
                List<DataSourceMetadataInfo> dataSourceMetadataInfoList = new ArrayList<>();
                int failureThreshHold = kruizeMetadataList.size();
                int failureCount = 0;

                // Create a single instance of DataSourceMetadataInfo
                DataSourceMetadataInfo dataSourceMetadataInfo = new DataSourceMetadataInfo(new HashMap<>());

                for (KruizeDSMetadataEntry kruizeMetadata : kruizeMetadataList) {
                    try {
                        DataSource dataSource = getOrCreateDataSourceFromDB(kruizeMetadata, dataSourceMetadataInfo);
                        DataSourceCluster dataSourceCluster = getOrCreateDataSourceClusterFromDB(kruizeMetadata, dataSource);
                        dataSourceCluster.setNamespaces(null);

                        // Update DataSourceMetadataInfo with the DataSource and DataSourceCluster
                        dataSourceMetadataInfo.getDatasources()
                                .put(kruizeMetadata.getDataSourceName(), dataSource);

                        dataSource.getClusters()
                                .put(kruizeMetadata.getClusterName(), dataSourceCluster);

                    } catch (Exception e) {
                        LOGGER.error("Error occurred while converting to dataSourceMetadataInfo from DB object : {}", e.getMessage());
                        LOGGER.error(e.getMessage());
                        failureCount++;
                    }
                }

                if (failureThreshHold > 0 && failureCount == failureThreshHold)
                    throw new Exception("None of the Metadata loaded from DB.");

                dataSourceMetadataInfoList.add(dataSourceMetadataInfo);
                return dataSourceMetadataInfoList;
            }

            /**
             * Converts KruizeDSMetadataEntry table objects to DataSourceMetadataInfo with only namespace-level metadata
             *
             * @param kruizeMetadataList List of KruizeDSMetadataEntry objects
             * @return DataSourceMetadataInfo with only namespace-level metadata
             * @throws Exception
             */
            public static List<DataSourceMetadataInfo> convertKruizeMetadataToNamespaceLevelDataSourceMetadata(List<KruizeDSMetadataEntry> kruizeMetadataList) throws Exception {
                List<DataSourceMetadataInfo> dataSourceMetadataInfoList = new ArrayList<>();
                int failureThreshHold = kruizeMetadataList.size();
                int failureCount = 0;

                // Create a single instance of DataSourceMetadataInfo
                DataSourceMetadataInfo dataSourceMetadataInfo = new DataSourceMetadataInfo(new HashMap<>());

                for (KruizeDSMetadataEntry kruizeMetadata : kruizeMetadataList) {
                    try {
                        DataSource dataSource = getOrCreateDataSourceFromDB(kruizeMetadata, dataSourceMetadataInfo);
                        DataSourceCluster dataSourceCluster = getOrCreateDataSourceClusterFromDB(kruizeMetadata, dataSource);
                        DataSourceNamespace dataSourceNamespace = getOrCreateDataSourceNamespaceFromDB(kruizeMetadata, dataSourceCluster);
                        dataSourceNamespace.setWorkloads(null);

                        // Update DataSourceMetadataInfo with the DataSource and DataSourceCluster
                        dataSourceMetadataInfo.getDatasources()
                                .put(kruizeMetadata.getDataSourceName(), dataSource);

                        dataSource.getClusters()
                                .put(kruizeMetadata.getClusterName(), dataSourceCluster);

                        dataSourceCluster.getNamespaces()
                                .put(kruizeMetadata.getNamespace(), dataSourceNamespace);

                    } catch (Exception e) {
                        LOGGER.error("Error occurred while converting to dataSourceMetadataInfo from DB object : {}", e.getMessage());
                        LOGGER.error(e.getMessage());
                        failureCount++;
                    }
                }

                if (failureThreshHold > 0 && failureCount == failureThreshHold)
                    throw new Exception("None of the Metadata loaded from DB.");

                dataSourceMetadataInfoList.add(dataSourceMetadataInfo);
                return dataSourceMetadataInfoList;
            }

            /**
             * Converts DataSourceMetadataInfo object to KruizeDSMetadataEntry objects
             *
             * @param dataSourceMetadataInfo DataSourceMetadataInfo object
             * @return List of KruizeDSMetadataEntry objects
             */
            public static List<KruizeDSMetadataEntry> convertDataSourceMetadataToMetadataObj(DataSourceMetadataInfo dataSourceMetadataInfo) {
                List<KruizeDSMetadataEntry> kruizeMetadataList = new ArrayList<>();
                try {

                    for (DataSource dataSource : dataSourceMetadataInfo.getDatasources().values()) {
                        String dataSourceName = dataSource.getDataSourceName();

                        for (DataSourceCluster dataSourceCluster : dataSource.getClusters().values()) {
                            String dataSourceClusterName = dataSourceCluster.getDataSourceClusterName();

                            for (DataSourceNamespace dataSourceNamespace : dataSourceCluster.getNamespaces().values()) {
                                String namespaceName = dataSourceNamespace.getNamespace();

                                if (null == dataSourceNamespace.getWorkloads()) {
                                    KruizeDSMetadataEntry kruizeMetadata = new KruizeDSMetadataEntry();
                                    kruizeMetadata.setVersion(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoConstants.version);
                                    kruizeMetadata.setDataSourceName(dataSourceName);
                                    kruizeMetadata.setClusterName(dataSourceClusterName);
                                    kruizeMetadata.setNamespace(namespaceName);
                                    kruizeMetadata.setWorkloadName(null);
                                    kruizeMetadata.setWorkloadType(null);
                                    kruizeMetadata.setContainerName(null);
                                    kruizeMetadata.setContainerImageName(null);
                                    kruizeMetadataList.add(kruizeMetadata);
                                    continue;
                                }

                                for (DataSourceWorkload dataSourceWorkload : dataSourceNamespace.getWorkloads().values()) {
                                    // handles 'job' workload type with no containers
                                    if (null == dataSourceWorkload.getContainers()) {
                                        KruizeDSMetadataEntry kruizeMetadata = new KruizeDSMetadataEntry();
                                        kruizeMetadata.setVersion(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoConstants.version);

                                        kruizeMetadata.setDataSourceName(dataSourceName);
                                        kruizeMetadata.setClusterName(dataSourceClusterName);
                                        kruizeMetadata.setNamespace(namespaceName);
                                        kruizeMetadata.setWorkloadType(dataSourceWorkload.getWorkloadType());
                                        kruizeMetadata.setWorkloadName(dataSourceWorkload.getWorkloadName());

                                        kruizeMetadata.setContainerName(null);
                                        kruizeMetadata.setContainerImageName(null);

                                        kruizeMetadataList.add(kruizeMetadata);
                                        continue;
                                    }

                                    for (DataSourceContainer dataSourceContainer : dataSourceWorkload.getContainers().values()) {
                                        KruizeDSMetadataEntry kruizeMetadata = new KruizeDSMetadataEntry();
                                        kruizeMetadata.setVersion(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoConstants.version);

                                        kruizeMetadata.setDataSourceName(dataSourceName);
                                        kruizeMetadata.setClusterName(dataSourceClusterName);
                                        kruizeMetadata.setNamespace(namespaceName);
                                        kruizeMetadata.setWorkloadType(dataSourceWorkload.getWorkloadType());
                                        kruizeMetadata.setWorkloadName(dataSourceWorkload.getWorkloadName());

                                        kruizeMetadata.setContainerName(dataSourceContainer.getContainerName());
                                        kruizeMetadata.setContainerImageName(dataSourceContainer.getContainerImageName());

                                        kruizeMetadataList.add(kruizeMetadata);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error while converting DataSourceMetadata Object to KruizeDSMetadataEntry table due to {}", e.getMessage());
                    e.printStackTrace();
                }
                return kruizeMetadataList;
            }

            public static KruizeAuthenticationEntry convertAuthDetailsToAuthDetailsDBObj(AuthenticationConfig authenticationConfig, String serviceType) {
                KruizeAuthenticationEntry kruizeAuthenticationEntry;
                try {
                    kruizeAuthenticationEntry = new KruizeAuthenticationEntry();
                    kruizeAuthenticationEntry.setAuthenticationType(authenticationConfig.getType().toString());
                    // set the authentication details
                    String credentialsString = new Gson().toJson(authenticationConfig.getCredentials());
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode credentials;
                    try {
                        credentials = objectMapper.readTree(credentialsString);
                    } catch (JsonProcessingException e) {
                        throw new Exception("Error occurred while creating credentials object : " + e.getMessage());
                    }
                    kruizeAuthenticationEntry.setCredentials(credentials);
                    kruizeAuthenticationEntry.setServiceType(serviceType);
                } catch (Exception e) {
                    kruizeAuthenticationEntry = null;
                    LOGGER.error("Error while converting Auth details Object to KruizeAuthentication table : {}", e.getMessage());
                    e.printStackTrace();
                }
                return kruizeAuthenticationEntry;
            }

            /**
             * Converts List of KruizeLMMetadataProfileEntry DB objects to List of MetadataProfile objects
             *
             * @param kruizeMetadataProfileEntryList List of KruizeLMMetadataProfileEntry DB objects
             * @return List of MetadataProfile objects
             */
            public static List<MetadataProfile> convertMetadataProfileEntryToMetadataProfileObject(List<KruizeLMMetadataProfileEntry> kruizeMetadataProfileEntryList) throws Exception {
                List<MetadataProfile> metadataProfiles = new ArrayList<>();
                int failureThreshHold = kruizeMetadataProfileEntryList.size();
                int failureCount = 0;
                for (KruizeLMMetadataProfileEntry entry : kruizeMetadataProfileEntryList) {
                    try {
                        JsonNode metadata = entry.getMetadata();
                        JsonNode query_variables = entry.getQuery_variables();
                        ArrayList<Metric> queryVariablesList = new ArrayList<>();

                        if (query_variables.isArray()) {
                            for (JsonNode node : query_variables) {
                                String metric_rawJson = node.toString();
                                Metric metric = new Gson().fromJson(metric_rawJson, Metric.class);
                                queryVariablesList.add(metric);
                            }
                        }

                        MetadataProfile metadataProfile = new MetadataProfile(
                                entry.getApi_version(), entry.getKind(), metadata, entry.getProfile_version(), entry.getK8s_type(), entry.getDatasource(), queryVariablesList);
                        metadataProfiles.add(metadataProfile);
                    } catch (Exception e) {
                        LOGGER.error(KruizeConstants.MetadataProfileConstants.MetadataProfileErrorMsgs.CONVERTING_METADATA_PROFILE_DB_OBJECT_ERROR, e.getMessage());
                        LOGGER.error(entry.toString());
                        failureCount++;
                    }
                }
                if (failureThreshHold > 0 && failureCount == failureThreshHold) {
                    throw new Exception(KruizeConstants.MetadataProfileConstants.MetadataProfileErrorMsgs.LOAD_METADATA_PROFILES_FROM_DB_FAILURE);
                }

                return metadataProfiles;
            }

            /**
             * Converts MetadataProfile object to KruizeLMMetadataProfileEntry object
             *
             * @param metadataProfile MetadataProfile object
             * @return KruizeLMMetadataProfileEntry objects
             */
            public static KruizeLMMetadataProfileEntry convertMetadataProfileObjToMetadataProfileDBObj(MetadataProfile metadataProfile) {
                KruizeLMMetadataProfileEntry kruizeMetadataProfileEntry = null;
                try {
                    kruizeMetadataProfileEntry = new KruizeLMMetadataProfileEntry();
                    kruizeMetadataProfileEntry.setApi_version(metadataProfile.getApiVersion());
                    kruizeMetadataProfileEntry.setKind(metadataProfile.getKind());
                    kruizeMetadataProfileEntry.setProfile_version(metadataProfile.getProfile_version());
                    kruizeMetadataProfileEntry.setK8s_type(metadataProfile.getK8s_type());
                    kruizeMetadataProfileEntry.setDatasource(metadataProfile.getDatasource());

                    ObjectMapper objectMapper = new ObjectMapper();

                    try {
                        JsonNode metadataNode = objectMapper.readTree(metadataProfile.getMetadata().toString());
                        kruizeMetadataProfileEntry.setMetadata(metadataNode);
                    } catch (JsonProcessingException e) {
                        throw new Exception(KruizeConstants.MetadataProfileConstants.MetadataProfileErrorMsgs.PROCESS_METADATA_PROFILE_OBJECT_ERROR + e.getMessage());
                    }
                    kruizeMetadataProfileEntry.setName(metadataProfile.getMetadata().get(KruizeConstants.JSONKeys.NAME).asText());

                    try {
                        kruizeMetadataProfileEntry.setQuery_variables(
                                objectMapper.readTree(new Gson().toJson(metadataProfile.getQueryVariables())));
                    } catch (JsonProcessingException e) {
                        throw new Exception(KruizeConstants.MetadataProfileConstants.MetadataProfileErrorMsgs.PROCESS_QUERY_VARIABLES_ERROR + e.getMessage());
                    }
                } catch (Exception e) {
                    LOGGER.error(KruizeConstants.MetadataProfileConstants.MetadataProfileErrorMsgs.CONVERT_METADATA_PROFILE_TO_DB_OBJECT_FAILURE, e.getMessage());
                    e.printStackTrace();
                }
                return kruizeMetadataProfileEntry;
            }
        }

    }
}
