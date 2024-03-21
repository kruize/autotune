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

import com.autotune.analyzer.exceptions.InvalidConversionOfRecommendationEntryException;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.kruizeObject.SloInfo;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForTimestamp;
import com.autotune.analyzer.serviceObjects.*;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.data.dataSourceDetails.*;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.datasource.DataSourceCollection;
import com.autotune.common.datasource.DataSourceDetailsOperator;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.database.table.*;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.autotune.analyzer.experiment.ExperimentInitiator.getErrorMap;

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

            // Check if Container data map is not empty
            if (containerDataMap.isEmpty()) {
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
                if (null == kubernetesAPIObject.getContainerAPIObjects()) {
                    throw new InvalidConversionOfRecommendationEntryException(
                            String.format(
                                    AnalyzerErrorConstants.ConversionErrors.KruizeRecommendationError.NOT_NULL,
                                    "List of " + ContainerAPIObject.class.getSimpleName()
                            )
                    );
                }
                // Check for empty list
                if (kubernetesAPIObject.getContainerAPIObjects().isEmpty()) {
                    throw new InvalidConversionOfRecommendationEntryException(
                            String.format(
                                    AnalyzerErrorConstants.ConversionErrors.KruizeRecommendationError.NOT_EMPTY,
                                    "List of " + ContainerAPIObject.class.getSimpleName()
                            )
                    );
                }


                for (K8sObject k8sObject : kruizeObject.getKubernetes_objects()) {
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
            public static KruizeExperimentEntry convertCreateAPIObjToExperimentDBObj(CreateExperimentAPIObject apiObject) {
                KruizeExperimentEntry kruizeExperimentEntry = null;
                try {
                    kruizeExperimentEntry = new KruizeExperimentEntry();
                    kruizeExperimentEntry.setExperiment_name(apiObject.getExperimentName());
                    kruizeExperimentEntry.setExperiment_id(Utils.generateID(apiObject));
                    kruizeExperimentEntry.setCluster_name(apiObject.getClusterName());
                    kruizeExperimentEntry.setMode(apiObject.getMode());
                    kruizeExperimentEntry.setPerformance_profile(apiObject.getPerformanceProfile());
                    kruizeExperimentEntry.setVersion(apiObject.getApiVersion());
                    kruizeExperimentEntry.setTarget_cluster(apiObject.getTargetCluster());
                    kruizeExperimentEntry.setStatus(AnalyzerConstants.ExperimentStatus.IN_PROGRESS);
                    kruizeExperimentEntry.setDatasource(apiObject.getDatasource());
                    // get metadata from the DB based on the datasource
                    if (apiObject.getDatasource() != null) {
                        KruizeMetadata kruizeMetadata = getMetadata(apiObject.getDatasource());
                        kruizeExperimentEntry.setMetadata(kruizeMetadata);
                    } else
                        kruizeExperimentEntry.setMetadata(null);
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        kruizeExperimentEntry.setExtended_data(
                                objectMapper.readTree(
                                        new Gson().toJson(apiObject)
                                )
                        );
                    } catch (JsonProcessingException e) {
                        throw new Exception("Error while creating Extended data due to : " + e.getMessage());
                    }
                } catch (Exception e) {
                    kruizeExperimentEntry = null;
                    LOGGER.error("Error while converting Kruize Object to experimentDetailTable due to {}", e.getMessage());
                    e.printStackTrace();
                }
                return kruizeExperimentEntry;
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
                    if (null == k8sObject.getContainerDataMap())
                        continue;
                    if (k8sObject.getContainerDataMap().isEmpty())
                        continue;
                    KubernetesAPIObject kubernetesAPIObject = new KubernetesAPIObject(k8sObject.getName(), k8sObject.getType(), k8sObject.getNamespace());
                    boolean matchFound = false;
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
                }
                return listRecommendationsAPIObject;
            }

            public static KruizeRecommendationEntry convertKruizeObjectTORecommendation(KruizeObject kruizeObject, ExperimentResultData experimentResultData) {
                KruizeRecommendationEntry kruizeRecommendationEntry = null;
                Timestamp monitoringEndTime = null;
                Boolean checkForTimestamp = false;
                Boolean getLatest = true;
                Gson gson = new GsonBuilder()
                        .disableHtmlEscaping()
                        .setPrettyPrinting()
                        .enableComplexMapKeySerialization()
                        .setDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT)
                        .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                        .create();
                try {
                    if (null != experimentResultData) {
                        monitoringEndTime = experimentResultData.getIntervalEndTime();
                    }
                    ListRecommendationsAPIObject listRecommendationsAPIObject = getListRecommendationAPIObjectForDB(
                            kruizeObject, monitoringEndTime);
                    if (null == listRecommendationsAPIObject) {
                        return null;
                    }
                    LOGGER.debug(new GsonBuilder().setPrettyPrinting().create().toJson(listRecommendationsAPIObject));
                    kruizeRecommendationEntry = new KruizeRecommendationEntry();
                    kruizeRecommendationEntry.setVersion(KruizeConstants.KRUIZE_RECOMMENDATION_API_VERSION.LATEST.getVersionNumber());
                    kruizeRecommendationEntry.setExperiment_name(listRecommendationsAPIObject.getExperimentName());
                    kruizeRecommendationEntry.setCluster_name(listRecommendationsAPIObject.getClusterName());
                    Timestamp endInterval = null;
                    // todo : what happens if two k8 objects or Containers with different timestamp
                    for (KubernetesAPIObject k8sObject : listRecommendationsAPIObject.getKubernetesObjects()) {
                        for (ContainerAPIObject containerAPIObject : k8sObject.getContainerAPIObjects()) {
                            endInterval = containerAPIObject.getContainerRecommendations().getData().keySet().stream().max(Timestamp::compareTo).get();
                            break;
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
                        apiObj.setDatasource(entry.getDatasource());
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
                    kubernetesAPIObjects.add(kubernetesAPIObject);
                }
                return kubernetesAPIObjects;
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

            public static KruizeDataSource convertDataSourceToDataSourceDBObj(DataSourceInfo dataSourceInfo) {
                KruizeDataSource kruizeDataSource;
                try {
                    kruizeDataSource = new KruizeDataSource();
                    kruizeDataSource.setVersion(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoConstants.version);
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

            public static List<DataSourceInfo> convertKruizeDataSourceToDataSourceObject(List<KruizeDataSource> kruizeDataSourceList) throws Exception {
                List<DataSourceInfo> dataSourceInfoList = new ArrayList<>();
                int failureThreshHold = kruizeDataSourceList.size();
                int failureCount = 0;
                for (KruizeDataSource kruizeDataSource : kruizeDataSourceList) {
                    try {
                        DataSourceInfo dataSourceInfo = null;
                        if(kruizeDataSource.getServiceName().isEmpty() && null != kruizeDataSource.getUrl())
                             dataSourceInfo = new DataSourceInfo(kruizeDataSource.getName(), kruizeDataSource
                                    .getProvider(), new URL(kruizeDataSource.getUrl()));
                        else{
                             dataSourceInfo = new DataSourceInfo(kruizeDataSource.getName(), kruizeDataSource
                                    .getProvider(), kruizeDataSource.getServiceName(), kruizeDataSource.getNamespace());
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


            public static List<DataSourceDetailsInfo> convertKruizeMetadataToDataSourceDetailsObject(List<KruizeMetadata> kruizeMetadataList) throws Exception {
                List<DataSourceDetailsInfo> dataSourceDetailsInfoList = new ArrayList<>();
                int failureThreshHold = kruizeMetadataList.size();
                int failureCount = 0;
                DataSourceDetailsInfo dataSourceDetailsInfo = new DataSourceDetailsInfo(new HashMap<>());

                for (KruizeMetadata kruizeMetadata : kruizeMetadataList) {
                    try {
                        DataSourceClusterGroup dataSourceClusterGroup = getDataSourceClusterGroup(kruizeMetadata, dataSourceDetailsInfo);
                        DataSourceCluster dataSourceCluster = getDataSourceCluster(kruizeMetadata, dataSourceClusterGroup);
                        DataSourceNamespace dataSourceNamespace = getDataSourceNamespace(kruizeMetadata, dataSourceCluster);
                        DataSourceWorkload dataSourceWorkload = getDataSourceWorkload(kruizeMetadata, dataSourceNamespace);
                        DataSourceContainer dataSourceContainer = getDataSourceContainer(kruizeMetadata, dataSourceWorkload);

                        // Update DataSourceDetailsInfo with the DataSourceClusterGroup, DataSourceCluster, DataSourceNamespace, DataSourceWorkload, and DataSourceContainer
                        dataSourceDetailsInfo.getDataSourceClusterGroupHashMap().put(kruizeMetadata.getClusterGroupName(), dataSourceClusterGroup);
                        dataSourceClusterGroup.getDataSourceClusterHashMap().put(kruizeMetadata.getClusterName(), dataSourceCluster);
                        dataSourceCluster.getDataSourceNamespaceHashMap().put(kruizeMetadata.getNamespace(), dataSourceNamespace);
                        if (null == dataSourceWorkload) {
                            dataSourceNamespace.setDataSourceWorkloadHashMap(null);
                            continue;
                        }
                        dataSourceNamespace.getDataSourceWorkloadHashMap().put(kruizeMetadata.getWorkloadName(), dataSourceWorkload);
                        dataSourceWorkload.getDataSourceContainerHashMap().put(kruizeMetadata.getContainerName(), dataSourceContainer);
                    } catch (Exception e) {
                        LOGGER.error("Error occurred while converting to dataSourceDetailsInfo from DB object : {}", e.getMessage());
                        LOGGER.error(e.getMessage());
                        failureCount++;
                    }
                }
                if (failureThreshHold > 0 && failureCount == failureThreshHold)
                    throw new Exception("None of the Metadata loaded from DB.");

                dataSourceDetailsInfoList.add(dataSourceDetailsInfo);
                return dataSourceDetailsInfoList;
            }
            public static List<DataSourceDetailsInfo> convertKruizeMetadataToClusterLevelDataSourceDetails(List<KruizeMetadata> kruizeMetadataList) throws Exception {
                List<DataSourceDetailsInfo> dataSourceDetailsInfoList = new ArrayList<>();
                int failureThreshHold = kruizeMetadataList.size();
                int failureCount = 0;

                // Create a single instance of DataSourceDetailsInfo
                DataSourceDetailsInfo dataSourceDetailsInfo = new DataSourceDetailsInfo(new HashMap<>());

                for (KruizeMetadata kruizeMetadata : kruizeMetadataList) {
                    try {
                        DataSourceClusterGroup dataSourceClusterGroup = getDataSourceClusterGroup(kruizeMetadata, dataSourceDetailsInfo);
                        DataSourceCluster dataSourceCluster = getDataSourceCluster(kruizeMetadata, dataSourceClusterGroup);
                        dataSourceCluster.setDataSourceNamespaceHashMap(null);

                        // Update DataSourceDetailsInfo with the DataSourceClusterGroup and DataSourceCluster
                        dataSourceDetailsInfo.getDataSourceClusterGroupHashMap()
                                .put(kruizeMetadata.getClusterGroupName(), dataSourceClusterGroup);

                        dataSourceClusterGroup.getDataSourceClusterHashMap()
                                .put(kruizeMetadata.getClusterName(), dataSourceCluster);

                    } catch (Exception e) {
                        LOGGER.error("Error occurred while converting to dataSourceDetailsInfo from DB object : {}", e.getMessage());
                        LOGGER.error(e.getMessage());
                        failureCount++;
                    }
                }

                if (failureThreshHold > 0 && failureCount == failureThreshHold)
                    throw new Exception("None of the Metadata loaded from DB.");

                dataSourceDetailsInfoList.add(dataSourceDetailsInfo);
                return dataSourceDetailsInfoList;
            }

            public static List<DataSourceDetailsInfo> convertKruizeMetadataToNamespaceLevelDataSourceDetails(List<KruizeMetadata> kruizeMetadataList) throws Exception {
                List<DataSourceDetailsInfo> dataSourceDetailsInfoList = new ArrayList<>();
                int failureThreshHold = kruizeMetadataList.size();
                int failureCount = 0;

                // Create a single instance of DataSourceDetailsInfo
                DataSourceDetailsInfo dataSourceDetailsInfo = new DataSourceDetailsInfo(new HashMap<>());

                for (KruizeMetadata kruizeMetadata : kruizeMetadataList) {
                    try {
                        DataSourceClusterGroup dataSourceClusterGroup = getDataSourceClusterGroup(kruizeMetadata, dataSourceDetailsInfo);
                        DataSourceCluster dataSourceCluster = getDataSourceCluster(kruizeMetadata, dataSourceClusterGroup);
                        DataSourceNamespace dataSourceNamespace = getDataSourceNamespace(kruizeMetadata, dataSourceCluster);
                        dataSourceNamespace.setDataSourceWorkloadHashMap(null);

                        // Update DataSourceDetailsInfo with the DataSourceClusterGroup and DataSourceCluster
                        dataSourceDetailsInfo.getDataSourceClusterGroupHashMap()
                                .put(kruizeMetadata.getClusterGroupName(), dataSourceClusterGroup);

                        dataSourceClusterGroup.getDataSourceClusterHashMap()
                                .put(kruizeMetadata.getClusterName(), dataSourceCluster);

                        dataSourceCluster.getDataSourceNamespaceHashMap()
                                .put(kruizeMetadata.getNamespace(), dataSourceNamespace);

                    } catch (Exception e) {
                        LOGGER.error("Error occurred while converting to dataSourceDetailsInfo from DB object : {}", e.getMessage());
                        LOGGER.error(e.getMessage());
                        failureCount++;
                    }
                }

                if (failureThreshHold > 0 && failureCount == failureThreshHold)
                    throw new Exception("None of the Metadata loaded from DB.");

                dataSourceDetailsInfoList.add(dataSourceDetailsInfo);
                return dataSourceDetailsInfoList;
            }

            public static List<KruizeMetadata> convertDataSourceDetailsToMetadataObj(DataSourceDetailsInfo dataSourceDetailsInfo) {
                List<KruizeMetadata> kruizeMetadataList = new ArrayList<>();
                try {

                    for (DataSourceClusterGroup dataSourceClusterGroup : dataSourceDetailsInfo.getDataSourceClusterGroupHashMap().values()) {
                        String clusterGroupName = dataSourceClusterGroup.getDataSourceClusterGroupName();

                        for (DataSourceCluster dataSourceCluster : dataSourceClusterGroup.getDataSourceClusterHashMap().values()) {
                            String dataSourceClusterName = dataSourceCluster.getDataSourceClusterName();

                            for (DataSourceNamespace dataSourceNamespace : dataSourceCluster.getDataSourceNamespaceHashMap().values()) {
                                String namespaceName = dataSourceNamespace.getDataSourceNamespaceName();

                                if (null == dataSourceNamespace.getDataSourceWorkloadHashMap()) {
                                    KruizeMetadata kruizeMetadata = new KruizeMetadata();
                                    kruizeMetadata.setVersion(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoConstants.version);
                                    kruizeMetadata.setClusterGroupName(clusterGroupName);
                                    kruizeMetadata.setClusterName(dataSourceClusterName);
                                    kruizeMetadata.setNamespace(namespaceName);
                                    kruizeMetadata.setWorkloadName(null);
                                    kruizeMetadata.setWorkloadType(null);
                                    kruizeMetadata.setContainerName(null);
                                    kruizeMetadata.setContainerImageName(null);
                                    kruizeMetadataList.add(kruizeMetadata);
                                    continue;
                                }

                                for (DataSourceWorkload dataSourceWorkload : dataSourceNamespace.getDataSourceWorkloadHashMap().values()) {

                                    for (DataSourceContainer dataSourceContainer : dataSourceWorkload.getDataSourceContainerHashMap().values()) {
                                        KruizeMetadata kruizeMetadata = new KruizeMetadata();
                                        kruizeMetadata.setVersion(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoConstants.version);

                                        kruizeMetadata.setClusterGroupName(clusterGroupName);
                                        kruizeMetadata.setClusterName(dataSourceClusterName);
                                        kruizeMetadata.setNamespace(namespaceName);
                                        kruizeMetadata.setWorkloadType(dataSourceWorkload.getDataSourceWorkloadType());
                                        kruizeMetadata.setWorkloadName(dataSourceWorkload.getDataSourceWorkloadName());

                                        kruizeMetadata.setContainerName(dataSourceContainer.getDataSourceContainerName());
                                        kruizeMetadata.setContainerImageName(dataSourceContainer.getDataSourceContainerImageName());

                                        kruizeMetadataList.add(kruizeMetadata);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error while converting DataSourceDetails Object to KruizeMetadata table due to {}", e.getMessage());
                    e.printStackTrace();
                }
                return kruizeMetadataList;
            }
        }
    }

    private static DataSourceClusterGroup getDataSourceClusterGroup(KruizeMetadata kruizeMetadata,  DataSourceDetailsInfo dataSourceDetailsInfo) {
        String clusterGroupName = kruizeMetadata.getClusterGroupName();

        // Check if the cluster group already exists
        if (dataSourceDetailsInfo.getDataSourceClusterGroupHashMap().containsKey(clusterGroupName)) {
            return dataSourceDetailsInfo.getDataSourceClusterGroupHashMap().get(clusterGroupName);
        }

        DataSourceClusterGroup dataSourceClusterGroup = new DataSourceClusterGroup(clusterGroupName, new HashMap<>());
        dataSourceDetailsInfo.getDataSourceClusterGroupHashMap().put(clusterGroupName, dataSourceClusterGroup);

        return dataSourceClusterGroup;
    }

    private static DataSourceCluster getDataSourceCluster(KruizeMetadata kruizeMetadata, DataSourceClusterGroup dataSourceClusterGroup) {
        String clusterName = kruizeMetadata.getClusterName();

        // Check if the cluster already exists in the DataSourceClusterGroup
        if (dataSourceClusterGroup.getDataSourceClusterHashMap().containsKey(clusterName)) {
            return dataSourceClusterGroup.getDataSourceClusterHashMap().get(clusterName);
        }

        DataSourceCluster dataSourceCluster = new DataSourceCluster(clusterName, new HashMap<>());
        dataSourceClusterGroup.getDataSourceClusterHashMap().put(clusterName, dataSourceCluster);

        return dataSourceCluster;
    }

    private static DataSourceNamespace getDataSourceNamespace(KruizeMetadata kruizeMetadata, DataSourceCluster dataSourceCluster) {
        String namespaceName = kruizeMetadata.getNamespace();

        // Check if the namespace already exists in the DataSourceCluster
        if (dataSourceCluster.getDataSourceNamespaceHashMap().containsKey(namespaceName)) {
            return dataSourceCluster.getDataSourceNamespaceHashMap().get(namespaceName);
        }

        DataSourceNamespace dataSourceNamespace = new DataSourceNamespace(namespaceName, new HashMap<>());
        dataSourceCluster.getDataSourceNamespaceHashMap().put(namespaceName, dataSourceNamespace);

        return dataSourceNamespace;
    }

    private static DataSourceWorkload getDataSourceWorkload(KruizeMetadata kruizeMetadata, DataSourceNamespace dataSourceNamespace) {
        String workloadName = kruizeMetadata.getWorkloadName();

        if (null == workloadName) {
            return null;
        }

        // Check if the workload already exists in the DataSourceNamespace
        if (dataSourceNamespace.getDataSourceWorkloadHashMap().containsKey(workloadName)) {
            return dataSourceNamespace.getDataSourceWorkloadHashMap().get(workloadName);
        }

        DataSourceWorkload dataSourceWorkload = new DataSourceWorkload(workloadName, kruizeMetadata.getWorkloadType(), new HashMap<>());
        dataSourceNamespace.getDataSourceWorkloadHashMap().put(workloadName, dataSourceWorkload);

        return dataSourceWorkload;
    }
    private static DataSourceContainer getDataSourceContainer(KruizeMetadata kruizeMetadata, DataSourceWorkload dataSourceWorkload) {
        String containerName = kruizeMetadata.getContainerName();

        if (null == containerName) {
            return null;
        }

        // Check if the container already exists in the DataSourceWorkload
        if (dataSourceWorkload.getDataSourceContainerHashMap().containsKey(containerName)) {
            return dataSourceWorkload.getDataSourceContainerHashMap().get(containerName);
        }

        DataSourceContainer dataSourceContainer = new DataSourceContainer(containerName, kruizeMetadata.getContainerImageName());
        dataSourceWorkload.getDataSourceContainerHashMap().put(containerName, dataSourceContainer);

        return dataSourceContainer;
    }

    private static KruizeMetadata getMetadata(String datasource) {
        DataSourceDetailsOperator dataSourceDetailsOperator = DataSourceDetailsOperator.getInstance();
        HashMap<String, DataSourceInfo> dataSources = DataSourceCollection.getInstance().getDataSourcesCollection();
        DataSourceDetailsInfo dataSourceDetailsInfo = dataSourceDetailsOperator.getDataSourceDetailsInfo(dataSources.get(datasource));
        List<KruizeMetadata> kruizeMetadataList = Converters.KruizeObjectConverters.convertDataSourceDetailsToMetadataObj(dataSourceDetailsInfo);
        if (kruizeMetadataList.isEmpty())
            return null;
        else
            return kruizeMetadataList.get(0);
    }

}
