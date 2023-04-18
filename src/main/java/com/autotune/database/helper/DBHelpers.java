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
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.KubernetesAPIObject;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.database.table.KruizeExperimentEntry;
import com.autotune.database.table.KruizeRecommendationEntry;
import com.autotune.database.table.KruizeResultsEntry;
import com.autotune.utils.KruizeConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;

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
             * @param kruizeObject
             * @return KruizeExperimentEntry
             * This methode facilitate to store data into db by accumulating required data from KruizeObject.
             */
            public static KruizeExperimentEntry convertKruizeObjectToExperimentDBObj(KruizeObject kruizeObject) {
                KruizeExperimentEntry kruizeExperimentEntry = null;
                try {
                    kruizeExperimentEntry = new KruizeExperimentEntry();
                    kruizeExperimentEntry.setExperiment_name(kruizeObject.getExperimentName());
                    kruizeExperimentEntry.setCluster_name(kruizeObject.getClusterName());
                    kruizeExperimentEntry.setMode(kruizeObject.getMode());
                    kruizeExperimentEntry.setPerformance_profile(kruizeObject.getPerformanceProfile());
                    kruizeExperimentEntry.setVersion(kruizeObject.getApiVersion());
                    kruizeExperimentEntry.setTarget_cluster(kruizeObject.getTarget_cluster());
                    kruizeExperimentEntry.setStatus(kruizeObject.getStatus());
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS, kruizeObject.getKubernetes_objects());
                    jsonObject.put(KruizeConstants.JSONKeys.TRIAL_SETTINGS, new JSONObject(
                            new Gson().toJson(kruizeObject.getTrial_settings())));
                    jsonObject.put(KruizeConstants.JSONKeys.RECOMMENDATION_SETTINGS, new JSONObject(
                            new Gson().toJson(kruizeObject.getRecommendation_settings())));
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        kruizeExperimentEntry.setExtended_data(
                                objectMapper.readTree(
                                        jsonObject.toString()
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
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS, experimentResultData.getKubernetes_objects());
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        kruizeResultsEntry.setExtended_data(
                                objectMapper.readTree(
                                        jsonObject.toString()
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
        }
    }
}

