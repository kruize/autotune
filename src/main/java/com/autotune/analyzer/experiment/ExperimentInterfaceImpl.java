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
import com.autotune.common.k8sObjects.DeploymentObject;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.Utils;
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
                    kruizeObject.setStatus(AnalyzerConstants.ExperimentStatus.QUEUED);
                    kruizeObject.setExperimentId(Utils.generateID(toString()));
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
                    Set<ExperimentResultData> results = null;
                    if (ko.getResultData() == null)
                        results = new HashSet<>();
                    else
                        results = ko.getResultData();
                    results.add(resultData);
                    ko.setResultData(results);
                    HashMap<String, DeploymentObject> deploymentsMap = ko.getDeployments();
                    if (null == deploymentsMap) {
                        deploymentsMap = new HashMap<>();
                    }
                    List<DeploymentResultData> resultDeploymentList = resultData.getDeployments();
                    for (DeploymentResultData deploymentResultData : resultDeploymentList) {
                        String dName = deploymentResultData.getDeployment_name();
                        DeploymentObject deploymentObject;
                        HashMap<String, ContainerData> containersMap;
                        if (null == deploymentsMap.get(dName)) {
                            deploymentObject = new DeploymentObject(dName);
                            containersMap = new HashMap<>();
                        } else {
                            deploymentObject = deploymentsMap.get(dName);
                            containersMap = deploymentObject.getContainers();
                        }
                        List<ContainerData> resultContainerDataList = deploymentResultData.getContainerDataList();
                        for (ContainerData resultContainerData : resultContainerDataList) {
                            String cName = resultContainerData.getContainer_name();
                            ContainerData containerData;
                            if (null == containersMap.get(cName)) {
                                containerData = new ContainerData();
                            } else {
                                containerData = containersMap.get(cName);
                            }
                            HashMap<AnalyzerConstants.AggregatorType, MetricResults> metricResultsHashMap = new HashMap<>();
                             for (Metric metrics : resultContainerData.getMetrics()) {
                                MetricResults metricResults = metrics.getMetricResult();
                                metricResultsHashMap.put(AnalyzerConstants.AggregatorType.valueOf(metrics.getName()), metricResults);
                            }
                            HashMap<Timestamp, IntervalResults> resultsAggregatorStartEndTimeStampMap = containerData.getResults();

                            if (null == resultsAggregatorStartEndTimeStampMap) {
                                resultsAggregatorStartEndTimeStampMap = new HashMap<>();
                            }
                            IntervalResults intervalResults = new IntervalResults(resultData.getStarttimestamp(), resultData.getEndtimestamp());
                            intervalResults.setMetricResultsMap(metricResultsHashMap);
                            resultsAggregatorStartEndTimeStampMap.put(resultData.getEndtimestamp(), intervalResults);

                            containerData.setResults(resultsAggregatorStartEndTimeStampMap);
                            containersMap.put(cName, containerData);
                        }
                        deploymentObject.setContainers(containersMap);
                        deploymentsMap.put(dName, deploymentObject);
                    }
                    ko.setDeployments(deploymentsMap);
                    LOGGER.debug("Added Results for Experiment name : {} with TimeStamp : {} into main map.", ko.getExperimentName(), resultData.getEndtimestamp());
                }
        );
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
