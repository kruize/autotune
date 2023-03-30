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

import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.*;
import com.autotune.common.data.result.ContainerData;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.utils.Utils;
import com.google.gson.Gson;
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
                    LOGGER.debug("kruizeObject = {}", kruizeObject.toString());
                    kruizeObject.setStatus(AnalyzerConstants.ExperimentStatus.QUEUED);
                    kruizeObject.setExperimentId(Utils.generateID(toString()));
                    mainKruizeExperimentMap.put(
                            kruizeObject.getExperimentName(),
                            kruizeObject
                    );
                    LOGGER.debug("Added Experiment name : {} into main map.", kruizeObject.getExperimentName());
                }
        );
        LOGGER.debug("mainKruizeExperimentMap = {}", mainKruizeExperimentMap);
        return true;
    }

    @Override
    public boolean addExperimentToDB(KruizeObject kruizeObject) {
        //TODO insert in to db
        updateExperimentStatus(kruizeObject, AnalyzerConstants.ExperimentStatus.IN_PROGRESS);
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
                    Set<ExperimentResultData> results;
                    if (ko.getResultData() == null)
                        results = new HashSet<>();
                    else
                        results = ko.getResultData();
                    results.add(resultData);
                    ko.setResultData(results);
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
                            IntervalResults intervalResults = new IntervalResults(resultData.getStarttimestamp(), resultData.getEndtimestamp());
                            intervalResults.setMetricResultsMap(metricResultsHashMap);
                            resultsIntervalMap.put(resultData.getEndtimestamp(), intervalResults);

                            containerData.setResults(resultsIntervalMap);
                            containerDataMap.put(cName, containerData);
                        }
                        k8sObject.setContainerDataMap(containerDataMap);
                        k8sObjectHashMap.put(dName, k8sObject);
                    }
                    List<K8sObject> k8sObjectList = new ArrayList<>(k8sObjectHashMap.values());
                    ko.setKubernetes_objects(k8sObjectList);
                    LOGGER.debug("Added Results for Experiment name : {} with TimeStamp : {} into main map.", ko.getExperimentName(), resultData.getEndtimestamp());
                }
        );
        LOGGER.debug("{}", new Gson().toJson(experimentResultDataList));
        // TODO   Insert into database
        return true;
    }

    @Override
    public boolean addResultsToDB(KruizeObject kruizeObject, ExperimentResultData resultData) {
        // TODO   Insert into database
        resultData.setStatus(AnalyzerConstants.ExperimentStatus.IN_PROGRESS);
        return false;
    }


    @Override
    public boolean loadAllExperiments(Map<String, KruizeObject> mainKruizeExperimentMap) {
        //TOdo load all experiments from DB
        return false;
    }

}
