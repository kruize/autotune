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

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.*;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.database.table.KruizeExperimentEntry;
import com.autotune.database.table.KruizeRecommendationEntry;
import com.autotune.database.table.KruizeResultsEntry;
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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper functions used by the DB to create entity objects.
 */
public class DBHelpers {
    private static final Logger LOGGER = LoggerFactory.getLogger(DBHelpers.class);


    private DBHelpers() {
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
                try {
                    kruizeResultsEntry = new KruizeResultsEntry();
                    kruizeResultsEntry.setExperiment_name(experimentResultData.getExperiment_name());
                    kruizeResultsEntry.setInterval_start_time(experimentResultData.getIntervalStartTime());
                    kruizeResultsEntry.setInterval_end_time(experimentResultData.getIntervalEndTime());
                    kruizeResultsEntry.setDuration_minutes(
                            Double.valueOf((experimentResultData.getIntervalEndTime().getTime() - experimentResultData.getIntervalStartTime().getTime()) / (60 * 1000))
                    );
                    //JSONObject jsonObject = new JSONObject();
                    //jsonObject.put(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS, experimentResultData.getKubernetes_objects());
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        kruizeResultsEntry.setExtended_data(
                                objectMapper.readTree(
                                        new Gson().toJson(experimentResultData.getUpdateResultsAPIObject())
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

            public static KruizeRecommendationEntry convertKruizeObjectTORecommendation(KruizeObject kruizeObject) {
                KruizeRecommendationEntry kruizeRecommendationEntry = null;
                try {
                    ListRecommendationsAPIObject listRecommendationsAPIObject = com.autotune.analyzer.serviceObjects.Converters.KruizeObjectConverters.
                            convertKruizeObjectToListRecommendationSO(
                                    kruizeObject,
                                    true,
                                    false,
                                    null);
                    LOGGER.debug(new GsonBuilder().setPrettyPrinting().create().toJson(listRecommendationsAPIObject).toString());
                    kruizeRecommendationEntry = new KruizeRecommendationEntry();
                    kruizeRecommendationEntry.setExperiment_name(listRecommendationsAPIObject.getExperimentName());
                    kruizeRecommendationEntry.setCluster_name(listRecommendationsAPIObject.getClusterName());
                    Timestamp endInterval = null;
                    for (KubernetesAPIObject k8sObject : listRecommendationsAPIObject.getKubernetesObjects()) {  // todo : what happens if two k8 objects or Containers with different timestamp
                        for (ContainerAPIObject containerAPIObject : k8sObject.getContainerAPIObjects()) {
                            endInterval = containerAPIObject.getContainerRecommendations().getData().keySet().stream().max(Timestamp::compareTo).get();
                            break;
                        }
                    }
                    kruizeRecommendationEntry.setInterval_end_time(endInterval);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS, listRecommendationsAPIObject.getKubernetesObjects());
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        kruizeRecommendationEntry.setExtended_data(
                                objectMapper.readTree(
                                        jsonObject.toString()
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

            public static List<CreateExperimentAPIObject> convertExperimentEntryToCreateExperimentAPIObject(List<KruizeExperimentEntry> entries) {
                List<CreateExperimentAPIObject> createExperimentAPIObjects = new ArrayList<>();
                for (KruizeExperimentEntry entry : entries) {
                    JsonNode extended_data = entry.getExtended_data();
                    String extended_data_rawJson = extended_data.toString();
                    CreateExperimentAPIObject apiObj = new Gson().fromJson(extended_data_rawJson, CreateExperimentAPIObject.class);
                    apiObj.setExperiment_id(entry.getExperiment_id());
                    apiObj.setStatus(entry.getStatus());
                    createExperimentAPIObjects.add(apiObj);
                    //LOGGER.debug(new GsonBuilder().setPrettyPrinting().create().toJson(apiObj));
                }
                return createExperimentAPIObjects;
            }
            public static List<UpdateResultsAPIObject> convertResultEntryToUpdateResultsAPIObject(List<KruizeResultsEntry> kruizeResultsEntries) {
                List<UpdateResultsAPIObject> updateResultsAPIObjects = new ArrayList<>();
                for (KruizeResultsEntry kruizeResultsEntry : kruizeResultsEntries) {
                    JsonNode extended_data = kruizeResultsEntry.getExtended_data();
                    String extended_data_rawJson = extended_data.toString();
                    UpdateResultsAPIObject updateResultsAPIObject = new Gson().fromJson(extended_data_rawJson, UpdateResultsAPIObject.class);
                    updateResultsAPIObjects.add(updateResultsAPIObject);
                }
                LOGGER.debug(new GsonBuilder().setPrettyPrinting().create().toJson(updateResultsAPIObjects));
                return updateResultsAPIObjects;
            }
        }
    }
}
