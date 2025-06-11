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
package com.autotune.analyzer.experiment;

import com.autotune.analyzer.exceptions.InvalidConversionOfRecommendationEntryException;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.data.result.NamespaceData;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.database.helper.DBHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class ExperimentInterfaceImpl implements ExperimentInterface {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentInterfaceImpl.class);

    @Override
    public boolean addExperimentToLocalStorage(Map<String, KruizeObject> mainKruizeExperimentMap, List<KruizeObject> kruizeExperimentList) {
        kruizeExperimentList.forEach(
                (kruizeObject) -> {
                    mainKruizeExperimentMap.put(
                            kruizeObject.getExperimentName(),
                            kruizeObject
                    );
                    LOGGER.debug("Added Experiment name : {} into main map.", kruizeObject.getExperimentName());
                }
        );
        return true;
    }

    @Override
    public boolean updateExperimentStatus(KruizeObject kruizeObject, AnalyzerConstants.ExperimentStatus status) {
        kruizeObject.setStatus(status);
        // TODO   update into database
        return true;
    }

    @Override
    public boolean addResultsToLocalStorage(Map<String, KruizeObject> mainKruizeExperimentMap, List<ExperimentResultData> experimentResultDataList) {
        try {
            experimentResultDataList.forEach(resultData -> {
                resultData.setStatus(AnalyzerConstants.ExperimentStatus.QUEUED);
                KruizeObject ko = mainKruizeExperimentMap.get(resultData.getExperiment_name());

                // Build a lookup map for existing K8sObjects
                Map<String, K8sObject> k8sObjectMap = ko.getKubernetes_objects().stream()
                        .collect(Collectors.toMap(K8sObject::getName, k -> k, (a, b) -> b, HashMap::new));

                for (K8sObject resultK8sObject : resultData.getKubernetes_objects()) {
                    String name = resultK8sObject.getName();
                    String type = resultK8sObject.getType();
                    String namespace = resultK8sObject.getNamespace();

                    K8sObject k8sObject = k8sObjectMap.getOrDefault(name, new K8sObject(name, type, namespace));
                    HashMap<String, ContainerData> containerDataMap =
                            k8sObject.getContainerDataMap() != null ? k8sObject.getContainerDataMap() : new HashMap<>();
                    HashMap<String, NamespaceData> namespaceDataMap =
                            k8sObject.getNamespaceDataMap() != null ? k8sObject.getNamespaceDataMap() : new HashMap<>();

                    if (resultK8sObject.getContainerDataMap() != null && !resultK8sObject.getContainerDataMap().isEmpty()) {
                        resultK8sObject.getContainerDataMap().forEach((cName, resultContainerData) -> {
                            ContainerData containerData = containerDataMap.getOrDefault(
                                    cName, new ContainerData(cName, resultContainerData.getContainer_image_name(),
                                            resultContainerData.getContainerRecommendations(),
                                            resultContainerData.getMetrics()));

                            containerData.setResults(mergeResults(
                                    containerData.getResults(),
                                    resultContainerData.getResults(),
                                    resultData.getIntervalStartTime(),
                                    resultData.getIntervalEndTime()));

                            containerDataMap.put(cName, containerData);
                        });
                        k8sObject.setContainerDataMap(containerDataMap);
                    } else if (resultK8sObject.getNamespaceDataMap() != null && !resultK8sObject.getNamespaceDataMap().isEmpty()) {
                        resultK8sObject.getNamespaceDataMap().forEach((nsName, resultNamespaceData) -> {
                            NamespaceData namespaceData = namespaceDataMap.getOrDefault(
                                    nsName, new NamespaceData(nsName,
                                            resultNamespaceData.getNamespaceRecommendations(),
                                            resultNamespaceData.getMetrics()));

                            namespaceData.setResults(mergeResults(
                                    namespaceData.getResults(),
                                    resultNamespaceData.getResults(),
                                    resultData.getIntervalStartTime(),
                                    resultData.getIntervalEndTime()));

                            namespaceDataMap.put(nsName, namespaceData);
                        });
                        k8sObject.setNamespaceDataMap(namespaceDataMap);
                    }

                    k8sObjectMap.put(name, k8sObject);
                }
                ko.setKubernetes_objects(new ArrayList<>(k8sObjectMap.values()));
                LOGGER.debug("Added Results for Experiment name : {} with TimeStamp : {} into main map.",
                        ko.getExperimentName(), resultData.getIntervalEndTime());
            });
        } catch (Exception e) {
            LOGGER.error(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.RESULTS_SAVE_FAILURE, e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Merges two maps of IntervalResults, combining existing results with new results for the specified time range.
     *
     * @param existingResults The current map of interval results, which will be updated.
     * @param newResults      The new map of interval results to merge into the existing results.
     * @param startTime       The start of the interval range (inclusive).
     * @param endTime         The end of the interval range (inclusive).
     * @return A merged map of Timestamp to IntervalResults, containing updated data in the specified time range.
     */
    private HashMap<Timestamp, IntervalResults> mergeResults(
            HashMap<Timestamp, IntervalResults> existingResults,
            Map<Timestamp, IntervalResults> newResults,
            Timestamp startTime,
            Timestamp endTime) {

        HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsHashMap = new HashMap<>();
        for (IntervalResults intervalResults : newResults.values()) {
            for (MetricResults metricResult : intervalResults.getMetricResultsMap().values()) {
                metricResultsHashMap.put(
                        AnalyzerConstants.MetricName.valueOf(metricResult.getName()), metricResult);
            }
        }

        if (existingResults == null) {
            existingResults = new HashMap<>();
        }

        IntervalResults newInterval = new IntervalResults(startTime, endTime);
        newInterval.setMetricResultsMap(metricResultsHashMap);
        existingResults.put(endTime, newInterval);

        return existingResults;
    }


    @Override
    public boolean addRecommendationsToLocalStorage(Map<String, KruizeObject> mainKruizeExperimentMap,
                                                    List<ListRecommendationsAPIObject> listRecommendationsAPIObjectList,
                                                    boolean dbPlayback) {
        if (null == mainKruizeExperimentMap)
            return false;
        if (null == listRecommendationsAPIObjectList)
            return false;
        if (mainKruizeExperimentMap.isEmpty() || listRecommendationsAPIObjectList.isEmpty())
            return false;
        if (dbPlayback) {
            for (String experimentName : mainKruizeExperimentMap.keySet()) {
                KruizeObject kruizeObject = mainKruizeExperimentMap.get(experimentName);
                List<ListRecommendationsAPIObject> experimentListRecObjs = new ArrayList<>();
                for (ListRecommendationsAPIObject apiObject : listRecommendationsAPIObjectList) {
                    if (null != apiObject.getExperimentName() && apiObject.getExperimentName().equals(experimentName))
                        experimentListRecObjs.add(apiObject);
                }
                try {
                    if (!experimentListRecObjs.isEmpty()) {
                        DBHelpers.setRecommendationsToKruizeObject(experimentListRecObjs, kruizeObject);
                    }
                } catch (InvalidConversionOfRecommendationEntryException e) {
                    e.printStackTrace();
                }
                experimentListRecObjs.clear();
            }
        } else {
            // TODO: Insert the recommendations to DB
        }
        // Returning true for now, needs to follow a specific
        return true;
    }

}
