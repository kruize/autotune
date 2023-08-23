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
package com.autotune.analyzer.performanceProfiles.PerformanceProfileInterface;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.kruizeObject.RecommendationSettings;
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.recommendations.engine.DurationBasedRecommendationEngine;
import com.autotune.analyzer.recommendations.engine.KruizeRecommendationEngine;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Util class to validate the performance profile metrics with the experiment results metrics.
 */
public class ResourceOptimizationOpenshiftImpl extends PerfProfileImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceOptimizationOpenshiftImpl.class);
    List<KruizeRecommendationEngine> kruizeRecommendationEngineList;

    public ResourceOptimizationOpenshiftImpl() {
        this.init();
    }

    private void init() {
        // Add new engines
        kruizeRecommendationEngineList = new ArrayList<KruizeRecommendationEngine>();
        // Create Duration based engine
        DurationBasedRecommendationEngine durationBasedRecommendationEngine = new DurationBasedRecommendationEngine();
        // TODO: Create profile based engine
        AnalyzerConstants.RegisterRecommendationEngineStatus _unused_status = registerEngine(durationBasedRecommendationEngine);
        // TODO: Add profile based once recommendation algos are available
    }

    @Override
    public AnalyzerConstants.RegisterRecommendationEngineStatus registerEngine(KruizeRecommendationEngine kruizeRecommendationEngine) {
        if (null == kruizeRecommendationEngine) {
            return AnalyzerConstants.RegisterRecommendationEngineStatus.INVALID;
        }
        for (KruizeRecommendationEngine engine : getEngines()) {
            if (engine.getEngineName().equalsIgnoreCase(kruizeRecommendationEngine.getEngineName()))
                return AnalyzerConstants.RegisterRecommendationEngineStatus.ALREADY_EXISTS;
        }
        // Add engines
        getEngines().add(kruizeRecommendationEngine);
        return AnalyzerConstants.RegisterRecommendationEngineStatus.SUCCESS;
    }

    @Override
    public List<KruizeRecommendationEngine> getEngines() {
        return this.kruizeRecommendationEngineList;
    }

    @Override
    public void generateRecommendation(KruizeObject kruizeObject, List<ExperimentResultData> experimentResultDataList, Timestamp interval_start_time, Timestamp interval_end_time) throws Exception {
        /*
            To restrict the number of rows in the result set, the Load results operation involves locating the appropriate method and configuring the desired limitation.
            It's important to note that in order for the Limit rows feature to function correctly,
            the CreateExperiment API must adhere strictly to the trail settings' measurement duration and should not allow arbitrary values
        */
        String experiment_name = kruizeObject.getExperimentName();
        int limitRows = (int) ((
                KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.DurationAmount.LONG_TERM_DURATION_DAYS *
                        KruizeConstants.DateFormats.MINUTES_FOR_DAY)
                / kruizeObject.getTrial_settings().getMeasurement_durationMinutes_inDouble());

        if (null != interval_start_time) {
            long diffMilliseconds = interval_end_time.getTime() - interval_start_time.getTime();
            long minutes = diffMilliseconds / (60 * 1000);
            int addToLimitRows = (int) (minutes / kruizeObject.getTrial_settings().getMeasurement_durationMinutes_inDouble());
            LOGGER.debug("add to limit rows set to {}", addToLimitRows);
            limitRows = limitRows + addToLimitRows;
        }
        LOGGER.debug("Limit rows set to {}", limitRows);

        Map<String, KruizeObject> mainKruizeExperimentMap = new HashMap<>();
        mainKruizeExperimentMap.put(experiment_name, kruizeObject);
        new ExperimentDBService().loadResultsFromDBByName(mainKruizeExperimentMap,
                experiment_name,
                interval_end_time,
                limitRows);
        //TODO: Will be updated once algo is completed
        for (ExperimentResultData experimentResultData : experimentResultDataList) {
            if (null != kruizeObject && null != experimentResultData) {
                RecommendationSettings recommendationSettings = kruizeObject.getRecommendation_settings();

                for (K8sObject k8sObjectResultData : experimentResultData.getKubernetes_objects()) {
                    // We only support one K8sObject currently
                    K8sObject k8sObjectKruizeObject = kruizeObject.getKubernetes_objects().get(0);
                    for (String cName : k8sObjectResultData.getContainerDataMap().keySet()) {
                        ContainerData containerDataResultData = k8sObjectResultData.getContainerDataMap().get(cName);
                        if (null == containerDataResultData.getResults())
                            continue;
                        if (containerDataResultData.getResults().isEmpty())
                            continue;
                        // Get the monitoringEndTime from ResultData's ContainerData. Should have only one element
                        Timestamp monitoringEndTime = containerDataResultData.getResults().keySet().stream().max(Timestamp::compareTo).get();
                        // Get the ContainerData from the KruizeObject and not from ResultData
                        ContainerData containerDataKruizeObject = k8sObjectKruizeObject.getContainerDataMap().get(cName);
                        for (KruizeRecommendationEngine engine : getEngines()) {
                            // Check if minimum data available to generate recommendation
                            if (!engine.checkIfMinDataAvailable(containerDataKruizeObject))
                                continue;

                            // Now generate a new recommendation for the new data corresponding to the monitoringEndTime
                            HashMap<String, Recommendation> recommendationHashMap = engine.generateRecommendation(containerDataKruizeObject, monitoringEndTime, recommendationSettings);
                            if (null == recommendationHashMap || recommendationHashMap.isEmpty())
                                continue;
                            ContainerRecommendations containerRecommendations = containerDataKruizeObject.getContainerRecommendations();
                            // Just to make sure the container recommendations object is not empty
                            if (null == containerRecommendations) {
                                containerRecommendations = new ContainerRecommendations();
                            }
                            // check if notification exists
                            boolean notificationExist = false;
                            if (containerRecommendations.getNotificationMap().containsKey(RecommendationConstants.NotificationCodes.INFO_DURATION_BASED_RECOMMENDATIONS_AVAILABLE))
                                notificationExist = true;

                            // If there is no notification add one
                            if (!notificationExist) {
                                RecommendationNotification recommendationNotification = new RecommendationNotification(
                                        RecommendationConstants.RecommendationNotification.INFO_DURATION_BASED_RECOMMENDATIONS_AVAILABLE
                                );
                                containerRecommendations.getNotificationMap().put(recommendationNotification.getCode(), recommendationNotification);
                            }

                            // Get the engine recommendation map for a time stamp if it exists else create one
                            HashMap<Timestamp, HashMap<String, HashMap<String, Recommendation>>> timestampBasedRecommendationMap
                                    = containerRecommendations.getData();
                            if (null == timestampBasedRecommendationMap) {
                                timestampBasedRecommendationMap = new HashMap<Timestamp, HashMap<String, HashMap<String, Recommendation>>>();
                            }
                            // check if engines map exists else create one
                            HashMap<String, HashMap<String, Recommendation>> enginesRecommendationMap = null;
                            if (timestampBasedRecommendationMap.containsKey(monitoringEndTime)) {
                                enginesRecommendationMap = timestampBasedRecommendationMap.get(monitoringEndTime);
                            } else {
                                enginesRecommendationMap = new HashMap<String, HashMap<String, Recommendation>>();
                            }
                            // put recommendations tagging to engine
                            enginesRecommendationMap.put(engine.getEngineKey(), recommendationHashMap);
                            // put recommendations tagging to timestamp
                            timestampBasedRecommendationMap.put(monitoringEndTime, enginesRecommendationMap);
                            // set the data object to map
                            containerRecommendations.setData(timestampBasedRecommendationMap);
                            // set the container recommendations in container object
                            containerDataKruizeObject.setContainerRecommendations(containerRecommendations);
                        }

                    }
                }
            }
        }
    }


}
