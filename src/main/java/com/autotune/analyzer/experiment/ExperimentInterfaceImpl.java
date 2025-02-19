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
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.database.helper.DBHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

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
        experimentResultDataList.forEach(
                (resultData) -> {
                    resultData.setStatus(AnalyzerConstants.ExperimentStatus.QUEUED);
                    KruizeObject ko = mainKruizeExperimentMap.get(resultData.getExperiment_name());
                    // creating a temp map to store k8sdata
                    HashMap<String, K8sObject> k8sObjectHashMap = new HashMap<>();
                    for (K8sObject k8sObj : ko.getKubernetes_objects())
                        k8sObjectHashMap.put(k8sObj.getName(), k8sObj);

                    List<K8sObject> resultK8sObjectList = resultData.getKubernetes_objects();
                    for (K8sObject resultK8sObject : resultK8sObjectList) {
                        String dName = resultK8sObject.getName();
                        String dType = resultK8sObject.getType();
                        String dNamespace = resultK8sObject.getNamespace();
                        K8sObject k8sObject;
                        HashMap<String, ContainerData> containerDataMap;
                        if (null == k8sObjectHashMap.get(dName)) {
                            k8sObject = new K8sObject(dName, dType, dNamespace);
                            containerDataMap = new HashMap<>();
                        } else {
                            k8sObject = k8sObjectHashMap.get(dName);
                            containerDataMap = k8sObject.getContainerDataMap();
                        }
                        HashMap<String, ContainerData> resultContainerDataMap = resultK8sObject.getContainerDataMap();
                        for (ContainerData resultContainerData : resultContainerDataMap.values()) {
                            String cName = resultContainerData.getContainer_name();
                            String imgName = resultContainerData.getContainer_image_name();
                            HashMap<AnalyzerConstants.MetricName, Metric> metricsMap = resultContainerData.getMetrics();
                            ContainerData containerData;
                            if (null == containerDataMap.get(cName)) {
                                containerData = new ContainerData(cName, imgName, resultContainerData.getContainerRecommendations(), metricsMap);
                            } else {
                                containerData = containerDataMap.get(cName);
                            }
                            HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsHashMap = new HashMap<>();
                            for (IntervalResults intervalResults : resultContainerData.getResults().values()) {
                                Collection<MetricResults> metricResultsList = intervalResults.getMetricResultsMap().values();
                                for (MetricResults metricResults : metricResultsList)
                                    metricResultsHashMap.put(AnalyzerConstants.MetricName.valueOf(metricResults.getName()), metricResults);
                            }
                            HashMap<Timestamp, IntervalResults> resultsIntervalMap = containerData.getResults();

                            if (null == resultsIntervalMap) {
                                resultsIntervalMap = new HashMap<>();
                            }
                            IntervalResults intervalResults = new IntervalResults(resultData.getIntervalStartTime(), resultData.getIntervalEndTime());
                            intervalResults.setMetricResultsMap(metricResultsHashMap);
                            resultsIntervalMap.put(resultData.getIntervalEndTime(), intervalResults);

                            containerData.setResults(resultsIntervalMap);
                            containerDataMap.put(cName, containerData);
                        }
                        k8sObject.setContainerDataMap(containerDataMap);
                        k8sObjectHashMap.put(dName, k8sObject);
                    }
                    List<K8sObject> k8sObjectList = new ArrayList<>(k8sObjectHashMap.values());
                    ko.setKubernetes_objects(k8sObjectList);
                    LOGGER.debug("Added Results for Experiment name : {} with TimeStamp : {} into main map.", ko.getExperimentName(), resultData.getIntervalEndTime());
                }
        );
        return true;
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
