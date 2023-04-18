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
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.recommendations.engine.DurationBasedRecommendationEngine;
import com.autotune.analyzer.recommendations.engine.KruizeRecommendationEngine;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.k8sObjects.K8sObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Util class to validate the performance profile metrics with the experiment results metrics.
 */
public class ResourceOptimizationOpenshiftImpl extends PerfProfileImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceOptimizationOpenshiftImpl.class);
    List<KruizeRecommendationEngine> kruizeRecommendationEngineList;

    private void init() {
        // Add new engines
        kruizeRecommendationEngineList = new ArrayList<KruizeRecommendationEngine>();
        // Create Duration based engine
        DurationBasedRecommendationEngine durationBasedRecommendationEngine =  new DurationBasedRecommendationEngine();
        // TODO: Create profile based engine
        AnalyzerConstants.RegisterRecommendationEngineStatus _unused_status = registerEngine(durationBasedRecommendationEngine);
        // TODO: Add profile based once recommendation algos are available
    }

    public ResourceOptimizationOpenshiftImpl() {
        this.init();
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
    public void recommend(KruizeObject kruizeObject) {
        //TODO: Will be updated once algo is completed
        if (null != kruizeObject) {
            for (K8sObject k8sObject : kruizeObject.getKubernetes_objects()) {
                for (String cName : k8sObject.getContainerDataMap().keySet()) {
                    ContainerData containerData = k8sObject.getContainerDataMap().get(cName);
                    if (null == containerData.getResults())
                        continue;
                    if (containerData.getResults().isEmpty())
                        continue;
                    Timestamp monitorEndTimestamp = containerData.getResults().keySet().stream().max(Timestamp::compareTo).get();
                    for (KruizeRecommendationEngine engine : getEngines()) {
                        // Check if minimum data available to generate recommendation
                        if (!engine.checKIfMinDataAvailable(containerData))
                            continue;

                        HashMap<String, Recommendation> recommendationHashMap = engine.getRecommendations(containerData, monitorEndTimestamp);
                        if (null == recommendationHashMap || recommendationHashMap.isEmpty())
                            continue;
                        ContainerRecommendations containerRecommendations = containerData.getContainerRecommendations();
                        // Just to make sure the container recommendations object is not empty
                        if (null == containerRecommendations) {
                            containerRecommendations = new ContainerRecommendations();
                        }
                        // check if notifiaction exists
                        boolean notificationExist = false;
                        for (RecommendationNotification notification : containerRecommendations.getNotifications()) {
                            if (notification.getMessage().equalsIgnoreCase(AnalyzerConstants.RecommendationNotificationMsgConstant.DURATION_BASED_AVAILABLE)) {
                                notificationExist = true;
                                break;
                            }
                        }
                        // If there is no notification add one
                        if (!notificationExist) {
                            RecommendationNotification recommendationNotification = new RecommendationNotification(
                                    AnalyzerConstants.RecommendationNotificationTypes.INFO.getName(),
                                    AnalyzerConstants.RecommendationNotificationMsgConstant.DURATION_BASED_AVAILABLE
                            );
                            containerRecommendations.getNotifications().add(recommendationNotification);
                        }

                        // Get the engine recommendation map for a time stamp if it exists else create one
                        HashMap<Timestamp, HashMap<String, HashMap<String,Recommendation>>> timestampBasedRecommendationMap
                                = containerRecommendations.getData();
                        if (null == timestampBasedRecommendationMap) {
                            timestampBasedRecommendationMap = new HashMap<Timestamp, HashMap<String, HashMap<String,Recommendation>>>();
                        }
                        // check if engines map exists else create one
                        HashMap<String, HashMap<String, Recommendation>> enginesRecommendationMap = null;
                        if (timestampBasedRecommendationMap.containsKey(monitorEndTimestamp)) {
                            enginesRecommendationMap = timestampBasedRecommendationMap.get(monitorEndTimestamp);
                        } else {
                            enginesRecommendationMap = new HashMap<String, HashMap<String, Recommendation>>();
                        }
                        // put recommendations tagging to engine
                        enginesRecommendationMap.put(engine.getEngineKey(), recommendationHashMap);
                        // put recommendations tagging to timestamp
                        timestampBasedRecommendationMap.put(monitorEndTimestamp, enginesRecommendationMap);
                        // set the data object to map
                        containerRecommendations.setData(timestampBasedRecommendationMap);
                        // set the container recommendations in container object
                        containerData.setContainerRecommendations(containerRecommendations);
                    }
                }
            }
        }
    }
}
